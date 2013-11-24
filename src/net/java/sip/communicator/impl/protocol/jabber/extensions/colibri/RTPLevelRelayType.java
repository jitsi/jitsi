/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

/**
 * Defines the RTP-level relay types as specified by RFC 3550 "RTP: A Transport
 * Protocol for Real-Time Applications" in section 2.3 "Mixers and Translators".
 *
 * @author Lyubomir Marinov
 */
public enum RTPLevelRelayType
{
    /**
     * The type of RTP-level relay which performs content mixing on the received
     * media. In order to mix the received content, the relay will usually
     * decode the received RTP and RTCP packets into raw media and will
     * subsequently generate new RTP and RTCP packets to send the new media
     * which represents the mix of the received content.
     */
    MIXER,

    /**
     * The type of RTP-level relay which does not perform content mixing on the
     * received media and rather forwards the received RTP and RTCP packets. The
     * relay will usually not decode the received RTP and RTCP into raw media.
     */
    TRANSLATOR;

    public static RTPLevelRelayType parseRTPLevelRelayType(String s)
    {
        for (RTPLevelRelayType value : RTPLevelRelayType.values())
        {
            if (s.equals(value.toString()))
                return value;
        }
        throw new IllegalArgumentException(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        switch (this)
        {
        case MIXER:
            return "mixer";
        case TRANSLATOR:
            return "translator";
        default:
            return super.toString();
        }
    }
}
