/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import javax.media.control.*;
import javax.media.protocol.*;

/**
 * Provides a base implementation of <tt>PushBufferStream</tt> in order to
 * facilitate implementers by taking care of boilerplate in the most common
 * cases.
 *
 * @author Lyubomir Marinov
 */
public abstract class AbstractPushBufferStream
    extends AbstractBufferStream
    implements PushBufferStream
{

    /**
     * The <tt>BufferTransferHandler</tt> which is notified by this
     * <tt>PushBufferStream</tt> when data is available for reading.
     */
    protected BufferTransferHandler transferHandler;

    /**
     * Initializes a new <tt>AbstractPushBufferStream</tt> instance which is to
     * have its <tt>Format</tt>-related information abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     */
    protected AbstractPushBufferStream(FormatControl formatControl)
    {
        super(formatControl);
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
