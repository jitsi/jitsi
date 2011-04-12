/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.ilbc;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;
import net.java.sip.communicator.impl.neomedia.codec.audio.*;

/**
 * The ilbc Encoder
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class JavaEncoder
    extends AbstractCodecExt
{
    private ilbc_encoder enc = null;

    private int ILBC_NO_OF_BYTES = 0;

    public JavaEncoder()
    {
        super(
                "iLBC Encoder",
                AudioFormat.class,
                new Format[]
                        {
                            new AudioFormat(
                                    Constants.ILBC_RTP,
                                    8000,
                                    16,
                                    1,
                                    AudioFormat.LITTLE_ENDIAN,
                                    AudioFormat.SIGNED)
                        });

        inputFormats
            = new Format[]
                    {
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                8000,
                                16,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED)
                    };

        addControl(
                new com.sun.media.controls.SilenceSuppressionAdapter(
                        this,
                        false,
                        false));
    }

    /**
     * Implements {@link AbstractCodecExt#doClose()}.
     *
     * @see AbstractCodecExt#doClose()
     */
    protected void doClose()
    {
    }

    /**
     * Implements {@link AbstractCodecExt#doOpen()}.
     *
     * @throws ResourceUnavailableException
     * @see AbstractCodecExt#doOpen()
     */
    protected void doOpen()
        throws ResourceUnavailableException
    {
        int mode = Constants.ILBC_MODE;

        enc = new ilbc_encoder(mode);

        switch (mode)
        {
        case 20:
            ILBC_NO_OF_BYTES = ilbc_constants.NO_OF_BYTES_20MS;
            break;
        case 30:
            ILBC_NO_OF_BYTES = ilbc_constants.NO_OF_BYTES_30MS;
            break;
        default:
            throw new IllegalStateException("mode");
        }
    }

    /**
     * Implements {@link AbstractCodecExt#doProcess(Buffer, Buffer)}.
     *
     * @param inputBuffer
     * @param outputBuffer
     * @return
     * @see AbstractCodecExt#doProcess(Buffer, Buffer)
     */
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        int inpLength = inputBuffer.getLength();
        int inOffset = inputBuffer.getOffset();
        byte[] inpData = (byte[]) inputBuffer.getData();

        if ((inpLength == 0) || (inpLength < enc.ULP_inst.blockl*2))
            return OUTPUT_BUFFER_NOT_FILLED;

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
}
