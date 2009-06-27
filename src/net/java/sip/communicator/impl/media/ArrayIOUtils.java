/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media;

/**
 * @author Lubomir Marinov
 */
public class ArrayIOUtils
{

    /**
     * Reads a short integer from a specific series of bytes starting the
     * reading at a specific offset in it.
     * 
     * @param input
     *            the series of bytes to read the short integer from
     * @param inputOffset
     *            the offset in <code>input</code> at which the reading of the
     *            short integer is to start
     * @return a short integer in the form of
     *         <tt>int</code> read from the specified series of bytes starting at the specified offset in it
     */
    public static int readInt16(byte[] input, int inputOffset)
    {
        return ((input[inputOffset + 1] << 8) | (input[inputOffset] & 0x00FF));
    }

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
     * offset in it.
     * 
     * @param input the short integer to be written out as a series of bytes
     *            specified as an integer i.e. the value to be converted is
     *            contained in only two of the four bytes made available by the
     *            integer
     * @param output the output to receive the conversion of the specified
     *            short integer to a series of bytes
     * @param outputOffset the offset in <code>output</code> at which the
     *            writing of the result of the conversion is to be started
     */
    public static void writeInt16(int input, byte[] output, int outputOffset)
    {
        output[outputOffset] = (byte) (input & 0xFF);
        output[outputOffset + 1] = (byte) (input >> 8);
    }

    public static void writeShort(short input, byte[] output, int outputOffset)
    {
        writeInt16(input, output, outputOffset);
    }
}
