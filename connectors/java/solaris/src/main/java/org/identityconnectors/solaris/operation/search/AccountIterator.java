/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 *
 * Portions Copyrighted 2012 Evolveum, Radovan Semancik
 */

package org.identityconnectors.solaris.operation.search;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.solaris.SolarisConnection;
import org.identityconnectors.solaris.attr.NativeAttribute;

/**
 * Iterators through Solaris accounts (both native and NIS).
 *
 * @author David Adam
 * @author Radovan Semancik
 */
public class AccountIterator implements Iterator<SolarisEntry> {

    /**
     * Implementational note: in case of NIS this is iterator through entry
     * lines, in case of native accounts it iterates over username list.
     *
     * By entry line we mean the line that resembles ":" separated list, like in
     * /etc/passwd.
     */
    private Iterator<String> it;
    private SolarisConnection conn;

    private SolarisEntry nextEntry;
    private Set<NativeAttribute> attrsToGet;

    AccountIterator(Set<NativeAttribute> attrsToGet, SolarisConnection conn) {
        this(Collections.<String> emptyList(), attrsToGet, conn);
    }

    AccountIterator(List<String> usernames, Set<NativeAttribute> attrsToGet, SolarisConnection conn) {
        this.conn = conn;

        if (CollectionUtil.isEmpty(usernames)) {
            usernames = fillUsernames();
        }
        it = usernames.iterator();

        this.attrsToGet = attrsToGet;
    }

    private List<String> fillUsernames() {
        String command =
                (!conn.isNis()) ? conn.buildCommand(false,
                        "cut -d: -f1 /etc/passwd | grep -v \"^[+-]\"")
                        : "ypcat passwd | cut -d: -f1";
        String newLineSeparatedUsernames = conn.executeCommand(command);
        String[] entries = newLineSeparatedUsernames.split("\n");
        List<String> result = CollectionUtil.<String> newList();
        for (String username : entries) {
            if (StringUtil.isNotBlank(username)) {
                result.add(username.trim());
            }
        }
        return result;
    }

    public boolean hasNext() {
        while ((nextEntry == null) && it.hasNext()) {
            nextEntry = conn.getModeDriver().buildAccountEntry(it.next(), attrsToGet);
        }
        return nextEntry != null;
    }

    /**
     * @return the next user as {@link SolarisEntry} or null instead if the user
     *         does not exist on the resource.
     */
    public SolarisEntry next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        SolarisEntry result = nextEntry;
        nextEntry = null;
        return result;
    }

//    /**
//     * constructs and returns the basic user based on the output of 'ypmatch userid passwd'
//     * @param name
//     * @return the initialized entry if entry found, otherwise null
//     */
//    private SolarisEntry buildNISUser(String username) {
//        String command = new StringBuilder("ypmatch \"").append(username).append("\" passwd").toString();
//        String usernameEntry = conn.executeCommand(command);
//        // The output from is colon delimited and looks like this:
//        // name:x(passwd in shadow file):uid:pgrp-num:comment:homedir:shell
//        List<String> attributes = Arrays.asList(usernameEntry.split(":", -1));
//        if (CollectionUtil.isEmpty(attributes) || attributes.size() < 2) {
//            return null;
//        }
//        Iterator<String> attrIt = attributes.iterator();
//
//        String accountId = attrIt.next();
//        if (!accountId.equals(username)) {
//            log.warn("The fetched username differs from what was expected: fetched = '" +  accountId + "', expected = '" + username + "'.");
//            return null;
//        }
//        SolarisEntry.Builder entryBuilder = new SolarisEntry.Builder(username).addAttr(NativeAttribute.NAME, username);
//
//        //This gets the password field. We don't use it.
//        attrIt.next(); // skip password field
//
//        if (isLogins) {
//            int userUid = Integer.valueOf(attrIt.next());
//            entryBuilder.addAttr(NativeAttribute.ID, userUid);
//            String group = attrIt.next();
//            entryBuilder.addAttr(NativeAttribute.GROUP_PRIM, group);
//            String comment = attrIt.next();
//            entryBuilder.addAttr(NativeAttribute.COMMENT, comment);
//            String dir = attrIt.next();
//            entryBuilder.addAttr(NativeAttribute.DIR, dir);
//            String shell = attrIt.next();
//            entryBuilder.addAttr(NativeAttribute.SHELL, shell);
//        }
//
//        if (isLast || isRoles || isAuths) {
//            log.warn("Last, Roles, Auths attributes are not supported for NIS accounts. Skipping them.");
//        }
//
//        return entryBuilder.build();
//    }

    public void remove() {
        throw new UnsupportedOperationException(
                "Internal error: AccountIterators do not allow remove().");
    }

}
