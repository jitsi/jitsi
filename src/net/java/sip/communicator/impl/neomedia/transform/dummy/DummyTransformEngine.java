/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.dummy;

import net.java.sip.communicator.impl.media.*;
import net.java.sip.communicator.impl.media.transform.*;

/**
 * DummyTransformEngine does nothing, its sole purpose is to test the
 * TransformConnector related classes.
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public class DummyTransformEngine
    implements TransformEngine, PacketTransformer
{
    /**
     * Gets the <tt>PacketTransformer</tt> for RTCP packets.
     *
     * @return the <tt>PacketTransformer</tt> for RTCP packets
     */
    public PacketTransformer getRTCPTransformer()
    {
        return this; 
    }

    /**
     * Gets the <tt>PacketTransformer</tt> for RTP packets.
     *
     * @return the <tt>PacketTransformer</tt> for RTP packets
     */
    public PacketTransformer getRTPTransformer()
    {
        return this;
    }

    /**
     * Transforms a specific packet. Does nothing, return the same packet.
     *
     * @param pkt the packet to be transformed
     * @return the input packet without modifications.
     */
    public RawPacket transform(RawPacket pkt)
    {
        return pkt;
    }

    /**
     * Does nothing just returns the input packet.
     *
     * @param pkt the transformed packet to be restored
     * @return the input packet without any modifications.
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {
        return pkt;
    }
}
