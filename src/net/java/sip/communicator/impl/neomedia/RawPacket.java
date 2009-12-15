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
     * The size of the extension header as defined by RFC 3550.
     */
    public static final int EXT_HEADER_SIZE = 12;

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
        if (off < 0 || len <= 0 || startOffset + len > this.buffer.length)
            return null;

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
        if (off < 0 || len <= 0 || startOffset + len > this.buffer.length)
            return;

        if (outBuff.length < len)
            return;

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
            return;

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
            return;

        this.length -= len;
        if (this.length < 0)
            this.length = 0;
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
            return 0;
        else
            return buffer[offset + length - 1];
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

    /**
     * Returns <tt>true</tt> if the extension bit of this packet has been set
     * and false otherwise.
     *
     * @return  <tt>true</tt> if the extension bit of this packet has been set
     * and false otherwise.
     */
    public boolean getExtensionBit()
    {
        return (buffer[offset] & 0x10) == 0x10;
    }

    /**
     * Raises the extension bit of this packet is <tt>extBit</tt> is
     * <tt>true</tt> or set it to <tt>0</tt> if <tt>extBit</tt> is
     * <tt>false</tt>.
     *
     * @param extBit the flag that indicates whether we are to set or clear
     * the extension bit of this packet.
     */
    private void setExtensionBit(boolean extBit)
    {
        if(extBit)
            buffer[offset] |= 0x10;
        else
            buffer[offset] &= 0xEF;
    }

    /**
     * Returns the length of the extensions currently added to this packet.
     *
     * @return the length of the extensions currently added to this packet.
     */
    public int getExtensionLength()
    {
        if (!getExtensionBit())
            return 0;

        //the extension length comes after the RTP header, the CSRC list, and
        //after two bytes in the extension header called "defined by profile"
        int extLenIndex =  offset
                        + FIXED_HEADER_SIZE
                        + getCsrcCount()*4 + 2;

        return ((buffer[extLenIndex] << 4) | buffer[extLenIndex + 1]);
    }

    /**
     * Sets the length of the extensions currently recorded in this packet's
     * buffer.
     *
     * @param length the length of the extensions currently recorded in the
     * buffer of this packet.
     */
    private void setExtensionLength(int length)
    {
        //the extension length comes after the RTP header, the CSRC list, and
        //after two bytes in the extension header called "defined by profile"
        int extLenIndex =  offset
                        + FIXED_HEADER_SIZE
                        + getCsrcCount()*4 + 2;

        buffer[extLenIndex] = (byte)(length >> 4);
        buffer[extLenIndex + 1] = (byte)length;
    }

    /**
     * Adds the <tt>extBuff</tt> buffer to as an extension of this packet
     * according the rules specified in RFC 5285. Note that this method does
     * not replace extensions so if you add the same buffer twice it would be
     * added as to separate extensions.
     *
     * @param extBuff the buffer that we'd like to add as an extension in this
     * packet.
     * @param length the length of the data in extBuff.
     */
    public void addExtension(byte[] extBuff, int length)
    {
        int newBuffLen = getLength() + length;

        //if there was no extension previously, we also need to consider adding
        //the extension header.
        if (!getExtensionBit())
            newBuffLen += EXT_HEADER_SIZE;

        byte[] newBuffer = new byte[ newBuffLen ];

        //copy header and CSRC list any previous extensions if any
        System.arraycopy(buffer, offset, newBuffer, offset,
                         FIXED_HEADER_SIZE
                             + getCsrcCount()*4 + getExtensionLength());

        //raise the extension bit.
        newBuffer[offset] |= 0x10;

        //if there were no extensions previously, we need to add the hdr now
        if(! getExtensionBit())
        {
           // we will now be adding the RFC 5285 ext header which looks like
           // this:
           //
           //  0                   1                   2                   3
           //  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
           // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           // |       0xBE    |    0xDE       |           length=3            |
           // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

           int extHdrOffset = FIXED_HEADER_SIZE + getCsrcCount()*4;
           newBuffer[extHdrOffset]   = (byte)0xBE;
           newBuffer[extHdrOffset+1] = (byte)0xDE;

           int newExtensionLen = length + getExtensionLength();
           newBuffer[extHdrOffset+2] = (byte)(newExtensionLen >>4);
           newBuffer[extHdrOffset+3] = (byte)newExtensionLen;
        }

        //copy the extension content from the new extension.
        System.arraycopy(extBuff, 0, newBuffer, FIXED_HEADER_SIZE
                                    + getCsrcCount()*4 + getExtensionLength(),
                         length);

        //now copy the payload
        int oldPayloadOffset = FIXED_HEADER_SIZE + getCsrcCount()*4
                                                    + getExtensionLength();
        int newPayloadOffset = oldPayloadOffset + length;

        System.arraycopy(buffer, oldPayloadOffset,
                        newBuffer, newPayloadOffset,
                        FIXED_HEADER_SIZE
                            + getCsrcCount()*4 + getExtensionLength());
    }

    /**
     * Returns a bi-dimensional byte array containing a map binding CSRC IDs to
     * audio levels as reported by the remote party that sent this packet.
     *
     * @param csrcExtID the ID of the extension that's transporting csrc audio
     * levels in the session that this <tt>RawPacket</tt> belongs to.
     *
     * @return a bi-dimensional byte array containing a map binding CSRC IDs to
     * audio levels as reported by the remote party that sent this packet.
     */
    public long[][] extractCsrcLevels(byte csrcExtID)
    {
        if( !getExtensionBit() || getExtensionLength() == 0
                        || getCsrcCount() == 0)
            return null;

        int csrcCount = getCsrcCount();
        long[][] csrcLevels = new long[csrcCount][2];

        //first extract the csrc IDs
        int csrcStartIndex = offset + FIXED_HEADER_SIZE;
        for (int i = 0; i < csrcCount; i++)
        {
            csrcLevels[i][0] = buffer[csrcStartIndex]     << 24
                             & buffer[csrcStartIndex + 1] << 16
                             & buffer[csrcStartIndex + 2] << 8
                             & buffer[csrcStartIndex + 3];


            csrcLevels[i][1] = getCsrcLevel(i, csrcExtID);

            csrcStartIndex += 4;
        }

        return csrcLevels;
    }

    /**
     * Returns the CSRC level at the specified index or <tt>0</tt> if there was
     * no level at that index.
     *
     * @param index the sequence number of the CSRC audio level extension to
     * return.
     * @param csrcExtID the ID of the extension that's transporting csrc audio
     * levels in the session that this <tt>RawPacket</tt> belongs to.
     *
     * @return the CSRC audio level at the specified index of the csrc audio
     * level option or <tt>0</tt> if there was no level at that index.
     */
    private int getCsrcLevel(int index, byte csrcExtID)
    {
        if( !getExtensionBit() || getExtensionLength() == 0)
            return 0;

        int levelsStart = findExtension(csrcExtID);
        int levelsCount = getLengthForExtension(levelsStart);

        if(levelsCount < index)
        {
            //apparently the remote side sent more CSRCs than levels.
            // ... yeah remote sides do that now and then ...
            return 0;
        }

        return buffer[levelsStart + index];
    }

    /**
     * Returns the index of the element in this packet's buffer where the
     * content of the header with the specified <tt>extensionID</tt> starts.
     *
     * @param extensionID the ID of the extension whose content we are looking
     * for.
     *
     * @return the index of the first byte of the content of the extension
     * with the specified <tt>extensionID</tt> or -1 if no such extension was
     * found.
     */
    private int findExtension(int extensionID)
    {
        if( !getExtensionBit() || getExtensionLength() == 0)
            return 0;

        int extOffset = FIXED_HEADER_SIZE + getCsrcCount()*4 + EXT_HEADER_SIZE;

        int extensionEnd = extOffset + getExtensionLength();
        int extHdrLen = getExtensionHeaderLength();

        if (extHdrLen != 1 && extHdrLen != 2)
            return -1;

        while (extOffset < extensionEnd)
        {
            int currType = -1;
            int currLen = -1;

            if(extHdrLen == 1)
            {
                //short header. type is in the lefter 4 bits and length is on
                //the right; like this:
                //      0
                //      0 1 2 3 4 5 6 7
                //      +-+-+-+-+-+-+-+-+
                //      |  ID   |  len  |
                //      +-+-+-+-+-+-+-+-+

                currType = buffer[extOffset] >> 4;
                currLen = (buffer[extOffset] & 0x0F) + 1; //add one as per 5285

                //now skip the header
                extOffset ++;
            }
            else
            {
                //long header. type is in the first byte and length is in the
                //second
                //       0                   1
                //       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
                //      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                //      |       ID      |     length    |
                //      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                currType = buffer[extOffset];
                currLen = buffer[extOffset + 1];

                //now skip the header
                extOffset += 2;
            }

            if(currType == extensionID)
                return extOffset;

            extOffset += currLen;
        }

        return -1;
    }

    /**
     * Returns the length of the header extension that is carrying the content
     * starting at <tt>contentStart</tt>. In other words this method checks the
     * size of extension headers in this packet and then either returns the
     * value of the byte right before <tt>contentStart</tt> or its lower 4 bits.
     * This is a very basic method so if you are using it - make sure u know
     * what you are doing.
     *
     * @param contentStart the index of the first element of the content of
     * the extension whose size we are trying to obtain.
     *
     * @return the length of the extension carrying the content starting at
     * <tt>contentStart</tt>.
     */
    private int getLengthForExtension(int contentStart)
    {
        int hdrLen = getExtensionHeaderLength();

        if( hdrLen == 1 )
            return ( buffer[contentStart - 1] & 0x0F ) + 1;
        else
            return buffer[contentStart - 1];
    }

    /**
     * Returns the length of the extension header being used in this packet or
     * <tt>-1</tt> in case there were no extension headers here or we didn't
     * understand the kind of extension being used.
     *
     * @return  the length of the extension header being used in this packet or
     * <tt>-1</tt> in case there were no extension headers here or we didn't
     * understand the kind of extension being used.
     */
    private int getExtensionHeaderLength()
    {
        if (!getExtensionBit())
            return -1;

        //the type of the extension header comes right after the RTP header and
        //the CSRC list.
        int extLenIndex =  offset + FIXED_HEADER_SIZE + getCsrcCount()*4;

        //0xBEDE means short extension header.
        if (buffer[extLenIndex] == 0xBE && buffer[extLenIndex + 1] == 0xDE)
            return 1;

        //0x100 means a two-byte extension header.
        if (buffer[extLenIndex]== 0x10 && (buffer[extLenIndex + 1] >> 4)== 0)
           return 2;

        return -1;
    }
}
