/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

/**
 * Implements functionality aiding the reading and writing of <tt>byte</tt>
 * arrays and primitive types such as <tt>short</tt>.
 *
 * @author Lubomir Marinov
 */
public class ArrayIOUtils
{

    /**
     * Reads a short integer from a specific series of bytes starting the
     * reading at a specific offset in it. The difference with
     * {@link #readShort(byte[], int)} is that the read short integer is an
     * <tt>int</tt> which has been formed by reading two bytes, not a
     * <tt>short</tt>.
     * 
     * @param input the series of bytes to read the short integer from
     * @param inputOffset the offset in <tt>input</tt> at which the reading of
     * the short integer is to start
     * @return a short integer in the form of <tt>int</tt> read from the
     * specified series of bytes starting at the specified offset in it
     */
    public static int readInt16(byte[] input, int inputOffset)
    {
        return ((input[inputOffset + 1] << 8) | (input[inputOffset] & 0x00FF));
    }

    /**
     * Reads a short integer from a specific series of bytes starting the
     * reading at a specific offset in it.
     *
     * @param input the series of bytes to read the short integer from
     * @param inputOffset the offset in <tt>input</tt> at which the reading of
     * the short integer is to start
     * @return a short integer in the form of <tt>short</tt> read from the
     * specified series of bytes starting at the specified offset in it
     */
    public static short readShort(byte[] input, int inputOffset)
    {
        return
            (short)
                ((input[inputOffset + 1] << 8)
                    | (input[inputOffset] & 0x00FF));
    }

    /**
     * Converts a short integer to a series of bytes and writes the result into
     * a specific output array of bytes starting the writing at a specific
     * offset in it. The difference with {@link #writeShort(short, byte[], int)}
     * is that the input is an <tt>int</tt> and just two bytes of it are
     * written.
     * 
     * @param input the short integer to be written out as a series of bytes
     * specified as an integer i.e. the value to be converted is contained in
     * only two of the four bytes made available by the integer
     * @param output the output to receive the conversion of the specified short
     * integer to a series of bytes
     * @param outputOffset the offset in <tt>output</tt> at which the writing of
     * the result of the conversion is to be started
     */
    public static void writeInt16(int input, byte[] output, int outputOffset)
    {
        output[outputOffset] = (byte) (input & 0xFF);
        output[outputOffset + 1] = (byte) (input >> 8);
    }

    /**
     * Converts a short integer to a series of bytes and writes the result into
     * a specific output array of bytes starting the writing at a specific
     * offset in it.
     *
     * @param input the short integer to be written out as a series of bytes
     * specified as <tt>short</tt>
     * @param output the output to receive the conversion of the specified short
     * integer to a series of bytes
     * @param outputOffset the offset in <tt>output</tt> at which the writing of
     * the result of the conversion is to be started
     */
    public static void writeShort(short input, byte[] output, int outputOffset)
    {
        writeInt16(input, output, outputOffset);
    }
}
