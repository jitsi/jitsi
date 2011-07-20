/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform;

import java.net.*;

import net.java.sip.communicator.impl.neomedia.*;

/**
 * Extends <tt>RTPConnectorInputStream</tt> with transform logic for UDP.
 *
 * In this implementation, we use UDP sockets to receive RTP/RTCP. We listen on
 * the address / port specified by local session address. When one packet is
 * received, it is first reverse transformed through PacketTransformer defined
 * by user. And then returned as normal RTP/RTCP packets to RTPManager.
 *
 * @author Bing SU (nova.su@gmail.com)
 * @author Lubomir Marinov
 */
public class TransformUDPInputStream
    extends RTPConnectorUDPInputStream
{
    /**
     * The user defined <tt>PacketTransformer</tt> which is used to reverse
     * transform packets.
     */
    private PacketTransformer transformer;

    /**
     * Initializes a new <tt>TransformInputStream</tt> which is to receive
     * packet data from a specific UDP socket.
     *
     * @param socket the UDP socket the new instance is to receive data from
     */
    public TransformUDPInputStream(DatagramSocket socket)
    {
        super(socket);
    }

    /**
     * Creates a new <tt>RawPacket</tt> from a specific <tt>DatagramPacket</tt>
     * in order to have this instance receive its packet data through its
     * {@link #read(byte[], int, int)} method. Reverse-transforms the received
     * packet.
     *
     * @param datagramPacket the <tt>DatagramPacket</tt> containing the packet
     * data
     * @return a new <tt>RawPacket</tt> containing the packet data of the
     * specified <tt>DatagramPacket</tt> or possibly its modification;
     * <tt>null</tt> to ignore the packet data of the specified
     * <tt>DatagramPacket</tt> and not make it available to this instance
     * through its {@link #read(byte[], int, int)} method
     * @see RTPConnectorInputStream#createRawPacket(DatagramPacket)
     */
    @Override
    protected RawPacket createRawPacket(DatagramPacket datagramPacket)
    {
        PacketTransformer transformer = getTransformer();
        RawPacket pkt = super.createRawPacket(datagramPacket);

        return (transformer == null) ? pkt : transformer.reverseTransform(pkt);
    }

    /**
     * Gets the <tt>PacketTransformer</tt> which is used to reverse-transform
     * packets.
     *
     * @return the <tt>PacketTransformer</tt> which is used to reverse-transform
     * packets
     */
    public PacketTransformer getTransformer()
    {
        return transformer;
    }

    /**
     * Sets the <tt>PacketTransformer</tt> which is to be used to
     * reverse-transform packets.
     *
     * @param transformer the <tt>PacketTransformer</tt> which is to be used to
     * reverse-transform packets
     */
    public void setTransformer(PacketTransformer transformer)
    {
        this.transformer = transformer;
    }
}
