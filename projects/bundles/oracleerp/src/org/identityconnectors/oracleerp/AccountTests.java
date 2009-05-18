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
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.oracleerp;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.Assert;
import org.junit.Test;



/**
 * Attempts to test the {@link OracleERPConnector} with the framework.
 * 
 * @author petr
 * @version 1.0
 * @since 1.0
 */
public class AccountTests { 
    
    //Schema id
    private static String SCHEMA_PREFIX="APPS.";

    @Test
    public void testCreateUserCall() {
        final Set<Attribute> attrs = createAllAccountAttributes();
        final Map<String, SQLParam> userValues =  Account.getParamsMap(ObjectClass.ACCOUNT, attrs, null, true);

        //test sql
        Assert.assertEquals("Invalid SQL",
                        "{ call APPS.fnd_user_pkg.CreateUser ( x_user_name => ?, x_owner => upper(?), "+
                        "x_unencrypted_password => ?, x_start_date => ?, x_end_date => ?, "+
                        "x_last_logon_date => ?, x_description => ?, x_password_date => ?, "+
                        "x_password_accesses_left => ?, x_password_lifespan_accesses => ?, "+
                        "x_password_lifespan_days => ?, x_employee_id => ?, x_email_address => ?, "+
                        "x_fax => ?, x_customer_id => ?, x_supplier_id => ? ) }",
                        Account.getUserCallSQL(userValues, true, SCHEMA_PREFIX));
        //session is always null, so 16 params
        Assert.assertEquals("Invalid number of  SQL Params", 16, Account.getUserSQLParams(userValues).size());
        Assert.assertFalse("Is update needed", Account.isUpdateNeeded(userValues));
    }    
    

    @Test
    public void testUpdateUserCall() {
        final Set<Attribute> attrs = createAllAccountAttributes();
        final Map<String, SQLParam> userValues =  Account.getParamsMap(ObjectClass.ACCOUNT, attrs, null, false);

        //test sql
        Assert.assertEquals("Invalid SQL",
                        "{ call APPS.fnd_user_pkg.UpdateUser ( x_user_name => ?, x_owner => upper(?), "+
                        "x_unencrypted_password => ?, x_start_date => ?, x_end_date => ?, "+
                        "x_description => ?, x_password_date => ?, x_password_accesses_left => ?, "+
                        "x_password_lifespan_accesses => ?, x_password_lifespan_days => ?, x_employee_id => ?, "+
                        "x_email_address => ?, x_fax => ?, x_customer_id => ?, x_supplier_id => ? ) }",
                        Account.getUserCallSQL(userValues, false, SCHEMA_PREFIX));
        //session is always null, so 15 params
        Assert.assertEquals("Invalid number of  SQL Params", 15, Account.getUserSQLParams(userValues).size());
        Assert.assertFalse("Is update needed", Account.isUpdateNeeded(userValues));
    }        
    
    @Test
    public void testCreateUserCreateUserAll() {
        final Set<Attribute> attrs = createAllAccountAttributes();
        final Map<String, SQLParam> userValues =  Account.getParamsMap(ObjectClass.ACCOUNT, attrs, null, true);
        
        //Old style of creating user
        Assert.assertEquals("Invalid All SQL",
                "{ call APPS.fnd_user_pkg.CreateUser ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) }",
                Account.getAllSQL(userValues, true, SCHEMA_PREFIX));
        
        //all 17 params
        Assert.assertEquals("Invalid number of  SQL Params", 17, Account.getAllSQLParams(userValues).size());
        Assert.assertFalse("Is update needed", Account.isUpdateNeeded(userValues));
    }     
    
    
    @Test
    public void testUpdateUserCreateUserAll() {
        final Set<Attribute> attrs = createAllAccountAttributes();
        final Map<String, SQLParam> userValues =  Account.getParamsMap(ObjectClass.ACCOUNT, attrs, null, false);
        
        //Old style of creating user
        Assert.assertEquals("Invalid All SQL",
                "{ call APPS.fnd_user_pkg.UpdateUser ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) }",
                Account.getAllSQL(userValues, false, SCHEMA_PREFIX));
        
        //all 17 params
        Assert.assertEquals("Invalid number of  SQL Params", 17, Account.getAllSQLParams(userValues).size());
        Assert.assertFalse("Is update needed", Account.isUpdateNeeded(userValues));
    }     
    

    @Test
    public void testCreateUserCallNulls() {
        final Set<Attribute> attrs = createNullAccountAttributes();
        final Map<String, SQLParam> userValues =  Account.getParamsMap(ObjectClass.ACCOUNT, attrs, null, true);
        //test sql
        Assert.assertEquals("Invalid SQL",
                "{ call APPS.fnd_user_pkg.CreateUser ( x_user_name => ?, x_owner => upper(?), "+
                "x_unencrypted_password => ?, x_end_date => FND_USER_PKG.null_date, "+
                "x_last_logon_date => ?, x_description => FND_USER_PKG.null_char, "+
                "x_password_date => ?, x_password_accesses_left => FND_USER_PKG.null_number, "+
                "x_password_lifespan_accesses => FND_USER_PKG.null_number, "+
                "x_password_lifespan_days => FND_USER_PKG.null_number, x_employee_id => FND_USER_PKG.null_number, "+
                "x_email_address => FND_USER_PKG.null_char, x_fax => FND_USER_PKG.null_char, "+
                "x_customer_id => FND_USER_PKG.null_number, x_supplier_id => FND_USER_PKG.null_number ) }",
                Account.getUserCallSQL(userValues, true, SCHEMA_PREFIX));
        //session is always null, so 16 params
        Assert.assertEquals("Invalid number of  SQL Params", 5, Account.getUserSQLParams(userValues).size());    
    }
    

    @Test
    public void testUpdateUserCallNulls() {
        final Set<Attribute> attrs = createNullAccountAttributes();
        final Map<String, SQLParam> userValues =  Account.getParamsMap(ObjectClass.ACCOUNT, attrs, null, false);
        //test sql
        Assert.assertEquals("Invalid SQL",
                "{ call APPS.fnd_user_pkg.UpdateUser ( x_user_name => ?, x_owner => upper(?), "+
                "x_unencrypted_password => ?, x_end_date => FND_USER_PKG.null_date, "+
                "x_description => FND_USER_PKG.null_char, "+
                "x_password_date => ?, x_password_accesses_left => FND_USER_PKG.null_number, "+
                "x_password_lifespan_accesses => FND_USER_PKG.null_number, "+
                "x_password_lifespan_days => FND_USER_PKG.null_number, x_employee_id => FND_USER_PKG.null_number, "+
                "x_email_address => FND_USER_PKG.null_char, x_fax => FND_USER_PKG.null_char, "+
                "x_customer_id => FND_USER_PKG.null_number, x_supplier_id => FND_USER_PKG.null_number ) }",
                Account.getUserCallSQL(userValues, false, SCHEMA_PREFIX));
        //session is always null, so 16 params
        Assert.assertEquals("Invalid number of  SQL Params", 4, Account.getUserSQLParams(userValues).size());    
    }    

    @Test
    public void testCreateUserCallAllNulls() {
        final Set<Attribute> attrs = createNullAccountAttributes();
        final Map<String, SQLParam> userValues =  Account.getParamsMap(ObjectClass.ACCOUNT, attrs, null, true);

        //Test Old style of creating user
        Assert.assertEquals("Invalid All SQL",
                "{ call APPS.fnd_user_pkg.CreateUser ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) }",
                Account.getAllSQL(userValues, true, SCHEMA_PREFIX));       
        //all 17 params
        Assert.assertEquals("Invalid number of  SQL Params", 17, Account.getAllSQLParams(userValues).size());
        Assert.assertTrue("Is update needed", Account.isUpdateNeeded(userValues)); 
    }
    @Test
    public void tesUpdateUserCallAllNulls() {
        final Set<Attribute> attrs = createNullAccountAttributes();
        final Map<String, SQLParam> userValues =  Account.getParamsMap(ObjectClass.ACCOUNT, attrs, null, true);

        Assert.assertTrue("Is update needed", Account.isUpdateNeeded(userValues));

        Assert.assertEquals("Invalid All SQL",
                "{ call APPS.fnd_user_pkg.UpdateUser ( x_user_name => ?, x_owner => upper(?), "+
                "x_end_date => FND_USER_PKG.null_date, "+
                "x_description => FND_USER_PKG.null_char, x_password_accesses_left => FND_USER_PKG.null_number, "+
                "x_password_lifespan_accesses => FND_USER_PKG.null_number, "+
                "x_password_lifespan_days => FND_USER_PKG.null_number, x_employee_id => FND_USER_PKG.null_number, "+
                "x_email_address => FND_USER_PKG.null_char, x_fax => FND_USER_PKG.null_char, "+
                "x_customer_id => FND_USER_PKG.null_number, x_supplier_id => FND_USER_PKG.null_number ) }",
                Account.getUserUpdateNullsSQL(userValues, SCHEMA_PREFIX));
        
        //the required user name and owner
        Assert.assertEquals("Invalid number of update SQL Params", 2, Account.getUserUpdateNullsParams(userValues).size());        
    }    
    
    
    /**
     * 
     * @return the All account attributes
     */
    static public Set<Attribute> createAllAccountAttributes() {
        final Set<Attribute> attrs = createRequiredAccountAttributes();    
        attrs.add(AttributeBuilder.buildPasswordExpired(false));                
        attrs.add(AttributeBuilder.build(Account.START_DATE, (new Timestamp(System.currentTimeMillis()-10*24*3600000)).toString()));
        attrs.add(AttributeBuilder.build(Account.END_DATE, (new Timestamp(System.currentTimeMillis()+10*24*3600000)).toString()));        
        attrs.add(AttributeBuilder.build(Account.RESP, "Cash Forecasting||Oracle Cash Management||Standard||Test Description||2004-04-12 00:00:00.0||null"));
        attrs.add(AttributeBuilder.build(Account.PWD_ACCESSES_LEFT, 56));
        attrs.add(AttributeBuilder.build(Account.PWD_LIFE_ACCESSES, 5));
        attrs.add(AttributeBuilder.build(Account.PWD_LIFE_DAYS, 5));
        attrs.add(AttributeBuilder.build(Account.EMP_ID, 5));
        attrs.add(AttributeBuilder.build(Account.DESCR, "Test Description"));        
        attrs.add(AttributeBuilder.build(Account.EMAIL, "person@somewhere.com"));
        attrs.add(AttributeBuilder.build(Account.FAX, "555-555-5555"));
        attrs.add(AttributeBuilder.build(Account.CUST_ID, 11223344));
        attrs.add(AttributeBuilder.build(Account.SUPP_ID, 102));
        attrs.add(AttributeBuilder.build(Account.SEC_ATTRS, "TO_PERSON_ID||Oracle Self-Service Web Applications||110"));
        return attrs;
    }
    
    /**
     * @return the All attributes null
     */
    static public Set<Attribute> createNullAccountAttributes() {
        Set<Attribute> attrs = createRequiredAccountAttributes();
        attrs.add(AttributeBuilder.buildPasswordExpired(false));        
        attrs.add(AttributeBuilder.build(Account.START_DATE, (String) null));
        attrs.add(AttributeBuilder.build(Account.END_DATE, (String) null));        
        attrs.add(AttributeBuilder.build(Account.RESP, (String) null));
        attrs.add(AttributeBuilder.build(Account.PWD_ACCESSES_LEFT, (String) null));
        attrs.add(AttributeBuilder.build(Account.PWD_LIFE_ACCESSES, (String) null));
        attrs.add(AttributeBuilder.build(Account.PWD_LIFE_DAYS, (String) null));
        attrs.add(AttributeBuilder.build(Account.EMP_ID, (String) null));
        attrs.add(AttributeBuilder.build(Account.DESCR, (String) null));        
        attrs.add(AttributeBuilder.build(Account.EMAIL, (String) null));
        attrs.add(AttributeBuilder.build(Account.FAX, (String) null));
        attrs.add(AttributeBuilder.build(Account.CUST_ID, (String) null));
        attrs.add(AttributeBuilder.build(Account.SUPP_ID, (String) null));
        attrs.add(AttributeBuilder.build(Account.SEC_ATTRS, (String) null));
        return attrs;
    }

    
    /**
     * @return the 
     */
    static public Set<Attribute> createRequiredAccountAttributes() {
        final Set<Attribute> attrs = CollectionUtil.newSet();       
        attrs.add(AttributeBuilder.build(Name.NAME, "TSTUSER"));
        attrs.add(AttributeBuilder.buildPassword("tstpwd".toCharArray()));
        attrs.add(AttributeBuilder.build(Account.OWNER, "CUST"));
        return attrs;
    } 
}