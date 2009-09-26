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
 * TransformInputStream implements PushSourceStream. It is used by RTPManager to
 * receive RTP/RTCP packet datas.
 * 
 * In this implementation, we use UDP sockets to receive RTP/RTCP. We listen on
 * the address / port specified by local session address. When one packet is
 * received, it is first reverse transformed through PacketTransformer defined
 * by user. And then returned as normal RTP/RTCP packets to RTPManager.
 * 
 * @author Bing SU (nova.su@gmail.com)
 * @author Lubomir Marinov
 */
public class TransformInputStream
    extends RTPConnectorInputStream
{

    /**
     * User defined PacketTransformer, which is used to reverse transform
     * packets.
     */
    private final PacketTransformer transformer;

    /**
     * Construct a TransformInputStream based on the receiving socket and
     * PacketTransformer
     * 
     * @param socket
     *            data receiving socket
     * @param transformer
     *            packet transformer used
     */
    public TransformInputStream(
        DatagramSocket socket,
        PacketTransformer transformer) 
    {
        super(socket);

        this.transformer = transformer;
    }

    /*
     * Overrides RTPConnectorInputStream#createRawPacket(DatagramPacket) to
     * perform reverse transformation of the received packet.
     */
    protected RawPacket createRawPacket(DatagramPacket datagramPacket)
    {
        return
            transformer.reverseTransform(super.createRawPacket(datagramPacket));
    }
}
