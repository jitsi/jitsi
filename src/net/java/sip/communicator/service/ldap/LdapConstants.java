/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.ldap;

import javax.naming.directory.*;

/**
 * Constants used by the LDAP service.
 *
 * @author Sebastien Mazy
 */
public interface LdapConstants
{
    /**
     * security methods used to connect to the remote host
     * and their default port
     */
    public static enum Encryption
    {
        /**
         * No encryption.
         */
        CLEAR(389, "ldap://"),

        /**
         * SSL encryption.
         */
        SSL(636, "ldaps://");

        private final int defaultPort;
        private final String protocolString;

        Encryption(int defaultPort, String protocolString)
        {
            this.defaultPort = defaultPort;
            this.protocolString = protocolString;
        }

        /**
         * Returns the default port for this security method.
         *
         * @return the default port for this security method.
         */
        public int defaultPort()
        {
            return this.defaultPort;
        }

        /**
         * Returns the protocol string for this security method.
         *
         * @return the protocol string
         */
        public String protocolString()
        {
            return this.protocolString;
        }

        /**
         * Returns default value for encryption.
         *
         * @return default value for encryption
         */
        public static Encryption defaultValue()
        {
            return CLEAR;
        }
    }

    /**
     * Authentication methods.
     */
    public static enum Auth
    {
        NONE, SIMPLE;

        /**
         * Returns default value for authentication.
         *
         * @return default value for authentication
         */
        public static Auth defaultValue()
        {
            return NONE;
        }
    }

    /**
     * search scope in the directory: one level, subtree
     */
    public static enum Scope
    {
        SUB(SearchControls.SUBTREE_SCOPE),
        ONE(SearchControls.ONELEVEL_SCOPE);

        private int constant;

        Scope(int constant)
        {
            this.constant = constant;
        }

        /**
         * Returns default value for scope.
         *
         * @return default value for scope
         */
        public static Scope defaultValue()
        {
            return SUB;
        }

        /**
         * Returns the matching constant field from SearchControls
         *
         * @return the matching constant field
         */
        public int getConstant()
        {
            return this.constant;
        }
    }

    /**
     * How long should we wait for the connection to establish?
     * (in ms)
     */
    public static final String LDAP_CONNECT_TIMEOUT = "5000";

    /**
     * How long should we wait for a LDAP response?
     * (in ms)
     */
    public static final String LDAP_READ_TIMEOUT = "60000";
}
