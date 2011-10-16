/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.zrtp;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.transform.*;

/**
 * This is currently not supported so packets are not changed.
 *
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */
public class ZRTCPTransformer
    implements PacketTransformer
{
    /**
     * We support different SRTCP contexts for input and output traffic:
     * 
     * Transform() uses the srtcpOut to perform encryption
     * reverseTransform() uses srtcpIn to perform decryption
     */
    private PacketTransformer srtcpIn = null;
    
    private PacketTransformer srtcpOut = null;

    /**
     * Constructs a ZRTCPTransformer object
     *
     * @param engine The associated ZRTPTransformEngine object
     */
    public ZRTCPTransformer(ZRTPTransformEngine engine) 
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
        if (srtcpOut == null) {
            return pkt;
        }
        return srtcpOut.transform(pkt);
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
        if (srtcpIn == null) {
            return pkt;
        }
        return srtcpIn.reverseTransform(pkt);

    }
    /**
     * @param srtcpIn the srtcpIn to set
     */
    public void setSrtcpIn(PacketTransformer srtcpIn) {
        this.srtcpIn = srtcpIn;
    }

    /**
     * @param srtcpOut the srtcpOut to set
     */
    public void setSrtcpOut(PacketTransformer srtcpOut) {
        this.srtcpOut = srtcpOut;
    }
}
