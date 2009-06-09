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
 * Represents a <code>PushBufferStream</code> which reads its data from a
 * specific <code>PushSourceStream</code>.
 * 
 * @author Lubomir Marinov
 */
public class PushBufferStreamAdapter
    extends BufferStreamAdapter<PushSourceStream>
    implements PushBufferStream
{

    /**
     * Initializes a new <code>PushBufferStreamAdapter</code> instance which
     * reads its data from a specific <code>PushSourceStream</code> with a
     * specific <code>Format</code>
     * 
     * @param stream the <code>PushSourceStream</code> the new instance is to
     *            read its data from
     * @param format the <code>Format</code> of the specified input
     *            <code>stream</code> and of the new instance
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
