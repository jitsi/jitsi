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
package net.java.sip.communicator.impl.protocol.sip;

import java.io.*;
import java.util.*;

import javax.sdp.*;

import net.java.sip.communicator.impl.protocol.sip.sdp.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The MSRP file transfer OperationSet.
 * Modelled after RFC 5547
 *
 * @author Tom Uijldert
 */
public class OperationSetFileTransferMsrpImpl
    implements  OperationSetFileTransfer
{
    private static final Logger logger =
        Logger.getLogger(OperationSetFileTransferMsrpImpl.class);

    /**
     * A call back to the provider that created us.
     */
    private ProtocolProviderServiceSipImpl sipProvider = null;

    /** We need SIP signalling to establish a chat-session  */
    private OperationSetBasicTelephonySipImpl opsetTelephony;

    private OperationSetPresenceSipImpl opSetPersPresence = null;

    private List<CallPeerMsrpImpl> transferList = null;

    /**
     * A list of listeners registered for file transfer events.
     */
    private ArrayList<FileTransferListener> fileTransferListeners
        = new ArrayList<FileTransferListener>();

    /**
     * Create a new FileTransfer OperationSet for this Sip provider
     * @param sipProvider SIP protocol provider service
     */
    public OperationSetFileTransferMsrpImpl(
        ProtocolProviderServiceSipImpl sipProvider)
    {
        this.sipProvider = sipProvider;

        sipProvider.addRegistrationStateChangeListener(
            new RegistrationStateListener());
        this.opsetTelephony = (OperationSetBasicTelephonySipImpl)
            sipProvider.getOperationSet(OperationSetBasicTelephony.class);
        this.transferList = Collections.synchronizedList(
            new ArrayList<CallPeerMsrpImpl>());
    }

    /**
     * Sends a file transfer request to the given <tt>toContact</tt> by
     * specifying the file.
     *
     * @param toContact the contact that should receive the file
     * @param file the file to send
     *
     * @return the transfer object
     *
     * @throws IllegalStateException if the protocol provider is not registered
     * or connected
     * @throws IllegalArgumentException if some of the arguments don't fit the
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
        FileTransferImpl activity = null;
        try
        {
            activity = new FileTransferImpl(
                null, null, toContact, file, FileTransfer.OUT);
            CallPeerMsrpImpl peer = (CallPeerMsrpImpl)
                opsetTelephony.createFileTransfer(toContact, activity)
                        .getCallPeers().next();
            transferList.add(peer);
            activity.fireStatusChangeEvent(
                FileTransferStatusChangeEvent.PREPARING);
        }
        catch (Exception e)
        {
            activity.fireStatusChangeEvent(
                FileTransferStatusChangeEvent.FAILED, e.getMessage());
        }
        return activity;
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
        if (sipProvider == null)
            throw new IllegalStateException(
            "The provider must be active before being able to send a file.");
        /*
         * TODO: no registration needed?
        else if (!sipProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to send a file.");
         */
    }

    /**
     * Called when a file transfer request arrives
     */
    public void handleTransferRequest(
        CallPeerMsrpImpl peer, MediaDescription request)
    {
        try
        {
            if (SdpUtils.isSendOnly(request))   // TODO: also handle pull req.
            {
                String id = peer.getAddress();
                Contact contact = opSetPersPresence.resolveContactID(id);
                if (contact == null)
                {
                    contact = opSetPersPresence.createVolatileContact(
                                id, peer.getDisplayName());
                }
                logger.debug("Contact: " + contact.getDisplayName()
                    + " requested file transfer");
                final Date date = new Date();
                final IncomingFileTransferRequest req =
                    new IncomingFileTransferRequestSipImpl(
                                    peer, this, request, contact, date);
                fireFileTransferRequest(
                    new FileTransferRequestEvent(this, req, date));

                transferList.add(peer);
            }
        }
        catch (SdpParseException e)
        {
            logger.warn("Error handling transfer request: ", e);
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
     * Returns the maximum file length supported, in bytes.
     *
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        return 2048*1024;   // Let's cap it at 20M for now
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
                    (OperationSetPresenceSipImpl) sipProvider
                        .getOperationSet(OperationSetPersistentPresence.class);
            }
        }
    }

    /**
     * Find the peer handler handling the transfer with this id.
     * @param id    the transfer-id
     * @return      the peer handler (null when not found)
     */
    protected CallPeerMsrpImpl getTransferHandler(String id)
    {
        synchronized(transferList)
        {
            for (CallPeerMsrpImpl peer : transferList)
                if (peer.getTransferActivity().getID().equals(id))
                    return peer;
        }
        return null;
    }
}
