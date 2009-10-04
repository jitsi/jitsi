/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.zrtp;

import net.java.sip.communicator.impl.media.*;
import net.java.sip.communicator.impl.media.transform.*;

/**
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */
public class ZRTPCTransformer implements PacketTransformer 
{
    /**
     * ZRTCPTransformer implements PacketTransformer.
     * It encapsulate the encryption / decryption logic for SRTCP packets
     * 
     * This class is currently not used.
     * 
     * @author Bing SU (nova.su@gmail.com)
     */
    //private ZRTPTransformEngine engine;

    /**
     * Constructs a SRTCPTransformer object
     *
     * @param engine The associated ZRTPTransformEngine object
     */
    public ZRTPCTransformer(ZRTPTransformEngine engine) 
    {
        //this.engine = engine;
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
