/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.mp3;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.util.*;
import net.sf.fmj.media.*;

/**
 * Implements a MP3 encoder using the native FFmpeg library.
 *
 * @author Lyubomir Marinov
 */
public class JNIEncoder
    extends AbstractCodecExt
{
    /**
     * The <tt>Logger</tt> used by the <tt>JNIEncoder</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(JNIEncoder.class);

    /**
     * The list of <tt>Format</tt>s of audio data supported as input by
     * <tt>JNIEncoder</tt> instances.
     */
    private static final Format[] SUPPORTED_INPUT_FORMATS
        = new Format[]
                {
                    new AudioFormat(
                            AudioFormat.LINEAR,
                            /* sampleRate */ Format.NOT_SPECIFIED,
                            16,
                            /* channels */ Format.NOT_SPECIFIED,
                            AudioFormat.LITTLE_ENDIAN,
                            AudioFormat.SIGNED,
                            /* frameSizeInBits */ Format.NOT_SPECIFIED,
                            /* frameRate */ Format.NOT_SPECIFIED,
                            Format.byteArray)
                };

    /**
     * The list of <tt>Format</tt>s of audio data supported as output by
     * <tt>JNIEncoder</tt> instances.
     */
    private static final Format[] SUPPORTED_OUTPUT_FORMATS
        = new Format[] { new AudioFormat(AudioFormat.MPEGLAYER3) };

    static
    {
        if (FFmpeg.avcodec_find_encoder(FFmpeg.CODEC_ID_MP3) == 0)
        {
            throw new RuntimeException(
                    "Could not find FFmpeg encoder CODEC_ID_MP3");
        }
    }

    /**
     * The <tt>AVCodecContext</tt> which performs the actual encoding and which
     * is the native counterpart of this open <tt>JNIEncoder</tt>.
     */
    private long avctx;

    /**
     * The number of bytes of audio data to be encoded with a single call to
     * {@link FFmpeg#avcodec_encode_audio(long, byte[], int, int, byte[], int)}
     * based on the <tt>frame_size</tt> of {@link #avctx}.
     */
    private int frameSizeInBytes;

    /**
     * The audio data which was given to this <tt>JNIEncoder</tt> in a previous
     * call to {@link #doProcess(Buffer, Buffer)} but was less than
     * {@link #frameSizeInBytes} in length and was thus left to be prepended to
     * the audio data in a next call to <tt>doProcess</tt>.
     */
    private byte[] prevInput;

    /**
     * The length of the valid audio data in {@link #prevInput}.
     */
    private int prevInputLength;

    /**
     * Initializes a new <tt>JNIEncoder</tt> instance.
     */
    public JNIEncoder()
    {
        super("MP3 JNI Encoder", AudioFormat.class, SUPPORTED_OUTPUT_FORMATS);

        inputFormats = SUPPORTED_INPUT_FORMATS;
    }

    /**
     * Implements {@link AbstractCodecExt#doClose()}.
     *
     * @see AbstractCodecExt#doClose()
     */
    protected synchronized void doClose()
    {
        if (avctx != 0)
        {
            FFmpeg.avcodec_close(avctx);
            FFmpeg.av_free(avctx);
            avctx = 0;
        }

        prevInput = null;
        prevInputLength = 0;
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
    protected synchronized void doOpen()
        throws ResourceUnavailableException
    {
        long encoder = FFmpeg.avcodec_find_encoder(FFmpeg.CODEC_ID_MP3);

        if (encoder == 0)
        {
            throw new ResourceUnavailableException(
                    "Could not find FFmpeg encoder CODEC_ID_MP3");
        }

        avctx = FFmpeg.avcodec_alloc_context();
        if (avctx == 0)
        {
            throw new ResourceUnavailableException(
                    "Could not allocate AVCodecContext"
                        + " for FFmpeg encoder CODEC_ID_MP3");
        }

        int avcodec_open = -1;

        try
        {
            AudioFormat inputFormat = (AudioFormat) getInputFormat();
            int channels = inputFormat.getChannels();
            int sampleRate = (int) inputFormat.getSampleRate();

            if (channels == Format.NOT_SPECIFIED)
                channels = 1;

            FFmpeg.avcodeccontext_set_bit_rate(avctx, 128000);
            FFmpeg.avcodeccontext_set_channels(avctx, channels);

            // FIXME
            try
            {
                FFmpeg.avcodeccontext_set_sample_fmt(avctx,
                        FFmpeg.AV_SAMPLE_FMT_S16);
            }
            catch (UnsatisfiedLinkError ule)
            {
                logger.warn("The FFmpeg JNI library is out-of-date.");
            }

            if (sampleRate != Format.NOT_SPECIFIED)
                FFmpeg.avcodeccontext_set_sample_rate(avctx, sampleRate);

            avcodec_open = FFmpeg.avcodec_open(avctx, encoder);

            frameSizeInBytes
                = FFmpeg.avcodeccontext_get_frame_size(avctx)
                    * (inputFormat.getSampleSizeInBits() / 8)
                    * channels;
        }
        finally
        {
            if (avcodec_open < 0)
            {
                FFmpeg.av_free(avctx);
                avctx = 0;
            }
        }
        if (avctx == 0)
        {
            throw new ResourceUnavailableException(
                    "Could not open FFmpeg encoder CODEC_ID_MP3");
        }
    }

    /**
     * Implements {@link AbstractCodecExt#doProcess(Buffer, Buffer)}.
     *
     * @param inputBuffer
     * @param outputBuffer
     * @see AbstractCodecExt#doProcess(Buffer, Buffer)
     */
    protected synchronized int doProcess(
            Buffer inputBuffer,
            Buffer outputBuffer)
    {
        byte[] input = (byte[]) inputBuffer.getData();
        int inputLength = inputBuffer.getLength();
        int inputOffset = inputBuffer.getOffset();

        if ((prevInputLength > 0) || (inputLength < frameSizeInBytes))
        {
            int newPrevInputLength
                = Math.min(frameSizeInBytes - prevInputLength, inputLength);

            if (newPrevInputLength > 0)
            {
                if (prevInput == null)
                {
                    prevInput = new byte[frameSizeInBytes];
                    prevInputLength = 0;
                }

                System.arraycopy(
                        input, inputOffset,
                        prevInput, prevInputLength,
                        newPrevInputLength);

                inputBuffer.setLength(inputLength - newPrevInputLength);
                inputBuffer.setOffset(inputOffset + newPrevInputLength);

                prevInputLength += newPrevInputLength;
                if (prevInputLength == frameSizeInBytes)
                {
                    input = prevInput;
                    inputLength = prevInputLength;
                    inputOffset = 0;

                    prevInputLength = 0;
                }
                else
                    return OUTPUT_BUFFER_NOT_FILLED;
            }
        }
        else 
        {
            inputBuffer.setLength(inputLength - frameSizeInBytes);
            inputBuffer.setOffset(inputOffset + frameSizeInBytes);
        }

        Object outputData = outputBuffer.getData();
        byte[] output
            = (outputData instanceof byte[]) ? (byte[]) outputData : null;
        int outputOffset = outputBuffer.getOffset();
        int minOutputLength
            = Math.max(FFmpeg.FF_MIN_BUFFER_SIZE, inputLength);

        if ((output == null)
                || ((output.length - outputOffset) < minOutputLength))
        {
            output = new byte[minOutputLength];
            outputBuffer.setData(output);
            outputOffset = 0;
            outputBuffer.setOffset(outputOffset);
        }

        int outputLength
            = FFmpeg.avcodec_encode_audio(
                    avctx,
                    output, outputOffset, output.length - outputOffset,
                    input, inputOffset);

        if (outputLength < 0)
            return BUFFER_PROCESSED_FAILED;
        else
        {
            outputBuffer.setLength(outputLength);

            if (inputBuffer.getLength() > 0)
                return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
            else
                return BUFFER_PROCESSED_OK;
        }
    }
}
