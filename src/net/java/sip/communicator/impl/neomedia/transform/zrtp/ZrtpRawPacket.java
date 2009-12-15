/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.zrtp;

import gnu.java.zrtp.packets.*;
import gnu.java.zrtp.utils.*;

import net.java.sip.communicator.impl.neomedia.*;

/**
 * ZRTP packet representation.
 * 
 * This class extends the RawPacket class and adds some methods
 * required by the ZRTP transformer.
 *  
 * @author Werner Dittmann <Werner.Dittmann@t-online.de>
 */
public class ZrtpRawPacket extends RawPacket 
{
    /**
     * Each ZRTP packet contains this magic number.
     */
    public static byte[] zrtpMagic;
    
    static {
        zrtpMagic = new byte[4];
        zrtpMagic[0]= 0x5a; 
        zrtpMagic[1]= 0x52;   
        zrtpMagic[2]= 0x54; 
        zrtpMagic[3]= 0x50;
    }

    /**
     * Construct an input ZrtpRawPacket using a received RTP raw packet.
     * 
     * @param pkt a raw RTP packet as received 
     */
    public ZrtpRawPacket(RawPacket pkt)  
    {
        super (pkt.getBuffer(), pkt.getOffset(), pkt.getLength());
    }

    /**
     * Construct an output ZrtpRawPacket using specified value.
     * 
     * Initialize this packet and set the ZRTP magic value
     * to mark it as a ZRTP packet.
     * 
     * @param buf Byte array holding the content of this Packet
     * @param off Start offset of packet content inside buffer
     * @param len Length of the packet's data
     */
    public ZrtpRawPacket(byte[] buf, int off, int len)  
    {
        super (buf, off, len);  
        buffer[offset] = 0x10;
        buffer[offset+1] = 0;

        int at = 4;
        buffer[offset + at++] = zrtpMagic[0];
        buffer[offset + at++] = zrtpMagic[1];
        buffer[offset + at++] = zrtpMagic[2];
        buffer[offset + at] = zrtpMagic[3];
    }

    /**
     * Check if it could be a ZRTP packet.
     * 
     * The method checks if the first byte of the received data
     * matches the defined ZRTP pattern 0x10
     *  
     * @return true if could be a ZRTP packet, false otherwise.
     */
    protected boolean isZrtpPacket() 
    {
        if ((buffer[offset] & 0x10) == 0x10) 
        {
            return true;
        }
        return false;
    }

    /**
     * Check if it is really a ZRTP packet.
     * 
     * The method checks if the packet contains the ZRTP magic
     * number.
     *  
     * @return true if packet contains the magic number, false otherwise.
     */
    protected boolean hasMagic() 
    {
        return
            (buffer[offset + 4] == zrtpMagic[0])
                && (buffer[offset + 5] == zrtpMagic[1])
                && (buffer[offset + 6] == zrtpMagic[2])
                && (buffer[offset + 7] == zrtpMagic[3]);
    }

    /**
     * Set the sequence number in this packet.
     * @param seq
     */
    protected void setSeqNum(short seq) 
    {
        int at = 2;
        buffer[offset + at++] = (byte)(seq>>8);
        buffer[offset + at] = (byte)seq;
    }

    /**
     * Set SSRC in this packet
     * @param ssrc
     */
    protected void setSSRC(int ssrc) 
    {
        setIntegerAt(ssrc, 8);
    }

    /**
     * Check if the CRC of this packet is ok.
     *
     * @return true if the CRC is valid, false otherwise
     */
    protected boolean checkCrc() 
    {
        int crc = readInt(length-ZrtpPacketBase.CRC_SIZE);
        return ZrtpCrc32.zrtpCheckCksum(buffer, offset, length-ZrtpPacketBase.CRC_SIZE, crc);
    }

    /**
     * Set ZRTP CRC in this packet
     */
    protected void setCrc() 
    {
        int crc = ZrtpCrc32.zrtpGenerateCksum(buffer, offset,length-ZrtpPacketBase.CRC_SIZE);
        // convert and store CRC in crc field of ZRTP packet.
        crc = ZrtpCrc32.zrtpEndCksum(crc);
        setIntegerAt(crc, length - ZrtpPacketBase.CRC_SIZE);
    }

    /**
     * Get the ZRTP message part from the ZRTP packet.
     * 
     * @return The ZRTP message part.
     */
    protected byte[] getMessagePart() 
    {
        int headerLen = getHeaderLength();
        return readRegion(headerLen, length-headerLen);
    }

    /**
     * Set an integer at specified offset in network order.
     * 
     * @param data The integer to store in the packet
     * @param at Offset into the buffer
     */
    private void setIntegerAt(int data, int at) 
    {
        buffer[offset + at++] = (byte)(data>>24);
        buffer[offset + at++] = (byte)(data>>16);
        buffer[offset + at++] = (byte)(data>>8);
        buffer[offset + at] = (byte)data;
    }
}
