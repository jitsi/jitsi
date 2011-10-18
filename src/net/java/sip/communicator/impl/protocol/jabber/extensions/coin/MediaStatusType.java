/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

/**
 * Media status.
 *
 * @author Sebastien Vincent
 */
public enum MediaStatusType
{
    /**
     * Receive only.
     */
    recvonly,

    /**
     * Send only.
     */
    sendonly,

    /**
     * Send and receive.
     */
    sendrecv,

    /**
     * Inactive.
     */
    inactive;
}
