/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import javax.media.rtp.*;

/**
 * Represents an RTCP feedback packet as described in RFC4585.
 *
 * @author Sebastien Vincent
 */
public class RTCPFeedbackPacket
{
    /**
     * Feedback message type.
     */
    private int fmt = 0;

    /**
     * Payload type.
     */
    private int payloadType = 0;

    /**
     * SSRC of packet sender.
     */
    private long senderSSRC = 0;

    /**
     * SSRC of media source.
     */
    private long sourceSSRC = 0;

    /**
     * Constructor.
     *
     * @param type feedback message type
     * @param payloadType payload type
     * @param sender sender SSRC
     * @param src source SSRC
     */
    public RTCPFeedbackPacket(int type, int payloadType, long sender, long src)
    {
        this.fmt = type;
        this.payloadType = payloadType;
        this.senderSSRC = sender;
        this.sourceSSRC = src;
    }

    /**
     * Write RTCP packet to output stream of a <tt>DatagramSocket</tt>.
     *
     * @param out <tt>OutputDataStream</tt> of a <tt>DatagramSocket</tt>
     */
    public void writeTo(OutputDataStream out)
    {
        byte data[] = new byte[12];
        byte vpfmt = (byte)((2 << 7) | (0 << 6) | (byte)fmt);

        data[0] = vpfmt;
        data[1] = (byte)payloadType;

        /* length (in 32-bit words minus one) */
        data[2] = 0;
        data[3] = 2; /* common packet is 12 bytes so (12/4) - 1 */

        /* sender SSRC */
        data[4] = (byte)(senderSSRC >> 24);
        data[5] = (byte)((senderSSRC >> 16) & 0xFF);
        data[6] = (byte)((senderSSRC >> 8) & 0xFF);
        data[7] = (byte)(senderSSRC & 0xFF);

        /* source SSRC */
        data[8] = (byte)(sourceSSRC >> 24);
        data[9] = (byte)((sourceSSRC >> 16) & 0xFF);
        data[10] = (byte)((sourceSSRC >> 8) & 0xFF);
        data[11] = (byte)(sourceSSRC & 0xFF);

        /* effective write */
        out.write(data, 0, 12);
    }
}

