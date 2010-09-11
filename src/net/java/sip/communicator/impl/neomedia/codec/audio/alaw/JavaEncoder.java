/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.alaw;

import javax.media.*;
import javax.media.format.*;

/**
 * The ALAW Encoder. Used the FMJ ALawEncoderUtil.
 *
 * @author Damian Minkov
 */
public class JavaEncoder
    extends com.ibm.media.codec.audio.AudioCodec
{
    /**
     * The last used format.
     */
    private Format lastFormat = null;

    /**
     * The input sample size in bits.
     */
    private int inputSampleSize;

    /**
     * The byte order.
     */
    private boolean bigEndian = false;

    /**
     * Constructs the encoder and init the supported formats.
     */
    public JavaEncoder()
    {
        supportedInputFormats = new AudioFormat[]
            {
            new AudioFormat(
                AudioFormat.LINEAR,
                Format.NOT_SPECIFIED,
                16,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
            ),
            new AudioFormat(
                AudioFormat.LINEAR,
                Format.NOT_SPECIFIED,
                8,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
            )}; // support 1 channel and 8/16 bit samples

        defaultOutputFormats = new AudioFormat[]
            {
            new AudioFormat(
                AudioFormat.ALAW,
                8000,
                8,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
            )};
        PLUGIN_NAME = "pcm to alaw converter";
    }

    /**
     * Returns the output formats according to the input.
     * @param in the input format.
     * @return the possible output formats.
     */
    protected Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat inFormat = (AudioFormat) in;
        int sampleRate = (int) (inFormat.getSampleRate());

        supportedOutputFormats = new AudioFormat[]
            {
            new AudioFormat(
                AudioFormat.ALAW,
                sampleRate,
                8,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED
            )};

        return supportedOutputFormats;
    }

    /**
     * No resources to be opened.
     * @throws ResourceUnavailableException if open failed (which cannot
     * happend for this codec since no resources are to be opened)
     */
    public void open() throws ResourceUnavailableException
    {}

    /**
     * No resources used to be cleared.
     */
    public void close()
    {}

    /**
     * Calculate the output data size.
     * @param inputLength input length.
     * @return the output data size.
     */
    private int calculateOutputSize(int inputLength)
    {
        if (inputSampleSize == 16)
        {
            inputLength /= 2;
        }

        return inputLength;
    }

    /**
     * Init the converter to the new format
     * @param inFormat AudioFormat
     */
    private void initConverter(AudioFormat inFormat)
    {
        lastFormat = inFormat;
        inputSampleSize = inFormat.getSampleSizeInBits();

        bigEndian = inFormat.getEndian()==AudioFormat.BIG_ENDIAN;
    }

    /**
     * Encodes the input buffer passing it to the output one
     * @param inputBuffer Buffer
     * @param outputBuffer Buffer
     * @return int
     */
    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        if (!checkInputBuffer(inputBuffer))
        {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(inputBuffer))
        {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        Format newFormat = inputBuffer.getFormat();

        if (lastFormat != newFormat)
        {
            initConverter( (AudioFormat) newFormat);
        }

        if (inputBuffer.getLength() == 0)
        {
            return OUTPUT_BUFFER_NOT_FILLED;
        }

        int outLength = calculateOutputSize(inputBuffer.getLength());

        byte[] inpData = (byte[]) inputBuffer.getData();
        byte[] outData = validateByteArraySize(outputBuffer, outLength);

        aLawEncode(bigEndian,
            inpData, inputBuffer.getOffset(), inputBuffer.getLength(),
            outData);

        updateOutput(outputBuffer, outputFormat, outLength, 0);
        return BUFFER_PROCESSED_OK;
    }

    /**
     * maximum that can be held in 15 bits
     */
    public static final int MAX = 0x7fff;

    /**
     *  An array where the index is the 16-bit PCM input, and the value is
     *  the a-law result.
     */
    private static byte[] pcmToALawMap;

    static
    {
        pcmToALawMap = new byte[65536];
        for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++)
            pcmToALawMap[uShortToInt((short) i)] = encode(i);
    }

    /**
     * 65535
     */
    public static final int MAX_USHORT = (Short.MAX_VALUE) * 2 + 1;

    /**
     * Unsigned short to integer.
     * @param value unsigned short.
     * @return integer.
     */
    public static int uShortToInt(short value)
    {
        if (value >= 0)
            return value;
        else
            return MAX_USHORT + 1 + value;
    }

    /**
     *  Encode an array of pcm values into a pre-allocated target array
     *
     * @param bigEndian the data byte order.
     * @param data An array of bytes in Little-Endian format
     * @param target A pre-allocated array to receive the A-law bytes.
     *      This array must be at least half the size of the source.
     * @param offset of the input.
     * @param length of the input.
     */
    public static void aLawEncode(boolean bigEndian,
        byte[] data, int offset, int length, byte[] target)
    {
        if (bigEndian)
            aLawEncodeBigEndian(data, offset, length, target);
        else
            aLawEncodeLittleEndian(data, offset, length, target);
     }

    /**
     * Encode little endian data.
     * @param data the input data.
     * @param offset the input offset.
     * @param length the input length.
     * @param target the target array to fill.
     */
    public static void aLawEncodeLittleEndian(byte[] data,
        int offset, int length, byte[] target)
    {
        int size = length / 2;
        for (int i = 0; i < size; i++)
            target[i] =
                aLawEncode(
                    ((data[offset + 2 * i + 1] & 0xff) << 8)
                    | (data[offset + 2 * i]) & 0xff);
    }

    /**
     * Encode big endian data.
     * @param data the input data.
     * @param offset the input offset.
     * @param length the input length.
     * @param target the target array to fill.
     */
    public static void aLawEncodeBigEndian(byte[] data, int offset, int length, byte[] target)
    {
        int size = length / 2;
        for (int i = 0; i < size; i++)
            target[i] = aLawEncode(((data[offset + 2 * i + 1]) & 0xff) | ((data[offset + 2 * i] & 0xff) << 8));
    }

    /**
     *  Encode a pcm value into a a-law byte
     *
     *  @param pcm A 16-bit pcm value
     *  @return A a-law encoded byte
     */
    public static byte aLawEncode(int pcm)
    {
        return pcmToALawMap[uShortToInt((short) (pcm & 0xffff))];
    }

    /**
     *  Encode one a-law byte from a 16-bit signed integer. Internal use only.
     *
     *  @param pcm A 16-bit signed pcm value
     *  @return A a-law encoded byte
     */
    private static byte encode(int pcm)
    {
        //Get the sign bit.  Shift it for later use without further modification
        int sign = (pcm & 0x8000) >> 8;
        //If the number is negative, make it positive (now it's a magnitude)
        if (sign != 0)
            pcm = -pcm;
        //The magnitude must fit in 15 bits to avoid overflow
        if (pcm > MAX) pcm = MAX;

        /* Finding the "exponent"
         * Bits:
         * 1 2 3 4 5 6 7 8 9 A B C D E F G
         * S 7 6 5 4 3 2 1 0 0 0 0 0 0 0 0
         * We want to find where the first 1 after the sign bit is.
         * We take the corresponding value from
         * the second row as the exponent value.
         * (i.e. if first 1 at position 7 -> exponent = 2)
         * The exponent is 0 if the 1 is not found in bits 2 through 8.
         * This means the exponent is 0 even if the "first 1" doesn't exist.
         */
        int exponent = 7;
        //Move to the right and decrement exponent until
        //we hit the 1 or the exponent hits 0
        for (int expMask = 0x4000;
            (pcm & expMask) == 0 && exponent>0;
            exponent--, expMask >>= 1) { }

        /* The last part - the "mantissa"
         * We need to take the four bits after the 1 we just found.
         * To get it, we shift 0x0f :
         * 1 2 3 4 5 6 7 8 9 A B C D E F G
         * S 0 0 0 0 0 1 . . . . . . . . . (say that exponent is 2)
         * . . . . . . . . . . . . 1 1 1 1
         * We shift it 5 times for an exponent of two, meaning
         * we will shift our four bits (exponent + 3) bits.
         * For convenience, we will actually just shift the number,
         * then AND with 0x0f.
         *
         * NOTE: If the exponent is 0:
         * 1 2 3 4 5 6 7 8 9 A B C D E F G
         * S 0 0 0 0 0 0 0 Z Y X W V U T S (we know nothing about bit 9)
         * . . . . . . . . . . . . 1 1 1 1
         * We want to get ZYXW, which means a shift of 4 instead of 3
         */
        int mantissa = (pcm >> ((exponent == 0) ? 4 : (exponent + 3))) & 0x0f;

        //The a-law byte bit arrangement is SEEEMMMM
        //(Sign, Exponent, and Mantissa.)
        byte alaw = (byte)(sign | exponent << 4 | mantissa);

        //Last is to flip every other bit, and the sign bit (0xD5 = 1101 0101)
        return (byte)(alaw^0xD5);
    }
}
