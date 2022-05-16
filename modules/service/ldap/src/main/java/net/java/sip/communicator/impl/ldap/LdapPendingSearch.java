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
package net.java.sip.communicator.impl.ldap;

import java.util.*;

import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.ldap.event.*;

/**
 * Stores a pending search
 *
 * @author Sebastien Mazy
 */
public class LdapPendingSearch
{
    /**
     * the LdapDirectory-s which are still being searched
     * for this search
     */
    private final List<LdapDirectory> pendingServers =
        new ArrayList<LdapDirectory>();

    /**
     * the caller for this search,
     * e.g. an LdapSearchDialog instance
     */
    private final LdapListener caller;

    /**
     * Simple constructor
     *
     * @param pendingServers pending LDAP servers
     * @param caller callback
     */
    public LdapPendingSearch(Collection<LdapDirectory> pendingServers,
            LdapListener caller)
    {
        this.pendingServers.addAll(pendingServers);
        this.caller = caller;
    }

    /**
     * Returns the LdapDirectory-s not finished searching
     *
     * @return the LdapDirectory-s not finished searching
     */
    public List<LdapDirectory> getPendingServers()
    {
        return this.pendingServers;
    }

    /**
     * Returns the initiator of the search
     *
     * @return the initiator of the search
     */
    public LdapListener getCaller()
    {
        return this.caller;
    }
}
