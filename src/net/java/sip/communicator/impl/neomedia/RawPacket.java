/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

/**
 * When using TransformConnector, a RTP/RTCP packet is represented using
 * RawPacket. RawPacket stores the buffer holding the RTP/RTCP packet, as well
 * as the inner offset and length of RTP/RTCP packet data.
 *
 * After transformation, data is also store in RawPacket objects, either the
 * original RawPacket (in place transformation), or a newly created RawPacket.
 *
 * Besides packet info storage, RawPacket also provides some other operations
 * such as readInt() to ease the development process.
 *
 * @author Werner Dittmann (Werner.Dittmann@t-online.de)
 * @author Bing SU (nova.su@gmail.com)
 * @author Emil Ivov
 * @author Damian Minkov
 */
public class RawPacket
{
    /**
     * Byte array storing the content of this Packet
     */
    protected byte[] buffer;

    /**
     * Start offset of the packet data inside buffer.
     * Usually this value would be 0. But in order to be compatible with
     * RTPManager we store this info. (Not assuming the offset is always zero)
     */
    protected int offset;

    /**
     * Length of this packet's data
     */
    protected int length;

    /**
     * The size of the fixed part of the RTP header as defined by RFC 3550.
     */
    public static final int FIXED_HEADER_SIZE = 12;

    /**
     * Construct a RawPacket using specified value.
     *
     * @param buffer Byte array holding the content of this Packet
     * @param offset Start offset of packet content inside buffer
     * @param length Length of the packet's data
     */
    public RawPacket(byte[] buffer, int offset, int length)
    {
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
    }

    /**
     * Get buffer containing the content of this packet
     *
     * @return buffer containing the content of this packet
     */
    public byte[] getBuffer()
    {
        return this.buffer;
    }

    /**
     * Get the length of this packet's data
     *
     * @return length of this packet's data
     */
    public int getLength()
    {
        return this.length;
    }

    /**
     * Get the start offset of this packet's data inside storing buffer
     *
     * @return start offset of this packet's data inside storing buffer
     */
    public int getOffset()
    {
        return this.offset;
    }

    /**
     * Read a integer from this packet at specified offset
     *
     * @param off start offset of the integer to be read
     * @return the integer to be read
     */
    public int readInt(int off)
    {
        return (this.buffer[this.offset + off + 0] << 24) |
               ((this.buffer[this.offset + off + 1] & 0xff) << 16) |
               ((this.buffer[this.offset + off + 2] & 0xff) << 8)  |
                (this.buffer[this.offset + off + 3] & 0xff);
    }

    /**
     * Read a short from this packet at specified offset
     *
     * @param off start offset of this short
     * @return short value at offset
     */
    public short readShort(int off)
    {
        return (short) ((this.buffer[this.offset + off + 0] << 8) |
                        (this.buffer[this.offset + off + 1] & 0xff));
    }

    /**
     * Read an unsigned short at specified offset as a int
     *
     * @param off start offset of the unsigned short
     * @return the int value of the unsigned short at offset
     */
    public int readUnsignedShortAsInt(int off)
    {
        int b1 = (0x000000FF & (this.buffer[this.offset + off + 0]));
        int b2 = (0x000000FF & (this.buffer[this.offset + off + 1]));
        int val = b1 << 8 | b2;
        return val;
    }

    /**
     * Read a byte from this packet at specified offset
     *
     * @param off start offset of the byte
     * @return byte at offset
     */
    public byte readByte(int off)
    {
        return buffer[offset + off];
    }

    /**
     * Read an unsigned integer as long at specified offset
     *
     * @param off start offset of this unsigned integer
     * @return unsigned integer as long at offset
     */
    public long readUnsignedIntAsLong(int off)
    {
        int b0 = (0x000000FF & (this.buffer[this.offset + off + 0]));
        int b1 = (0x000000FF & (this.buffer[this.offset + off + 1]));
        int b2 = (0x000000FF & (this.buffer[this.offset + off + 2]));
        int b3 = (0x000000FF & (this.buffer[this.offset + off + 3]));

        return  ((b0 << 24 | b1 << 16 | b2 << 8 | b3)) & 0xFFFFFFFFL;
    }

    /**
     * Read a byte region from specified offset with specified length
     *
     * @param off start offset of the region to be read
     * @param len length of the region to be read
     * @return byte array of [offset, offset + length)
     */
    public byte[] readRegion(int off, int len)
    {
        int startOffset = this.offset + off;
        if (off < 0 || len <= 0
            || startOffset + len > this.buffer.length)
        {
            return null;
        }

        byte[] region = new byte[len];

        System.arraycopy(this.buffer, startOffset, region, 0, len);

        return region;
    }

    /**
     * Read a byte region from specified offset with specified length in given
     * buffer
     *
     * @param off start offset of the region to be read
     * @param len length of the region to be read
     * @param outBuff output buffer
     */
    public void readRegionToBuff(int off, int len, byte[] outBuff)
    {
        int startOffset = this.offset + off;
        if (off < 0 || len <= 0
            || startOffset + len > this.buffer.length)
        {
            return;
        }

        if (outBuff.length < len)
        {
            return;
        }
        System.arraycopy(this.buffer, startOffset, outBuff, 0, len);
    }

    /**
     * Append a byte array to then end of the packet. This will change the data
     * buffer of this packet.
     *
     * @param data byte array to append
     * @param len the number of bytes to append
     */
    public void append(byte[] data, int len)
    {
        if (data == null || len == 0)
        {
            return;
        }

        byte[] newBuffer = new byte[this.length + len];
        System.arraycopy(this.buffer, this.offset, newBuffer, 0, this.length);
        System.arraycopy(data, 0, newBuffer, this.length, len);
        this.offset = 0;
        this.length = this.length + len;
        this.buffer = newBuffer;

    }
    /**
     * Shrink the buffer of this packet by specified length
     *
     * @param len length to shrink
     */
    public void shrink(int len)
    {
        if (len <= 0)
        {
            return;
        }

        this.length -= len;
        if (this.length < 0)
        {
            this.length = 0;
        }
    }

    /**
     * Returns the number of CSRC identifiers currently included in this packet.
     *
     * @return the CSRC count for this <tt>RawPacket</tt>.
     */
    public int getCsrcCount()
    {
        return (buffer[offset] & 0x0f);
    }

    /**
     * Replaces the existing CSRC list (even if empty) with <tt>newCsrcList</tt>
     * and updates the CC (CSRC count) field of this <tt>RawPacket</tt>
     * accordingly.
     *
     * @param newCsrcList the list of CSRC identifiers that we'd like to set for
     * this <tt>RawPacket</tt>.
     */
    public void setCsrcList(long[] newCsrcList)
    {

        int newCsrcCount = newCsrcList.length;
        byte[] csrcBuff = new byte[newCsrcCount * 4];
        int csrcOffset = 0;

        for(long csrc : newCsrcList)
        {
            csrcBuff[csrcOffset] = (byte)(csrc >> 24);
            csrcBuff[csrcOffset+1] = (byte)(csrc >> 16);
            csrcBuff[csrcOffset+2] = (byte)(csrc >> 8);
            csrcBuff[csrcOffset+3] = (byte)csrc;

            csrcOffset += 4;
        }

        int oldCsrcCount = getCsrcCount();

        byte[] oldBuffer = this.getBuffer();

        //the new buffer needs to be bigger than the new one in order to
        //accommodate the list of CSRC IDs (unless there were more of them
        //previously than after setting the new list).
        byte[] newBuffer
            = new byte[oldBuffer.length + csrcBuff.length - oldCsrcCount*4];

        //copy the part up to the CSRC list
        System.arraycopy(
                    oldBuffer, 0, newBuffer, 0, offset + FIXED_HEADER_SIZE);

        //copy the new CSRC list
        System.arraycopy( csrcBuff, 0, newBuffer,
                        offset + FIXED_HEADER_SIZE, csrcBuff.length);

        //now copy the payload from the old buff and make sure we don't copy
        //the CSRC list if there was one in the old packet
        int payloadOffsetForOldBuff
            = offset + FIXED_HEADER_SIZE + oldCsrcCount*4;

        int payloadOffsetForNewBuff
            = offset + FIXED_HEADER_SIZE + newCsrcCount*4;

        System.arraycopy( oldBuffer, payloadOffsetForOldBuff,
                          newBuffer, payloadOffsetForNewBuff,
                          oldBuffer.length - payloadOffsetForOldBuff);

        //set the new CSRC count
        newBuffer[offset] = (byte)((newBuffer[offset] & 0xF0)
                                    | newCsrcCount);

        this.buffer = newBuffer;
        this.length = newBuffer.length;

    }

    /**
     * Returns the list of CSRC IDs, currently encapsulated in this packet.
     *
     * @return an array containing the list of CSRC IDs, currently encapsulated
     * in this packet.
     */
    public long[] extractCsrcList()
    {
        int csrcCount = getCsrcCount();

        long[] csrcList = new long[csrcCount];

        int csrcStartIndex = offset + FIXED_HEADER_SIZE;
        for (int i = 0; i < csrcCount; i++)
        {
            csrcList[i] =   buffer[csrcStartIndex]     << 24
                          & buffer[csrcStartIndex + 1] << 16
                          & buffer[csrcStartIndex + 2] << 8
                          & buffer[csrcStartIndex + 3];

            csrcStartIndex += 4;
        }

        return csrcList;
    }

    /**
     * Get RTP padding size from a RTP packet
     *
     * @return RTP padding size from source RTP packet
     */
    public int getPaddingSize()
    {
        if ((buffer[offset] & 0x4) == 0)
        {
            return 0;
        }
        else
        {
            return buffer[offset + length - 1];
        }
    }

    /**
     * Get RTP header length from a RTP packet
     *
     * @return RTP header length from source RTP packet
     */
    public int getHeaderLength()
    {
        byte flags = buffer[offset];
        boolean hasExtension = (flags & 0x8) != 0;

        int len = FIXED_HEADER_SIZE + 4 * getCsrcCount();

        if (hasExtension)
        {
            // the first 16 bits are reserved and defined by
            // the extension profile
            // 2bit profile + 2bit len + len*32bit
            len += readUnsignedShortAsInt(len + 2)*4 + 4;
        }

        return len;
    }

    /**
     * Get RTP payload length from a RTP packet
     *
     * @return RTP payload length from source RTP packet
     */
    public int getPayloadLength()
    {
        return length - getHeaderLength();
    }

    /**
     * Get RTP SSRC from a RTP packet
     *
     * @return RTP SSRC from source RTP packet
     */
    public int getSSRC()
    {
        return (int)(readUnsignedIntAsLong(8) & 0xffffffff);
    }

    /**
     * Get RTP sequence number from a RTP packet
     *
     * @return RTP sequence num from source packet
     */
    public int getSequenceNumber()
    {
        return readUnsignedShortAsInt(2);
    }

    /**
     * Test whether if a RTP packet is padded
     *
     * @return whether if source RTP packet is padded
     */
    public boolean isPacketMarked()
    {
        return (buffer[offset + 1] & 0x80) != 0;
    }

    /**
     * Get RTP payload type from a RTP packet
     *
     * @return RTP payload type of source RTP packet
     */
    public byte getPayloadType()
    {
        return (byte) (buffer[offset + 1] & (byte)0x7F);
    }

    /**
     * Get RTP timestamp from a RTP packet
     *
     * @return RTP timestamp of source RTP packet
     */
    public byte[] readTimeStampIntoByteArray()
    {
        return readRegion(4, 4);
    }
}
