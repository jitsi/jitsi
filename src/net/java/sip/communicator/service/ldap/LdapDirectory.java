/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap;

import java.util.*;

import net.java.sip.communicator.service.ldap.event.*;

/**
 * LdapDirectory is the "core" object of the service, which
 * should be used to perform LDAP queries.
 * It is comparable in order to display LdapDirectory(s)
 * in alphabetic order in the UI.
 *
 * @author Sebastien Mazy
 */
public interface LdapDirectory
    extends LdapConstants,
            LdapEventManager,
            Comparable<LdapDirectory>
{
    /**
     * Returns the state of the enabled marker
     *
     * @return the state of the enabled marker
     */
    public boolean isEnabled();

    /**
     * Sets the state of the enabled marker
     *
     * @param enabled whether the server is marked as enabled
     */
    public void setEnabled(boolean enabled);

    /**
     * Returns an LdapDirectorySettings object containing
     * a copy of the settings of this server
     *
     * @return a copy of this server settings
     *
     * @see LdapDirectorySettings
     */
    public LdapDirectorySettings getSettings();

    /**
     * Searches a person in the directory, based on a search string.
     * Since that method might take time to process, it should be
     * implemented asynchronously and send the results (LdapPersonFound)
     * with an LdapEvent to its listeners
     *
     * @param query assumed name (can be partial) of the person searched
     * e.g. "john", "doe", "john doe"
     * @param caller the LdapListener which called the method and will
     * receive results.
     * @param searchSettings custom settings for this search, null if you
     * want to stick with the defaults
     *
     * @see LdapDirectory#searchPerson
     * @see LdapPersonFound
     * @see LdapEvent
     */
    public void searchPerson(final LdapQuery query, final LdapListener caller,
            LdapSearchSettings searchSettings);

    /**
     * search the children nodes of the given dn
     *
     * @param dn the distinguished name of the node to search for children
     * @return list of childrens
     * @see net.java.sip.communicator.service.ldap.LdapDirectory#searchChildren
     */
    public Collection<String> searchChildren(final String dn);

    /**
     * Adds listener to our list of listeners
     *
     * @param listener listener to be added
     */
    public void addLdapListener(LdapListener listener);
}
