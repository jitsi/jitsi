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
        writeByte(0, (byte)0x10);
        writeByte(1, (byte)0);

        int at = 4;
        writeByte(at++, zrtpMagic[0]);
        writeByte(at++, zrtpMagic[1]);
        writeByte(at++, zrtpMagic[2]);
        writeByte(at, zrtpMagic[3]);
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
        return isZrtpData(this);
    }

    /**
     * Checks whether extension bit is set and if so is the extension header
     * an zrtp one.
     * @param pkt the packet to check.
     * @return <tt>true</tt> if data is zrtp packet.
     */
    static boolean isZrtpData(RawPacket pkt)
    {
        if(!pkt.getExtensionBit())
            return false;

        if(pkt.getHeaderExtensionType() == 0x505a)
            return true;

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
            (readByte(4) == zrtpMagic[0])
                && (readByte(5) == zrtpMagic[1])
                && (readByte(6) == zrtpMagic[2])
                && (readByte(7) == zrtpMagic[3]);
    }

    /**
     * Set the sequence number in this packet.
     * @param seq
     */
    protected void setSeqNum(short seq) 
    {
        int at = 2;
        writeByte(at++, (byte)(seq>>8));
        writeByte(at, (byte)seq);
    }

    /**
     * Set SSRC in this packet
     * @param ssrc
     */
    protected void setSSRC(int ssrc) 
    {
        writeInt(8, ssrc);
    }

    /**
     * Check if the CRC of this packet is ok.
     *
     * @return true if the CRC is valid, false otherwise
     */
    protected boolean checkCrc() 
    {
        int crc = readInt(getLength()-ZrtpPacketBase.CRC_SIZE);
        return ZrtpCrc32.zrtpCheckCksum(getBuffer(), getOffset(),
            getLength()-ZrtpPacketBase.CRC_SIZE, crc);
    }

    /**
     * Set ZRTP CRC in this packet
     */
    protected void setCrc() 
    {
        int crc = ZrtpCrc32.zrtpGenerateCksum(getBuffer(), getOffset(),
            getLength() - ZrtpPacketBase.CRC_SIZE);
        // convert and store CRC in crc field of ZRTP packet.
        crc = ZrtpCrc32.zrtpEndCksum(crc);
        writeInt(getLength() - ZrtpPacketBase.CRC_SIZE, crc);
    }
}
