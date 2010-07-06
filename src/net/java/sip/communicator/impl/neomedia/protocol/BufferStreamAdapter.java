/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.protocol;

import java.io.*;

import javax.media.*;
import javax.media.protocol.*;

/**
 * Represents a base class for adapters of <tt>SourceStream</tt>s, usually
 * ones reading data in arrays of bytes and not in <tt>Buffer</tt>s, to
 * <tt>SourceStream</tt>s reading data in <tt>Buffer</tt>s. An example
 * use is creating a PushBufferStream representation of a PushSourceStream.
 *
 * @param <T> the very type of <tt>SourceStream</tt> to be adapted by a
 * <tt>BufferStreamAdapter</tt>
 * @author Lubomir Marinov
 */
public abstract class BufferStreamAdapter<T extends SourceStream>
    implements SourceStream
{

    /**
     * The <tt>Format</tt> of this stream to be reported through the output
     * <tt>Buffer</tt> this instance reads data into.
     */
    private final Format format;

    /**
     * The <tt>SourceStream</tt> being adapted by this instance.
     */
    protected final T stream;

    /**
     * Initializes a new <tt>BufferStreamAdapter</tt> which is to adapt a
     * specific <tt>SourceStream</tt> into a <tt>SourceStream</tt> with
     * a specific <tt>Format</tt>.
     *
     * @param stream
     * @param format
     */
    public BufferStreamAdapter(T stream, Format format)
    {
        this.stream = stream;
        this.format = format;
    }

    /*
     * Implements SourceStream#endOfStream(). Delegates to the wrapped
     * SourceStream.
     */
    public boolean endOfStream()
    {
        return stream.endOfStream();
    }

    /*
     * Implements SourceStream#getContentDescriptor(). Delegates to the wrapped
     * SourceStream.
     */
    public ContentDescriptor getContentDescriptor()
    {
        return stream.getContentDescriptor();
    }

    /*
     * Implements SourceStream#getContentLength(). Delegates to the wrapped
     * SourceStream.
     */
    public long getContentLength()
    {
        return stream.getContentLength();
    }

    /*
     * Implements Controls#getControl(String). Delegates to the wrapped
     * SourceStream.
     */
    public Object getControl(String controlType)
    {
        return stream.getControl(controlType);
    }

    /*
     * Implements Controls#getControls(). Delegates to the wrapped SourceStream.
     */
    public Object[] getControls()
    {
        return stream.getControls();
    }

    /**
     * Gets the <tt>Format</tt> of the data this stream provides.
     *
     * @return the <tt>Format</tt> of the data this stream provides
     */
    public Format getFormat()
    {
        return format;
    }

    /**
     * Gets the <tt>SourceStream</tt> wrapped by this instance.
     *
     * @return the <tt>SourceStream</tt> wrapped by this instance
     */
    public T getStream()
    {
        return stream;
    }

    /**
     * Reads byte data from this stream into a specific <tt>Buffer</tt>
     * which is to use a specific array of bytes for its data.
     *
     * @param buffer the <tt>Buffer</tt> to read byte data into from this
     *            instance
     * @param bytes the array of <tt>byte</tt>s to read data into from this
     *            instance and to be set as the data of the specified
     *            <tt>buffer</tt>
     * @throws IOException
     */
    protected void read(Buffer buffer, byte[] bytes)
        throws IOException
    {
        int offset = 0;
        int numberOfBytesRead = read(bytes, offset, bytes.length);

        if (numberOfBytesRead > -1)
        {
            buffer.setData(bytes);
            buffer.setOffset(offset);
            buffer.setLength(numberOfBytesRead);

            Format format = getFormat();

            if (format != null)
                buffer.setFormat(format);
        }
    }

    /**
     * Reads byte data from this stream into a specific array of
     * <tt>byte</tt>s starting the storing at a specific offset and reading
     * at most a specific number of bytes.
     *
     * @param buffer the array of <tt>byte</tt>s into which the data read
     *            from this stream is to be written
     * @param offset the offset in the specified <tt>buffer</tt> at which
     *            writing data read from this stream should start
     * @param length the maximum number of bytes to be written into the
     *            specified <tt>buffer</tt>
     * @return the number of bytes read from this stream and written into the
     *         specified <tt>buffer</tt>
     * @throws IOException
     */
    protected abstract int read(byte[] buffer, int offset, int length)
        throws IOException;
}
