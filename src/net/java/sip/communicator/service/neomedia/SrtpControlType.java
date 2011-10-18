/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * The <tt>SrtpControlType</tt> enumeration contains all currently known
 * <tt>SrtpControl</tt> implementations.
 * 
 * @author Ingo Bauersachs
 */
public enum SrtpControlType
{
    /**
     * Session Description Protocol (SDP) Security Descriptions for Media
     * Streams (RFC 4568)
     */
    SDES,

    /**
     * ZRTP: Media Path Key Agreement for Unicast Secure RTP (RFC 6189)
     */
    ZRTP,

    /**
     * Multimedia Internet KEYing (RFC 3830)
     */
    MIKEY
}
