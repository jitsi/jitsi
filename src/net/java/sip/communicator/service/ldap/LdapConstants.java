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
        /**
         * No authentication.
         */
        NONE,

        /**
         * Authentication with login and password.
         */
        SIMPLE;

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
        /**
         * Subtree search.
         */
        SUB(SearchControls.SUBTREE_SCOPE),

        /**
         * One level search.
         */
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
