/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * OperationSetFileTransferSSHImpl.java
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 *
 */

package net.java.sip.communicator.impl.protocol.ssh;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * This class provides operations to upload/download files to remote machines
 *
 * @author Shobhit Jindal
 */
public class OperationSetFileTransferSSHImpl
        implements OperationSetFileTransfer
{
    private static final Logger logger
            = Logger.getLogger(OperationSetFileTransferSSHImpl.class);
    
    /**
     * Currently registered message listeners.
     */
    private Vector fileTransferListeners = new Vector();
    
    /**
     * The currently valid persistent presence operation set..
     */
    private OperationSetPersistentPresenceSSHImpl opSetPersPresence = null;
    
    /**
     * The currently valid ssh instant messaging operation set
     */
    private OperationSetBasicInstantMessagingSSHImpl instantMessaging = null;
    
    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceSSHImpl parentProvider = null;
    
    
    /** Creates a new instance of OperationSetFileTransferSSHImpl */
    public OperationSetFileTransferSSHImpl(
            ProtocolProviderServiceSSHImpl        parentProvider)
    {
        this.parentProvider = parentProvider;
        this.opSetPersPresence = (OperationSetPersistentPresenceSSHImpl)
                parentProvider.getOperationSet(OperationSetPersistentPresence.class);
        this.instantMessaging = (OperationSetBasicInstantMessagingSSHImpl)
                parentProvider.getOperationSet(OperationSetBasicInstantMessaging.class);
    }
    
    /**
     * Registers a FileTransferListener with this operation set so that it gets
     * notifications of start, complete, failure of file transfers
     *
     * @param listener the <tt>FileListener</tt> to register.
     */
    public void addFileListener(FileListener listener)
    {
        if(!fileTransferListeners.contains(listener))
            fileTransferListeners.add(listener);
    }
    
    /**
     * The file transfer method to/from the remote machine
     * either toContact is null(we are downloading file from remote machine
     * or fromContact is null(we are uploading file to remote machine
     *
     * @param toContact - the file recipient
     * @param fromContact - the file sender
     * @param remotePath - the identifier for the remote file
     * @param localPath - the identifier for the local file
     */
    public void sendFile(
            Contact toContact,
            Contact fromContact,
            String remotePath,
            String localPath)
    {
        if(toContact == null)
        {
            SSHFileTransferDaemon fileTransferDaemon
                    = new SSHFileTransferDaemon(
                    (ContactSSH)fromContact,
                    parentProvider);
            
            if(localPath.endsWith(System.getProperty("file.separator")))
                localPath += remotePath.substring(remotePath.lastIndexOf(
                        System.getProperty("file.separator")) + 1);
                
                fileTransferDaemon.downloadFile(
                    remotePath,
                    localPath);
            
            return;
        }
        else if(fromContact == null)
        {
            SSHFileTransferDaemon fileTransferDaemon
                    = new SSHFileTransferDaemon(
                    (ContactSSH) toContact,
                    parentProvider);
            
            fileTransferDaemon.uploadFile(
                    remotePath,
                    localPath);
            
            return;
        }
        
        // code should not reach here
        // assert false;
        logger.error("we should not be here !");
    }

}
