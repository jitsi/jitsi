/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.ldap;

import net.java.sip.communicator.service.contactsource.*;

/**
 * The Ldap Service allows other modules to query an LDAP server
 *
 * @author Sebastien Mazy
 */
public interface LdapService
    extends LdapConstants
{
    /**
     * Returns all the LDAP directories
     *
     * @return the LdapDirectorySet containing all the LdapDirectory(s)
     * registered
     */
    public LdapDirectorySet getServerSet();

    /**
     * Returns the LdapFactory, used to
     * create LdapDirectoryS, LdapDirectorySettings, LdapQuery, ...
     *
     * @return the LdapFactory, used to
     */
    public LdapFactory getFactory();

    /**
     * Creates a contact source corresponding to the given ldap directory.
     *
     * @param ldapDir the ldap directory, for which we're creating the contact
     * source
     * @return the created contact source service
     */
    public ContactSourceService createContactSource(LdapDirectory ldapDir);

    /**
     * Removes the contact source corresponding to the given ldap directory.
     *
     * @param ldapDir the ldap directory, which contact source we'd like to
     * remove
     */
    public void removeContactSource(LdapDirectory ldapDir);
}
