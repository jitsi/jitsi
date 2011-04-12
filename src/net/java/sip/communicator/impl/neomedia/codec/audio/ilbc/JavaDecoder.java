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
 * iLbc to PCM java decoder
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class JavaDecoder
    extends AbstractCodecExt
{

    /**
     * The decoder
     */
    private ilbc_decoder dec;

    /**
     * The input length in bytes with which {@link #dec} has been initialized. 
     */
    private int inputLength;

    public JavaDecoder()
    {
        super(
                "iLBC Decoder",
                AudioFormat.class,
                new Format[] { new AudioFormat(AudioFormat.LINEAR) });

        inputFormats
            = new Format[]
                    {
                        new AudioFormat(
                                Constants.ILBC_RTP,
                                8000,
                                16,
                                1,
                                Format.NOT_SPECIFIED /* endian */,
                                Format.NOT_SPECIFIED /* signed */)
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
     * @see AbstractCodecExt#doOpen()
     */
    protected void doOpen()
    {
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
        byte[] inData = (byte[]) inputBuffer.getData();

        int inpLength = inputBuffer.getLength();
        int inOffset = inputBuffer.getOffset();

        if (this.inputLength != inpLength)
            initConverter(inpLength);

        short[] data = Utils.byteToShortArray(inData, inOffset, inpLength, false);
        short[] decodedData = new short[dec.ULP_inst.blockl];

        dec.decode(decodedData, data, (short) 1);

        int outLength = dec.ULP_inst.blockl * 2;
        byte[] outData = validateByteArraySize(outputBuffer, outLength);

        Utils.shortArrToByteArr(decodedData, outData, true);

        updateOutput(outputBuffer, getOutputFormat(), outLength, 0);

        return BUFFER_PROCESSED_OK;
    }

    @Override
    protected Format[] getMatchingOutputFormats(Format inputFormat)
    {
        AudioFormat inputAudioFormat = (AudioFormat) inputFormat;

        return
            new AudioFormat[]
                    {
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                inputAudioFormat.getSampleRate(),
                                16,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED)
                    };
    }

    private void initConverter(int inputLength)
    {
        int mode;

        switch (inputLength)
        {
        case ilbc_constants.NO_OF_BYTES_20MS:
            mode = 20;
            break;
        case ilbc_constants.NO_OF_BYTES_30MS:
            mode = 30;
            break;
        default:
            throw new IllegalArgumentException("inputLength");
        }

        dec = new ilbc_decoder(mode, 1);
        this.inputLength = inputLength;
    }
}
