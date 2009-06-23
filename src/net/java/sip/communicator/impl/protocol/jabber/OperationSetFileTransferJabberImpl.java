/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.protocol.jabber;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.event.FileTransferListener;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.filetransfer.*;
import org.jivesoftware.smackx.filetransfer.FileTransfer.*;

/**
 * The Jabber implementation of the <tt>OperationSetFileTransfer</tt>
 * interface.
 *
 * @author Gregory Bande
 * @author Nicolas Riegel
 * @author Yana Stamcheva
 */
public class OperationSetFileTransferJabberImpl
    implements OperationSetFileTransfer
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetFileTransferJabberImpl.class);

     /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * The Jabber file transfer manager.
     */
    private FileTransferManager manager = null;

    /**
     * The Jabber file transfer listener.
     */
    private JabberFileTransferListener jabberFileTransferListener;

    /**
     * A list of listeners registered for file transfer events.
     */
    private Vector<FileTransferListener> fileTransferListeners
        = new Vector<FileTransferListener>();

    /**
     * Constructor
     * @param provider is the provider that created us
     */
    public OperationSetFileTransferJabberImpl(
            ProtocolProviderServiceJabberImpl provider)
    {
        this.jabberProvider = provider;

        provider.addRegistrationStateChangeListener(
            new RegistrationStateListener());
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
    public FileTransfer sendFile(   Contact toContact,
                                    File file)
        throws  IllegalStateException,
                IllegalArgumentException
    {
        AbstractFileTransfer outgoingTransfer = null;

        try
        {
            assertConnected();

            Roster roster = jabberProvider.getConnection().getRoster();
            Presence presence = roster.getPresence(toContact.getAddress());

            OutgoingFileTransfer transfer
                = manager.createOutgoingFileTransfer(presence.getFrom());

            outgoingTransfer
                = new OutgoingFileTransferJabberImpl(transfer);

            // Notify all interested listeners that a file transfer has been
            // created.
            fireFileTransferCreated(outgoingTransfer);

            // Send the file through the Jabber file transfer.
            transfer.sendFile(file, "Sending file.");

            // Start the status and progress thread.
            new FileTransferProgressThread(
                transfer, outgoingTransfer).start();
        }
        catch(XMPPException e)
        {
            logger.error("Failed to send file.", e);
        }

        return outgoingTransfer;
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
    public FileTransfer sendFile(   Contact toContact,
                            Contact fromContact,
                            String remotePath,
                            String localPath)
        throws  IllegalStateException,
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
    public void addFileTransferListener(
        FileTransferListener listener)
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
    public void removeFileTransferListener(
        FileTransferListener listener)
    {
        synchronized(fileTransferListeners)
        {
            this.fileTransferListeners.remove(listener);
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected()
        throws  IllegalStateException
    {
        if (jabberProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                +"service before being able to send a file.");
        else if (!jabberProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to send a file.");
    }

    /**
     * Our listener that will tell us when we're registered to
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            logger.debug("The provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                // Create the Jabber FileTransferManager.
                manager = new FileTransferManager(
                            jabberProvider.getConnection());

                // Create the Jabber file transfer listener.
                jabberFileTransferListener = new JabberFileTransferListener();

                // Add the Jabber file transfer listener to the manager.
                manager.addFileTransferListener(jabberFileTransferListener);
            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERED)
            {
                if(jabberFileTransferListener != null
                    && manager != null)
                {
                    manager.removeFileTransferListener(
                        jabberFileTransferListener);

                    manager = null;
                    jabberFileTransferListener = null;
                }
            }
        }
    }

    /** 
     * Listener for Jabber incoming file transfer requests.
     */
    private class JabberFileTransferListener
        implements org.jivesoftware.smackx.filetransfer.FileTransferListener
    {
        /**
         * Function called when a jabber file transfer request arrive.
         */
        public void fileTransferRequest(FileTransferRequest request)
        {
            logger.debug("Incoming Jabber file transfer request."); 

            // Create a global incoming file transfer request.
            IncomingFileTransferRequest incomingFileTransferRequest
                = new IncomingFileTransferRequestJabberImpl(
                        jabberProvider,
                        OperationSetFileTransferJabberImpl.this,
                        request);

            // Create an event associated to this global request.
            FileTransferRequestEvent fileTransferRequestEvent
                = new FileTransferRequestEvent( incomingFileTransferRequest,
                                                new Date());

            // Notify the global listener that a request has arrived.
            fireFileTransferRequest(fileTransferRequestEvent);
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
    void fireFileTransferCreated(FileTransfer fileTransfer)
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

            listener.fileTransferCreated(fileTransfer);
        }
    }

    /**
     * Updates file transfer progress and status while sending or receiving a
     * file.
     */
    protected static class FileTransferProgressThread extends Thread
    {
        private final org.jivesoftware.smackx.filetransfer.FileTransfer
            jabberTransfer;
        private final AbstractFileTransfer fileTransfer;

        private long initialFileSize;

        public FileTransferProgressThread(
            org.jivesoftware.smackx.filetransfer.FileTransfer jabberTransfer,
            AbstractFileTransfer transfer,
            long initialFileSize)
        {
            this.jabberTransfer = jabberTransfer;
            this.fileTransfer = transfer;
            this.initialFileSize = initialFileSize;
        }
        
        public FileTransferProgressThread(
            org.jivesoftware.smackx.filetransfer.FileTransfer jabberTransfer,
            AbstractFileTransfer transfer)
        {
            this.jabberTransfer = jabberTransfer;
            this.fileTransfer = transfer;
        }

        public void run()
        {
            int status;
            double progress;

            while (true)
            {
                try
                {
                    Thread.sleep(10);

                    status = parseJabberStatus(jabberTransfer.getStatus());
                    progress = fileTransfer.getTransferedBytes();

                    if (status == FileTransfer.FAILED
                        || status == FileTransfer.COMPLETED
                        || status == FileTransfer.CANCELED
                        || status == FileTransfer.REFUSED)
                    {
                        break;
                    }

                    fileTransfer.fireStatusChangeEvent(status);
                    fileTransfer.fireProgressChangeEvent((int)progress);
                }
                catch (InterruptedException e)
                {
                    logger.debug("Unable to sleep thread.", e);
                }
            }

            if (initialFileSize > 0
                && status == FileTransfer.COMPLETED
                && fileTransfer.getTransferedBytes() < initialFileSize)
            {
                status = FileTransfer.CANCELED;
            }

            fileTransfer.fireStatusChangeEvent(status);
            fileTransfer.fireProgressChangeEvent((int)progress);
        }
    }

    /**
     * Parses the given Jabber status to a <tt>FileTransfer</tt> interface
     * status.
     * 
     * @param jabberStatus the Jabber status to parse
     * @return the parsed status
     */
    private static int parseJabberStatus(Status jabberStatus)
    {
        if (jabberStatus.equals(Status.complete))
            return FileTransfer.COMPLETED;
        else if (jabberStatus.equals(Status.cancelled))
            return FileTransfer.CANCELED;
        else if (jabberStatus.equals(Status.in_progress)
                || jabberStatus.equals(Status.negotiated))
            return FileTransfer.IN_PROGRESS;
        else if (jabberStatus.equals(Status.error))
            return FileTransfer.FAILED;
        else if (jabberStatus.equals(Status.refused))
            return FileTransfer.REFUSED;
        else if (jabberStatus.equals(Status.negotiating_transfer)
                || jabberStatus.equals(Status.negotiating_stream))
            return FileTransfer.PREPARING;
        else
             // FileTransfer.Status.initial
            return FileTransfer.WAITING;
    }
}
