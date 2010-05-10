/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.speex;

import javax.media.*;
import javax.media.format.*;

import org.xiph.speex.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.sf.fmj.media.*;

/**
 * The Speex Encoder
 *
 * @author Damian Minkov
 */
public class JavaEncoder
    extends AbstractCodec
{
    /**
     * Default output formats.
     */
    protected Format[] DEFAULT_OUTPUT_FORMATS = new Format[] {
        new AudioFormat(Constants.SPEEX_RTP)};

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
     * The frame size used for current encoder configuration.
     */
    private int FRAME_SIZE = -1;

    /**
     * The speex encoder instance.
     */
    private SpeexEncoder encoder = null;

    /**
     * Narrow band used for 8 kHz.
     */
    final static int NARROW_BAND= 0;

    /**
     * Wide band used for 16 kHz.
     */
    final static int WIDE_BAND= 1;

    /**
     * Utra wide band used for 32 kHz.
     */
    final static int ULTRA_WIDE_BAND= 2;

    /**
     * Creates the encoder and init supported formats
     */
    public JavaEncoder()
    {
        this.inputFormats = new AudioFormat[]
        {
            new AudioFormat(
                AudioFormat.LINEAR,
                8000,
                16,
                1,
                AudioFormat.LITTLE_ENDIAN, //isBigEndian(),
                AudioFormat.SIGNED //isSigned());
            ),
            new AudioFormat(
                AudioFormat.LINEAR,
                16000,
                16,
                1,
                AudioFormat.LITTLE_ENDIAN, //isBigEndian(),
                AudioFormat.SIGNED //isSigned());
            ),
            new AudioFormat(
                AudioFormat.LINEAR,
                32000,
                16,
                1,
                AudioFormat.LITTLE_ENDIAN, //isBigEndian(),
                AudioFormat.SIGNED //isSigned());
            )
            };
    }

    /**
     * Returns the name of this plugin/codec.
     * @return the name.
     */
	public String getName()
	{
		return "pcm to speex converter";
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
     * @return array of formats that match input ones
     */
    private Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat af = (AudioFormat) in;

        return new AudioFormat[]
            {new AudioFormat(
                Constants.SPEEX_RTP,
                af.getSampleRate(),
                af.getSampleSizeInBits(),
                af.getChannels(),
                af.getEndian(),
                af.getSigned()
            )};
    }

    /**
     * Does nothing.
     */
    public void open()
    {

    }

    /**
     * Does nothing.
     */
    public void close()
    {

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
             * The length in nanoseconds.
             */
            public long computeDuration(long l)
            {
                // 20ms in nano
                return 20 * 1000000;
            }
        };
    
        encoder = new SpeexEncoder();

        int sampleRate = 
            (int)inFormat.getSampleRate();

        int band = NARROW_BAND;

        if(sampleRate == 16000)
        {
            band = WIDE_BAND;
        }
        else if(sampleRate == 32000)
        {
            band = ULTRA_WIDE_BAND;            
        }

        encoder.init(band, 4, sampleRate, 1);
        FRAME_SIZE = 2 * encoder.getFrameSize();
    }

    /**
     * Process the input and encodes it.
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

        Format newFormat = inputBuffer.getFormat();

        if (lastFormat != newFormat)
        {
            initConverter( (AudioFormat) newFormat);
        }

        int inpLength = inputBuffer.getLength();

        byte[] inpData = (byte[]) inputBuffer.getData();
        int inOffset = inputBuffer.getOffset();

        if (inpLength == 0)
        {
            return OUTPUT_BUFFER_NOT_FILLED;
        }

        if ( (inpLength - inOffset) >= FRAME_SIZE)
        {
            encoder.processData(inpData, inOffset, FRAME_SIZE);
            byte[] buff = new byte[encoder.getProcessedDataByteSize()];
            encoder.getProcessedData(buff, 0);

            /*
             * Do not always allocate a new data array for outBuffer, try to reuse
             * the existing one if it is suitable.
             */
            Object outData = outputBuffer.getData();
            byte[] out;

            if (outData instanceof byte[])
            {
                out = (byte[]) outData;
                if (out.length < buff.length)
                    out = null;
            }
            else
                out = null;
            if (out == null)
                out = new byte[buff.length];

            System.arraycopy(buff, 0, out, outputBuffer.getOffset(),
                             buff.length);

            outputBuffer.setData(out);
            outputBuffer.setLength(out.length);
            outputBuffer.setOffset(0);
            outputBuffer.setFormat(outFormat);

            if ( (inpLength - inOffset) > FRAME_SIZE)
            {
                inputBuffer.setOffset(inOffset + FRAME_SIZE);

                return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
            }
            else
            {
                return BUFFER_PROCESSED_OK;
            }
        }
        else
        {
            return OUTPUT_BUFFER_NOT_FILLED;
        }
    }
}
