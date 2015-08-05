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
package net.java.sip.communicator.impl.protocol.icq;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.rvcmd.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.*;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.*;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.*;

/**
 * The ICQ protocol filetransfer OperationSet.
 *
 * @author Anthony Schmitt
 * @author Damian Minkov
 */
public class OperationSetFileTransferIcqImpl
    implements  OperationSetFileTransfer,
                RvConnectionManagerListener
{
    private static final Logger logger =
        Logger.getLogger(OperationSetFileTransferIcqImpl.class);

    /**
     * A call back to the ICQ provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

    /**
     * A list of listeners registered for file transfer events.
     */
    private ArrayList<FileTransferListener> fileTransferListeners
        = new ArrayList<FileTransferListener>();

    /**
     * Create a new FileTransfer OperationSet over the specified Icq provider
     * @param icqProvider ICQ protocol provider service
     */
    public OperationSetFileTransferIcqImpl(
        ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.icqProvider = icqProvider;

        icqProvider.addRegistrationStateChangeListener(
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

        // Get the aim connection
        AimConnection aimConnection = icqProvider.getAimConnection();

        // Create an outgoing file transfer instance
        OutgoingFileTransfer outgoingFileTransfer =
            aimConnection.getIcbmService().getRvConnectionManager().
            createOutgoingFileTransfer(new Screenname(toContact.getAddress()));

        String id = String.valueOf(outgoingFileTransfer.getRvSessionInfo()
                .getRvSession().getRvSessionId());

        FileTransferImpl outFileTransfer = new FileTransferImpl(
            outgoingFileTransfer,
            id, toContact, file, FileTransfer.OUT);

        // Adding the file to the outgoing file transfer
        try
        {
            outgoingFileTransfer.setSingleFile(new File(file.getPath()));
        }
        catch (IOException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Error sending file",e);
            return null;
        }

        // Notify all interested listeners that a file transfer has been
        // created.
        FileTransferCreatedEvent event
            = new FileTransferCreatedEvent(outFileTransfer, new Date());

        fireFileTransferCreated(event);

        // Sending the file
        outgoingFileTransfer.sendRequest(
            new InvitationMessage(""));

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
        if (icqProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                +"service before being able to send a file.");
        else if (!icqProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to send a file.");
    }

    /**
     * Function called when a icq file transfer request arrive
     * @param manager the joustsim manager
     * @param transfer the incoming transfer
     */
    public void handleNewIncomingConnection(
        RvConnectionManager manager, IncomingRvConnection transfer)
    {
        if (transfer instanceof IncomingFileTransfer)
        {
            if (logger.isTraceEnabled())
                logger.trace("Incoming Icq file transfer request " + transfer.getClass());

            if(!(transfer instanceof IncomingFileTransfer))
            {
                logger.warn("Wrong file transfer.");
                return;
            }

            OperationSetPersistentPresenceIcqImpl opSetPersPresence
                = (OperationSetPersistentPresenceIcqImpl)
                    icqProvider.getOperationSet(
                        OperationSetPersistentPresence.class);

            Contact sender =
                opSetPersPresence.findContactByID(
                    transfer.getBuddyScreenname().getFormatted());

            IncomingFileTransfer incomingFileTransfer =
                (IncomingFileTransfer)transfer;

            final Date newDate = new Date();
            final IncomingFileTransferRequest req =
                new IncomingFileTransferRequestIcqImpl(
                        icqProvider,
                        this,
                        incomingFileTransfer, sender, newDate);

            // this handels when we receive request and before accept or decline
            // it we receive cancel
            transfer.addEventListener(new RvConnectionEventListener() {
                public void handleEventWithStateChange(
                    RvConnection transfer,
                    RvConnectionState state,
                    RvConnectionEvent event)
                {
                    if (state==FileTransferState.FAILED
                        && event instanceof BuddyCancelledEvent)
                    {
                        fireFileTransferRequestCanceled(
                            new FileTransferRequestEvent(
                                OperationSetFileTransferIcqImpl.this,
                                req,
                                newDate));
                    }
                }

                public void handleEvent(RvConnection arg0, RvConnectionEvent arg1)
                {}
            });

            fireFileTransferRequest(
                new FileTransferRequestEvent(this, req, newDate));
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
                AimConnection aimConnection = icqProvider.getAimConnection();
                aimConnection.getIcbmService().getRvConnectionManager().
                    addConnectionManagerListener(OperationSetFileTransferIcqImpl.this);
            }
        }
    }
}
