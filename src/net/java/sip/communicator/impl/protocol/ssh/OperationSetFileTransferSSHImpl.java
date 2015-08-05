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
package net.java.sip.communicator.impl.protocol.ssh;

import java.io.*;
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
    private Vector<FileTransferListener> fileTransferListeners
        = new Vector<FileTransferListener>();

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceSSHImpl parentProvider = null;


    /**
     * Creates a new instance of OperationSetFileTransferSSHImpl
     *
     * @param parentProvider the parent protocol provider service
     */
    public OperationSetFileTransferSSHImpl(
            ProtocolProviderServiceSSHImpl        parentProvider)
    {
        this.parentProvider = parentProvider;
    }

    /**
     * Registers a FileTransferListener with this operation set so that it gets
     * notifications of start, complete, failure of file transfers
     *
     * @param listener the <tt>FileListener</tt> to register.
     */
    public void addFileTransferListener(
        FileTransferListener listener)
    {
        synchronized (fileTransferListeners)
        {
            if(!fileTransferListeners.contains(listener))
                fileTransferListeners.add(listener);
        }
    }

    public void removeFileTransferListener(
        FileTransferListener listener)
    {
        synchronized (fileTransferListeners)
        {
            fileTransferListeners.remove(listener);
        }
    }

    /**
     * Sends a file transfer request to the given <tt>toContact</tt>.
     * @param toContact the contact that should receive the file
     * @param file the file to send
     */
    public FileTransfer sendFile(   Contact toContact,
                            File file)
    {
        return this.sendFile(   toContact,
                                null,
                                file.getAbsolutePath(),
                                file.getAbsolutePath());
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
    public FileTransfer sendFile(
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

            return new FileTransferSSHImpl(fileTransferDaemon, new Date());
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

            return new FileTransferSSHImpl(fileTransferDaemon, new Date());
        }

        // code should not reach here
        // assert false;
        logger.error("we should not be here !");
        return null;
    }

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        return 2048*1024*1024;
    }
}
