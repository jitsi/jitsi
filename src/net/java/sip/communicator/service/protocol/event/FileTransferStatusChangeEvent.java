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
 * The <tt>FileTransferStatusChangeEvent</tt> is the event indicating of a
 * change in the state of a file transfer.
 * 
 * @author Yana Stamcheva
 */
public class FileTransferStatusChangeEvent
    extends EventObject
{
    /**
     * The state of the file transfer before this event occured.
     */
    private final int oldStatus;

    /**
     * The new state of the file transfer.
     */
    private final int newStatus;

    /**
     * Creates a <tt>FileTransferStatusChangeEvent</tt> by specifying the
     * source <tt>fileTransfer</tt>, the old transfer status and the new status.
     * 
     * @param fileTransfer the source file transfer, for which this status
     * change occured
     * @param oldStatus the old status
     * @param newStatus the new status
     */
    public FileTransferStatusChangeEvent(   FileTransfer fileTransfer,
                                            int oldStatus,
                                            int newStatus)
    {
        super(fileTransfer);

        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
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
     * Returns the state of the file transfer before this event occured.
     * 
     * @return the old state
     */
    public int getOldStatus()
    {
        return oldStatus;
    }

    /**
     * The new state of the file transfer.
     * 
     * @return the new state
     */
    public int getNewStatus()
    {
        return newStatus;
    }
}
