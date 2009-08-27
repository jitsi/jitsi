/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;
import net.sf.jml.event.*;

/**
 * The Msn protocol filetransfer OperationSet.
 *
 * @author Damian Minkov
 */
public class OperationSetFileTransferMsnImpl
    implements  OperationSetFileTransfer
{
    private static final Logger logger =
        Logger.getLogger(OperationSetFileTransferMsnImpl.class);

    /**
     * A call back to the Msn provider that created us.
     */
    private ProtocolProviderServiceMsnImpl msnProvider = null;

    /**
     * A list of listeners registered for file transfer events.
     */
    private ArrayList<FileTransferListener> fileTransferListeners
        = new ArrayList<FileTransferListener>();

    /**
     * A list of active fileTransfers.
     */
    private Hashtable<String, Object> activeFileTransfers
        = new Hashtable<String, Object>();

    /**
     * Create a new FileTransfer OperationSet over the specified Msn provider
     * @param provider
     */
    public OperationSetFileTransferMsnImpl(
        ProtocolProviderServiceMsnImpl msnProvider)
    {
        this.msnProvider = msnProvider;

        msnProvider.addRegistrationStateChangeListener(
            new RegistrationStateListener());
    }


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
                IllegalArgumentException
    {
        assertConnected();

        if(file.length() > getMaximumFileLength())
                throw new IllegalArgumentException(
                    "File length exceeds the allowed one for this protocol");

        if( !(toContact instanceof ContactMsnImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an msn contact." + toContact);

        MsnFileTransfer ft = msnProvider.getMessenger().
            getFileTransferManager().
                sendFile(
                    ((ContactMsnImpl)toContact).getSourceContact().getEmail(),
                    file);

        if(ft == null)
            new IllegalStateException(
                "A problem occured sending file, contact not found or offline");

        FileTransferImpl outFileTransfer = new FileTransferImpl(
            ft, toContact, file, FileTransfer.OUT);


        // Notify all interested listeners that a file transfer has been
        // created.
        FileTransferCreatedEvent event
            = new FileTransferCreatedEvent(outFileTransfer, new Date());

        fireFileTransferCreated(event);

        outFileTransfer.fireStatusChangeEvent(
            FileTransferStatusChangeEvent.PREPARING);

        return outFileTransfer;
    }

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
        if (msnProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                +"service before being able to send a file.");
        else if (!msnProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to send a file.");
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
     * Delivers the specified event to all registered file transfer listeners.
     *
     * @param event the <tt>EventObject</tt> that we'd like delivered to all
     * registered file transfer listeners.
     */
    void fireFileTransferRequestRejected(FileTransferRequestEvent event)
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

            listener.fileTransferRequestRejected(event);
        }
    }

    /**
     * Delivers the specified event to all registered file transfer listeners.
     *
     * @param event the <tt>EventObject</tt> that we'd like delivered to all
     * registered file transfer listeners.
     */
    void fireFileTransferRequestCanceled(FileTransferRequestEvent event)
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

            listener.fileTransferRequestCanceled(event);
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
        activeFileTransfers.put(
            event.getFileTransfer().getID(), event.getFileTransfer());

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
     * Supports up to 2GB.
     *
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        return 2147483648l;// = 2048*1024*1024;
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
                msnProvider.getMessenger().addFileTransferListener(
                    new FileTransferProtocolListener());
            }
        }
    }

    /**
     * Receives notifications from the slick about new filetransfers
     * and filetransfer changes.
     */
    private class FileTransferProtocolListener
        implements MsnFileTransferListener
    {
        public void fileTransferRequestReceived(MsnFileTransfer ft)
        {
            OperationSetPersistentPresenceMsnImpl opSetPersPresence
            = (OperationSetPersistentPresenceMsnImpl)
                msnProvider.getOperationSet(
                    OperationSetPersistentPresence.class);

            Contact sender = opSetPersPresence.findContactByID(
                ft.getContact().getEmail().getEmailAddress());

            if(sender == null)
                return;

            Date recvDate = new Date();

            IncomingFileTransferRequest req =
                new IncomingFileTransferRequestMsnImpl(
                        OperationSetFileTransferMsnImpl.this,
                        ft, sender, recvDate);

            activeFileTransfers.put(ft.getID(), req);

            fireFileTransferRequest(
                    new FileTransferRequestEvent(
                            OperationSetFileTransferMsnImpl.this, req, recvDate));
        }

        public void fileTransferStarted(MsnFileTransfer ft)
        {
            Object ftObj = activeFileTransfers.get(ft.getID());

            if(ftObj != null && ftObj instanceof FileTransferImpl)
            {
                FileTransferImpl fileTransfer = (FileTransferImpl)ftObj;

                fileTransfer.fireStatusChangeEvent(
                    FileTransferStatusChangeEvent.IN_PROGRESS);
            }
        }

        public void fileTransferProcess(MsnFileTransfer ft)
        {
            Object ftObj = activeFileTransfers.get(ft.getID());

            if(ftObj != null && ftObj instanceof FileTransferImpl)
            {
                FileTransferImpl fileTransfer = (FileTransferImpl)ftObj;

                fileTransfer.setTransferedBytes(ft.getTransferredSize());
                fileTransfer.fireProgressChangeEvent(
                    System.currentTimeMillis(), ft.getTransferredSize());
            }
        }

        public void fileTransferFinished(MsnFileTransfer ft)
        {
            Object ftObj = activeFileTransfers.get(ft.getID());

            if(ftObj == null)
                return;

            if(ftObj instanceof FileTransferImpl)
            {
                FileTransferImpl fileTransfer = (FileTransferImpl)ftObj;

                if(ft.getState() == MsnFileTransferState.COMPLETED)
                    fileTransfer.fireStatusChangeEvent(
                        FileTransferStatusChangeEvent.COMPLETED);
                else if(ft.getState() == MsnFileTransferState.CANCELED)
                    fileTransfer.fireStatusChangeEvent(
                        FileTransferStatusChangeEvent.CANCELED);
                else if(ft.getState() == MsnFileTransferState.REFUSED)
                    fileTransfer.fireStatusChangeEvent(
                        FileTransferStatusChangeEvent.REFUSED);
            }
            else if(ftObj instanceof IncomingFileTransferRequest)
            {
                IncomingFileTransferRequestMsnImpl inReq =
                    (IncomingFileTransferRequestMsnImpl)ftObj;

                if(!inReq.isRejected()
                    && ft.getState() == MsnFileTransferState.CANCELED)
                        fireFileTransferRequestCanceled(
                            new FileTransferRequestEvent(
                                OperationSetFileTransferMsnImpl.this,
                                inReq,
                                inReq.getDate()));
            }

            activeFileTransfers.remove(ft.getID());
        }
    }
}
