/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.codec.audio.speex;

import javax.media.*;
import javax.media.format.*;

import org.xiph.speex.*;
import net.java.sip.communicator.impl.media.codec.*;

/**
 * The Speex Encoder
 *
 * @author Damian Minkov
 */
public class JavaEncoder
    extends com.ibm.media.codec.audio.AudioCodec
{
    private Format lastFormat = null;

    private int FRAME_SIZE = -1;

    private SpeexEncoder encoder = null;

    final static int NARROW_BAND= 0;
    final static int WIDE_BAND= 1;
    final static int ULTRA_WIDE_BAND= 2;

    /**
     * Creates the encoder and init supported formats
     */
    public JavaEncoder()
    {
        supportedInputFormats = new AudioFormat[]
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

        defaultOutputFormats = new AudioFormat[]
            {new AudioFormat(Constants.SPEEX_RTP)};

        PLUGIN_NAME = "pcm to speex converter";
    }

    /**
     * Returns the output format that matches the supplied input format.
     * @param in
     * @return
     */
    protected Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat af = (AudioFormat) in;

        supportedOutputFormats = new AudioFormat[]
            {new AudioFormat(
                Constants.SPEEX_RTP,
                af.getSampleRate(),
                af.getSampleSizeInBits(),
                af.getChannels(),
                af.getEndian(),
                af.getSigned()
            )};

        return supportedOutputFormats;
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

    private void initConverter(AudioFormat inFormat)
    {
        lastFormat = inFormat;

        encoder = new SpeexEncoder();

        int sampleRate = 
            (int)inFormat.getSampleRate();
        
        int band = NARROW_BAND;
        FRAME_SIZE = 320;

        if(sampleRate == 16000)
        {
            band = WIDE_BAND;
            FRAME_SIZE = 640;
        }
        else if(sampleRate == 32000)
        {
            band = ULTRA_WIDE_BAND;
            FRAME_SIZE = 1280;
        }

        encoder.init(band, 4, sampleRate, 1);
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

            byte[] outData = validateByteArraySize(outputBuffer, buff.length);

            System.arraycopy(buff, 0, outData, outputBuffer.getOffset(),
                             buff.length);

            updateOutput(outputBuffer, outputFormat, outData.length, 0);

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
