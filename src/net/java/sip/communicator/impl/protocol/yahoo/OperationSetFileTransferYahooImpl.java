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
package net.java.sip.communicator.impl.protocol.yahoo;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import ymsg.network.event.*;

/**
 * The Yahoo protocol filetransfer OperationSet.
 *
 * @author Damian Minkov
 */
public class OperationSetFileTransferYahooImpl
    implements  OperationSetFileTransfer,
                SessionFileTransferListener
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetFileTransferYahooImpl.class);

    /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceYahooImpl yahooProvider;

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
     * Constructor
     * @param provider is the provider that created us
     */
    public OperationSetFileTransferYahooImpl(
            ProtocolProviderServiceYahooImpl provider)
    {
        this.yahooProvider = provider;

        provider.addRegistrationStateChangeListener(
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
        try
        {
            assertConnected();

            if(file.length() > getMaximumFileLength())
                throw new IllegalArgumentException(
                    "File length exceeds the allowed one for this protocol");

            ArrayList<String> filesToSend = new ArrayList<String>();
            filesToSend.add(file.getCanonicalPath());
            Date sentDate = new Date();
            String id = yahooProvider.getYahooSession().sendFiles(
                filesToSend, toContact.getAddress());

            FileTransferImpl ft =
                new FileTransferImpl(yahooProvider,
                    id, toContact, file, FileTransfer.OUT);

            // Notify all interested listeners that a file transfer has been
            // created.
            FileTransferCreatedEvent event
                = new FileTransferCreatedEvent(ft, sentDate);

            fireFileTransferCreated(event);

            ft.fireStatusChangeEvent(FileTransferStatusChangeEvent.PREPARING);

            return ft;
        }
        catch(IOException e)
        {
            logger.error("Cannot send fileTransfer", e);
            return null;
        }
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
        if (yahooProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                +"service before being able to send a file.");
        else if (!yahooProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to send a file.");
    }

    /**
     * Delivers the file transfer to all registered listeners.
     *
     * @param event the <tt>FileTransferEvent</tt> that we'd like delivered to
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

    private int getStateMapping(int s)
    {
        switch(s)
        {
            case SessionFileTransferEvent.REFUSED :
                return FileTransferStatusChangeEvent.REFUSED;
            case SessionFileTransferEvent.CANCEL :
                return FileTransferStatusChangeEvent.CANCELED;
            case SessionFileTransferEvent.FAILED :
                return FileTransferStatusChangeEvent.FAILED;
            case SessionFileTransferEvent.IN_PROGRESS :
                return FileTransferStatusChangeEvent.IN_PROGRESS;
            case SessionFileTransferEvent.RECEIVED :
                return FileTransferStatusChangeEvent.COMPLETED;
            case SessionFileTransferEvent.SENT :
                return FileTransferStatusChangeEvent.COMPLETED;
            default: return FileTransferStatusChangeEvent.WAITING;
        }
    }

    /**
     * Starting point for incoming filetransfer.
     * @param ev
     */
    public void fileTransferRequestReceived(SessionFileTransferEvent ev)
    {
        OperationSetPersistentPresenceYahooImpl opSetPersPresence
            = (OperationSetPersistentPresenceYahooImpl)
                yahooProvider.getOperationSet(
                    OperationSetPersistentPresence.class);

        Contact sender = opSetPersPresence.findContactByID(ev.getFrom());

        if(sender == null)
            return;

        Date recvDate = new Date();

        for(int i = 0; i < ev.getFileNames().size(); i++)
        {
            String fileName = ev.getFileNames().get(i);
            String fileSize = ev.getFileSizes().get(i);

            IncomingFileTransferRequest req =
                new IncomingFileTransferRequestYahooImpl(
                        yahooProvider, this, sender, recvDate,
                        fileName, fileSize,
                        ev.getId());

            activeFileTransfers.put(ev.getId(), req);
            fireFileTransferRequest(
                new FileTransferRequestEvent(this, req, recvDate));
        }
    }

    /**
     * Status changed for filetransfer.
     * @param ev
     */
    public void statusChanged(SessionFileTransferEvent ev)
    {
        if(ev.getId() == null)
            return;

        Object ftObj = activeFileTransfers.get(ev.getId());

        if(ftObj == null)
        {
            logger.warn("File Transfer or request not found. " + ev.getId() + "/ " + ev.getState());
            return;
        }

        int newState = ev.getState();

        if(newState == SessionFileTransferEvent.CANCEL
            || newState == SessionFileTransferEvent.FAILED
            || newState == SessionFileTransferEvent.RECEIVED
            || newState == SessionFileTransferEvent.REFUSED
            || newState == SessionFileTransferEvent.SENT)
        {
            // this is an final state so remove it from active filetransfers
            activeFileTransfers.remove(ev.getId());
        }

        if(ftObj instanceof IncomingFileTransferRequest)
        {
            if(newState == SessionFileTransferEvent.REFUSED)
            {
                IncomingFileTransferRequestYahooImpl req =
                    (IncomingFileTransferRequestYahooImpl)ftObj;
                fireFileTransferRequestCanceled(
                    new FileTransferRequestEvent(this, req, req.getDate()));
                return;
            }
        }

        if(!(ftObj instanceof FileTransferImpl))
        {
            logger.warn("File Transfer not found." + ftObj);
            return;
        }

        FileTransferImpl ft = (FileTransferImpl)ftObj;

        if( newState == SessionFileTransferEvent.IN_PROGRESS)
        {
            // if we start sending progress fire that we are in progress
            if(ev.getProgress() == 0)
                ft.fireStatusChangeEvent(
                    FileTransferStatusChangeEvent.IN_PROGRESS);

            ft.setTransferedBytes(ev.getProgress());
            ft.fireProgressChangeEvent(
                System.currentTimeMillis(), ev.getProgress());
        }
        else
            ft.fireStatusChangeEvent(getStateMapping(newState));
    }

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * Supports up to 256MB.
     *
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        return 268435456l;// = 256*1024*1024;
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
                yahooProvider.getYahooSession().addSessionFileListener(
                    OperationSetFileTransferYahooImpl.this);
            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERED)
            {
                YahooSession ys = yahooProvider.getYahooSession();
                if(ys != null)
                    ys.removeSessionFileListener(
                        OperationSetFileTransferYahooImpl.this);
            }
        }
    }
}
