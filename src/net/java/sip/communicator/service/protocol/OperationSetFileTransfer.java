/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
     */
    public FileTransfer sendFile(   Contact toContact,
                                    File file)
        throws  IllegalStateException,
                IllegalArgumentException;

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
     */
    public FileTransfer sendFile(   Contact toContact, 
                                    Contact fromContact,
                                    String remotePath,
                                    String localPath)
        throws  IllegalStateException,
                IllegalArgumentException;

    /**
     * Adds the given <tt>FileTransferRequestListener</tt> that would listen for
     * <tt>FileTransferRequestEvent</tt>-s and that should be notified every
     * time a file transfer request has been received.
     * 
     * @param listener the <tt>FileTransferRequestListener</tt> to add
     */
    public void addFileTransferRequestListener(
        FileTransferListener listener);

    /**
     * Removes the given <tt>FileTransferRequestListener</tt> that listens for
     * <tt>FileTransferRequestEvent</tt>-s and is notified every time a file
     * transfer request has been received.
     * 
     * @param listener the <tt>FileTransferRequestListener</tt> to remove
     */
    public void removeFileTransferRequestListener(
        FileTransferListener listener);
}
