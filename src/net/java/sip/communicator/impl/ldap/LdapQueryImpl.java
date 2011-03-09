/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import net.java.sip.communicator.service.ldap.*;

/**
 * Represents an LDAP search query
 *
 * @author Sebastien Mazy
 */
public class LdapQueryImpl
    implements LdapQuery
{
    /**
     * the query
     * e.g. "John Doe"
     */
    private final String query;

    /**
     * the current state
     */
    private volatile State state;

    /**
     * A simple constructor
     *
     * @param query the query
     */
    public LdapQueryImpl(String query)
    {
        if(query == null)
            throw new RuntimeException("query is null!");
        this.query = query;
        this.state = State.PENDING;
    }

    /**
     * Sets the query state to newState
     *
     * @param newState the query state
     */
    public void setState(State newState)
    {
        this.state = newState;
    }

    /**
     * Returns the query state
     *
     * @return the query state
     */
    public State getState()
    {
        return this.state;
    }

    /**
     * Required by LdapQuery interface
     *
     * Returns the query string
     * e.g "John Doe"
     *
     * @return the query string
     *
     * @see net.java.sip.communicator.service.ldap.LdapQuery#toString
     */
    public String toString()
    {
        return this.query;
    }
}
