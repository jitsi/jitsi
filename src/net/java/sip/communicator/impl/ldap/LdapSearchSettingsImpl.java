/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import net.java.sip.communicator.service.ldap.*;

/**
 * LDAP Settings.
 *
 * @author Sebastien Mazy
 */
public class LdapSearchSettingsImpl
    implements LdapSearchSettings
{
    private boolean isMaxResultsSet = false;
    private int maxResults;
    private boolean isScopeSet = false;
    private Scope scope;
    private boolean isDelaySet = false;
    private int delay;

    public boolean isMaxResultsSet()
    {
        return isMaxResultsSet;
    }

    /**
     * Sets the maximum number of results to fetch from the
     * directory when performing the search query.
     *
     * @param maxResults the maximum number of results
     */
    public void setMaxResults(int maxResults)
    {
        if(maxResults < 1)
            throw new IllegalArgumentException(
                    "number of max results should be > 0");
        this.maxResults = maxResults;
        this.isMaxResultsSet = true;
    }

    /**
     * Returns the maximum number of results to fetch from the
     * directory when performing the search query.
     *
     * @return the maximum number of results
     */
    public int getMaxResults()
    {
        return this.maxResults;
    }

    public boolean isScopeSet()
    {
        return isScopeSet;
    }

    public void setScope(Scope scope)
    {
        this.scope = scope;
        this.isScopeSet = true;
    }

    public Scope getScope()
    {
        return this.scope;
    }

    public boolean isDelaySet()
    {
        return this.isDelaySet;
    }

    public void setDelay(int delay)
    {
        this.delay = delay;
        this.isDelaySet = true;
    }

    public int getDelay()
    {
        return this.delay;
    }
}
