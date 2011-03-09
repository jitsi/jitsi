/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
