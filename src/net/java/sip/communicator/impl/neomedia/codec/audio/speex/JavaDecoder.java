/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.speex;

import java.io.*;
import javax.media.*;
import javax.media.format.*;

import org.xiph.speex.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.sf.fmj.media.*;

/**
 * Speex to PCM java decoder
 * @author Damian Minkov
 **/
public class JavaDecoder
    extends AbstractCodec
{
    /**
     * Default output formats.
     */
    protected Format[] DEFAULT_OUTPUT_FORMATS = new Format[] {
        new AudioFormat(AudioFormat.LINEAR)};

    /**
     * The input format used to initialize the decoder.
     * If null decoder must be created.
     */
    private Format lastFormat = null;

    /**
     * The decoder.
     */
    private SpeexDecoder decoder = null;

    /**
     * Creates the decoder and inits supported input formats.
     */
    public JavaDecoder()
    {
        this.inputFormats = new AudioFormat[]
        {
            new AudioFormat(Constants.SPEEX_RTP,
                            8000,
                            8,
                            1,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED),
            new AudioFormat(Constants.SPEEX_RTP,
                            16000,
                            8,
                            1,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED),
            new AudioFormat(Constants.SPEEX_RTP,
                            32000,
                            8,
                            1,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED)
        };
    }

    /**
     * Returns the name of this plugin/codec.
     * @return the name.
     */
    public String getName()
    {
        return "Speex Java Decoder";
    }

    /**
     * Return the list of formats supported at the output.
     *
     * @param in the input format.
     * @return array of formats supported at output
     */
    public Format[] getSupportedOutputFormats(Format in)
    {
        // null input format
        if (in == null)
            return DEFAULT_OUTPUT_FORMATS;

        // mismatch input format
        if (!(in instanceof AudioFormat)
                || (null == AbstractCodecExt.matches(in, inputFormats)))
            return new Format[0];

        return getMatchingOutputFormats(in);
    }

    /**
     * Returns the output format that matches the supplied input format.
     * @param in input format
     * @return array of format that match input ones
     */
    private Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat af = (AudioFormat) in;

        return new AudioFormat[]
            {
                new AudioFormat(
                    AudioFormat.LINEAR,
                    af.getSampleRate(),
                    16,
                    af.getChannels(),
                    AudioFormat.LITTLE_ENDIAN, //isBigEndian(),
                    AudioFormat.SIGNED //isSigned());
            )};
    }

    /**
     * Does nothing.
     */
    public void open()
    {}

    /**
     * Does nothing.
     */
    public void close()
    {}

    /**
     * Initialize the decoder.
     * @param inFormat the input format.
     */
    private void initConverter(AudioFormat inFormat)
    {
        lastFormat = inFormat;

        decoder = new SpeexDecoder();

        int sampleRate =
            (int)inFormat.getSampleRate();

        int band = JavaEncoder.NARROW_BAND;

        if(sampleRate == 16000)
            band = JavaEncoder.WIDE_BAND;
        else if(sampleRate == 32000)
            band = JavaEncoder.ULTRA_WIDE_BAND;

        decoder.init(band,
                     sampleRate,
                     inFormat.getChannels(),
                     false);
    }

    /**
     * Process the input and decodes it.
     * @param inputBuffer the input data.
     * @param outputBuffer the result data.
     * @return state of the process.
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

        byte[] inData = (byte[]) inputBuffer.getData();

        int inpLength = inputBuffer.getLength();
        int outLength = 0;

        int inOffset = inputBuffer.getOffset();
        int outOffset = outputBuffer.getOffset();

        Format newFormat = inputBuffer.getFormat();

        if (lastFormat != newFormat)
        {
            initConverter( (AudioFormat) newFormat);
        }

        try
        {
            int fullyProcessed =
                decoder.processData(inData, inOffset, inpLength);

            // sometimes we get more than a frame in single speex packet
            // we must process it all
            while(fullyProcessed != 1)
            {
                fullyProcessed = decoder.processData(false);
            }

            outLength = decoder.getProcessedDataByteSize();

            /*
             * Do not always allocate a new data array for outBuffer, try to reuse
             * the existing one if it is suitable.
             */
            Object outData = outputBuffer.getData();
            byte[] out;

            if (outData instanceof byte[])
            {
                out = (byte[]) outData;
                if (out.length < outLength)
                    out = null;
            }
            else
                out = null;
            if (out == null)
                out = new byte[outLength];

            decoder.getProcessedData(out, outOffset);

            outputBuffer.setData(out);
            outputBuffer.setLength(out.length);
            outputBuffer.setOffset(0);
            outputBuffer.setFormat(outputFormat);
        }
        catch (StreamCorruptedException ex)
        {
            ex.printStackTrace();
        }

        return BUFFER_PROCESSED_OK;
    }
}
