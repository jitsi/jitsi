/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.codec.audio.g729;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.media.*;

/**
 * @author Lubomir Marinov
 */
public class JavaDecoder
    extends AbstractCodecExt
{
    private static final short BIT_0 = Ld8k.BIT_0;

    private static final short BIT_1 = Ld8k.BIT_1;

    private static final int L_FRAME = Ld8k.L_FRAME;

    private static final int SERIAL_SIZE = Ld8k.SERIAL_SIZE;

    private static final short SIZE_WORD = Ld8k.SIZE_WORD;

    private static final short SYNC_WORD = Ld8k.SYNC_WORD;

    private static final int INPUT_FRAME_SIZE_IN_BYTES = L_FRAME / 8;

    private static final int OUTPUT_FRAME_SIZE_IN_BYTES = 2 * L_FRAME;

    private Decoder decoder;

    private short[] serial;

    private short[] sp16;

    /**
     * Initializes a new <code>JavaDecoder</code> instance.
     */
    public JavaDecoder()
    {
        super(
            "G.729 Decoder",
            AudioFormat.class,
            new AudioFormat[]
                    {
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                8000,
                                16,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED)
                    });

        inputFormats
            = new AudioFormat[]
                    {
                        new AudioFormat(
                                AudioFormat.G729_RTP,
                                8000,
                                AudioFormat.NOT_SPECIFIED,
                                1)
                    };
    }

    private void depacketize(
        byte[] inputFrame,
        int inputFrameOffset,
        short[] serial)
    {
        serial[0] = SYNC_WORD;
        serial[1] = SIZE_WORD;
        for (int s = 0; s < L_FRAME; s++)
        {
            int input = inputFrame[inputFrameOffset + s / 8];

            input &= 1 << (7 - (s % 8));
            serial[2 + s] = (0 != input) ? BIT_1 : BIT_0;
        }
    }

    /*
     * Implements AbstractCodecExt#doClose().
     */
    protected void doClose()
    {
        serial = null;
        sp16 = null;
        decoder = null;
    }

    /*
     * Implements AbstractCodecExt#doOpen().
     */
    protected void doOpen()
    {
        serial = new short[SERIAL_SIZE];
        sp16 = new short[L_FRAME];
        decoder = new Decoder();
    }

    /*
     * Implements AbstractCodecExt#doProcess(Buffer, Buffer).
     */
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer)
    {
        byte[] input = (byte[]) inputBuffer.getData();

        int inputLength = inputBuffer.getLength();

        if (inputLength < INPUT_FRAME_SIZE_IN_BYTES)
        {
            discardOutputBuffer(outputBuffer);
            return BUFFER_PROCESSED_OK | OUTPUT_BUFFER_NOT_FILLED;
        }

        int inputOffset = inputBuffer.getOffset();

        depacketize(input, inputOffset, serial);
        inputLength -= INPUT_FRAME_SIZE_IN_BYTES;
        inputBuffer.setLength(inputLength);
        inputOffset += INPUT_FRAME_SIZE_IN_BYTES;
        inputBuffer.setOffset(inputOffset);

        decoder.process(serial, sp16);

        byte[] output
            = validateByteArraySize(
                    outputBuffer,
                    outputBuffer.getOffset() + OUTPUT_FRAME_SIZE_IN_BYTES);

        writeShorts(sp16, output, outputBuffer.getOffset());
        outputBuffer.setLength(OUTPUT_FRAME_SIZE_IN_BYTES);

        int processResult = BUFFER_PROCESSED_OK;

        if (inputLength > 0)
            processResult |= INPUT_BUFFER_NOT_CONSUMED;
        return processResult;
    }

    private static void writeShorts(
        short[] input,
        byte[] output,
        int outputOffset)
    {
        for (int i=0, o=outputOffset; i<input.length; i++, o+=2)
            ArrayIOUtils.writeShort(input[i], output, o);
    }
}
