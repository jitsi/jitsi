package net.java.sip.communicator.impl.protocol.mock;

import java.io.*;

import java.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A mock implementation of a basic telephony opearation set
 *
 * @author Damian Minkov
 */
public class MockOperationSetFileTransfer
    implements OperationSetFileTransfer
{
    private static final Logger logger =
        Logger.getLogger(MockOperationSetFileTransfer.class);

    /**
     * A list of listeners registered for file transfer events.
     */
    private Vector<FileTransferListener> fileTransferListeners
        = new Vector<FileTransferListener>();

    /**
     * A reference to the <tt>ProtocolProviderServiceSipImpl</tt> instance
     * that created us.
     */
    private MockProvider protocolProvider = null;

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
     * @param fromContact the contact sending the file
     * @param remotePath the remote file path
     * @param localPath the local file path
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
     * @param fileTransfer the <tt>FileTransfer</tt> that we'd like delivered to
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
