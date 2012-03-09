/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.ulaw;

import javax.media.*;
import javax.media.format.*;

public class JavaEncoder
    extends com.ibm.media.codec.audio.AudioCodec
{
    private boolean downmix = false;

    private int inputBias;

    private int inputSampleSize;

    private Format lastFormat = null;

    private int lsbOffset;

    private int msbOffset;

    private int numberOfInputChannels;

    private int numberOfOutputChannels = 1;

    private int signMask;

    public JavaEncoder()
    {
        supportedInputFormats = new AudioFormat[] {
                new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 16,
                        1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED),
                new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 16,
                        2, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED),
                new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 8, 1,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED),
                new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 8, 2,
                        Format.NOT_SPECIFIED, Format.NOT_SPECIFIED) }; // support
                                                                       // 1/2
                                                                       // channels
                                                                       // and
                                                                       // 8/16
                                                                       // bit
                                                                       // samples

        defaultOutputFormats = new AudioFormat[] { new AudioFormat(
                AudioFormat.ULAW, 8000, 8, 1, Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED) };
        PLUGIN_NAME = "pcm to mu-law converter";
    }

    private int calculateOutputSize(int inputLength)
    {
        if (inputSampleSize == 16)
            inputLength /= 2;

        if (downmix)
            inputLength /= 2;

        return inputLength;
    }

    private void convert(byte[] input, int inputOffset, int inputLength,
            byte[] outData, int outputOffset)
    {
        int sample, signBit, inputSample;
        int i;

        for (i = inputOffset + msbOffset; i < (inputLength + inputOffset);)
        {

            if (8 == inputSampleSize)
            {
                inputSample = input[i++] << 8;

                if (downmix)
                    inputSample
                        = ((inputSample & signMask)
                                + ((input[i++] << 8) & signMask))
                            >> 1;
            } else
            {
                inputSample = (input[i] << 8) + (0xff & input[i + lsbOffset]);
                i += 2;

                if (downmix)
                {
                    inputSample
                        = ((inputSample & signMask) + (((input[i] << 8)
                                + (0xff & input[i + lsbOffset])) & signMask))
                            >> 1;
                    i += 2;
                }
            }

            sample = (int) ((short) (inputSample + inputBias));

            if (sample >= 0)
            {
                signBit = 0x80; // sign bit
            }
            else
            {
                sample = -sample;
                signBit = 0x00;
            }

            sample = (132 + sample) >> 3; // bias

            outData[outputOffset++] = (byte) ((sample < 0x0020) ? (signBit
                    | (7 << 4) | (31 - (sample >> 0)))
                    : (sample < 0x0040) ? (signBit | (6 << 4) | (31 - (sample >> 1)))
                            : (sample < 0x0080) ? (signBit | (5 << 4) | (31 - (sample >> 2)))
                                    : (sample < 0x0100) ? (signBit | (4 << 4) | (31 - (sample >> 3)))
                                            : (sample < 0x0200) ? (signBit
                                                    | (3 << 4) | (31 - (sample >> 4)))
                                                    : (sample < 0x0400) ? (signBit
                                                            | (2 << 4) | (31 - (sample >> 5)))
                                                            : (sample < 0x0800) ? (signBit
                                                                    | (1 << 4) | (31 - (sample >> 6)))
                                                                    : (sample < 0x1000) ? (signBit
                                                                            | (0 << 4) | (31 - (sample >> 7)))
                                                                            : (signBit
                                                                                    | (0 << 4) | (31 - (0xfff >> 7))));
        }

    }

    @Override
    protected Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat inFormat = (AudioFormat) in;
        int channels = inFormat.getChannels();
        int sampleRate = (int) (inFormat.getSampleRate());

        if (channels == 2)
        {
            supportedOutputFormats = new AudioFormat[] {
                    new AudioFormat(AudioFormat.ULAW, sampleRate, 8, 2,
                            Format.NOT_SPECIFIED, Format.NOT_SPECIFIED),
                    new AudioFormat(AudioFormat.ULAW, sampleRate, 8, 1,
                            Format.NOT_SPECIFIED, Format.NOT_SPECIFIED) };

        }
        else
        {
            supportedOutputFormats = new AudioFormat[] { new AudioFormat(
                    AudioFormat.ULAW, sampleRate, 8, 1, Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED) };

        }
        return supportedOutputFormats;
    }

    private void initConverter(AudioFormat inFormat)
    {
        lastFormat = inFormat;
        numberOfInputChannels = inFormat.getChannels();
        if (outputFormat != null)
            numberOfOutputChannels = outputFormat.getChannels();
        inputSampleSize = inFormat.getSampleSizeInBits();

        if ((inFormat.getEndian() == AudioFormat.BIG_ENDIAN)
                || (8 == inputSampleSize))
        {
            lsbOffset = 1;
            msbOffset = 0;
        }
        else
        {
            lsbOffset = -1;
            msbOffset = 1;
        }

        if (inFormat.getSigned() == AudioFormat.SIGNED)
        {
            inputBias = 0;
            signMask = 0xffffffff;
        }
        else
        {
            inputBias = 32768;
            signMask = 0x0000ffff;
        }

        downmix
            = (numberOfInputChannels == 2) && (numberOfOutputChannels == 1);
    }

    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        if (!checkInputBuffer(inputBuffer))
            return BUFFER_PROCESSED_FAILED;
        if (isEOM(inputBuffer))
        {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        Format newFormat = inputBuffer.getFormat();

        if (lastFormat != newFormat)
            initConverter((AudioFormat) newFormat);

        int inpLength = inputBuffer.getLength();
        int outLength = calculateOutputSize(inputBuffer.getLength());

        byte[] inpData = (byte[]) inputBuffer.getData();
        byte[] outData = validateByteArraySize(outputBuffer, outLength);

        convert(inpData, inputBuffer.getOffset(), inpLength, outData, 0);

        updateOutput(outputBuffer, outputFormat, outLength, 0);
        return BUFFER_PROCESSED_OK;
    }
}
