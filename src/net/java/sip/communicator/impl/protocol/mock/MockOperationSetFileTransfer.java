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
package net.java.sip.communicator.impl.protocol.mock;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A mock implementation of a basic telephony operation set
 *
 * @author Damian Minkov
 */
public class MockOperationSetFileTransfer
    implements OperationSetFileTransfer
{

    /**
     * A list of listeners registered for file transfer events.
     */
    private final Vector<FileTransferListener> fileTransferListeners
        = new Vector<FileTransferListener>();

    /**
     * A reference to the <tt>ProtocolProviderServiceSipImpl</tt> instance
     * that created us.
     */
    private final MockProvider protocolProvider;

    public MockOperationSetFileTransfer(MockProvider protocolProvider)
    {
        this.protocolProvider = protocolProvider;
    }

    /**
     * Sends a file transfer request to the given <tt>toContact</tt> by
     * specifying the local and remote file path and the <tt>fromContact</tt>,
     * sending the file.
     *
     * @return the transfer object
     *
     * @param toContact the contact that should receive the file
     * @param file file to send
     */
    public FileTransfer sendFile(Contact toContact, File file)
        throws IllegalStateException,
               IllegalArgumentException
    {
        MockFileTransferImpl fileTrans = new MockFileTransferImpl(
            toContact,
            file,
            generateID(),
            FileTransfer.OUT);

        fireFileTransferCreated(new FileTransferCreatedEvent(fileTrans, new Date()));

        changeFileTransferStatus(fileTrans, FileTransferStatusChangeEvent.PREPARING);

        return fileTrans;
    }

    public void changeFileTransferStatus(FileTransfer ft, int newstatus)
    {
        ((MockFileTransferImpl)ft).fireStatusChangeEvent(newstatus);
    }

    private String generateID()
    {
        return String.valueOf( System.currentTimeMillis()) +
            String.valueOf(hashCode());
    }

    public void receiveFile(final File file,
        final Contact from)
    {
        final Date requestDate = new Date();

        final String id = generateID();

        fireFileTransferRequest(
            new FileTransferRequestEvent(
                this,
                new IncomingFileTransferRequest()
                {
                    public String getID()
                    {
                        return id;
                    }

                    public String getFileName()
                    {
                        return file.getName();
                    }

                    public String getFileDescription()
                    {
                        return file.toString();
                    }

                    public long getFileSize()
                    {
                        return file.length();
                    }

                    public Contact getSender()
                    {
                        return from;
                    }

                    public FileTransfer acceptFile(File file)
                    {
                        MockFileTransferImpl fileTrans =
                            new MockFileTransferImpl(
                                    from,
                                    file,
                                    id,
                                    FileTransfer.IN);

                        fireFileTransferCreated(
                            new FileTransferCreatedEvent(fileTrans, requestDate));

                        changeFileTransferStatus(fileTrans,
                            FileTransferStatusChangeEvent.PREPARING);

                        return fileTrans;
                    }

                    public void rejectFile()
                    {
                    }

                    public byte[] getThumbnail()
                    {
                        return null;
                    }
                }, requestDate));
    }

    /**
     * Sends a file transfer request to the given <tt>toContact</tt> by
     * specifying the local and remote file path and the <tt>fromContact</tt>,
     * sending the file.
     *
     * @return the transfer object
     *
     * @param toContact the contact that should receive the file
     * @param fromContact the contact sending the file
     * @param remotePath the remote file path
     * @param localPath the local file path
     */
    public FileTransfer sendFile(Contact toContact, Contact fromContact, String remotePath, String localPath)
        throws IllegalStateException,
               IllegalArgumentException
    {
        return this.sendFile(toContact, new File(localPath));
    }

    /**
     * Adds the given <tt>FileTransferListener</tt> that would listen for
     * file transfer requests and created file transfers.
     *
     * @param listener the <tt>FileTransferListener</tt> to add
     */
    public void addFileTransferListener(FileTransferListener listener)
    {
        synchronized(fileTransferListeners)
        {
            if(!fileTransferListeners.contains(listener))
            {
                this.fileTransferListeners.add(listener);
            }
        }
    }

    /**
     * Removes the given <tt>FileTransferListener</tt> that listens for
     * file transfer requests and created file transfers.
     *
     * @param listener the <tt>FileTransferListener</tt> to remove
     */
    public void removeFileTransferListener(FileTransferListener listener)
    {
        synchronized(fileTransferListeners)
        {
            this.fileTransferListeners.remove(listener);
        }
    }

        /**
     * Delivers the specified event to all registered file transfer listeners.
     *
     * @param event the <tt>EventObject</tt> that we'd like delivered to all
     * registered file transfer listeners.
     */
    private void fireFileTransferRequest(FileTransferRequestEvent event)
    {
        Iterator<FileTransferListener> listeners = null;
        synchronized (fileTransferListeners)
        {
            listeners = new ArrayList<FileTransferListener>
                            (fileTransferListeners).iterator();
        }

        while (listeners.hasNext())
        {
            FileTransferListener listener = listeners.next();

            listener.fileTransferRequestReceived(event);
        }
    }

    /**
     * Delivers the file transfer to all registered listeners.
     *
     * @param event the <tt>FileTransferEvent</tt> that we'd like delivered to
     * all registered file transfer listeners.
     */
    void fireFileTransferCreated(FileTransferCreatedEvent event)
    {
        Iterator<FileTransferListener> listeners = null;
        synchronized (fileTransferListeners)
        {
            listeners = new ArrayList<FileTransferListener>
                            (fileTransferListeners).iterator();
        }

        while (listeners.hasNext())
        {
            FileTransferListener listener = listeners.next();

            listener.fileTransferCreated(event);
        }
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
