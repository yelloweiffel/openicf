/**
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
* " Portions Copyrighted [year] [name of copyright owner]"
*
* @author Gael Allioux <gael.allioux@forgerock.com>
*/
import groovy.sql.Sql;
import groovy.sql.DataSet;

// Parameters:
// The connector sends us the following:
// connection : SQL connection
// configuration : handler to the connector's configuration object
//
// action: String correponding to the action (UPDATE/ADD_ATTRIBUTE_VALUES/REMOVE_ATTRIBUTE_VALUES)
//   - UPDATE : For each input attribute, replace all of the current values of that attribute
//     in the target object with the values of that attribute.
//   - ADD_ATTRIBUTE_VALUES: For each attribute that the input set contains, add to the current values
//     of that attribute in the target object all of the values of that attribute in the input set.
//   - REMOVE_ATTRIBUTE_VALUES: For each attribute that the input set contains, remove from the current values
//     of that attribute in the target object any value that matches one of the values of the attribute from the input set.
// 
// log: a handler to the Log facility
//
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
//
// uid: a String representing the entry uid (__UID__)
//
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
//
// password: password string, clear text (only for UPDATE)
//
// options: a handler to the OperationOptions Map
//
// RETURNS: the uid (__UID__)

log.info("Entering "+action+" Script");
def sql = new Sql(connection);

// Prepare the Update statement
def updateStmt = "UPDATE Users SET ";
attributes.each() { key, value -> first = value[0]; updateStmt += "${key}='${first}', " };
updateStmt = updateStmt.substring(0,updateStmt.size()-2);

switch ( objectClass ) {
    case "__ACCOUNT__":
        sql.executeUpdate(updateStmt + " WHERE uid = '$uid'");
        break;
        
    case "__GROUP__":
    case "organization":
        sql.executeUpdate(updateStmt + " WHERE name = '$uid'");
        break;

    default:
        uid
}
return uid
