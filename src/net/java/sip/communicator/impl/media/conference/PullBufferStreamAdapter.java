/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.conference;

import java.io.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

/**
 * Represents a <code>PullBufferStream</code> which reads its data from a
 * specific <code>PullSourceStream</code>.
 * 
 * @author Lubomir Marinov
 */
public class PullBufferStreamAdapter
    extends BufferStreamAdapter<PullSourceStream>
    implements PullBufferStream
{

    /**
     * Initializes a new <code>PullBufferStreamAdapter</code> instance which
     * reads its data from a specific <code>PullSourceStream</code> with a
     * specific <code>Format</code>
     * 
     * @param stream the <code>PullSourceStream</code> the new instance is to
     *            read its data from
     * @param format the <code>Format</code> of the specified input
     *            <code>stream</code> and of the new instance
     */
    public PullBufferStreamAdapter(PullSourceStream stream, Format format)
    {
        super(stream, format);
    }

    /**
     * Gets the frame size measured in bytes defined by a specific
     * <code>Format</code>.
     * 
     * @param format the <code>Format</code> to determine the frame size in
     *            bytes of
     * @return the frame size measured in bytes defined by the specified
     *         <code>Format</code>
     */
    private static int getFrameSizeInBytes(Format format)
    {
        AudioFormat audioFormat = (AudioFormat) format;
        int frameSizeInBits = audioFormat.getFrameSizeInBits();

        if (frameSizeInBits <= 0)
            return
                (audioFormat.getSampleSizeInBits() / 8)
                    * audioFormat.getChannels();
        return (frameSizeInBits <= 8) ? 1 : (frameSizeInBits / 8);
    }

    /*
     * Implements PullBufferStream#read(Buffer). Delegates to the wrapped
     * PullSourceStream by either allocating a new byte[] buffer or using the
     * existing one in the specified Buffer.
     */
    public void read(Buffer buffer)
        throws IOException
    {
        Object data = buffer.getData();
        byte[] bytes = null;

        if (data != null)
        {
            if (data instanceof byte[])
                bytes = (byte[]) data;
            else if (data instanceof short[])
            {
                short[] shorts = (short[]) data;

                bytes = new byte[2 * shorts.length];
            }
            else if (data instanceof int[])
            {
                int[] ints = (int[]) data;

                bytes = new byte[4 * ints.length];
            }
        }
        if (bytes == null)
        {
            int frameSizeInBytes = getFrameSizeInBytes(getFormat());

            bytes
                = new byte[
                        1024 * ((frameSizeInBytes <= 0) ? 4 : frameSizeInBytes)];
        }

        read(buffer, bytes);
    }

    /*
     * Implements BufferStreamAdapter#read(byte[], int, int). Delegates to the
     * wrapped PullSourceStream.
     */
    protected int read(byte[] buffer, int offset, int length)
        throws IOException
    {
        return stream.read(buffer, offset, length);
    }

    /*
     * Implements PullBufferStream#willReadBlock(). Delegates to the wrapped
     * PullSourceStream.
     */
    public boolean willReadBlock()
    {
        return stream.willReadBlock();
    }
}
