/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.ilbc;

import java.util.*;

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
     * List of offsets for a "more than one" iLBC frame per RTP packet.
     */
    private List<Integer> offsets = new ArrayList<Integer>();

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

        if(offsets.size() == 0 &&
            ((inputLength > ilbc_constants.NO_OF_BYTES_20MS &&
                inputLength != ilbc_constants.NO_OF_BYTES_30MS) ||
            inputLength > ilbc_constants.NO_OF_BYTES_30MS))
        {
            int nb = 0;
            int len = 0;

            if((inputLength % ilbc_constants.NO_OF_BYTES_20MS) == 0)
            {
                nb = (inputLength % ilbc_constants.NO_OF_BYTES_20MS);
                len = ilbc_constants.NO_OF_BYTES_20MS;
            }
            else if((inputLength % ilbc_constants.NO_OF_BYTES_30MS) == 0)
            {
                nb = (inputLength % ilbc_constants.NO_OF_BYTES_30MS);
                len = ilbc_constants.NO_OF_BYTES_30MS;
            }

            if (this.inputLength != len)
                initDec(len);

            for(int i = 0 ; i < nb ; i++)
            {
                offsets.add(new Integer(inputLength + (i * len)));
            }
        }
        else
            if (this.inputLength != inputLength)
                initDec(inputLength);

        int outputLength = dec.ULP_inst.blockl * 2;
        byte[] output = validateByteArraySize(outputBuffer, outputLength);
        int outputOffset = 0;

        int offsetToAdd = 0;

        if(offsets.size() > 0)
            offsetToAdd = offsets.remove(0).intValue();

        dec.decode(
                output, outputOffset,
                input, inputBuffer.getOffset() + offsetToAdd,
                (short) 1);

        updateOutput(
                outputBuffer,
                getOutputFormat(), outputLength, outputOffset);
        int flags = BUFFER_PROCESSED_OK;

        if(offsets.size() > 0)
        {
            flags |= INPUT_BUFFER_NOT_CONSUMED;
        }

        return flags;
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
