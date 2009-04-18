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

    private static int FRAME_SIZE = 320;

    private SpeexEncoder encoder = null;

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
            )};

        defaultOutputFormats = new AudioFormat[]
            {new AudioFormat(Constants.SPEEX_RTP)};

        PLUGIN_NAME = "pcm to speex converter";
    }

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

    public void open() throws ResourceUnavailableException
    {

    }

    public void close()
    {

    }

    private void initConverter(AudioFormat inFormat)
    {
        lastFormat = inFormat;

        encoder = new SpeexEncoder();

        encoder.init(0, 4, (int)inFormat.getSampleRate(), 1);
    }

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
