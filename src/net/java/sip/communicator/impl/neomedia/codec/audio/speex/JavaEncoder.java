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
import net.sf.fmj.media.*;

import org.xiph.speex.*;

/**
 * The Speex Encoder
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
 */
public class JavaEncoder
    extends AbstractCodecExt
{

    /**
     * Narrow band used for 8 kHz.
     */
    static final int NARROW_BAND= 0;

    /**
     * Utra wide band used for 32 kHz.
     */
    static final int ULTRA_WIDE_BAND= 2;

    /**
     * Wide band used for 16 kHz.
     */
    static final int WIDE_BAND= 1;

    /**
     * The speex encoder instance.
     */
    private SpeexEncoder encoder = null;

    /**
     * The frame size used for current encoder configuration.
     */
    private int frameSize = -1;

    /**
     * The last input format used by this instance of the encoder.
     * If null we must create the encoder with the current input format.
     */
    private Format lastFormat = null;

    /**
     * Our custom output format which computes the rtp data duration correctly.
     */
    private AudioFormat outFormat = null;

    /**
     * Creates the encoder and init supported formats
     */
    public JavaEncoder()
    {
        super(
            "Speex Java Encoder",
            AudioFormat.class,
            new Format[] { new AudioFormat(Constants.SPEEX_RTP) });

        inputFormats
            = new AudioFormat[]
                    {
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                8000,
                                16,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED),
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                16000,
                                16,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED),
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                32000,
                                16,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED)
                    };
    }

    /**
     * Does nothing.
     *
     * @see AbstractCodecExt#doClose()
     */
    public void doClose()
    {
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
    public void doOpen()
        throws ResourceUnavailableException
    {
    }

    /**
     * Process the input and encodes it.
     *
     * @param inputBuffer the input data.
     * @param outputBuffer the result data
     * @return state of the process
     * @see AbstractCodecExt#doProcess(Buffer, Buffer)
     */
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        Format newFormat = inputBuffer.getFormat();

        if (lastFormat != newFormat)
            initConverter((AudioFormat) newFormat);

        int inputLength = inputBuffer.getLength();

        if (inputLength == 0)
            return OUTPUT_BUFFER_NOT_FILLED;

        if (inputLength >= frameSize)
        {
            byte[] inputData = (byte[]) inputBuffer.getData();
            int inputOffset = inputBuffer.getOffset();

            encoder.processData(inputData, inputOffset, frameSize);

            byte[] buff = new byte[encoder.getProcessedDataByteSize()];

            encoder.getProcessedData(buff, 0);

            /*
             * Do not always allocate a new data array for outBuffer, try to
             * reuse the existing one if it is suitable.
             */
            Object outputData = outputBuffer.getData();
            byte[] output;

            if (outputData instanceof byte[])
            {
                output = (byte[]) outputData;
                if (output.length < buff.length)
                    output = null;
            }
            else
                output = null;
            if (output == null)
                output = new byte[buff.length];

            System.arraycopy(buff, 0, output, 0, buff.length);

            outputBuffer.setData(output);
            outputBuffer.setFormat(outFormat);
            outputBuffer.setLength(output.length);
            outputBuffer.setOffset(0);

            if (inputLength > frameSize)
            {
                inputBuffer.setLength(inputLength - frameSize);
                inputBuffer.setOffset(inputOffset + frameSize);
                return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
            }
            else
                return BUFFER_PROCESSED_OK;
        }
        else
            return OUTPUT_BUFFER_NOT_FILLED;
    }

    /**
     * Returns the output format that matches the supplied input format.
     *
     * @param in input format
     * @return array of formats that match input ones
     * @see AbstractCodecExt#getMatchingOutputFormats(Format)
     */
    @Override
    protected Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat af = (AudioFormat) in;

        return
            new Format[]
                    {
                        new AudioFormat(
                                Constants.SPEEX_RTP,
                                af.getSampleRate(),
                                af.getSampleSizeInBits(),
                                af.getChannels(),
                                af.getEndian(),
                                af.getSigned())
                    };
    }

    /**
     * Initialize the encoder with the supplied input format.
     * @param inFormat the input format.
     */
    private void initConverter(AudioFormat inFormat)
    {
        lastFormat = inFormat;

        AudioFormat oFormat = (AudioFormat)outputFormat;
        outFormat = new AudioFormat(
            oFormat.getEncoding(),
            oFormat.getSampleRate(),
            oFormat.getSampleSizeInBits(),
            oFormat.getChannels(),
            oFormat.getEndian(),
            oFormat.getSigned(),
            oFormat.getFrameSizeInBits(),
            oFormat.getFrameRate(),
            oFormat.getDataType())
        {
            /**
             * Serial version UID.
             */
            private static final long serialVersionUID = 0L;

            /**
             * The length in nanoseconds.
             */
            @Override
            public long computeDuration(long length)
            {
                // 20ms in nano
                return 20 * 1000000;
            }
        };

        encoder = new SpeexEncoder();

        int sampleRate = (int) inFormat.getSampleRate();
        int band = NARROW_BAND;

        if (sampleRate == 16000)
            band = WIDE_BAND;
        else if (sampleRate == 32000)
            band = ULTRA_WIDE_BAND;

        encoder.init(band, 4, sampleRate, 1);

        frameSize = 2 * encoder.getFrameSize();
    }
}
