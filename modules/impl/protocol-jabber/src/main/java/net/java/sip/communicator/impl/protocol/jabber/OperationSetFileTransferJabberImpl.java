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
import java.lang.reflect.*;
import java.util.*;
import org.jitsi.xmpp.extensions.thumbnail.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.event.FileTransferListener;
import net.java.sip.communicator.service.protocol.jabberconstants.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.XMPPException.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.roster.*;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.*;
import org.jivesoftware.smackx.si.packet.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.jxmpp.stringprep.*;

import static org.jivesoftware.smack.packet.StanzaError.Condition.*;

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
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetFileTransferJabberImpl.class);

     /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * An active instance of the opSetPersPresence operation set.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * A list of listeners registered for file transfer events.
     */
    private final Vector<FileTransferListener> fileTransferListeners
        = new Vector<>();

    // Register file transfer features on every established connection
    // to make sure we register them before creating our
    // ServiceDiscoveryManager
    static
    {
        XMPPConnectionRegistry.addConnectionCreationListener(
            new ConnectionCreationListener()
        {
            @Override
            public void connectionCreated(XMPPConnection connection)
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
    }

    /**
     * Sends a file transfer request to the given <tt>toContact</tt>.
     * @return the transfer object
     *
     * @param toContact the contact that should receive the file
     * @param file file to send
     */
    public FileTransfer sendFile(Contact toContact, File file)
        throws  IllegalStateException,
                IllegalArgumentException,
                OperationNotSupportedException
    {
        OutgoingFileTransferJabberImpl outgoingTransfer;
        try
        {
            assertConnected();

            if(file.length() > getMaximumFileLength())
                throw new IllegalArgumentException(
                    "File length exceeds the allowed one for this protocol");

            EntityFullJid fullJid = null;
            // Find the jid of the contact which support file transfer
            // and is with highest priority if more than one found
            // if we have equals priorities
            // choose the one that is more available
            OperationSetMultiUserChat mucOpSet = jabberProvider
                .getOperationSet(OperationSetMultiUserChat.class);
            if(mucOpSet != null
                && mucOpSet.isPrivateMessagingContact(toContact.getAddress()))
            {
                fullJid = JidCreate.entityFullFrom(toContact.getAddress());
            }
            else
            {
                Jid jid = JidCreate.from(toContact.getAddress());
                Roster r = Roster.getInstanceFor(jabberProvider.getConnection());
                int bestPriority = -1;
                PresenceStatus jabberStatus = null;

                for (Presence presence : r.getPresences(jid.asBareJid()))
                {
                    if(jabberProvider.isFeatureListSupported(
                        presence.getFrom(),
                        "http://jabber.org/protocol/si",
                        "http://jabber.org/protocol/si/profile/file-transfer"))
                    {

                        int priority =
                            (presence.getPriority() == Integer.MIN_VALUE)
                                ? 0
                                : presence.getPriority();

                        if(priority > bestPriority)
                        {
                            bestPriority = priority;
                            fullJid = presence.getFrom().asEntityFullJidIfPossible();
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
                                fullJid = presence.getFrom().asEntityFullJidIfPossible();
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

            FileTransferManager manager = FileTransferManager
                .getInstanceFor(jabberProvider.getConnection());
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
        catch(XmppStringprepException | SmackException e)
        {
            logger.error("Failed to send file.", e);
            throw new OperationNotSupportedException(
                "Could not start file transfer",
                e
            );
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
        return 2147483648L;// = 2048*1024*1024;
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
        @Override
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (logger.isDebugEnabled())
                logger.debug("The provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());

            if (evt.getNewState() != RegistrationState.REGISTERED)
            {
                return;
            }

            opSetPersPresence =
                (OperationSetPersistentPresenceJabberImpl) jabberProvider
                    .getOperationSet(OperationSetPersistentPresence.class);

            // Create the Jabber FileTransferManager
            FileTransferManager.getInstanceFor(
                jabberProvider.getConnection())
                .addFileTransferListener(new FileTransferRequestListener());
        }
    }

    /**
     * Listener for Jabber incoming file transfer requests.
     */
    private class FileTransferRequestListener
        implements org.jivesoftware.smackx.filetransfer.FileTransferListener
    {
        private StreamInitiation getStreamInitiation(FileTransferRequest request)
        {
            Method gsi;
            try
            {
                gsi = request.getClass().getDeclaredMethod("getStreamInitiation");
                gsi.setAccessible(true);
                return (StreamInitiation)gsi.invoke(request);
            }
            catch (Exception e)
            {
                logger.error("Cannot invoke getStreamInitiation", e);
                return null;
            }
        }

        /**
         * Listens for file transfer packets.
         * @param request packet to be processed
         */
        @Override
        public void fileTransferRequest(final FileTransferRequest request)
        {
            logger.info("Incoming XMPP file transfer request");

            // Create a global incoming file transfer request.
            IncomingFileTransferRequestJabberImpl incomingFileTransferRequest
                = new IncomingFileTransferRequestJabberImpl(
                        jabberProvider,
                        OperationSetFileTransferJabberImpl.this,
                        request);

            // Send a thumbnail request if a thumbnail is advertised in the
            // streamInitiation packet.
            boolean isThumbnailedFile = false;
            StreamInitiation si = getStreamInitiation(request);
            if (si != null)
            {
                StreamInitiation.File file = si.getFile();
                if (file instanceof ThumbnailFile)
                {
                    Thumbnail thumbnail = ((ThumbnailFile)file).getThumbnail();
                    if (thumbnail != null)
                    {
                        isThumbnailedFile = true;
                        incomingFileTransferRequest
                            .fetchThumbnailAndNotify(thumbnail.getCid());
                    }
                }
            }

            if (!isThumbnailedFile)
            {
                // Notify the global listener that a request has arrived.
                fireFileTransferRequest(incomingFileTransferRequest);
            }
        }
    }

    /**
     * Delivers the specified event to all registered file transfer listeners.
     *
     * @param request the <tt>EventObject</tt> that we'd like delivered to all
     * registered file transfer listeners.
     */
    void fireFileTransferRequest(IncomingFileTransferRequestJabberImpl request)
    {
        // Create an event associated to this global request.
        FileTransferRequestEvent event
            = new FileTransferRequestEvent(
                OperationSetFileTransferJabberImpl.this,
                request,
                new Date());

        Iterator<FileTransferListener> listeners;
        synchronized (fileTransferListeners)
        {
            listeners = new ArrayList<>
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
        Iterator<FileTransferListener> listeners;
        synchronized (fileTransferListeners)
        {
            listeners = new ArrayList<>
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
        Iterator<FileTransferListener> listeners;
        synchronized (fileTransferListeners)
        {
            listeners = new ArrayList<>
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
                                .removeThumbnailHandler();
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

                if(jabberTransfer.getException() instanceof XMPPErrorException)
                {
                    StanzaError error = ((XMPPErrorException)
                        jabberTransfer.getException()).getStanzaError();
                    if (error != null)
                        if(error.getCondition() == not_acceptable
                           || error.getCondition() == forbidden)
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
