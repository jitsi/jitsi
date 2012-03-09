/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.gsm;

import javax.media.*;
import javax.media.format.*;

import net.sf.fmj.media.*;

/**
 * GSM to PCM java decoder.
 * Decodes GSM frame (33 bytes long) into 160 16-bit PCM samples (320 bytes).
 *
 * @author Martin Harvan
 * @author Damian Minkov
 */
public class Decoder
    extends AbstractCodec
{
    private Buffer innerBuffer = new Buffer();
    private static final int PCM_BYTES = 320;
    private static final int GSM_BYTES = 33;
    private int innerDataLength = 0;

    byte[] innerContent;

    @Override
    public String getName()
    {
        return "GSM Decoder";
    }

    /**
     * Constructs a new <tt>Decoder</tt>.
     */
    public Decoder()
    {
        super();
        this.inputFormats = new Format[]
        {
            new AudioFormat(
                    AudioFormat.GSM,
                    8000,
                    8,
                    1,
                    Format.NOT_SPECIFIED,
                    AudioFormat.SIGNED,
                    264,
                    Format.NOT_SPECIFIED,
                    Format.byteArray)
        };
    }

    // TODO: move to base class?
    protected Format[] outputFormats = new Format[]
    {
        new AudioFormat(
                    AudioFormat.LINEAR,
                    8000,
                    16,
                    1,
                    Format.NOT_SPECIFIED,
                    AudioFormat.SIGNED,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    Format.byteArray)
    };

    @Override
    public Format setOutputFormat(Format format)
    {
        if (!(format instanceof AudioFormat))
            return null;

        final AudioFormat audioFormat = (AudioFormat) format;
        return super.setOutputFormat(
                AudioFormatCompleter.complete(audioFormat));
    }

    @Override
    public Format[] getSupportedOutputFormats(Format input)
    {
        if (input == null)
            return outputFormats;
        else
        {
            if (!(input instanceof AudioFormat))
            {
                return new Format[]{null};
            }

            final AudioFormat inputCast = (AudioFormat) input;
            if (!inputCast.getEncoding().equals(AudioFormat.GSM)
                || (inputCast.getSampleSizeInBits() != 8
                    && inputCast.getSampleSizeInBits() != Format.NOT_SPECIFIED)
                || (inputCast.getChannels() != 1
                    && inputCast.getChannels() != Format.NOT_SPECIFIED)
                || (inputCast.getSigned() != AudioFormat.SIGNED
                    && inputCast.getSigned() != Format.NOT_SPECIFIED)
                || (inputCast.getFrameSizeInBits() != 264
                    && inputCast.getFrameSizeInBits() != Format.NOT_SPECIFIED)
                || (inputCast.getDataType() != null
                    && inputCast.getDataType() != Format.byteArray))
            {
                return new Format[] {null};
            }

            final AudioFormat result = new AudioFormat(
                    AudioFormat.LINEAR,
                    inputCast.getSampleRate(),
                    16,
                    1,
                    inputCast.getEndian(),
                    AudioFormat.SIGNED,
                    16,
                    Format.NOT_SPECIFIED,
                    Format.byteArray);

            return new Format[]{result};
        }
    }

    @Override
    public void open()
    {

    }

    @Override
    public void close()
    {

    }

    private static final boolean TRACE = false;

    @Override
    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        byte [] inputContent=new byte[inputBuffer.getLength()];

        System.arraycopy(
            inputBuffer.getData(),
            inputBuffer.getOffset(),
            inputContent,
            0,
            inputContent.length);


        byte[] mergedContent =
            mergeArrays((byte[]) innerBuffer.getData(), inputContent);
        innerBuffer.setData(mergedContent);
        innerBuffer.setLength(mergedContent.length);
        innerDataLength = innerBuffer.getLength();

        if (TRACE) dump("input ", inputBuffer);

        if (!checkInputBuffer(inputBuffer))
        {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(inputBuffer))
        {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        if (TRACE) dump("input ", inputBuffer);

        if (!checkInputBuffer(inputBuffer))
        {
            return BUFFER_PROCESSED_FAILED;
        }

        if (isEOM(inputBuffer)) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        final int result;
        byte[] outputBufferData = (byte[]) outputBuffer.getData();

        if (outputBufferData == null
            || outputBufferData.length <
                PCM_BYTES * innerBuffer.getLength() / GSM_BYTES)
        {
            outputBufferData = new byte[
                    PCM_BYTES * (innerBuffer.getLength() / GSM_BYTES)];
            outputBuffer.setData(outputBufferData);
        }

        if (innerBuffer.getLength() < GSM_BYTES)
        {
            result = OUTPUT_BUFFER_NOT_FILLED;
        } else
        {
            final boolean bigEndian =
                    ((AudioFormat) outputFormat).getEndian()
                            == AudioFormat.BIG_ENDIAN;

            outputBufferData = new byte[
                    PCM_BYTES * (innerBuffer.getLength() / GSM_BYTES)];
            outputBuffer.setData(outputBufferData);
            outputBuffer.setLength(
                    PCM_BYTES * (innerBuffer.getLength() / GSM_BYTES));

            GSMDecoderUtil.gsmDecode(
                    bigEndian,
                    (byte[]) innerBuffer.getData(),
                    inputBuffer.getOffset(),
                    innerBuffer.getLength(),
                    outputBufferData);

            outputBuffer.setFormat(outputFormat);
            result = BUFFER_PROCESSED_OK;
            byte[] temp = new byte[
                innerDataLength - (innerDataLength / GSM_BYTES) * GSM_BYTES];
            innerContent = (byte[]) innerBuffer.getData();
            System.arraycopy(
                innerContent,
                (innerDataLength / GSM_BYTES) * GSM_BYTES,
                temp,
                0,
                temp.length);
            outputBuffer.setOffset(0);

            innerBuffer.setLength(temp.length);
            innerBuffer.setData(temp);
        }

        if (TRACE)
        {
            dump("input ", inputBuffer);
            dump("output", outputBuffer);
        }
        return result;
    }

    private byte[] mergeArrays(byte[] arr1, byte[] arr2)
    {
        if (arr1 == null) return arr2;
        if (arr2 == null) return arr1;
        byte[] merged = new byte[arr1.length + arr2.length];
        System.arraycopy(arr1, 0, merged, 0, arr1.length);
        System.arraycopy(arr2, 0, merged, arr1.length, arr2.length);
        return merged;
    }

    @Override
    public Format setInputFormat(Format arg0)
    {
        // TODO: force sample size, etc
        return super.setInputFormat(arg0);
    }
}
