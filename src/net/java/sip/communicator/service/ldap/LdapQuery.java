/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap;

/**
 * Represents an LDAP search query. A query needs
 * to be created in order to use searchPerson()
 * in LdapDirectorySettings or LdapDirectory
 *
 * @author Sebastien Mazy
 */
public interface LdapQuery
{
    /**
     * State of LDAP query.
     */
    public static enum State
    {
        /**
         * Pending state.
         */
        PENDING,
        /**
         * Completed state.
         */
        COMPLETED,

        /**
         * Results left.
         */
        RESULTS_LEFT,

        /**
         * Cancelled state.
         */
        CANCELLED;
    }

    /**
     * Sets the query state to newState
     *
     * @param newState the query state
     */
    public void setState(State newState);

    /**
     * Returns the query state
     *
     * @return the query state
     */
    public State getState();

    /**
     * Returns the query string
     * e.g "John Doe"
     *
     * @return the query string
     */
    public String toString();
}
