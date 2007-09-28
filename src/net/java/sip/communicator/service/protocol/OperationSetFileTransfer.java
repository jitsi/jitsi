/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * The File Transfer Operation Set provides an interface towards those functions
 * of a given protocl, that allow transferring files among users.
 *
 * @todo say that meta contacts must be implemented by the user interface
 *
 * @author Emil Ivov
 */
public interface OperationSetFileTransfer
    extends OperationSet
{
    public void sendFile(
            Contact toContact, 
            Contact fromContact,
            String remotePath,
            String localPath);

    public void addFileListener(FileListener listener);
}
