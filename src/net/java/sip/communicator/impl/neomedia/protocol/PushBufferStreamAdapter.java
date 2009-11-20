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
 * Represents a <tt>PushBufferStream</tt> which reads its data from a
 * specific <tt>PushSourceStream</tt>.
 * 
 * @author Lubomir Marinov
 */
public class PushBufferStreamAdapter
    extends BufferStreamAdapter<PushSourceStream>
    implements PushBufferStream
{

    /**
     * Initializes a new <tt>PushBufferStreamAdapter</tt> instance which
     * reads its data from a specific <tt>PushSourceStream</tt> with a
     * specific <tt>Format</tt>
     * 
     * @param stream the <tt>PushSourceStream</tt> the new instance is to
     *            read its data from
     * @param format the <tt>Format</tt> of the specified input
     *            <tt>stream</tt> and of the new instance
     */
    public PushBufferStreamAdapter(PushSourceStream stream, Format format)
    {
        super(stream, format);
    }

    /*
     * Implements PushBufferStream#read(Buffer). Delegates to the wrapped
     * PushSourceStream by allocating a new byte[] buffer of size equal to
     * PushSourceStream#getMinimumTransferSize(). 
     */
    public void read(Buffer buffer)
        throws IOException
    {
        read(buffer, new byte[stream.getMinimumTransferSize()]);
    }

    /*
     * Implements BufferStreamAdapter#read(byte[], int, int). Delegates to the
     * wrapped PushSourceStream.
     */
    protected int read(byte[] buffer, int offset, int length)
        throws IOException
    {
        return stream.read(buffer, offset, length);
    }

    /*
     * Implements PushBufferStream#setTransferHandler(BufferTransferHandler).
     * Delegates to the wrapped PushSourceStream by translating the specified
     * BufferTransferHandler to a SourceTransferHandler.
     */
    public void setTransferHandler(final BufferTransferHandler transferHandler)
    {
        stream.setTransferHandler(new SourceTransferHandler()
        {
            public void transferData(PushSourceStream stream) {
                transferHandler.transferData(PushBufferStreamAdapter.this);
            }
        });
    }
}
