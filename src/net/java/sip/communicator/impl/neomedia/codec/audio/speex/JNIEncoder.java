/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.speex;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.sf.fmj.media.*;

/**
 * Implements a Speex encoder and RTP packetizer using the native Speex library.
 *
 * @author Lubomir Marinov
 */
public class JNIEncoder
    extends AbstractCodecExt
{

    /**
     * The list of <tt>Format</tt>s of audio data supported as input by
     * <tt>JNIEncoder</tt> instances.
     */
    private static final Format[] SUPPORTED_INPUT_FORMATS;

    /**
     * The list of sample rates of audio data supported as input by
     * <tt>JNIEncoder</tt> instances.
     */
    static final double[] SUPPORTED_INPUT_SAMPLE_RATES
        = new double[] { 8000, 16000, 32000 };

    /**
     * The list of <tt>Format</tt>s of audio data supported as output by
     * <tt>JNIEncoder</tt> instances.
     */
    private static final Format[] SUPPORTED_OUTPUT_FORMATS
        = new Format[] { new AudioFormat(Constants.SPEEX_RTP) };

    static
    {
        Speex.assertSpeexIsFunctional();

        int supportedInputCount = SUPPORTED_INPUT_SAMPLE_RATES.length;

        SUPPORTED_INPUT_FORMATS = new Format[supportedInputCount];
        for (int i = 0; i < supportedInputCount; i++)
        {
            SUPPORTED_INPUT_FORMATS[i]
                = new AudioFormat(
                        AudioFormat.LINEAR,
                        SUPPORTED_INPUT_SAMPLE_RATES[i],
                        16,
                        1,
                        AudioFormat.LITTLE_ENDIAN,
                        AudioFormat.SIGNED,
                        Format.NOT_SPECIFIED,
                        Format.NOT_SPECIFIED,
                        Format.byteArray);
        }
    }

    /**
     * The pointer to the native <tt>SpeexBits</tt> into which the native Speex
     * encoder (i.e. {@link #state}) writes the encoded audio data.
     */
    private long bits = 0;

    /**
     * The duration in nanoseconds of an output <tt>Buffer</tt> produced by this
     * <tt>Codec</tt>.
     */
    private long duration = 0;

    /**
     * The number of bytes from an input <tt>Buffer</tt> that this
     * <tt>Codec</tt> processes in one call of its
     * {@link #process(Buffer, Buffer)}.
     */
    private int frameSize = 0;

    /**
     * The bytes from an input <tt>Buffer</tt> from a previous call to
     * {@link #process(Buffer, Buffer)} that this <tt>Codec</tt> didn't process
     * because the total number of bytes was less than {@link #frameSize} and
     * need to be prepended to a subsequent input <tt>Buffer</tt> in order to
     * process a total of {@link #frameSize} bytes.
     */
    private byte[] previousInput;

    /**
     * The length of the audio data in {@link #previousInput}.
     */
    private int previousInputLength = 0;

    /**
     * The sample rate configured into {@link #state}.
     */
    private int sampleRate = 0;

    /**
     * The native Speex encoder represented by this instance.
     */
    private long state = 0;

    /**
     * Initializes a new <tt>JNIEncoder</tt> instance.
     */
    public JNIEncoder()
    {
        super(
            "Speex JNI Encoder",
            AudioFormat.class,
            SUPPORTED_OUTPUT_FORMATS);

        inputFormats = SUPPORTED_INPUT_FORMATS;
    }

    /**
     * @see AbstractCodecExt#doClose()
     */
    protected void doClose()
    {
        // state
        if (state != 0)
        {
            Speex.speex_encoder_destroy(state);
            state = 0;
            sampleRate = 0;
            frameSize = 0;
            duration = 0;
        }
        // bits
        Speex.speex_bits_destroy(bits);
        bits = 0;
        // previousInput
        previousInput = null;
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
        bits = Speex.speex_bits_init();
        if (bits == 0)
            throw new ResourceUnavailableException("speex_bits_init");
    }

    /**
     * Processes (encode) a specific input <tt>Buffer</tt>.
     *
     * @param inputBuffer input buffer
     * @param outputBuffer output buffer
     * @return <tt>BUFFER_PROCESSED_OK</tt> if buffer has been successfully
     * processed
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

        /*
         * Make sure that the native Speex encoder which is represented by this
         * instance is configured to work with the inputFormat.
         */
        AudioFormat inputAudioFormat = (AudioFormat) inputFormat;
        int inputSampleRate = (int) inputAudioFormat.getSampleRate();

        if ((state != 0) && (sampleRate != inputSampleRate))
        {
            Speex.speex_encoder_destroy(state);
            state = 0;
            sampleRate = 0;
            frameSize = 0;
        }
        if (state == 0)
        {
            long mode
                = Speex.speex_lib_get_mode(
                        (inputSampleRate == 16000)
                            ? Speex.SPEEX_MODEID_WB
                            : (inputSampleRate == 32000)
                                ? Speex.SPEEX_MODEID_UWB
                                : Speex.SPEEX_MODEID_NB);

            if (mode == 0)
                return BUFFER_PROCESSED_FAILED;
            state = Speex.speex_encoder_init(mode);
            if (state == 0)
                return BUFFER_PROCESSED_FAILED;
            if (Speex.speex_encoder_ctl(
                        state,
                        Speex.SPEEX_SET_QUALITY,
                        4)
                    != 0)
                return BUFFER_PROCESSED_FAILED;
            if (Speex.speex_encoder_ctl(
                        state,
                        Speex.SPEEX_SET_SAMPLING_RATE,
                        inputSampleRate)
                    != 0)
                return BUFFER_PROCESSED_FAILED;

            int frameSize
                = Speex.speex_encoder_ctl(state, Speex.SPEEX_GET_FRAME_SIZE);

            if (frameSize < 0)
                return BUFFER_PROCESSED_FAILED;

            sampleRate = inputSampleRate;
            this.frameSize = frameSize * 2 /* (sampleSizeInBits / 8) */;
            duration
                = (((long) frameSize) * 1000 * 1000000) / ((long) sampleRate);
        }

        /*
         * The native Speex encoder always processes frameSize bytes from the
         * input in one call. If any specified inputBuffer is with a different
         * length, then we'll have to wait for more bytes to arrive until we
         * have frameSize bytes. Remember whatever is left unprocessed in
         * previousInput and prepend it to the next inputBuffer.
         */
        byte[] input = (byte[]) inputBuffer.getData();
        int inputLength = inputBuffer.getLength();
        int inputOffset = inputBuffer.getOffset();

        if ((previousInput != null) && (previousInputLength > 0))
        {
            if (previousInputLength < this.frameSize)
            {
                if (previousInput.length < this.frameSize)
                {
                    byte[] newPreviousInput = new byte[this.frameSize];

                    System.arraycopy(
                            previousInput, 0,
                            newPreviousInput, 0,
                            previousInput.length);
                    previousInput = newPreviousInput;
                }

                int bytesToCopyFromInputToPreviousInput
                    = Math.min(
                            this.frameSize - previousInputLength,
                            inputLength);

                if (bytesToCopyFromInputToPreviousInput > 0)
                {
                    System.arraycopy(
                            input, inputOffset,
                            previousInput, previousInputLength,
                            bytesToCopyFromInputToPreviousInput);
                    previousInputLength += bytesToCopyFromInputToPreviousInput;
                    inputLength -= bytesToCopyFromInputToPreviousInput;
                    inputBuffer.setLength(inputLength);
                    inputBuffer.setOffset(
                            inputOffset + bytesToCopyFromInputToPreviousInput);
                }
            }

            if (previousInputLength == this.frameSize)
            {
                input = previousInput;
                inputOffset = 0;
                previousInputLength = 0;
            }
            else if (previousInputLength > this.frameSize)
            {
                input = new byte[this.frameSize];
                System.arraycopy(previousInput, 0, input, 0, input.length);
                inputOffset = 0;
                previousInputLength -= input.length;
                System.arraycopy(
                        previousInput, input.length,
                        previousInput, 0,
                        previousInputLength);
            }
            else
            {
                outputBuffer.setLength(0);
                discardOutputBuffer(outputBuffer);
                if (inputLength < 1)
                    return BUFFER_PROCESSED_OK;
                else
                    return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
            }
        }
        else if (inputLength < 1)
        {
            outputBuffer.setLength(0);
            discardOutputBuffer(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }
        else if (inputLength < this.frameSize)
        {
            if ((previousInput == null) || (previousInput.length < inputLength))
                previousInput = new byte[this.frameSize];
            System.arraycopy(input, inputOffset, previousInput, 0, inputLength);
            previousInputLength = inputLength;
            outputBuffer.setLength(0);
            discardOutputBuffer(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }
        else
        {
            inputLength -= this.frameSize;
            inputBuffer.setLength(inputLength);
            inputBuffer.setOffset(inputOffset + this.frameSize);
        }

        /* At long last, do the actual encoding. */
        Speex.speex_bits_reset(bits);
        Speex.speex_encode_int(state, input, inputOffset, bits);

        /* Read the encoded audio data from the SpeexBits into outputBuffer. */
        int outputLength = Speex.speex_bits_nbytes(bits);

        if (outputLength > 0)
        {
            byte[] output = validateByteArraySize(outputBuffer, outputLength);

            outputLength
                = Speex.speex_bits_write(bits, output, 0, output.length);
            if (outputLength > 0)
            {
                outputBuffer.setDuration(duration);
                outputBuffer.setFormat(getOutputFormat());
                outputBuffer.setLength(outputLength);
                outputBuffer.setOffset(0);
            }
            else
            {
                outputBuffer.setLength(0);
                discardOutputBuffer(outputBuffer);
            }
        }
        else
        {
            outputBuffer.setLength(0);
            discardOutputBuffer(outputBuffer);
        }

        if (inputLength < 1)
            return BUFFER_PROCESSED_OK;
        else
            return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
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
        AudioFormat inputAudioFormat = (AudioFormat) inputFormat;

        return
            new Format[]
                    {
                        new AudioFormat(
                                Constants.SPEEX_RTP,
                                inputAudioFormat.getSampleRate(),
                                Format.NOT_SPECIFIED,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED,
                                Format.NOT_SPECIFIED,
                                Format.NOT_SPECIFIED,
                                Format.byteArray)
                    };
    }

    /**
     * Get the output format.
     *
     * @return output format
     * @see net.sf.fmj.media.AbstractCodec#getOutputFormat()
     */
    @Override
    public Format getOutputFormat()
    {
        Format outputFormat = super.getOutputFormat();

        if ((outputFormat != null)
                && (outputFormat.getClass() == AudioFormat.class))
        {
            AudioFormat outputAudioFormat = (AudioFormat) outputFormat;

            outputFormat = setOutputFormat(
                new AudioFormat(
                            outputAudioFormat.getEncoding(),
                            outputAudioFormat.getSampleRate(),
                            outputAudioFormat.getSampleSizeInBits(),
                            outputAudioFormat.getChannels(),
                            outputAudioFormat.getEndian(),
                            outputAudioFormat.getSigned(),
                            outputAudioFormat.getFrameSizeInBits(),
                            outputAudioFormat.getFrameRate(),
                            outputAudioFormat.getDataType())
                        {
                            private static final long serialVersionUID = 0L;

                            @Override
                            public long computeDuration(long length)
                            {
                                return JNIEncoder.this.duration;
                            }
                        });
        }
        return outputFormat;
    }

    /**
     * Sets the input format.
     *
     * @param format format to set
     * @return format
     * @see AbstractCodecExt#setInputFormat(Format)
     */
    @Override
    public Format setInputFormat(Format format)
    {
        Format inputFormat = super.setInputFormat(format);

        if (inputFormat != null)
        {
            double outputSampleRate;
            int outputChannels;

            if (outputFormat == null)
            {
                outputSampleRate = Format.NOT_SPECIFIED;
                outputChannels = Format.NOT_SPECIFIED;
            }
            else
            {
                AudioFormat outputAudioFormat = (AudioFormat) outputFormat;

                outputSampleRate = outputAudioFormat.getSampleRate();
                outputChannels = outputAudioFormat.getChannels();
            }

            AudioFormat inputAudioFormat = (AudioFormat) inputFormat;
            double inputSampleRate = inputAudioFormat.getSampleRate();
            int inputChannels = inputAudioFormat.getChannels();

            if ((outputSampleRate != inputSampleRate)
                    || (outputChannels != inputChannels))
            {
                setOutputFormat(
                    new AudioFormat(
                            Constants.SPEEX_RTP,
                            inputSampleRate,
                            Format.NOT_SPECIFIED,
                            inputChannels,
                            AudioFormat.LITTLE_ENDIAN,
                            AudioFormat.SIGNED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.byteArray));
            }
        }
        return inputFormat;
    }
}
