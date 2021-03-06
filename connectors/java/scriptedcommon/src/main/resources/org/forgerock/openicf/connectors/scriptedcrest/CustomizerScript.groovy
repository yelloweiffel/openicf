/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openicf.connectors.scriptedcrest

import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.AuthenticationException
import org.apache.http.auth.InvalidCredentialsException
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.conn.ssl.SSLContexts
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy
import org.apache.http.nio.reactor.ConnectingIOReactor
import org.forgerock.json.fluent.JsonValue
import org.forgerock.json.resource.Context
import org.forgerock.json.resource.QueryResult
import org.forgerock.json.resource.Resource
import org.forgerock.openicf.connectors.scriptedcrest.ScriptedCRESTConfiguration.AuthMethod
import org.identityconnectors.common.security.GuardedString

import javax.net.ssl.SSLContext
import java.security.KeyStore

/**
 * A customizer script defines the custom closures to interact with the default implementation and customize it.
 *
 * @author Laszlo Hordos
 */
customize {
    init { HttpAsyncClientBuilder builder ->

        //SEE: http://hc.apache.org/httpcomponents-asyncclient-4.0.x/httpasyncclient/examples/org/apache/http/examples/nio/client/AsyncClientConfiguration.java
        def c = delegate as ScriptedCRESTConfiguration

        def httpHost = new HttpHost(c.serviceAddress?.host, c.serviceAddress?.port, c.serviceAddress?.scheme);

        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
        PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(ioReactor);
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);
        // Increase max connections for httpHost to 50
        cm.setMaxPerRoute(new HttpRoute(httpHost), 50);

        builder.setConnectionManager(cm)


        // configure timeout on the entire client
        RequestConfig requestConfig = RequestConfig.custom()/*
                                                             * .
                                                             * setConnectionRequestTimeout
                                                             * ( 50).
                                                             * setConnectTimeout
                                                             * (50)
                                                             * .setSocketTimeout
                                                             * (50)
                                                             */.build();
        builder.setDefaultRequestConfig(requestConfig)

        if (false) {

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            FileInputStream instream = new FileInputStream(new File("my.keystore"));
            try {
                trustStore.load(instream, "nopassword".toCharArray());
            } finally {
                instream.close();
            }
            // Trust own CA and all self-signed certs
            SSLContext sslcontext = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, new TrustSelfSignedStrategy())
                    .build();
            // Allow TLSv1 protocol only
            SSLIOSessionStrategy sslSessionStrategy = new SSLIOSessionStrategy(
                    sslcontext,
                    ["TLSv1"] as String[],
                    null,
                    SSLIOSessionStrategy.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        }

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        //PROXY
        if (c.proxyAddress != null) {
            HttpHost proxy = new HttpHost(c.proxyAddress.host, c.proxyAddress.port, c.proxyAddress.scheme);
            RequestConfig config = RequestConfig.custom().setProxy(proxy)

            // configure timeout on the entire client
            /*
                                                 * .
                                                 * setConnectionRequestTimeout
                                                 * ( 50).
                                                 * setConnectTimeout
                                                 * (50)
                                                 * .setSocketTimeout
                                                 * (50)

            */
                    .build();

            builder.setDefaultRequestConfig(config)

            if (false) {
                credentialsProvider.setCredentials(
                        new AuthScope(proxy.getHostName(), proxy.getPort()),
                        new UsernamePasswordCredentials("proxy_user", "proxy_password"));
            }           
        }

        // Authentication
        switch (AuthMethod.valueOf(c.defaultAuthMethod)) {
            case AuthMethod.BASIC_PREEMPTIVE:

                // Create AuthCache instance
                def authCache = new BasicAuthCache();
                // Generate BASIC scheme object and add it to the local auth cache
                authCache.put(httpHost, new BasicScheme());
                c.propertyBag.put(HttpClientContext.AUTH_CACHE, authCache)

            case AuthMethod.BASIC:
                // It's part of the http client spec to request the resource anonymously
                // first and respond to the 401 with the Authorization header.
                
                c.password.access(
                        {
                            credentialsProvider.setCredentials(new AuthScope(httpHost.getHostName(), httpHost.getPort()),
                                    new UsernamePasswordCredentials(c.username, new String(it)));
                        } as GuardedString.Accessor
                );

                
                builder.setDefaultCredentialsProvider(credentialsProvider);
                break;
            case AuthMethod.NONE:
                break;
            default:
                throw new IllegalArgumentException();
        }

        c.propertyBag.put(HttpClientContext.COOKIE_STORE, new BasicCookieStore());

    }

    release {
        propertyBag.clear()
    }

    beforeRequest { Context context, HttpClientContext clientContext, HttpUriRequest request ->
        //SecurityContext sec = context.asContext(SecurityContext)
        clientContext.setCookieStore(propertyBag.get(HttpClientContext.COOKIE_STORE))
        clientContext.setAuthCache(propertyBag.get(HttpClientContext.AUTH_CACHE))
    }

    onFail { Context context, HttpClientContext clientContext, HttpUriRequest request, Exception ex ->
        if (true) {
            completed(new HashMap<String, Object>())
        } else {
            if (ex instanceof InvalidCredentialsException) {
                failed(ex)
            } else if (ex instanceof AuthenticationException) {
                //Authenticate again
                failed(ex)
                //execute(context, request, delegate)
            } else {
                failed(ex)
            }
        }
    }

    onComplete { Object result ->
        if (result instanceof JsonValue) {

        } else if (result instanceof Resource) {

        } else if (result instanceof QueryResult) {

        }
        completed(result)
    }

}