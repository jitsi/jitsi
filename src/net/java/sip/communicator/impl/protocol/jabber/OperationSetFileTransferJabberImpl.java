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

package net.java.sip.communicator.impl.protocol.jabber;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.event.FileTransferListener;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.*;
import org.jivesoftware.smackx.packet.*;

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
     * An active instance of the opSetPersPresence operation set.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * The Jabber file transfer manager.
     */
    private FileTransferManager manager = null;

    /**
     * The Jabber file transfer listener.
     */
    private FileTransferRequestListener fileTransferRequestListener;

    /**
     * A list of listeners registered for file transfer events.
     */
    private Vector<FileTransferListener> fileTransferListeners
        = new Vector<FileTransferListener>();

    // Register file transfer features on every established connection
    // to make sure we register them before creating our
    // ServiceDiscoveryManager
    static
    {
        Connection.addConnectionCreationListener(new ConnectionCreationListener()
        {
            public void connectionCreated(Connection connection)
            {
                FileTransferNegotiator.getInstanceFor(connection);
            }
        });
    }

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

        // use only ibb for file transfers
        FileTransferNegotiator.IBB_ONLY = true;
    }

    /**
     * Sends a file transfer request to the given <tt>toContact</tt>.
     * @return the transfer object
     *
     * @param toContact the contact that should receive the file
     * @param file file to send
     */
    public FileTransfer sendFile(   Contact toContact,
                                    File file)
        throws  IllegalStateException,
                IllegalArgumentException,
                OperationNotSupportedException
    {
        return sendFile(toContact, file, null);
    }

    /**
     * Sends a file transfer request to the given <tt>toContact</tt>.
     * @return the transfer object
     *
     * @param toContact the contact that should receive the file
     * @param file file to send
     * @param gw special gateway to be used for receiver if its jid
     * misses the domain part
     */
    FileTransfer sendFile(Contact toContact,
                          File file,
                          String gw)
        throws  IllegalStateException,
                IllegalArgumentException,
                OperationNotSupportedException
    {
        OutgoingFileTransferJabberImpl outgoingTransfer = null;

        try
        {
            assertConnected();

            if(file.length() > getMaximumFileLength())
                throw new IllegalArgumentException(
                    "File length exceeds the allowed one for this protocol");

            String fullJid = null;
            // Find the jid of the contact which support file transfer
            // and is with highest priority if more than one found
            // if we have equals priorities
            // choose the one that is more available
            OperationSetMultiUserChat mucOpSet = jabberProvider
                .getOperationSet(OperationSetMultiUserChat.class);
            if(mucOpSet != null
                && mucOpSet.isPrivateMessagingContact(toContact.getAddress()))
            {
                fullJid = toContact.getAddress();
            }
            else
            {
                Iterator<Presence> iter = jabberProvider.getConnection().getRoster()
                    .getPresences(toContact.getAddress());
                int bestPriority = -1;
                
                PresenceStatus jabberStatus = null;
    
                while(iter.hasNext())
                {
                    Presence presence = iter.next();
    
                    if(jabberProvider.isFeatureListSupported(presence.getFrom(),
                        new String[]{"http://jabber.org/protocol/si",
                            "http://jabber.org/protocol/si/profile/file-transfer"}))
                    {
    
                        int priority =
                            (presence.getPriority() == Integer.MIN_VALUE) ?
                                0 : presence.getPriority();
    
                        if(priority > bestPriority)
                        {
                            bestPriority = priority;
                            fullJid = presence.getFrom();
                            jabberStatus = OperationSetPersistentPresenceJabberImpl
                                .jabberStatusToPresenceStatus(
                                    presence, jabberProvider);
                        }
                        else if(priority == bestPriority && jabberStatus != null)
                        {
                            PresenceStatus tempStatus =
                                OperationSetPersistentPresenceJabberImpl
                                   .jabberStatusToPresenceStatus(
                                       presence, jabberProvider);
                            if(tempStatus.compareTo(jabberStatus) > 0)
                            {
                                fullJid = presence.getFrom();
                                jabberStatus = tempStatus;
                            }
                        }
                    }
                }
            }

            // First we check if file transfer is at all supported for this
            // contact.
            if (fullJid == null)
            {
                throw new OperationNotSupportedException(
                    "Contact client or server does not support file transfers.");
            }

            if(gw != null
               && !fullJid.contains("@")
               && !fullJid.endsWith(gw))
            {
                fullJid = fullJid + "@" + gw;
            }

            OutgoingFileTransfer transfer
                = manager.createOutgoingFileTransfer(fullJid);

            outgoingTransfer
                = new OutgoingFileTransferJabberImpl(
                    toContact, file, transfer, jabberProvider);

            // Notify all interested listeners that a file transfer has been
            // created.
            FileTransferCreatedEvent event
                = new FileTransferCreatedEvent(outgoingTransfer, new Date());

            fireFileTransferCreated(event);

            // Send the file through the Jabber file transfer.
            transfer.sendFile(file, "Sending file");

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
                IllegalArgumentException,
                OperationNotSupportedException
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
        {
            // if we are not registered but the current status is online
            // change the current status
            if(opSetPersPresence.getPresenceStatus().isOnline())
            {
                opSetPersPresence.fireProviderStatusChangeEvent(
                    opSetPersPresence.getPresenceStatus(),
                    jabberProvider.getJabberStatusEnum().getStatus(
                        JabberStatusEnum.OFFLINE));
            }

            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to send a file.");
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
            if (logger.isDebugEnabled())
                logger.debug("The provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                opSetPersPresence =
                    (OperationSetPersistentPresenceJabberImpl) jabberProvider
                        .getOperationSet(OperationSetPersistentPresence.class);

                // Create the Jabber FileTransferManager.
                manager = new FileTransferManager(
                            jabberProvider.getConnection());

                fileTransferRequestListener = new FileTransferRequestListener();

                ProviderManager.getInstance().addIQProvider(
                    FileElement.ELEMENT_NAME,
                    FileElement.NAMESPACE,
                    new FileElement());

                ProviderManager.getInstance().addIQProvider(
                    ThumbnailIQ.ELEMENT_NAME,
                    ThumbnailIQ.NAMESPACE,
                    new ThumbnailIQ());

                jabberProvider.getConnection().addPacketListener(
                    fileTransferRequestListener,
                    new AndFilter(  new PacketTypeFilter(StreamInitiation.class),
                                    new IQTypeFilter(IQ.Type.SET)));
            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERED)
            {
                if(fileTransferRequestListener != null
                    && jabberProvider.getConnection() != null)
                {
                    jabberProvider.getConnection().removePacketListener(
                        fileTransferRequestListener);
                }

                ProviderManager providerManager = ProviderManager.getInstance();
                if (providerManager != null)
                {
                    ProviderManager.getInstance().removeIQProvider(
                        FileElement.ELEMENT_NAME,
                        FileElement.NAMESPACE);

                    ProviderManager.getInstance().removeIQProvider(
                        ThumbnailIQ.ELEMENT_NAME,
                        ThumbnailIQ.NAMESPACE);
                }

                fileTransferRequestListener = null;
                manager = null;
            }
        }
    }

    /**
     * Listener for Jabber incoming file transfer requests.
     */
    private class FileTransferRequestListener implements PacketListener
    {
        /**
         * Listens for file transfer packets.
         * @param packet packet to be processed
         */
        public void processPacket(Packet packet)
        {
            if (!(packet instanceof StreamInitiation))
                return;

            if (logger.isDebugEnabled())
                logger.debug("Incoming Jabber file transfer request.");

            StreamInitiation streamInitiation = (StreamInitiation) packet;

            FileTransferRequest jabberRequest
                = new FileTransferRequest(manager, streamInitiation);

            // Create a global incoming file transfer request.
            IncomingFileTransferRequestJabberImpl incomingFileTransferRequest
                = new IncomingFileTransferRequestJabberImpl(
                        jabberProvider,
                        OperationSetFileTransferJabberImpl.this,
                        jabberRequest);

            // Send a thumbnail request if a thumbnail is advertised in the
            // streamInitiation packet.
            org.jivesoftware.smackx.packet.StreamInitiation.File file
                = streamInitiation.getFile();

            boolean isThumbnailedFile = false;
            if (file instanceof FileElement)
            {
                ThumbnailElement thumbnailElement
                    = ((FileElement) file).getThumbnailElement();

                if (thumbnailElement != null)
                {
                    isThumbnailedFile = true;
                    incomingFileTransferRequest
                        .createThumbnailListeners(thumbnailElement.getCid());

                    ThumbnailIQ thumbnailRequest
                        = new ThumbnailIQ(  streamInitiation.getTo(),
                                            streamInitiation.getFrom(),
                                            thumbnailElement.getCid(),
                                            IQ.Type.GET);

                    if (logger.isDebugEnabled())
                        logger.debug("Sending thumbnail request:"
                        + thumbnailRequest.toXML());

                    jabberProvider.getConnection().sendPacket(thumbnailRequest);
                }
            }

            if (!isThumbnailedFile)
            {
                // Create an event associated to this global request.
                FileTransferRequestEvent fileTransferRequestEvent
                    = new FileTransferRequestEvent(
                        OperationSetFileTransferJabberImpl.this,
                        incomingFileTransferRequest,
                        new Date());

                // Notify the global listener that a request has arrived.
                fireFileTransferRequest(fileTransferRequestEvent);
            }
        }
    }

    /**
     * Delivers the specified event to all registered file transfer listeners.
     *
     * @param event the <tt>EventObject</tt> that we'd like delivered to all
     * registered file transfer listeners.
     */
    void fireFileTransferRequest(FileTransferRequestEvent event)
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

        /**
         * Thread entry point.
         */
        @Override
        public void run()
        {
            int status;
            long progress;
            String statusReason = "";

            while (true)
            {
                try
                {
                    Thread.sleep(10);

                    status = parseJabberStatus(jabberTransfer.getStatus());
                    progress = fileTransfer.getTransferedBytes();

                    if (status == FileTransferStatusChangeEvent.FAILED
                        || status == FileTransferStatusChangeEvent.COMPLETED
                        || status == FileTransferStatusChangeEvent.CANCELED
                        || status == FileTransferStatusChangeEvent.REFUSED)
                    {
                        if (fileTransfer instanceof
                                OutgoingFileTransferJabberImpl)
                        {
                            ((OutgoingFileTransferJabberImpl) fileTransfer)
                                .removeThumbnailRequestListener();
                        }

                        // sometimes a filetransfer can be preparing
                        // and than completed :
                        // transfered in one iteration of current thread
                        // so it won't go through intermediate state - inProgress
                        // make sure this won't happen
                        if(status == FileTransferStatusChangeEvent.COMPLETED
                            && fileTransfer.getStatus()
                                == FileTransferStatusChangeEvent.PREPARING)
                        {
                            fileTransfer.fireStatusChangeEvent(
                                FileTransferStatusChangeEvent.IN_PROGRESS,
                                "Status changed");
                            fileTransfer.fireProgressChangeEvent(
                                System.currentTimeMillis(), progress);
                        }

                        break;
                    }

                    fileTransfer.fireStatusChangeEvent(status, "Status changed");
                    fileTransfer.fireProgressChangeEvent(
                        System.currentTimeMillis(), progress);
                }
                catch (InterruptedException e)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Unable to sleep thread.", e);
                }
            }

            if (jabberTransfer.getError() != null)
            {
                logger.error("An error occured while transfering file: "
                    +  jabberTransfer.getError().getMessage());
            }

            if (jabberTransfer.getException() != null)
            {
                logger.error("An exception occured while transfering file: ",
                    jabberTransfer.getException());

                if(jabberTransfer.getException() instanceof XMPPException)
                {
                    XMPPError error =
                        ((XMPPException)jabberTransfer.getException())
                            .getXMPPError();
                    if (error != null)
                        if(error.getCode() == 406
                           || error.getCode() == 403)
                            status = FileTransferStatusChangeEvent.REFUSED;
                }

                statusReason = jabberTransfer.getException().getMessage();
            }

            if (initialFileSize > 0
                && status == FileTransferStatusChangeEvent.COMPLETED
                && fileTransfer.getTransferedBytes() < initialFileSize)
            {
                status = FileTransferStatusChangeEvent.CANCELED;
            }

            fileTransfer.fireStatusChangeEvent(status, statusReason);
            fileTransfer.fireProgressChangeEvent(
                System.currentTimeMillis(), progress);
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
            return FileTransferStatusChangeEvent.COMPLETED;
        else if (jabberStatus.equals(Status.cancelled))
            return FileTransferStatusChangeEvent.CANCELED;
        else if (jabberStatus.equals(Status.in_progress)
                || jabberStatus.equals(Status.negotiated))
            return FileTransferStatusChangeEvent.IN_PROGRESS;
        else if (jabberStatus.equals(Status.error))
            return FileTransferStatusChangeEvent.FAILED;
        else if (jabberStatus.equals(Status.refused))
            return FileTransferStatusChangeEvent.REFUSED;
        else if (jabberStatus.equals(Status.negotiating_transfer)
                || jabberStatus.equals(Status.negotiating_stream))
            return FileTransferStatusChangeEvent.PREPARING;
        else
             // FileTransfer.Status.initial
            return FileTransferStatusChangeEvent.WAITING;
    }
}
