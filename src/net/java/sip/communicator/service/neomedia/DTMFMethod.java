/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

/**
 * Enumerates all available DTMF methods.
 *
 * @author Vincent Lucas
 */
public enum DTMFMethod
{
    /**
     * {@link #RTP_DTMF} if telephony-event are available; otherwise,
     * {@link #INBAND_DTMF}.
     */
    AUTO_DTMF,

    /** RTP DTMF as defined in RFC4733. */
    RTP_DTMF,

    /** SIP INFO DTMF. */
    SIP_INFO_DTMF,

    /** INBAND DTMF as defined in ITU-T recommendation Q.23. */
    INBAND_DTMF
}
