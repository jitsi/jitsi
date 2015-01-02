/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

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
}
