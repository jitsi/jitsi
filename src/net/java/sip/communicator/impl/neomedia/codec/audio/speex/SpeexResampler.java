/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.speex;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.sf.fmj.media.*;

/**
 * Implements an audio resampler using Speex.
 *
 * @author Lyubomir Marinov
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
                    12000,
                    16000,
                    22050,
                    24000,
                    32000,
                    44100,
                    48000,
                    Format.NOT_SPECIFIED
                };

    static
    {
        Speex.assertSpeexIsFunctional();

        int supportedCount = SUPPORTED_SAMPLE_RATES.length;

        SUPPORTED_FORMATS = new Format[2 * supportedCount];
        for (int i = 0; i < supportedCount; i++)
        {
            int j = 2 * i;

            SUPPORTED_FORMATS[j]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        SUPPORTED_SAMPLE_RATES[i],
                        16 /* sampleSizeInBits */,
                        1 /* channels */,
                        AudioFormat.LITTLE_ENDIAN,
                        AudioFormat.SIGNED,
                        Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED,
                        Format.byteArray);
            SUPPORTED_FORMATS[j + 1]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        SUPPORTED_SAMPLE_RATES[i],
                        16 /* sampleSizeInBits */,
                        1 /* channels */,
                        AudioFormat.LITTLE_ENDIAN,
                        AudioFormat.SIGNED,
                        Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED,
                        Format.shortArray);
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
     * Opens this <tt>Codec</tt> and acquires the resources that it needs to
     * operate. A call to {@link PlugIn#open()} on this instance will result in
     * a call to <tt>doOpen</tt> only if {@link AbstractCodec#opened} is
     * <tt>false</tt>. All required input and/or output formats are assumed to
     * have been set on this <tt>Codec</tt> before <tt>doOpen</tt> is called.
     *
     * @throws ResourceUnavailableException if any of the resources that this
     * <tt>Codec</tt> needs to operate cannot be acquired
     * @see AbstractCodecExt#doOpen()
     */
    protected void doOpen()
        throws ResourceUnavailableException
    {
    }

    /**
     * Resamples audio from a specific input <tt>Buffer</tt> into a specific
     * output <tt>Buffer</tt>.
     *
     * @param inputBuffer input <tt>Buffer</tt>
     * @param outputBuffer output <tt>Buffer</tt>
     * @return <tt>BUFFER_PROCESSED_OK</tt> if <tt>inputBuffer</tt> has been
     * successfully processed
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
            Class<?> inputDataType = inputAudioFormat.getDataType();
            Class<?> outputDataType = outputAudioFormat.getDataType();

            if (Format.byteArray.equals(inputDataType))
            {
                byte[] input = (byte[]) inputBuffer.getData();

                if (Format.byteArray.equals(outputDataType))
                {
                    int length = (input == null) ? 0 : input.length;
                    byte[] output = validateByteArraySize(outputBuffer, length);

                    if ((input != null) && (output != null))
                        System.arraycopy(input, 0, output, 0, length);
                    outputBuffer.setFormat(inputBuffer.getFormat());
                    outputBuffer.setLength(inputBuffer.getLength());
                    outputBuffer.setOffset(inputBuffer.getOffset());
                }
                else
                {
                    int inputLength = inputBuffer.getLength();
                    int outputOffset = 0;
                    int outputLength = inputLength / 2;
                    short[] output
                        = validateShortArraySize(outputBuffer, outputLength);

                    for (int i = inputBuffer.getOffset(), o = outputOffset;
                            o < outputLength;
                            o++)
                    {
                        output[o]
                            = (short)
                                (((input[i++] & 0xFF)
                                        | (input[i++] & 0xFF) << 8));
                    }
                    outputBuffer.setFormat(outputAudioFormat);
                    outputBuffer.setLength(outputLength);
                    outputBuffer.setOffset(outputOffset);
                }
            }
            else
            {
                short[] input = (short[]) inputBuffer.getData();

                if (Format.byteArray.equals(outputDataType))
                {
                    int inputLength = inputBuffer.getLength();
                    int outputOffset = 0;
                    int outputLength = inputLength * 2;
                    byte[] output
                        = validateByteArraySize(outputBuffer, outputLength);

                    for (int i = inputBuffer.getOffset(), o = outputOffset;
                            o < outputLength;
                            i++)
                    {
                        short s = input[i];

                        output[o++] = (byte) (s & 0x00FF);
                        output[o++] = (byte) ((s & 0xFF00) >>> 8);
                    }
                    outputBuffer.setFormat(outputAudioFormat);
                    outputBuffer.setLength(outputLength);
                    outputBuffer.setOffset(outputOffset);
                }
                else
                {
                    int length = (input == null) ? 0 : input.length;
                    short[] output
                        = validateShortArraySize(outputBuffer, length);

                    if ((input != null) && (output != null))
                        System.arraycopy(input, 0, output, 0, length);
                    outputBuffer.setFormat(inputBuffer.getFormat());
                    outputBuffer.setLength(inputBuffer.getLength());
                    outputBuffer.setOffset(inputBuffer.getOffset());
                }
            }
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
     * Get the output formats matching a specific input format.
     *
     * @param inputFormat the input format to get the matching output formats of
     * @return the output formats matching the specified input format
     * @see AbstractCodecExt#getMatchingOutputFormats(Format)
     */
    @Override
    protected Format[] getMatchingOutputFormats(Format inputFormat)
    {
        Class<?> inputFormatDataType = inputFormat.getDataType();
        List<Format> matchingOutputFormats = new ArrayList<Format>();

        for (Format supportedFormat : SUPPORTED_FORMATS)
        {
            if ((Format.byteArray.equals(supportedFormat.getDataType())
                        && Format.byteArray.equals(inputFormatDataType))
                    || (((AudioFormat) supportedFormat).getSampleRate()
                            == ((AudioFormat) inputFormat).getSampleRate()))
                matchingOutputFormats.add(supportedFormat);
        }
        return
            matchingOutputFormats.toArray(
                    new Format[matchingOutputFormats.size()]);
    }

    /**
     * Sets the <tt>Format</tt> of the media data to be input for processing in
     * this <tt>Codec</tt>.
     *
     * @param format the <tt>Format</tt> of the media data to be input for
     * processing in this <tt>Codec</tt>
     * @return the <tt>Format</tt> of the media data to be input for processing
     * in this <tt>Codec</tt> if <tt>format</tt> is compatible with this
     * <tt>Codec</tt>; otherwise, <tt>null</tt>
     * @see AbstractCodecExt#setInputFormat(Format)
     */
    @Override
    public Format setInputFormat(Format format)
    {
        AudioFormat inputFormat = (AudioFormat) super.setInputFormat(format);

        if (inputFormat != null)
        {
            double outputSampleRate;
            Class<?> outputDataType;

            if (outputFormat == null)
            {
                outputSampleRate = inputFormat.getSampleRate();
                outputDataType = inputFormat.getDataType();
            }
            else
            {
                AudioFormat outputAudioFormat = (AudioFormat) outputFormat;

                outputSampleRate = outputAudioFormat.getSampleRate();
                outputDataType = outputAudioFormat.getDataType();
                /*
                 * Conversion between data types is only supported when not
                 * resampling but rather passing through.
                 */
                if (outputSampleRate != inputFormat.getSampleRate())
                    outputDataType = inputFormat.getDataType();
            }

            setOutputFormat(
                new AudioFormat(
                        inputFormat.getEncoding(),
                        outputSampleRate,
                        inputFormat.getSampleSizeInBits(),
                        inputFormat.getChannels(),
                        inputFormat.getEndian(),
                        inputFormat.getSigned(),
                        Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED,
                        outputDataType));
        }
        return inputFormat;
    }
}
