/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.control.*;

/**
 * Provides a base implementation of <tt>PushBufferStream</tt> in order to
 * facilitate implementers by taking care of boilerplate in the most common
 * cases.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractPushBufferStream
    extends ControlsAdapter
    implements PushBufferStream
{

    /**
     * The (default) <tt>ContentDescriptor</tt> of the
     * <tt>AbstractPushBufferStream</tt> instances.
     */
    private static final ContentDescriptor CONTENT_DESCRIPTOR
        = new ContentDescriptor(ContentDescriptor.RAW);

    /**
     * The <tt>BufferTransferHandler</tt> which is notified by this
     * <tt>PushBufferStream</tt> when data is available for reading.
     */
    protected BufferTransferHandler transferHandler;

    /**
     * Determines whether the end of this <tt>SourceStream</tt> has been
     * reached. The <tt>AbstractPushBufferStream</tt> implementation always
     * returns <tt>false</tt>.
     *
     * @return <tt>true</tt> if the end of this <tt>SourceStream</tt> has been
     * reached; otherwise, <tt>false</tt>
     */
    public boolean endOfStream()
    {
        return false;
    }

    /**
     * Gets a <tt>ContentDescriptor</tt> which describes the type of the content
     * made available by this <tt>SourceStream</tt>. The
     * <tt>AbstractPushBufferStream</tt> implementation always returns a
     * <tt>ContentDescriptor</tt> with content type equal to
     * <tt>ContentDescriptor#RAW</tt>.
     *
     * @return a <tt>ContentDescriptor</tt> which describes the type of the
     * content made available by this <tt>SourceStream</tt>
     */
    public ContentDescriptor getContentDescriptor()
    {
        return CONTENT_DESCRIPTOR;
    }

    /**
     * Gets the length in bytes of the content made available by this
     * <tt>SourceStream</tt>. The <tt>AbstractPushBufferStream</tt>
     * implementation always returns <tt>LENGTH_UNKNOWN</tt>.
     *
     * @return the length in bytes of the content made available by this
     * <tt>SourceStream</tt> if it is known; otherwise, <tt>LENGTH_UKNOWN</tt>
     */
    public long getContentLength()
    {
        return LENGTH_UNKNOWN;
    }

    /**
     * Sets the <tt>BufferTransferHandler</tt> which is to be notified by this
     * <tt>PushBufferStream</tt> when data is available for reading.
     *
     * @param transferHandler the <tt>BufferTransferHandler</tt> which is to be
     * notified by this <tt>PushBufferStream</tt> when data is available for
     * reading
     */
    public void setTransferHandler(BufferTransferHandler transferHandler)
    {
        this.transferHandler = transferHandler;
    }
}
