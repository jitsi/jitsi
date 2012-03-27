/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform;

import net.java.sip.communicator.impl.neomedia.*;

/**
 * Encapsulate the concept of packet transformation. Given a packet,
 * <tt>PacketTransformer</tt> can either transform it or reverse the
 * transformation.
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public interface PacketTransformer
{
    /**
     * Transforms a specific packet.
     * 
     * @param pkt the packet to be transformed
     * @return the transformed packet
     */
    public RawPacket transform(RawPacket pkt);

    /**
     * Reverse-transforms a specific packet (i.e. transforms a transformed
     * packet back).
     *
     * @param pkt the transformed packet to be restored
     * @return the restored packet
     */
    public RawPacket reverseTransform(RawPacket pkt);

    /**
     * Close the transformer and underlying transform engine.
     * 
     * The close functions closes all stored crypto contexts. This deletes key data 
     * and forces a cleanup of the crypto contexts.
     */
    public void close();
}
