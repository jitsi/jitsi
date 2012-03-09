/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.ulaw;

import com.sun.media.controls.*;

import javax.media.*;
import javax.media.format.*;

public class JavaDecoder
    extends com.ibm.media.codec.audio.AudioCodec
{
    static private final byte[] lutTableH = new byte[256];

    static private final byte[] lutTableL = new byte[256];

    public JavaDecoder()
    {
        supportedInputFormats = new AudioFormat[]
        { new AudioFormat(AudioFormat.ULAW) };
        defaultOutputFormats = new AudioFormat[]
        { new AudioFormat(AudioFormat.LINEAR) };
        PLUGIN_NAME = "Mu-Law Decoder";
    }

    public Object[] getControls()
    {
        if (controls == null)
        {
            controls
                = new Object[]
                        {
                            new SilenceSuppressionAdapter(this, false, false)
                        };
        }
        return controls;
    }

    protected Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat af = (AudioFormat) in;

        supportedOutputFormats =
            new AudioFormat[]
            { new AudioFormat(AudioFormat.LINEAR, af.getSampleRate(), 16,
                af.getChannels(), AudioFormat.LITTLE_ENDIAN, // isBigEndian(),
                AudioFormat.SIGNED // isSigned());
            ) };
        return supportedOutputFormats;
    }

    private void initTables()
    {
        for (int i = 0; i < 256; i++)
        {
            int input = ~i;
            int mantissa = ((input & 0xf) << 3) + 0x84;
            int segment = (input & 0x70) >> 4;
            int value = mantissa << segment;

            value -= 0x84;

            if ((input & 0x80) != 0)
                value = -value;

            lutTableL[i] = (byte) value;
            lutTableH[i] = (byte) (value >> 8);
        }
    }

    /** Initializes the codec. */
    public void open()
    {
        initTables();
    }

    /** Decodes the buffer */
    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        if (!checkInputBuffer(inputBuffer))
            return BUFFER_PROCESSED_FAILED;
        if (isEOM(inputBuffer))
        {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        byte[] inData = (byte[]) inputBuffer.getData();
        byte[] outData = validateByteArraySize(outputBuffer, inData.length * 2);

        int inpLength = inputBuffer.getLength();
        int outLength = 2 * inpLength;

        int inOffset = inputBuffer.getOffset();
        int outOffset = outputBuffer.getOffset();
        for (int i = 0; i < inpLength; i++)
        {
            int temp = inData[inOffset++] & 0xff;
            outData[outOffset++] = lutTableL[temp];
            outData[outOffset++] = lutTableH[temp];
        }

        updateOutput(outputBuffer, outputFormat, outLength,
            outputBuffer.getOffset());

        return BUFFER_PROCESSED_OK;
    }
}
