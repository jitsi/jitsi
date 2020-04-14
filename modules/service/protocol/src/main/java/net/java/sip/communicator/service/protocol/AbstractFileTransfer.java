/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * An abstract implementation of the <tt>FileTransfer</tt> interface providing
 * implementation of status and progress events related methods and leaving all
 * protocol specific methods abstract. A protocol specific implementation could
 * extend this class and implement only <tt>cancel()</tt> and
 * <tt>getTransferredBytes()</tt>.
 *
 * @author Yana Stamcheva
 */
public abstract class AbstractFileTransfer
    implements FileTransfer
{
    private static final Logger logger =
        Logger.getLogger(AbstractFileTransfer.class);

    /**
     * A list of listeners registered for file transfer status events.
     */
    private Vector<FileTransferStatusListener> statusListeners
        = new Vector<FileTransferStatusListener>();

    /**
     * A list of listeners registered for file transfer status events.
     */
    private Vector<FileTransferProgressListener> progressListeners
        = new Vector<FileTransferProgressListener>();

    private int status;

    /**
     * Cancels this file transfer. When this method is called transfer should
     * be interrupted.
     */
    abstract public void cancel();

    /**
     * Returns the number of bytes already transfered through this file transfer.
     *
     * @return the number of bytes already transfered through this file transfer
     */
    abstract public long getTransferedBytes();

    /**
     * Adds the given <tt>FileTransferProgressListener</tt> to listen for
     * status changes on this file transfer.
     *
     * @param listener the listener to add
     */
    public void addProgressListener(FileTransferProgressListener listener)
    {
        synchronized(progressListeners)
        {
            if(!progressListeners.contains(listener))
            {
                this.progressListeners.add(listener);
            }
        }
    }

    /**
     * Adds the given <tt>FileTransferStatusListener</tt> to listen for
     * status changes on this file transfer.
     *
     * @param listener the listener to add
     */
    public void addStatusListener(FileTransferStatusListener listener)
    {
        synchronized(statusListeners)
        {
            if(!statusListeners.contains(listener))
            {
                this.statusListeners.add(listener);
            }
        }
    }

    /**
     * Removes the given <tt>FileTransferProgressListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeProgressListener(FileTransferProgressListener listener)
    {
        synchronized(progressListeners)
        {
            this.progressListeners.remove(listener);
        }
    }

    /**
     * Removes the given <tt>FileTransferStatusListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeStatusListener(FileTransferStatusListener listener)
    {
        synchronized(statusListeners)
        {
            this.statusListeners.remove(listener);
        }
    }

    /**
     * Returns the current status of the transfer. This information could be
     * used from the user interface to show a progress bar indicating the
     * file transfer status.
     *
     * @return the current status of the transfer
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * Notifies all status listeners that a new
     * <tt>FileTransferStatusChangeEvent</tt> occured.
     * @param newStatus the new status
     */
    public void fireStatusChangeEvent(int newStatus)
    {
        this.fireStatusChangeEvent(newStatus, null);
    }

    /**
     * Notifies all status listeners that a new
     * <tt>FileTransferStatusChangeEvent</tt> occured.
     * @param newStatus the new status
     * @param reason the reason of the status change
     */
    public void fireStatusChangeEvent(int newStatus, String reason)
    {
        // ignore if status is the same
        if(this.status == newStatus)
            return;

        Collection<FileTransferStatusListener> listeners = null;
        synchronized (statusListeners)
        {
            listeners
                = new ArrayList<FileTransferStatusListener>(statusListeners);
        }

        if (logger.isDebugEnabled())
            logger.debug("Dispatching a FileTransfer Event to" + listeners.size()
            + " listeners. Status=" + status);

        FileTransferStatusChangeEvent statusEvent
            = new FileTransferStatusChangeEvent(
                this, status, newStatus, reason);

        // Updates the status.
        this.status = newStatus;

        Iterator<FileTransferStatusListener> listenersIter
            = listeners.iterator();

        while (listenersIter.hasNext())
        {
            FileTransferStatusListener statusListener = listenersIter.next();

            statusListener.statusChanged(statusEvent);
        }
    }

    /**
     * Notifies all status listeners that a new
     * <tt>FileTransferProgressEvent</tt> occured.
     * @param timestamp the date on which the event occured
     * @param progress the bytes representing the progress of the transfer
     */
    public void fireProgressChangeEvent(long timestamp, long progress)
    {
        Collection<FileTransferProgressListener> listeners = null;
        synchronized (progressListeners)
        {
            listeners
                = new ArrayList<FileTransferProgressListener>(progressListeners);
        }

        FileTransferProgressEvent progressEvent
            = new FileTransferProgressEvent(this, timestamp, progress);

        Iterator<FileTransferProgressListener> listenersIter
            = listeners.iterator();

        while (listenersIter.hasNext())
        {
            FileTransferProgressListener statusListener = listenersIter.next();

            statusListener.progressChanged(progressEvent);
        }
    }
}
