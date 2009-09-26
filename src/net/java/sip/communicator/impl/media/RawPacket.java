/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

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
     * Read a byte region from specified offset with specified length in given buffer
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
}
