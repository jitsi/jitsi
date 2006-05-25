/*
 *  Copyright (c) 2003, The Joust Project
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *  - Neither the name of the Joust Project nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 *
 *  File created by jkohen @ Oct 19, 2003
 *  Most code copied from the LEBinaryTools class in the same package.
 */

package net.java.sip.communicator.impl.protocol.icq.message.common;

import java.io.IOException;
import java.io.OutputStream;
import net.kano.joscar.ByteBlock;

/**
 * Provides a set of methods for converting binary data sent over an OSCAR
 * connection to various logical structures. The methods of this class work with
 * little-endian data, which is mainly required by the old-ICQ commands in family 21
 * of SNAC packets. For regular packets use {@link LEBinaryTools} instead.
 *<br>
 * <br>
 * Note that for all types such that <code>get<i>[type]</i></code> and
 * <code>write<i>[type]</i></code> exist, the two methods are inverses:
 * calling <code>getLong</code> on a block written by <codE>writeLong</code>
 * will return the original value written.
 * <br>
 * <br>
 * The methods in this class attempt to be very robust: in the cases of the
 * four-, two-, and one-byte unsigned integers (as in {@link #writeUShort}),
 * if the given value is too large to fit into that size a block, it will be
 * wrapped (for example, <code>writeUShort(out, 65537)</code> would be written
 * to the stream as if <code>writeUShort(out, 1)</code> had been called). See
 * {@link #UINT_MAX}, {@link #USHORT_MAX}, and {@link #UBYTE_MAX} for the
 * maximum values such methods can take without wrapping. Note that those values
 * are <b>not</b> appropriate for the right-hand side of of a modulo operation
 * (like <code>65537 % LELEBinaryTools.USHORT_MAX</code>), as these are
 * <b>maxima</b>. (An appropriate operation, if one were necessary, would be
 * <code>65537 % (LELEBinaryTools.USHORT_MAX + 1)</code>.)
 * <br>
 * <br>
 * Another area in which the methods attempt to be robust is that none of
 * these methods declare any exceptions. If a
 * <code>get<i>[type]</i>(ByteBlock, int)</code> method is
 * passed a byte block that is too small, it simply returns <code>-1</code>
 * (this is okay because these methods only return unsigned values).
 * <br>
 * <br>
 * Lastly, it is important to note that all numbers transferred over OSCAR in
 * <i>unsigned</i> format. Why do <code>getLong</code> and
 * <code>writeLong</code> exist, you may ask, when they work with <i>signed</i>
 * <code>long</code>s? Let me start by saying that nowhere in the OSCAR protocol
 * is an eight-byte integer used. Why do these methods exist at all?, you may
 * now ask. The answer is simple: IM and Rendezvous ID's are eight bytes,
 * and are effectively represented as Java's <code>long</code>. Whether these
 * values are read as signed or unsigned matters not, as the <code>long</code>
 * is only an internal representation just as a <code>ByteBlock</code> is an
 * internal representation of a block of bytes. I hope that explanation was
 * clear enough.
 *
 * @see LEBinaryTools
 * @see net.kano.joscar.OscarTools
 */
public final class LEBinaryTools
{
    /**
     * Represents the largest value a four-byte unsigned integer can have. All
     * numbers sent over OSCAR are unsigned.
     */
    public static final long UINT_MAX = 4294967295L;

    /**
     * Represents the largest value a two-byte unsigned integer can have. All
     * numbers sent over OSCAR are unsigned.
     */
    public static final int USHORT_MAX = 65535;

    /**
     * Represents the largest value a one-byte unsigned integer can have. All
     * numbers sent over OSCAR are unsigned.
     */
    public static final short UBYTE_MAX = 255;

    /**
     * This is never called, and ensures an instance of <code>LEBinaryTools</code>
     * cannot exist.
     */
    private LEBinaryTools()
    {}

    /**
     * Returns the unsigned integer stored in the given data block. The
     * returned integer is extracted from the first four bytes of the given
     * block, starting at index <code>pos</code>. If there are fewer than four
     * bytes at <code>pos</code>, <code>-1</code> is returned.
     * <br>
     * <br>
     * Note that this is returned
     * as a <code>long</code> because all values of an unsigned four-byte
     * integer cannot be stored in a four-byte signed integer, Java's
     * <code>int</code>.
     *
     * @param data the data block from which to read
     * @param pos the starting index of <code>data</code> to read from
     * @return the value of the unsigned four-byte integer stored in the given
     *         block at the given index, or <code>-1</code> if fewer than four
     *         bytes are present in the block
     *
     * @see #writeUInt
     * @see #getUInt(long)
     */
    public static long getUInt(final ByteBlock data, final int pos)
    {
        if (data.getLength() - pos < 4)
        {
            return -1;
        }

        return ( ( (long) data.get(pos + 3) & 0xffL) << 24)
            | ( ( (long) data.get(pos + 2) & 0xffL) << 16)
            | ( ( (long) data.get(pos + 1) & 0xffL) << 8)
            | ( (long) data.get(pos) & 0xffL);
    }

    /**
     * Returns an unsigned two-byte integer stored in the given block. The
     * returned integer is extracted from the first two bytes of the given
     * block, starting at index <code>pos</code>. If there are fewer than two
     * bytes at <code>pos</code>, <code>-1</code> is returned.
     * <br>
     * <br>
     * Note that this is returned as an <code>int</code> because all values of
     * an unsigned two-byte integer cannot be stored in a signed short, Java's
     * <code>short</code>.
     *
     * @param data the data block from which to read
     * @param pos the starting index of <code>data</code> to read from
     * @return the value of the two-byte integer stored at the given index of
     *         the given block, or <code>-1</code> if fewer than two bytes exist
     *         at that index
     *
     * @see #writeUShort
     * @see #getUShort(int)
     */
    public static int getUShort(final ByteBlock data, final int pos)
    {
        if (data.getLength() - pos < 2)
        {
            return -1;
        }

        return ( (data.get(pos + 1) & 0xff) << 8) | (data.get(pos) & 0xff);
    }

    /**
     * Returns an unsigned one-byte integer stored in the given block. The
     * returned integer is extracted from the byte of the given block at index
     * <code>pos</code>. If there is no byte at <code>pos</code>,
     * <code>-1</code> is returned.
     * <br>
     * <br>
     * Note that this is returned as a <code>short</code> because all values of
     * an unsigned one-byte integer cannot be stored in a signed byte, Java's
     * <code>byte</code>.
     *
     * @param data the data block to read from
     * @param pos the index in <code>data</code> of the byte to read
     * @return the value of the single-byte integer stored at the given index
     *         of the given block, or <code>-1</code> if there is no byte at
     *         that index (that is, if <code>data.getLength() <= pos</code>)
     *
     * @see #writeUByte
     * @see #getUByte(int)
     */
    public static short getUByte(final ByteBlock data, final int pos)
    {
        if (data.getLength() - pos < 1)
        {
            return -1;
        }

        return (short) (data.get(pos) & 0xff);
    }

    /**
     * Writes a block of four bytes representing the given unsigned value to
     * the given stream.
     *
     * @param out the stream to write to
     * @param number the value to write to the stream in binary format
     * @throws IOException if an I/O error occurs
     *
     * @see #getUInt(long)
     * @see #getUInt(ByteBlock, int)
     */
    public static void writeUInt(final OutputStream out, final long number) throws
        IOException
    {
        out.write(getUInt(number));
    }

    /**
     * Writes a block of two bytes representing the given unsigned value to the
     * given stream.
     *
     * @param out the stream to write to
     * @param number the value to write to the stream in binary format
     * @throws IOException if an I/O error occurs
     *
     * @see #getUShort(int)
     * @see #getUShort(ByteBlock, int)
     */
    public static void writeUShort(final OutputStream out, final int number) throws
        IOException
    {
        out.write(getUShort(number));
    }

    /**
     * Writes a single (unsigned) byte representing the given unsigned value
     * to the given stream.
     *
     * @param out the stream to write to
     * @param number the value to write to the stream in binary format
     * @throws IOException if an I/O error occurs
     *
     * @see #getUByte(int)
     * @see #getUByte(ByteBlock, int)
     */
    public static void writeUByte(final OutputStream out, final int number) throws
        IOException
    {
        out.write(getUByte(number));
    }

    /**
     * Returns a four-byte block representing the given unsigned value in binary
     * format.
     *
     * @param number the value to be written to the returned array
     * @return a four-byte binary representation of the given unsigned value
     *
     * @see #writeUInt
     * @see #getUInt(ByteBlock, int)
     */
    public static byte[] getUInt(final long number)
    {
        return new byte[]
            {
            (byte) ( (number) & 0xff),
            (byte) ( (number >> 8) & 0xff),
            (byte) ( (number >> 16) & 0xff),
            (byte) ( (number >> 24) & 0xff)
        };
    }

    /**
     * Returns a two-byte block representing the given unsigned value in binary
     * format.
     *
     * @param number the value to be written to the returned array
     * @return a two-byte binary representation of the given unsigned value
     *
     * @see #writeUShort
     * @see #getUShort(ByteBlock, int)
     */
    public static byte[] getUShort(final int number)
    {
        return new byte[]
            {
            (byte) (number & 0xff),
            (byte) ( (number >> 8) & 0xff)
        };
    }

    /**
     * Returns a single-byte block representing the given unsigned value in
     * binary format.
     *
     * @param number the value to be written to the returned array
     * @return a one-byte binary representation of the given unsigned value
     *
     * @see #writeUByte
     * @see #getUByte(ByteBlock, int)
     */
    public static byte[] getUByte(final int number)
    {
        return new byte[]
            {
            (byte) (number & 0xff)};
    }
}
