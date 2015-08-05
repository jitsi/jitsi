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

import java.io.*;

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
     * File transfer is incoming.
     */
    public static final int IN = 1;

    /**
     * File transfer is outgoing.
     */
    public static final int OUT = 2;

    /**
     * Uniquie ID that is identifying the FileTransfer
     * if the request has been accepted.
     *
     * @return the id.
     */
    public String getID();

    /**
     * Cancels this file transfer. When this method is called transfer should
     * be interrupted.
     */
    public void cancel();

    /**
     * The file transfer direction.
     * @return returns the direction of the file transfer : IN or OUT.
     */
    public int getDirection();

    /**
     * Returns the local file that is being transferred or to which we transfer.
     *
     * @return the file
     */
    public File getLocalFile();

    /**
     * Returns the contact that we are transfering files with.
     * @return the contact.
     */
    public Contact getContact();

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
