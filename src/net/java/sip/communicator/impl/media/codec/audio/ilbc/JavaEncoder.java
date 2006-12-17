/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.codec.audio.ilbc;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.media.codec.*;
import net.java.sip.communicator.impl.media.codec.audio.*;

/**
 * The ilbc Encoder
 *
 * @author Damian Minkov
 */
public class JavaEncoder
    extends com.ibm.media.codec.audio.AudioCodec
{
    private Format lastFormat = null;

    private ilbc_encoder enc = null;
    private int ILBC_NO_OF_BYTES = 0;

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
            {new AudioFormat(
                Constants.ILBC_RTP,
                8000.0,
                16,
                1,
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.SIGNED)};

        PLUGIN_NAME = "pcm to iLbc converter";
    }

    protected Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat af = (AudioFormat) in;

        supportedOutputFormats = new AudioFormat[]
            {new AudioFormat(
                Constants.ILBC_RTP,
                8000.0,
                16,
                1,
                AudioFormat.LITTLE_ENDIAN,
                AudioFormat.SIGNED)};

        return supportedOutputFormats;
    }

    public void open() throws ResourceUnavailableException
    {
        int mode = Constants.ILBC_MODE;
        enc = new ilbc_encoder(mode);

        if(mode == 20)
            ILBC_NO_OF_BYTES = ilbc_constants.NO_OF_BYTES_20MS;
        else if(mode == 30)
            ILBC_NO_OF_BYTES = ilbc_constants.NO_OF_BYTES_30MS;
    }

    public void close()
    {

    }



    private void initConverter(AudioFormat inFormat)
    {
        lastFormat = inFormat;
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
        int inOffset = inputBuffer.getOffset();
        byte[] inpData = (byte[]) inputBuffer.getData();

        if (inpLength == 0)
        {
            return OUTPUT_BUFFER_NOT_FILLED;
        }
        else if(inpLength < enc.ULP_inst.blockl*2)
        {
            return OUTPUT_BUFFER_NOT_FILLED;
        }

		short[] encoded_data = new short[ILBC_NO_OF_BYTES / 2];

        int outLength = ILBC_NO_OF_BYTES;
        byte[] outdata = validateByteArraySize(outputBuffer, outLength);

        short[] data = Utils.byteToShortArray(inpData, inOffset, inpLength, true);
        enc.encode(encoded_data, data);

        Utils.shortArrToByteArr(encoded_data, outdata, false);

        updateOutput(outputBuffer, outputFormat, outLength, 0);


        inputBuffer.setLength(inpLength - enc.ULP_inst.blockl*2);
        inputBuffer.setOffset(inOffset + enc.ULP_inst.blockl*2);

        return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
    }

    public java.lang.Object[] getControls()
    {
        if (controls == null)
        {
            controls = new Control[1];
            controls[0] = new com.sun.media.controls.SilenceSuppressionAdapter(this, false, false);
        }
        return (Object[]) controls;
    }
}
