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
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.*;

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
    private static final Logger logger =
        Logger.getLogger(IncomingFileTransferRequestJabberImpl.class);

    private String id;

    /**
     * The Jabber file transfer request.
     */
    private final FileTransferRequest fileTransferRequest;

    private final OperationSetFileTransferJabberImpl fileTransferOpSet;

    private final ProtocolProviderServiceJabberImpl jabberProvider;

    private Contact sender;

    private String thumbnailCid;

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

        String fromUserID
            = fileTransferRequest.getRequestor();

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
                    .getChatRoom(StringUtils.parseBareAddress(fromUserID));
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
    public Contact getSender()
    {
        return sender;
    }

    /**
     * Returns the description of the file corresponding to this request.
     *
     * @return the description of the file corresponding to this request
     */
    public String getFileDescription()
    {
        return fileTransferRequest.getDescription();
    }

    /**
     * Returns the name of the file corresponding to this request.
     *
     * @return the name of the file corresponding to this request
     */
    public String getFileName()
    {
        return fileTransferRequest.getFileName();
    }

    /**
     * Returns the size of the file corresponding to this request.
     *
     * @return the size of the file corresponding to this request
     */
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

            jabberTransfer.recieveFile(file);

            new OperationSetFileTransferJabberImpl
                .FileTransferProgressThread(
                jabberTransfer, incomingTransfer, getFileSize()).start();
        }
        catch (XMPPException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Receiving file failed.", e);
        }

        return incomingTransfer;
    }

    /**
     * Refuses the file transfer request.
     */
    public void rejectFile()
    {
        fileTransferRequest.reject();

        fileTransferOpSet.fireFileTransferRequestRejected(
            new FileTransferRequestEvent(fileTransferOpSet, this, new Date()));
    }

    /**
     * The unique id.
     * @return the id.
     */
    public String getID()
    {
        return id;
    }

    /**
     * Returns the thumbnail contained in this request.
     *
     * @return the thumbnail contained in this request
     */
    public byte[] getThumbnail()
    {
        return thumbnail;
    }

    /**
     * Sets the thumbnail content-ID.
     * @param cid the thumbnail content-ID
     */
    public void createThumbnailListeners(String cid)
    {
        this.thumbnailCid = cid;

        if (jabberProvider.getConnection() != null)
        {
            jabberProvider.getConnection().addPacketListener(
                new ThumbnailResponseListener(),
                new AndFilter(  new PacketTypeFilter(IQ.class),
                                new IQTypeFilter(IQ.Type.RESULT)));
        }
    }

    /**
     * The <tt>ThumbnailResponseListener</tt> listens for events triggered by
     * the reception of a <tt>ThumbnailIQ</tt> packet. The packet is examined
     * and a file transfer request event is fired when the thumbnail is
     * extracted.
     */
    private class ThumbnailResponseListener implements PacketListener
    {
        public void processPacket(Packet packet)
        {
            // If this is not an IQ packet, we're not interested.
            if (!(packet instanceof ThumbnailIQ))
                return;

            if (logger.isDebugEnabled())
                logger.debug("Thumbnail response received.");

            ThumbnailIQ thumbnailResponse = (ThumbnailIQ) packet;

            if (thumbnailResponse.getCid() != null
                && thumbnailResponse.getCid().equals(thumbnailCid))
            {
                thumbnail = thumbnailResponse.getData();

                // Create an event associated to this global request.
                FileTransferRequestEvent fileTransferRequestEvent
                    = new FileTransferRequestEvent(
                        fileTransferOpSet,
                        IncomingFileTransferRequestJabberImpl.this,
                        new Date());

                // Notify the global listener that a request has arrived.
                fileTransferOpSet.fireFileTransferRequest(
                    fileTransferRequestEvent);
            }
            else
            {
                //TODO: RETURN <item-not-found/>
            }

            if (jabberProvider.getConnection() != null)
            {
                jabberProvider.getConnection()
                    .removePacketListener(this);
            }
        }
    }
}
