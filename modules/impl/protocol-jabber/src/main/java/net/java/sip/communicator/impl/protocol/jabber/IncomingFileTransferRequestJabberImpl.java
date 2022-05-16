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
import java.util.concurrent.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.event.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.XMPPException.*;
import org.jivesoftware.smackx.bob.*;
import org.jivesoftware.smackx.filetransfer.*;
import org.jxmpp.jid.*;

/**
 * Jabber implementation of the incoming file transfer request
 *
 * @author Nicolas Riegel
 * @author Yana Stamcheva
 */
public class IncomingFileTransferRequestJabberImpl
    implements IncomingFileTransferRequest
{
    /**
     * The logger for this class.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IncomingFileTransferRequestJabberImpl.class);

    /** Thread to fetch thumbnails in the background, one at a time */
    private static ExecutorService thumbnailCollector
        = Executors.newSingleThreadExecutor();

    private String id;

    /**
     * The Jabber file transfer request.
     */
    private final FileTransferRequest fileTransferRequest;

    private final OperationSetFileTransferJabberImpl fileTransferOpSet;

    private final ProtocolProviderServiceJabberImpl jabberProvider;

    private Contact sender;

    private byte[] thumbnail;

    /**
     * Creates an <tt>IncomingFileTransferRequestJabberImpl</tt> based on the
     * given <tt>fileTransferRequest</tt>, coming from the Jabber protocol.
     *
     * @param jabberProvider the protocol provider
     * @param fileTransferOpSet file transfer operation set
     * @param fileTransferRequest the request coming from the Jabber protocol
     */
    public IncomingFileTransferRequestJabberImpl(
        ProtocolProviderServiceJabberImpl jabberProvider,
        OperationSetFileTransferJabberImpl fileTransferOpSet,
        FileTransferRequest fileTransferRequest)
    {
        this.jabberProvider = jabberProvider;
        this.fileTransferOpSet = fileTransferOpSet;
        this.fileTransferRequest = fileTransferRequest;

        Jid fromUserID = fileTransferRequest.getRequestor();

        OperationSetPersistentPresenceJabberImpl opSetPersPresence
            = (OperationSetPersistentPresenceJabberImpl)
                jabberProvider
                    .getOperationSet(OperationSetPersistentPresence.class);

        sender = opSetPersPresence.findContactByID(fromUserID);
        if(sender == null)
        {
            ChatRoom privateContactRoom = null;
            OperationSetMultiUserChatJabberImpl mucOpSet =
                (OperationSetMultiUserChatJabberImpl)jabberProvider
                    .getOperationSet(OperationSetMultiUserChat.class);

            if(mucOpSet != null)
                privateContactRoom = mucOpSet
                    .getChatRoom(fromUserID.asBareJid());
            if(privateContactRoom != null)
            {
                sender = ((OperationSetPersistentPresenceJabberImpl)
                    jabberProvider.getOperationSet(
                        OperationSetPersistentPresence.class))
                    .createVolatileContact(fromUserID, true);
                privateContactRoom.updatePrivateContactPresenceStatus(sender);
            }
        }


        this.id = String.valueOf( System.currentTimeMillis())
                    + String.valueOf(hashCode());
    }

    /**
     * Returns the <tt>Contact</tt> making this request.
     *
     * @return the <tt>Contact</tt> making this request
     */
    @Override
    public Contact getSender()
    {
        return sender;
    }

    /**
     * Returns the description of the file corresponding to this request.
     *
     * @return the description of the file corresponding to this request
     */
    @Override
    public String getFileDescription()
    {
        return fileTransferRequest.getDescription();
    }

    /**
     * Returns the name of the file corresponding to this request.
     *
     * @return the name of the file corresponding to this request
     */
    @Override
    public String getFileName()
    {
        return fileTransferRequest.getFileName();
    }

    /**
     * Returns the size of the file corresponding to this request.
     *
     * @return the size of the file corresponding to this request
     */
    @Override
    public long getFileSize()
    {
        return fileTransferRequest.getFileSize();
    }

    /**
     * Accepts the file and starts the transfer.
     *
     * @return a boolean : <code>false</code> if the transfer fails,
     * <code>true</code> otherwise
     */
    @Override
    public FileTransfer acceptFile(File file)
    {
        AbstractFileTransfer incomingTransfer = null;

        IncomingFileTransfer jabberTransfer = fileTransferRequest.accept();
        try
        {
            incomingTransfer
                = new IncomingFileTransferJabberImpl(
                        id, sender, file, jabberTransfer);

            FileTransferCreatedEvent event
                = new FileTransferCreatedEvent(incomingTransfer, new Date());

            fileTransferOpSet.fireFileTransferCreated(event);

            jabberTransfer.receiveFile(file);

            new OperationSetFileTransferJabberImpl
                .FileTransferProgressThread(
                jabberTransfer, incomingTransfer, getFileSize()).start();
        }
        catch (IOException | SmackException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Receiving file failed.", e);
        }

        return incomingTransfer;
    }

    /**
     * Refuses the file transfer request.
     */
    @Override
    public void rejectFile()
        throws OperationFailedException
    {
        try
        {
            fileTransferRequest.reject();
        }
        catch (SmackException | InterruptedException e)
        {
            throw new OperationFailedException(
                "Could not reject file transfer",
                OperationFailedException.GENERAL_ERROR,
                e
            );
        }

        fileTransferOpSet.fireFileTransferRequestRejected(
            new FileTransferRequestEvent(fileTransferOpSet, this, new Date()));
    }

    /**
     * The unique id.
     * @return the id.
     */
    @Override
    public String getID()
    {
        return id;
    }

    /**
     * Returns the thumbnail contained in this request.
     *
     * @return the thumbnail contained in this request
     */
    @Override
    public byte[] getThumbnail()
    {
        return thumbnail;
    }

    /**
     * Requests the thumbnail from the peer and fire the incoming transfer
     * request event.
     * @param cid the thumbnail content-ID
     */
    public void fetchThumbnailAndNotify(final ContentId cid)
    {
        final BoBManager bobManager = BoBManager.getInstanceFor(
            jabberProvider.getConnection());
        thumbnailCollector.submit(new Runnable()
        {
            @Override
            public void run()
            {
                logger.debug("Sending thumbnail request");
                try
                {
                    thumbnail = bobManager.requestBoB(
                        ((ContactJabberImpl)sender).getAddressAsJid(),
                        cid).getContent();
                }
                catch (NotLoggedInException
                    | NoResponseException
                    | XMPPErrorException
                    | NotConnectedException
                    | InterruptedException e)
                {
                    logger.error("Could not get thumbnail", e);
                }
                finally
                {
                    // Notify the global listener that a request has arrived.
                    fileTransferOpSet.fireFileTransferRequest(
                        IncomingFileTransferRequestJabberImpl.this);
                }
            }
        });
    }
}
