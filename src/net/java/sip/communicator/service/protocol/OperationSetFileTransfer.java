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
 * The File Transfer Operation Set provides an interface towards those functions
 * of a given protocol, that allow transferring files among users.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public interface OperationSetFileTransfer
    extends OperationSet
{
    /**
     * Sends a file transfer request to the given <tt>toContact</tt> by
     * specifying the local and remote file path and the <tt>fromContact</tt>,
     * sending the file.
     *
     * @param toContact the contact that should receive the file
     * @param file the file to send
     *
     * @return the transfer object
     *
     * @throws IllegalStateException if the protocol provider is not registered
     * or connected
     * @throws IllegalArgumentException if some of the arguments doesn't fit the
     * protocol requirements
     * @throws OperationNotSupportedException if the given contact client or
     * server does not support file transfers
     */
    public FileTransfer sendFile(   Contact toContact,
                                    File file)
        throws  IllegalStateException,
                IllegalArgumentException,
                OperationNotSupportedException;

    /**
     * Sends a file transfer request to the given <tt>toContact</tt> by
     * specifying the local and remote file path and the <tt>fromContact</tt>,
     * sending the file.
     *
     * @param toContact the contact that should receive the file
     * @param fromContact the contact sending the file
     * @param remotePath the remote file path
     * @param localPath the local file path
     *
     * @return the transfer object
     *
     * @throws IllegalStateException if the protocol provider is not registered
     * or connected
     * @throws IllegalArgumentException if some of the arguments doesn't fit the
     * protocol requirements
     * @throws OperationNotSupportedException if the given contact client or
     * server does not support file transfers.
     */
    public FileTransfer sendFile(   Contact toContact,
                                    Contact fromContact,
                                    String remotePath,
                                    String localPath)
        throws  IllegalStateException,
                IllegalArgumentException,
                OperationNotSupportedException;

    /**
     * Adds the given <tt>FileTransferListener</tt> that would listen for
     * file transfer requests and created file transfers.
     *
     * @param listener the <tt>FileTransferListener</tt> to add
     */
    public void addFileTransferListener(
        FileTransferListener listener);

    /**
     * Removes the given <tt>FileTransferListener</tt> that listens for
     * file transfer requests and created file transfers.
     *
     * @param listener the <tt>FileTransferListener</tt> to remove
     */
    public void removeFileTransferListener(
        FileTransferListener listener);

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * @return the file length that is supported.
     */
    public long getMaximumFileLength();
}
