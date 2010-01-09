/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol;

import java.io.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.util.*;

/**
 * Provides a base implementation of <tt>PushBufferStream</tt> in order to
 * facilitate implementers by taking care of boilerplate in the most common
 * cases.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractPushBufferStream
    extends AbstractControls
    implements PushBufferStream
{

    /**
     * The <tt>Logger</tt> used by the <tt>AbstractPushBufferStream</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractPushBufferStream.class);

    /**
     * The (default) <tt>ContentDescriptor</tt> of the
     * <tt>AbstractPushBufferStream</tt> instances.
     */
    private static final ContentDescriptor CONTENT_DESCRIPTOR
        = new ContentDescriptor(ContentDescriptor.RAW);

    /**
     * The <tt>FormatControl</tt> which gives access to the <tt>Format</tt> of
     * the media data provided by this <tt>SourceStream</tt> and which,
     * optionally, allows setting it.
     */
    private final FormatControl formatControl;

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
        this.formatControl = formatControl;
    }

    /**
     * Releases the resources used by this instance throughout its existence and
     * makes it available for garbage collection. This instance is considered
     * unusable after closing.
     */
    public void close()
    {
        try
        {
            stop();
        }
        catch (IOException ioex)
        {
            logger.error("Failed to stop " + getClass().getSimpleName(), ioex);
        }
    }

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
     * Implements {@link Controls#getControls()}. Gets the controls available
     * for this instance.
     *
     * @return an array of <tt>Object</tt>s which represent the controls
     * available for this instance
     */
    public Object[] getControls()
    {
        if (formatControl != null)
            return new Object[] { formatControl };
        else
            return ControlsAdapter.EMPTY_CONTROLS;
    }

    /**
     * Gets the <tt>Format</tt> of the media data made available by this
     * <tt>PushBufferStream</tt>.
     *
     * @return the <tt>Format</tt> of the media data made available by this
     * <tt>PushBufferStream</tt>
     */
    public Format getFormat()
    {
        return (formatControl == null) ? null : formatControl.getFormat();
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

    /**
     * Starts the transfer of media data from this <tt>PushBufferStream</tt>.
     *
     * @throws IOException if anything goes wrong while starting the transfer of
     * media data from this <tt>PushBufferStream</tt>
     */
    public abstract void start()
        throws IOException;

    /**
     * Stops the transfer of media data from this <tt>PushBufferStream</tt>.
     *
     * @throws IOException if anything goes wrong while stopping the transfer of
     * media data from this <tt>PushBufferStream</tt>
     */
    public abstract void stop()
        throws IOException;
}
