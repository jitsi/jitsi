/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.srtp;

import net.java.sip.communicator.impl.media.*;

/**
 * PacketManipulator contains methods for parsing RTP packets.
 * It is used to get certain RTP packet field from a RawPacket, which holds
 * the content of a RTP packet.
 * 
 * Because all the methods operates on certain RawPacket, all methods in this
 * class are static methods.
 * 
 * @author Bing SU (nova.su@gmail.com)
 */
public class PacketManipulator
{
    /**
     * Get RTP padding size from a RTP packet
     * 
     * @param pkt the source RTP packet
     * @return RTP padding size from source RTP packet
     */
    public static int GetRTPPaddingSize(RawPacket pkt)
    {
        if ((pkt.readByte(0) & (0x01 << 2)) == 0)
        {
            return 0;
        }
        else
        {
            return pkt.readByte(pkt.getLength() - 1);
        }
    }

    /**
     * Get RTP header length from a RTP packet
     * 
     * @param pkt the source RTP packet
     * @return RTP header length from source RTP packet
     */
    public static int GetRTPHeaderLength(RawPacket pkt)
    {
        boolean hasExtension = ((pkt.readByte(0) & (0x01 << 2)) != 0);
        if (hasExtension)
        {
            // TODO header extension is not supported yet
            return -1;
        }

        int csrcNum = pkt.readByte(0) & 0xF;
        return 12 + 4 * csrcNum; 
    }

    /**
     * Get RTP payload length from a RTP packet
     * 
     * @param pkt the source RTP packet
     * @return RTP payload length from source RTP packet
     */
    public static int GetRTPPayloadLength(RawPacket pkt)
    {
        return pkt.getLength() - GetRTPHeaderLength(pkt);
    }

    /**
     * Get RTP SSRC from a RTP packet
     *
     * @param pkt the source RTP packet
     * @return RTP SSRC from source RTP packet
     */
    public static long GetRTPSSRC(RawPacket pkt)
    {
        return pkt.readUnsignedIntAsLong(8);
    }

    /**
     * Get RTP sequence number from a RTP packet
     *
     * @param pkt the source RTP packet
     * @return RTP sequence num from source packet
     */
    public static int GetRTPSequenceNumber(RawPacket pkt)
    {
        return pkt.readUnsignedShortAsInt(2);
    }

    /**
     * Test whether if a RTP packet is padded
     *
     * @param pkt the source RTP packet
     * @return whether if source RTP packet is padded
     */
    public static boolean IsPacketMarked(RawPacket pkt)
    {
        return (pkt.readByte(1) & (1 << 7)) != 0;
    }

    /**
     * Get RTP payload type from a RTP packet
     *
     * @param pkt the source RTP packet
     * @return RTP payload type of source RTP packet
     */
    public static byte GetRTPPayloadType(RawPacket pkt)
    {
        return (byte) (pkt.readByte(1) & (byte)0x7F);
    }
    
    /**
     * Get RTP timestamp from a RTP packet
     *
     * @param pkt the source RTP packet
     * @return RTP timestamp of source RTP packet
     */
    public static byte[] ReadTimeStampIntoByteArray(RawPacket pkt)
    {
        return pkt.readRegion(4, 4);
    }

}
