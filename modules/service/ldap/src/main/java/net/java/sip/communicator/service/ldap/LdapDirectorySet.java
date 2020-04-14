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

import java.util.*;

import net.java.sip.communicator.service.ldap.event.*;

/**
 * The LdapDirectorySet is a simple data structure
 * linking server names (ie displayed name)
 * to LdapDirectory objects. It store all the
 * servers registered in the configuration and
 * provides methods that address them all.
 *
 * @author Sebastien Mazy
 */
public interface LdapDirectorySet
    extends Iterable<LdapDirectory>,
            LdapEventManager,
            LdapListener
{
    /**
     * @param name server name
     * @return the LdapDirectory with name name or null
     * if it isn't present in the LdapDirectorySet
     */
    public LdapDirectory getServerWithName(String name);

    /**
     * Tries to remove an LdapDirectory from the LdapDirectorySet
     * using the name given by the getName method.
     *
     * @param name name of the LdapDirectory to remove
     *
     * @return LdapDirectory removed LdapDirectory or null if failed
     */
    public LdapDirectory removeServerWithName(String name);

    /**
     * Tries to add an LdapDirectory to the LdapDirectorySet
     *
     * @param server the server to be added
     *
     * @return whether it succeeded
     */
    public boolean addServer(LdapDirectory server);

    /**
     * @param name of the server to check presence in the LdapDirectorySet
     *
     * @return whether the server is in the LdapDirectorySet
     */
    public boolean containsServerWithName(String name);

    /**
     * @return the number of LdapDirectory(s) in the LdapDirectorySet
     */
    public int size();

    /**
     * Returns a set of the marked enabled
     * LdapDirectory(s) alphabetically sorted
     *
     * @return a set of the enabled LdapDirectory(s)
     */
    public SortedSet<LdapDirectory> getEnabledServers();

    /**
     * Returns a set of the marked disabled
     * LdapDirectory(s) alphabetically sorted
     *
     * @return a set of the disabled LdapDirectory(s)
     */
    public SortedSet<LdapDirectory> getDisabledServers();

    /**
     * Performs a search on every LdapDirectory provided
     *
     * @param servers a set of LdapDirectory to search for the person
     * @param query the query to perform
     * @param caller the LdapListener that will receive the results
     * @param searchSettings the custom settings for this search
     */
    public void searchPerson(
            Set<LdapDirectory> servers,
            LdapQuery query,
            LdapListener caller,
            LdapSearchSettings searchSettings
            );

    /**
     * Performs a search on every enabled LdapDirectory of this set.
     *
     * @param query the query to perform
     * @param caller the LdapListener that will receive the results
     * @param searchSettings the custom settings for this search
     */
    public void searchPerson(
            LdapQuery query,
            LdapListener caller,
            LdapSearchSettings searchSettings
            );
}
