/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.conference;

import java.io.*;

import javax.media.*;
import javax.media.protocol.*;

/**
 * Represents a base class for adapters of <code>SourceStream</code>s, usually
 * ones reading data in arrays of bytes and not in <code>Buffer</code>s, to
 * <code>SourceStream</code>s reading data in <code>Buffer</code>s. An example
 * use is creating a PushBufferStream representation of a PushSourceStream.
 * 
 * @author Lubomir Marinov
 */
public abstract class BufferStreamAdapter<T extends SourceStream>
    implements SourceStream
{

    /**
     * The <code>Format</code> of this stream to be reported through the output
     * <code>Buffer</code> this instance reads data into.
     */
    private final Format format;

    /**
     * The <code>SourceStream</code> being adapted by this instance.
     */
    protected final T stream;

    /**
     * Initializes a new <code>BufferStreamAdapter</code> which is to adapt a
     * specific <code>SourceStream</code> into a <code>SourceStream</code> with
     * a specific <code>Format</code>. 
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
     * Gets the <code>Format</code> of the data this stream provides.
     * 
     * @return the <code>Format</code> of the data this stream provides
     */
    public Format getFormat()
    {
        return format;
    }

    /**
     * Reads byte data from this stream into a specific <code>Buffer</code>
     * which is to use a specific array of bytes for its data.
     * 
     * @param buffer the <code>Buffer</code> to read byte data into from this
     *            instance
     * @param bytes the array of <code>byte</code>s to read data into from this
     *            instance and to be set as the data of the specified
     *            <code>buffer</code>
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
     * <code>byte</code>s starting the storing at a specific offset and reading
     * at most a specific number of bytes.
     * 
     * @param buffer the array of <code>byte</code>s into which the data read
     *            from this stream is to be written
     * @param offset the offset in the specified <code>buffer</code> at which
     *            writing data read from this stream should start 
     * @param length the maximum number of bytes to be written into the
     *            specified <code>buffer</code>
     * @return the number of bytes read from this stream and written into the
     *         specified <code>buffer</code>
     * @throws IOException
     */
    protected abstract int read(byte[] buffer, int offset, int length)
        throws IOException;
}
