/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.g722;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;

/**
 * @author Lyubomir Marinov
 */
public class JNIEncoder
    extends AbstractCodecExt
{
    static
    {
        System.loadLibrary("jg722");
    }

    private static native void g722_encoder_close(long encoder);

    private static native long g722_encoder_open();

    private static native void g722_encoder_process(
            long encoder,
            byte[] input, int inputOffset,
            byte[] output, int outputOffset, int outputLength);

    private long encoder;

    /**
     * Initializes a new <tt>JNIEncoder</tt> instance.
     */
    public JNIEncoder()
    {
        super(
            "G.722 JNI Encoder",
            AudioFormat.class,
            JNIDecoder.SUPPORTED_INPUT_FORMATS);

        inputFormats = JNIDecoder.SUPPORTED_OUTPUT_FORMATS;
    }

    /**
     *
     * @param length
     * @return
     */
    private long computeDuration(long length)
    {
        return (length * 1000000L) / 8L;
    }

    /**
     *
     * @see AbstractCodecExt#doClose()
     */
    protected void doClose()
    {
        g722_encoder_close(encoder);
    }

    /**
     *
     * @throws ResourceUnavailableException
     * @see AbstractCodecExt#doOpen()
     */
    protected void doOpen()
        throws ResourceUnavailableException
    {
        encoder = g722_encoder_open();
        if (encoder == 0)
            throw new ResourceUnavailableException("g722_encoder_open");
    }

    /**
     *
     * @param inputBuffer
     * @param outputBuffer
     * @return
     * @see AbstractCodecExt#doProcess(Buffer, Buffer)
     */
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        int inputOffset = inputBuffer.getOffset();
        int inputLength = inputBuffer.getLength();
        byte[] input = (byte[]) inputBuffer.getData();

        int outputOffset = outputBuffer.getOffset();
        int outputLength = inputLength / 4;
        byte[] output
            = validateByteArraySize(outputBuffer, outputOffset + outputLength);

        // G.722 is defined to encode from 14-bit samples.
        for (int i = inputOffset; i < inputLength; i += 2)
        {
            short sample = ArrayIOUtils.readShort(input, i);

            sample >>>= 2;
            ArrayIOUtils.writeShort(sample, input, i);
        }

        g722_encoder_process(
                encoder,
                input, inputOffset,
                output, outputOffset, outputLength);
        outputBuffer.setDuration(computeDuration(outputLength));
        outputBuffer.setFormat(getOutputFormat());
        outputBuffer.setLength(outputLength);
        return BUFFER_PROCESSED_OK;
    }

    /**
     *
     * @return
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

            setOutputFormat(
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
                            @Override
                            public long computeDuration(long length)
                            {
                                return JNIEncoder.this.computeDuration(length);
                            }
                        });
        }
        return outputFormat;
    }
}
