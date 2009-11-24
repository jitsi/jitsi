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
 * The ALAW Encoder
 *
 * @author Damian Minkov
 */
public class JavaEncoder
    extends com.ibm.media.codec.audio.AudioCodec
{
    private Format lastFormat = null;
    private int inputSampleSize;
    private boolean bigEndian = false;

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

    public void open() throws ResourceUnavailableException
    {}

    public void close()
    {}

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

        pcm162alaw(
            inpData, inputBuffer.getOffset(), outData, 0, outData.length, bigEndian);

        updateOutput(outputBuffer, outputFormat, outLength, 0);
        return BUFFER_PROCESSED_OK;
    }

    /*
     * This source code is a product of Sun Microsystems, Inc. and is provided
     * for unrestricted use.  Users may copy or modify this source code without
     * charge.
     *
     * linear2alaw() - Convert a 16-bit linear PCM value to 8-bit A-law
     *
     * linear2alaw() accepts an 16-bit integer and encodes it as A-law data.
     *
     *      Linear Input Code   Compressed Code
     *  ------------------------    ---------------
     *  0000000wxyza            000wxyz
     *  0000001wxyza            001wxyz
     *  000001wxyzab            010wxyz
     *  00001wxyzabc            011wxyz
     *  0001wxyzabcd            100wxyz
     *  001wxyzabcde            101wxyz
     *  01wxyzabcdef            110wxyz
     *  1wxyzabcdefg            111wxyz
     *
     * For further information see John C. Bellamy's Digital Telephony, 1982,
     * John Wiley & Sons, pps 98-111 and 472-476.
     */
    private static final byte QUANT_MASK = 0xf; /* Quantization field mask. */
    private static final byte SEG_SHIFT = 4;
        /* Left shift for segment number. */
    private static final short[] seg_end =
        {
        0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF
    };

    private static byte linear2alaw(short pcm_val)
        /* 2's complement (16-bit range) */
    {
        byte mask;
        byte seg = 8;
        byte aval;

        if (pcm_val >= 0)
        {
            mask = (byte) 0xD5; /* sign (7th) bit = 1 */
        }
        else
        {
            mask = 0x55; /* sign bit = 0 */
            pcm_val = (short) ( -pcm_val - 8);
        }

        /* Convert the scaled magnitude to segment number. */
        for (int i = 0; i < 8; i++)
        {
            if (pcm_val <= seg_end[i])
            {
                seg = (byte) i;
                break;
            }
        }

        /* Combine the sign, segment, and quantization bits. */
        if (seg >= 8) /* out of range, return maximum value. */
        {
            return (byte) ( (0x7F ^ mask) & 0xFF);
        }
        else
        {
            aval = (byte) (seg << SEG_SHIFT);
            if (seg < 2)
            {
                aval |= (pcm_val >> 4) & QUANT_MASK;
            }
            else
            {
                aval |= (pcm_val >> (seg + 3)) & QUANT_MASK;
            }
            return (byte) ( (aval ^ mask) & 0xFF);
        }
    }

    /**
     * Converts the input buffer to the output one using the alaw codec
     * @param inBuffer byte[]
     * @param inByteOffset int
     * @param outBuffer byte[]
     * @param outByteOffset int
     * @param sampleCount int
     * @param bigEndian boolean
     */
    private static void pcm162alaw(byte[] inBuffer, int inByteOffset,
                                  byte[] outBuffer, int outByteOffset,
                                  int sampleCount, boolean bigEndian)
    {
        int shortIndex = inByteOffset;
        int alawIndex = outByteOffset;
        if (bigEndian)
        {
            while (sampleCount > 0)
            {
                outBuffer[alawIndex++] = linear2alaw
                    (bytesToShort16(inBuffer[shortIndex],
                                    inBuffer[shortIndex + 1]));
                shortIndex++;
                shortIndex++;
                sampleCount--;
            }
        }
        else
        {
            while (sampleCount > 0)
            {
                outBuffer[alawIndex++] = linear2alaw
                    (bytesToShort16(inBuffer[shortIndex + 1],
                                    inBuffer[shortIndex]));
                shortIndex++;
                shortIndex++;
                sampleCount--;
            }
        }
    }

    /**
     * Converts the 2 bytes to the corresponding short value
     * @param highByte byte
     * @param lowByte byte
     * @return short
     */
    private static short bytesToShort16(byte highByte, byte lowByte)
    {
        return (short) ( (highByte << 8) | (lowByte & 0xFF));
    }
}
