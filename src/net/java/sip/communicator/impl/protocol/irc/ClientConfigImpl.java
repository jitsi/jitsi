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
package net.java.sip.communicator.impl.protocol.irc;

import java.net.*;

/**
 * Basic implementation of ClientConfig.
 * 
 * <p>
 * The implementation of ClientConfig enables advanced options by default.
 * Options can be disabled at will.
 * </p>
 *
 * @author Danny van Heumen
 */
public class ClientConfigImpl
    implements ClientConfig
{
    /**
     * Allow IRC version 3 capabilities.
     */
    private boolean version3Allowed = true;

    /**
     * Contact presence periodic task enable flag.
     */
    private boolean contactPresenceTaskEnabled = true;

    /**
     * Channel presence periodic task enable flag.
     */
    private boolean channelPresenceTaskEnabled = true;

    /**
     * The proxy configuration.
     */
    private Proxy proxy = null;

    /**
     * Resolve addresses through proxy.
     */
    private boolean resolveByProxy = true;

    /**
     * Get SASL authentication data.
     */
    private SASLImpl sasl = null;

    /**
     * Get version 3 allowed flag.
     *
     * @return returns <tt>true</tt> if allowed, or <tt>false</tt> if not.
     */
    @Override
    public boolean isVersion3Allowed()
    {
        return this.version3Allowed;
    }

    /**
     * Set version 3 allowed.
     *
     * @param allowed version 3 allowed
     */
    public void setVersion3Allowed(final boolean allowed)
    {
        this.version3Allowed = allowed;
    }

    /**
     * Get current value of contact presence enable flag.
     *
     * @return returns <tt>true</tt> if enabled.
     */
    @Override
    public boolean isContactPresenceTaskEnabled()
    {
        return this.contactPresenceTaskEnabled;
    }

    /**
     * Set new value for contact presence enable flag.
     *
     * @param value new value for flag
     */
    public void setContactPresenceTaskEnabled(final boolean value)
    {
        this.contactPresenceTaskEnabled = value;
    }

    /**
     * Get current value of channel presence enable flag.
     *
     * @return returns <tt>true</tt> if enabled.
     */
    @Override
    public boolean isChannelPresenceTaskEnabled()
    {
        return this.channelPresenceTaskEnabled;
    }

    /**
     * Set new value for channel presence enable flag.
     *
     * @param value new value for flag
     */
    public void setChannelPresenceTaskEnabled(final boolean value)
    {
        this.channelPresenceTaskEnabled = value;
    }

    /**
     * Get the proxy to use connecting to IRC server.
     *
     * @return returns proxy configuration or <tt>null</tt> if no proxy should
     *         be used
     */
    @Override
    public Proxy getProxy()
    {
        return this.proxy;
    }

    /**
     * Set a new proxy instance.
     *
     * The proxy may be <tt>null</tt> signaling that a proxy connection is not
     * necessary.
     *
     * @param proxy the new proxy instance
     */
    public void setProxy(final Proxy proxy)
    {
        this.proxy = proxy;
    }

    /**
     * Get resolve by proxy value.
     *
     * @return returns <tt>true</tt> to resolve by proxy, or <tt>false</tt> to
     *         resolve via (local) DNS.
     */
    @Override
    public boolean isResolveByProxy()
    {
        return this.resolveByProxy;
    }

    /**
     * Set resolve by proxy value. Indicates whether or not to use the proxy to
     * resolve addresses.
     */
    public void setResolveByProxy(final boolean resolveByProxy)
    {
        this.resolveByProxy = resolveByProxy;
    }

    /**
     * Get the configured SASL authentication data.
     *
     * @return Returns the SASL authentication data if set, or null if no
     *         authentication data is set. If no authentication data is set,
     *         this would mean SASL authentication need not be used.
     */
    @Override
    public SASL getSASL()
    {
        return this.sasl;
    }

    /**
     * Set SASL authentication data.
     *
     * @param sasl the SASL authentication data
     */
    public void setSASL(final SASLImpl sasl)
    {
        this.sasl = sasl;
    }

    /**
     * Type for storing SASL authentication data.
     *
     * @author Danny van Heumen
     */
    public static class SASLImpl implements SASL
    {
        /**
         * User name.
         */
        private final String user;

        /**
         * Password.
         */
        private final String pass;

        /**
         * Authorization role.
         */
        private final String role;

        /**
         * Constructor for authentication without an explicit authorization
         * role.
         *
         * @param userName the user name
         * @param password the password
         */
        public SASLImpl(final String userName, final String password)
        {
            this(userName, password, null);
        }

        /**
         * Constructor for authentication with authorization role.
         *
         * @param userName the user name
         * @param password the password
         * @param role the authorization role
         */
        public SASLImpl(final String userName, final String password,
            final String role)
        {
            if (userName == null)
            {
                throw new NullPointerException("userName");
            }
            this.user = userName;
            if (password == null)
            {
                throw new NullPointerException("password");
            }
            this.pass = password;
            this.role = role;
        }

        /**
         * Get the user name.
         *
         * @return Returns the user name.
         */
        public String getUser()
        {
            return this.user;
        }

        /**
         * Get the password.
         *
         * @return Returns the password.
         */
        public String getPass()
        {
            return this.pass;
        }

        /**
         * Get the authorization role.
         *
         * @return Returns the authorization role.
         */
        public String getRole()
        {
            return this.role;
        }
    }
}
