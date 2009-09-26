/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform;

import java.net.*;

import net.java.sip.communicator.impl.media.*;

/**
 * TransformOutputStream implements OutputDataStream. It is use by RTPManager
 * to send RTP/RTCP packet data out.
 *
 * In this implementation, UDP socket is used to send the data out. When a
 * normal RTP/RTCP packet is passed down from RTPManager, we first transform
 * the packet using user define PacketTransformer and then send it out through
 * network to all the stream targets.
 *
 * @author Bing SU (nova.su@gmail.com)
 * @author Lubomir Marinov
 */
public class TransformOutputStream
    extends RTPConnectorOutputStream
{

    /**
     * PacketTransformer used to transform RTP/RTCP packets
     */
    private final PacketTransformer transformer;

    /**
     * Construct a TransformOutputStream based on the given UDP socket and
     * PacketTransformer
     *
     * @param socket UDP socket used to send packet data out
     * @param transformer PacketTransformer used to transform RTP/RTCP packets
     */
    public TransformOutputStream(
        DatagramSocket socket,
        PacketTransformer transformer)
    {
        super(socket);

        this.transformer = transformer;
    }

    /*
     * Overrides RTPConnectorOutputStream#createRawPacket(byte[], int, int) to
     * perform transformation of the packet to be sent.
     */
    protected RawPacket createRawPacket(byte[] buffer, int offset, int length)
    {
        RawPacket pkt
            = transformer
                .transform(super.createRawPacket(buffer, offset, length));

        /*
         * This is for the case when the ZRTP engine stops the media stream
         * allowing only ZRTP packets.
         */
        // TODO Comment in order to use the GoClear feature.
        if ((pkt == null) && (targets.size() > 0))
            throw new NullPointerException("pkt");

        return pkt;
    }
}
