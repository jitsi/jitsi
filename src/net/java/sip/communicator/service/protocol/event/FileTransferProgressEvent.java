/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>FileTransferProgressEvent</tt> indicates the progress of a file
 * transfer.
 *
 * @author Yana Stamcheva
 */
public class FileTransferProgressEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates the progress of a file transfer in bytes.
     */
    private long progress;

    /**
     * Indicates when this event occured.
     */
    private long timestamp;

    /**
     * Creates a <tt>FileTransferProgressEvent</tt> by specifying the source
     * file transfer object, that triggered the event and the new progress
     * value.
     *
     * @param fileTransfer the source file transfer object, that triggered the
     * event
     * @param timestamp when this event occured
     * @param progress the new progress value
     */
    public FileTransferProgressEvent(   FileTransfer fileTransfer,
                                        long timestamp,
                                        long progress)
    {
        super(fileTransfer);

        this.timestamp = timestamp;
        this.progress = progress;
    }

    /**
     * Returns the source <tt>FileTransfer</tt> that triggered this event.
     *
     * @return the source <tt>FileTransfer</tt> that triggered this event
     */
    public FileTransfer getFileTransfer()
    {
        return (FileTransfer) source;
    }

    /**
     * Returns the progress of the file transfer in transferred bytes.
     *
     * @return the progress of the file transfer
     */
    public long getProgress()
    {
        return progress;
    }

    /**
     * Returns the timestamp when this event initially occured.
     *
     * @return the timestamp when this event initially occured
     */
    public long getTimestamp()
    {
        return timestamp;
    }
}
