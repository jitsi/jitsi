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
 * A listener that would gather events notifying of incoming file transfer
 * requests.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public interface FileTransferListener
    extends EventListener
{
    /**
     * Called when a new <tt>IncomingFileTransferRequest</tt> has been received.
     * 
     * @param event the <tt>FileTransferRequestEvent</tt> containing the newly
     * received request and other details.
     */
    public void fileTransferRequestReceived(FileTransferRequestEvent event);

    /**
     * Called when an incoming or outgoing <tt>FileTransfer</tt> has been
     * created.
     * @param fileTransfer the file transfer object that has been created
     */
    public void fileTransferCreated(FileTransfer fileTransfer);
}
