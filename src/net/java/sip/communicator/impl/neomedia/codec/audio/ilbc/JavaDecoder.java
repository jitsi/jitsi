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

/**
 * Implements an iLBC decoder and RTP depacketizer as a {@link Codec}.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class JavaDecoder
    extends AbstractCodecExt
{

    /**
     * The <tt>ilbc_decoder</tt> adapted to <tt>Codec</tt> by this instance.
     */
    private ilbc_decoder dec;

    /**
     * The input length in bytes with which {@link #dec} has been initialized. 
     */
    private int inputLength;

    /**
     * Initializes a new iLBC <tt>JavaDecoder</tt> instance.
     */
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
        dec = null;
        inputLength = 0;
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
        byte[] input = (byte[]) inputBuffer.getData();

        int inputLength = inputBuffer.getLength();

        if (this.inputLength != inputLength)
            initDec(inputLength);

        int outputLength = dec.ULP_inst.blockl * 2;
        byte[] output = validateByteArraySize(outputBuffer, outputLength);
        int outputOffset = 0;

        dec.decode(
                output, outputOffset,
                input, inputBuffer.getOffset(),
                (short) 1);

        updateOutput(
                outputBuffer,
                getOutputFormat(), outputLength, outputOffset);
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

    /**
     * Initializes {@link #dec} so that it processes a specific number of bytes
     * as input.
     *
     * @param inputLength the number of bytes of input to be processed by
     * {@link #dec}
     */
    private void initDec(int inputLength)
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
