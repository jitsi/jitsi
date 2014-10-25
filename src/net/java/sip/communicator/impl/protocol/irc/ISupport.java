/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

/**
 * ISUPPORT parameters by IRC server.
 *
 * @author Danny van Heumen
 */
public enum ISupport
{
    /**
     * Maximum nick length allowed by IRC server.
     */
    NICKLEN,
    /**
     * Maximum channel name length allowed by IRC server.
     */
    CHANNELLEN,
    /**
     * Maximum topic length allowed by IRC server.
     */
    TOPICLEN,
    /**
     * Maximum kick message length allowed by IRC server.
     */
    KICKLEN,
    /**
     * Maximum away message length allowed by IRC server.
     */
    AWAYLEN;
}
