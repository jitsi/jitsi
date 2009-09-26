/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform;

import net.java.sip.communicator.impl.media.*;

/**
 * Interface PacketTransformer encapsulate the concept of packet transformation.
 * Given a packet, PacketTransformer can either transform it or reverse the
 * transformation.
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public interface PacketTransformer
{
    /**
     * Transform a packet.
     * 
     * @param pkt The packet to be transformed
     * @return Transformed packet
     */
    public RawPacket transform(RawPacket pkt);

    /**
     * Reverse transform a packet (transform a transformed packet back)
     *
     * @param pkt The transformed packet to be restored
     * @return Restored packet
     */
    public RawPacket reverseTransform(RawPacket pkt);
}
