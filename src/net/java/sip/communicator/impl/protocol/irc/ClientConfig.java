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
 * Configuration type for maintaining client configuration.
 *
 * @author Danny van Heumen
 */
public interface ClientConfig
{
    /**
     * Allow IRC version 3 capabilities.
     *
     * @return returns <tt>true</tt> if IRC version 3 capabilities are allowed,
     *         or <tt>false</tt> if we explicitly disallow anything related to
     *         IRCv3. (Disabling may regress the connection to "classic" IRC
     *         (RFC1459).)
     */
    boolean isVersion3Allowed();

    /**
     * Enable contact presence periodic task for keeping track of contact
     * presence (offline or online).
     *
     * @return returns <tt>true</tt> to use contact presence task or
     *         <tt>false</tt> otherwise.
     */
    boolean isContactPresenceTaskEnabled();

    /**
     * Enable channel presence periodic task for keeping track of channel
     * members presence (available or away).
     *
     * @return returns <tt>true</tt> to use channel presence task or
     *         <tt>false</tt> otherwise.
     */
    boolean isChannelPresenceTaskEnabled();

    /**
     * Use a SOCKS proxy to connect to the configured IRC server.
     *
     * The proxy may be <tt>null</tt> which means that no proxy will be used
     * when connecting to the IRC server.
     *
     * @return returns Proxy configuration or <tt>null</tt> if no proxy should
     *         be used.
     */
    Proxy getProxy();

    /**
     * Resolve addresses through the proxy, instead of using a (local) DNS
     * resolver.
     *
     * @return returns <tt>true</tt> if addresses should be resolved through
     *         proxy, or <tt>false</tt> if it should NOT be resolved through
     *         proxy
     */
    boolean isResolveByProxy();

    /**
     * Get the configured SASL authentication data.
     *
     * @return Returns the SASL authentication data if set, or null if no
     *         authentication data is set. If no authentication data is set,
     *         this would mean SASL authentication need not be used.
     */
    SASL getSASL();

    /**
     * SASL authentication data.
     *
     * @author Danny van Heumen
     */
    static interface SASL
    {
        /**
         * Get user name.
         *
         * @return Returns the user name.
         */
        String getUser();

        /**
         * Get password.
         *
         * @return Returns the password.
         */
        String getPass();

        /**
         * Get authorization role.
         *
         * @return Returns the authorization role if set. (Optional)
         */
        String getRole();
    }
}
