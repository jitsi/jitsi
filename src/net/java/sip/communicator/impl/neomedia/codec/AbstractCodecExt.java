/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec;

import javax.media.*;

import net.sf.fmj.media.*;

/**
 * Extends FMJ's <tt>AbstractCodec</tt> to make it even easier to implement a
 * <tt>Codec</tt>.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractCodecExt
    extends AbstractCodec
{
    private final Class<? extends Format> formatClass;

    private final String name;

    private final Format[] supportedOutputFormats;

    protected AbstractCodecExt(
        String name,
        Class<? extends Format> formatClass,
        Format[] supportedOutputFormats)
    {
        this.formatClass = formatClass;
        this.name = name;
        this.supportedOutputFormats = supportedOutputFormats;
    }

    public void close()
    {
        if (!opened)
            return;

        doClose();

        opened = false;
        super.close();
    }

    protected void discardOutputBuffer(Buffer outputBuffer)
    {
        outputBuffer.setDiscard(true);
    }

    protected abstract void doClose();

    protected abstract void doOpen()
        throws ResourceUnavailableException;

    protected abstract int doProcess(Buffer inputBuffer, Buffer outputBuffer);

    protected Format[] getMatchingOutputFormats(Format inputFormat)
    {
        if (supportedOutputFormats != null)
            return supportedOutputFormats.clone();
        return new Format[0];
    }

    public String getName()
    {
        return (name == null) ? super.getName() : name;
    }

    /*
     * Implements AbstractCodec#getSupportedOutputFormats(Format).
     */
    public Format[] getSupportedOutputFormats(Format inputFormat)
    {
        if (inputFormat == null)
            return supportedOutputFormats;

        if (!formatClass.isInstance(inputFormat)
                || (null == matches(inputFormat, inputFormats)))
            return new Format[0];

        return getMatchingOutputFormats(inputFormat);
    }

    /**
     * Utility to perform format matching.
     */
    public static Format matches(Format in, Format outs[])
    {
        for (Format out : outs)
            if (in.matches(out))
                return out;
        return null;
    }

    public void open()
        throws ResourceUnavailableException
    {
        if (opened)
            return;

        doOpen();

        opened = true;
        super.open();
    }

    /*
     * Implements AbstractCodec#process(Buffer, Buffer).
     */
    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        if (!checkInputBuffer(inputBuffer))
            return BUFFER_PROCESSED_FAILED;
        if (isEOM(inputBuffer))
        {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }
        if (inputBuffer.isDiscard())
        {
            discardOutputBuffer(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }

        return doProcess(inputBuffer, outputBuffer);
    }

    public Format setInputFormat(Format format)
    {
        if (!formatClass.isInstance(format)
                || (null == matches(format, inputFormats)))
            return null;

        return super.setInputFormat(format);
    }

    public Format setOutputFormat(Format format)
    {
        if (!formatClass.isInstance(format)
                || (null == matches(format, getMatchingOutputFormats(inputFormat))))
            return null;

        return super.setOutputFormat(format);
    }

    protected byte[] validateByteArraySize(Buffer buffer, int newSize)
    {
        Object data = buffer.getData();
        byte[] newBytes;

        if (data instanceof byte[])
        {
            byte[] bytes = (byte[]) data;

            if (bytes.length >= newSize)
                return bytes;

            newBytes = new byte[newSize];
            System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
        }
        else
        {
            newBytes = new byte[newSize];
            buffer.setLength(0);
            buffer.setOffset(0);
        }

        buffer.setData(newBytes);
        return newBytes;
    }
}
