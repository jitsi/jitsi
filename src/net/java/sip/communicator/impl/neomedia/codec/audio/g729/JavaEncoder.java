/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.g729;

import java.util.*;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.codec.*;
import net.sf.fmj.media.*;

/**
 * @author Lubomir Marinov
 */
public class JavaEncoder
    extends AbstractCodecExt
{
    private static final short BIT_1 = Ld8k.BIT_1;

    private static final int L_FRAME = Ld8k.L_FRAME;

    private static final int SERIAL_SIZE = Ld8k.SERIAL_SIZE;

    private static final int INPUT_FRAME_SIZE_IN_BYTES = 2 * L_FRAME;

    private static final int OUTPUT_FRAME_SIZE_IN_BYTES = L_FRAME / 8;

    private Coder coder;

    private int outputFrameCount;

    /**
     * The previous input if it was less than the input frame size and which is
     * to be prepended to the next input in order to form a complete input
     * frame.
     */
    private byte[] prevInput;

    /**
     * The length of the previous input if it was less than the input frame size
     * and which is to be prepended to the next input in order to form a
     * complete input frame.
     */
    private int prevInputLength;

    private short[] serial;

    private short[] sp16;

    /**
     * Initializes a new <code>JavaEncoder</code> instance.
     */
    public JavaEncoder()
    {
        super(
            "G.729 Encoder",
            AudioFormat.class,
            new AudioFormat[]
                    {
                        new AudioFormat(
                                AudioFormat.G729_RTP,
                                8000,
                                AudioFormat.NOT_SPECIFIED,
                                1)
                    });

        inputFormats
            = new AudioFormat[]
                    {
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                8000,
                                16,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED)
                    };
    }

    protected void discardOutputBuffer(Buffer outputBuffer)
    {
        super.discardOutputBuffer(outputBuffer);

        outputFrameCount = 0;
    }

    /*
     * Implements AbstractCodecExt#doClose().
     */
    protected void doClose()
    {
        prevInput = null;
        prevInputLength = 0;

        sp16 = null;
        serial = null;
        coder = null;
    }

    /**
     * Opens this <tt>Codec</tt> and acquires the resources that it needs to
     * operate. A call to {@link PlugIn#open()} on this instance will result in
     * a call to <tt>doOpen</tt> only if {@link AbstractCodec#opened} is
     * <tt>false</tt>. All required input and/or output formats are assumed to
     * have been set on this <tt>Codec</tt> before <tt>doOpen</tt> is called.
     *
     * @throws ResourceUnavailableException if any of the resources that this
     * <tt>Codec</tt> needs to operate cannot be acquired
     * @see AbstractCodecExt#doOpen()
     */
    protected void doOpen()
        throws ResourceUnavailableException
    {
        prevInput = new byte[INPUT_FRAME_SIZE_IN_BYTES];
        prevInputLength = 0;

        sp16 = new short[L_FRAME];
        serial = new short[SERIAL_SIZE];
        coder = new Coder();

        outputFrameCount = 0;
    }

    /*
     * Implements AbstractCodecExt#doProcess(Buffer, Buffer).
     */
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        byte[] input = (byte[]) inputBuffer.getData();

        int inputLength = inputBuffer.getLength();
        int inputOffset = inputBuffer.getOffset();

        if ((prevInputLength + inputLength) < INPUT_FRAME_SIZE_IN_BYTES)
        {
            System.arraycopy(
                input,
                inputOffset,
                prevInput,
                prevInputLength,
                inputLength);
            prevInputLength += inputLength;
            return BUFFER_PROCESSED_OK | OUTPUT_BUFFER_NOT_FILLED;
        }

        int readShorts = 0;

        if (prevInputLength > 0)
        {
            readShorts
                += readShorts(prevInput, 0, sp16, 0, prevInputLength / 2);
            prevInputLength = 0;
        }
        readShorts
            = readShorts(
                    input,
                    inputOffset,
                    sp16,
                    readShorts,
                    sp16.length - readShorts);

        int readBytes = 2 * readShorts;

        inputLength -= readBytes;
        inputBuffer.setLength(inputLength);
        inputOffset += readBytes;
        inputBuffer.setOffset(inputOffset);

        coder.process(sp16, serial);

        byte[] output
            = validateByteArraySize(
                    outputBuffer,
                    outputBuffer.getOffset() + 2 * OUTPUT_FRAME_SIZE_IN_BYTES);

        packetize(
            serial,
            output,
            outputBuffer.getOffset()
                + OUTPUT_FRAME_SIZE_IN_BYTES * outputFrameCount);
        outputBuffer.setLength(
            outputBuffer.getLength() + OUTPUT_FRAME_SIZE_IN_BYTES);

        outputBuffer.setFormat(outputFormat);

        int processResult = BUFFER_PROCESSED_OK;

        if (outputFrameCount == 1)
            outputFrameCount = 0;
        else
        {
            outputFrameCount = 1;
            processResult |= OUTPUT_BUFFER_NOT_FILLED;
        }
        if (inputLength > 0)
            processResult |= INPUT_BUFFER_NOT_CONSUMED;
        return processResult;
    }

    private void packetize(
        short[] serial,
        byte[] outputFrame,
        int outputFrameOffset)
    {
        Arrays.fill(
            outputFrame,
            outputFrameOffset,
            outputFrameOffset + L_FRAME / 8,
            (byte) 0);

        for (int s = 0; s < L_FRAME; s++)
            if (BIT_1 == serial[2 + s])
            {
                int o = outputFrameOffset + s / 8;
                int output = outputFrame[o];

                output |= 1 << (7 - (s % 8));
                outputFrame[o] = (byte) (output & 0xFF);
            }
    }

    private static int readShorts(
        byte[] input,
        int inputOffset,
        short[] output,
        int outputOffset,
        int outputLength)
    {
        for (int o=outputOffset, i=inputOffset; o<outputLength; o++, i+=2)
            output[o] = ArrayIOUtils.readShort(input, i);
        return outputLength;
    }
}
