/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.speex;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;

/**
 * @author Lubomir Marinov
 */
public class SpeexResampler
    extends AbstractCodecExt
{
    /**
     * The list of <tt>Format</tt>s of audio data supported as input and output
     * by <tt>SpeexResampler</tt> instances.
     */
    private static final Format[] SUPPORTED_FORMATS;

    /**
     * The list of sample rates of audio data supported as input and output by
     * <tt>SpeexResampler</tt> instances.
     */
    private static final double[] SUPPORTED_SAMPLE_RATES
        = new double[]
                {
                    8000,
                    11025,
                    16000,
                    22050,
                    32000,
                    44100,
                    48000,
                    Format.NOT_SPECIFIED
                };

    static
    {
        Speex.assertSpeexIsFunctional();

        int count = SUPPORTED_SAMPLE_RATES.length;

        SUPPORTED_FORMATS = new Format[count];
        for (int i = 0; i < count; i++)
        {
            SUPPORTED_FORMATS[i]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        SUPPORTED_SAMPLE_RATES[i],
                        16 /* sampleSizeInBits */,
                        1 /* channels */,
                        AudioFormat.LITTLE_ENDIAN,
                        AudioFormat.SIGNED,
                        16,
                        SUPPORTED_SAMPLE_RATES[i],
                        Format.byteArray);
        }
    }

    /**
     * The input sample rate configured in {@link #resampler}.
     */
    private int inputSampleRate;

    /**
     * The output sample rate configured in {@link #resampler}.
     */
    private int outputSampleRate;

    /**
     * The pointer to the native <tt>SpeexResamplerState</tt> which is
     * represented by this instance.
     */
    private long resampler;

    /**
     * Initializes a new <tt>SpeexResampler</tt> instance.
     */
    public SpeexResampler()
    {
        super("Speex Resampler", AudioFormat.class, SUPPORTED_FORMATS);

        inputFormats = SUPPORTED_FORMATS;
    }

    /**
     * @see AbstractCodecExt#doClose()
     */
    protected void doClose()
    {
        if (resampler != 0)
        {
            Speex.speex_resampler_destroy(resampler);
            resampler = 0;
        }
    }

    /**
     * @throws ResourceUnavailableException
     * @see AbstractCodecExt#doOpen()
     */
    protected void doOpen()
        throws ResourceUnavailableException
    {
    }

    /**
     * @param inputBuffer
     * @param outputBuffer
     * @return
     * @see AbstractCodecExt#doProcess(Buffer, Buffer)
     */
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        Format inputFormat = inputBuffer.getFormat();

        if ((inputFormat != null)
                && (inputFormat != this.inputFormat)
                && !inputFormat.equals(this.inputFormat))
        {
            if (null == setInputFormat(inputFormat))
                return BUFFER_PROCESSED_FAILED;
        }
        inputFormat = this.inputFormat;

        AudioFormat inputAudioFormat = (AudioFormat) inputFormat;
        int inputSampleRate = (int) inputAudioFormat.getSampleRate();
        AudioFormat outputAudioFormat = (AudioFormat) getOutputFormat();
        int outputSampleRate = (int) outputAudioFormat.getSampleRate();

        if (inputSampleRate == outputSampleRate)
        {
            // passthrough
            byte[] input = (byte[]) inputBuffer.getData();
            int size = (input == null) ? 0 : input.length;
            byte[] output = validateByteArraySize(outputBuffer, size);

            if ((input != null) && (output != null))
                System.arraycopy(input, 0, output, 0, size);
            outputBuffer.setFormat(inputBuffer.getFormat());
            outputBuffer.setLength(inputBuffer.getLength());
            outputBuffer.setOffset(inputBuffer.getOffset());
        }
        else
        {
            if ((this.inputSampleRate != inputSampleRate)
                    || (this.outputSampleRate != outputSampleRate))
            {
                if (resampler == 0)
                {
                    resampler
                        = Speex.speex_resampler_init(
                                1,
                                inputSampleRate,
                                outputSampleRate,
                                Speex.SPEEX_RESAMPLER_QUALITY_VOIP,
                                0);
                }
                else
                {
                    Speex.speex_resampler_set_rate(
                            resampler,
                            inputSampleRate,
                            outputSampleRate);
                }
                if (resampler != 0)
                {
                    this.inputSampleRate = inputSampleRate;
                    this.outputSampleRate = outputSampleRate;
                }
            }
            if (resampler == 0)
                return BUFFER_PROCESSED_FAILED;

            byte[] input = (byte[]) inputBuffer.getData();
            int inputLength = inputBuffer.getLength();
            int sampleSizeInBytes = inputAudioFormat.getSampleSizeInBits() / 8;
            int inputSampleCount = inputLength / sampleSizeInBytes;
            int outputLength
                = (inputLength * outputSampleRate) / inputSampleRate;
            byte[] output = validateByteArraySize(outputBuffer, outputLength);
            int outputSampleCount = outputLength / sampleSizeInBytes;

            outputSampleCount
                = Speex.speex_resampler_process_interleaved_int(
                        resampler,
                        input, inputBuffer.getOffset(), inputSampleCount,
                        output, 0, outputSampleCount);
            outputBuffer.setFormat(outputAudioFormat);
            outputBuffer.setLength(outputSampleCount * sampleSizeInBytes);
            outputBuffer.setOffset(0);
        }
        outputBuffer.setDuration(inputBuffer.getDuration());
        outputBuffer.setEOM(inputBuffer.isEOM());
        outputBuffer.setFlags(inputBuffer.getFlags());
        outputBuffer.setHeader(inputBuffer.getHeader());
        outputBuffer.setSequenceNumber(inputBuffer.getSequenceNumber());
        outputBuffer.setTimeStamp(inputBuffer.getTimeStamp());
        return BUFFER_PROCESSED_OK;
    }

    /**
     * @param format
     * @return
     * @see AbstractCodecExt#setInputFormat(Format)
     */
    @Override
    public Format setInputFormat(Format format)
    {
        AudioFormat inputFormat = (AudioFormat) super.setInputFormat(format);

        if (inputFormat != null)
        {
            double outputSampleRate
                = (outputFormat == null)
                    ? inputFormat.getSampleRate()
                    : ((AudioFormat) outputFormat).getSampleRate();

            setOutputFormat(
                new AudioFormat(
                        inputFormat.getEncoding(),
                        outputSampleRate,
                        inputFormat.getSampleSizeInBits(),
                        inputFormat.getChannels(),
                        inputFormat.getEndian(),
                        inputFormat.getSigned(),
                        Format.NOT_SPECIFIED,
                        inputFormat.getFrameSizeInBits(),
                        inputFormat.getDataType()));
        }
        return inputFormat;
    }
}
