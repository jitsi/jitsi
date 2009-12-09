/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.srtp;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;

/**
 * SRTCPTransformer implements PacketTransformer.
 * It encapsulate the encryption / decryption logic for SRTCP packets
 * 
 * This class is currently not used.
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public class SRTCPTransformer
    implements PacketTransformer
{
    
    /**
     * Constructs a SRTCPTransformer object
     *
     * @param engine The associated SRTPTransformEngine object
     */
    public SRTCPTransformer(SRTPTransformEngine engine)
    {
    }

    /**
     * Encrypt a SRTCP packet
     * 
     * Currently SRTCP packet encryption / decryption is not supported
     * So this method does not change the packet content
     * 
     * @param pkt plain SRTCP packet to be encrypted
     * @return encrypted SRTCP packet
     */
    public RawPacket transform(RawPacket pkt)
    {
        return pkt;
    }

    /**
     * Decrypt a SRTCP packet
     * 
     * Currently SRTCP packet encryption / decryption is not supported
     * So this method does not change the packet content
     * 
     * @param pkt encrypted SRTCP packet to be decrypted
     * @return decrypted SRTCP packet
     */
    public RawPacket reverseTransform(RawPacket pkt)
    {
        return pkt;
    }
}
