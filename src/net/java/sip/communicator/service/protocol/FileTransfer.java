/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * The <tt>FileTransfer</tt> interface is meant to be used by parties interested
 * in the file transfer process. It contains information about the status and
 * the progress of the transfer as well as the bytes that have been transfered.
 * 
 * @author Yana Stamcheva
 */
public interface FileTransfer
{
    /**
     * Indicates that the file transfer has been completed.
     */
    public static final int COMPLETED = 0;

    /**
     * Indicates that the file transfer has been canceled.
     */
    public static final int CANCELED = 1;

    /**
     * Indicates that the file transfer has failed.
     */
    public static final int FAILED = 2;

    /**
     * Indicates that the file transfer has been refused.
     */
    public static final int REFUSED = 3;

    /**
     * Indicates that the file transfer is in progress.
     */
    public static final int IN_PROGRESS = 4;

    /**
     * Indicates that the file transfer waits for the recipient to accept the
     * file.
     */
    public static final int WAITING = 5;

    /**
     * Indicates that the file transfer is in negotiation.
     */
    public static final int PREPARING = 6;

    /**
     * Cancels this file transfer. When this method is called transfer should
     * be interrupted.
     */
    public void cancel();

    /**
     * Returns the current status of the transfer. This information could be
     * used from the user interface to show the current status of the transfer.
     * The status is returned as an <tt>int</tt> and could be equal to one of
     * the static constants declared in this interface (i.e. COMPLETED,
     * CANCELED, FAILED, etc.).
     * 
     * @return the current status of the transfer
     */
    public int getStatus();

    /**
     * Returns the number of bytes already transfered through this file transfer.
     * 
     * @return the number of bytes already transfered through this file transfer
     */
    public long getTransferedBytes();

    /**
     * Adds the given <tt>FileTransferStatusListener</tt> to listen for
     * status changes on this file transfer.
     * 
     * @param listener the listener to add
     */
    public void addStatusListener(FileTransferStatusListener listener);

    /**
     * Removes the given <tt>FileTransferStatusListener</tt>.
     * 
     * @param listener the listener to remove
     */
    public void removeStatusListener(FileTransferStatusListener listener);

    /**
     * Adds the given <tt>FileTransferProgressListener</tt> to listen for
     * status changes on this file transfer.
     * 
     * @param listener the listener to add
     */
    public void addProgressListener(FileTransferProgressListener listener);

    /**
     * Removes the given <tt>FileTransferProgressListener</tt>.
     * 
     * @param listener the listener to remove
     */
    public void removeProgressListener(FileTransferProgressListener listener);
}
