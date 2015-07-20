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

import net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.*;
import org.jivesoftware.smackx.packet.*;

/**
 * The Jabber protocol extension of the <tt>AbstractFileTransfer</tt>.
 *
 * @author Yana Stamcheva
 */
public class OutgoingFileTransferJabberImpl
    extends AbstractFileTransfer
    implements PacketInterceptor
{
    /**
     * The logger of this class.
     */
    private final Logger logger
        = Logger.getLogger(OutgoingFileTransferJabberImpl.class);

    private final String id;

    private final Contact receiver;

    private final File file;

    private ThumbnailElement thumbnailElement;

    private final ThumbnailRequestListener thumbnailRequestListener
        = new ThumbnailRequestListener();

    /**
     * The jabber outgoing file transfer.
     */
    private final OutgoingFileTransfer jabberTransfer;

    private final ProtocolProviderServiceJabberImpl protocolProvider;

    /**
     * Creates an <tt>OutgoingFileTransferJabberImpl</tt> by specifying the
     * <tt>receiver</tt> contact, the <tt>file</tt>, the <tt>jabberTransfer</tt>,
     * that would be used to send the file through Jabber and the
     * <tt>protocolProvider</tt>.
     *
     * @param receiver the destination contact
     * @param file the file to send
     * @param jabberTransfer the Jabber transfer object, containing all transfer
     * information
     * @param protocolProvider the parent protocol provider
     */
    public OutgoingFileTransferJabberImpl(
        Contact receiver,
        File file,
        OutgoingFileTransfer jabberTransfer,
        ProtocolProviderServiceJabberImpl protocolProvider)
    {
        this.receiver = receiver;
        this.file = file;
        this.jabberTransfer = jabberTransfer;
        this.protocolProvider = protocolProvider;

        // Create the identifier of this file transfer that is used from the
        // history and the user interface to track this transfer.
        this.id = String.valueOf(System.currentTimeMillis())
            + String.valueOf(hashCode());

        // Add this outgoing transfer as a packet interceptor in
        // order to manage thumbnails.
        if (file instanceof ThumbnailedFile
             && ((ThumbnailedFile) file).getThumbnailData() != null
             && ((ThumbnailedFile) file).getThumbnailData().length > 0)
        {
            if (protocolProvider.isFeatureListSupported(
                            protocolProvider.getFullJid(receiver),
                            new String[]{"urn:xmpp:thumbs:0",
                                "urn:xmpp:bob"}))
            {
                protocolProvider.getConnection().addPacketInterceptor(
                    this,
                    new IQTypeFilter(IQ.Type.SET));
            }
        }
    }

    /**
     * Cancels the file transfer.
     */
    @Override
    public void cancel()
    {
        this.jabberTransfer.cancel();
    }

    /**
     * Returns the number of bytes already sent to the recipient.
     *
     * @return the number of bytes already sent to the recipient.
     */
    @Override
    public long getTransferedBytes()
    {
        return jabberTransfer.getBytesSent();
    }

    /**
     * The direction is outgoing.
     * @return OUT.
     */
    public int getDirection()
    {
        return OUT;
    }

    /**
     * Returns the local file that is being transferred or to which we transfer.
     *
     * @return the file
     */
    public File getLocalFile()
    {
        return file;
    }

    /**
     * The contact we are sending the file.
     * @return the receiver.
     */
    public Contact getContact()
    {
        return receiver;
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
     * Removes previously added thumbnail request listener.
     */
    public void removeThumbnailRequestListener()
    {
        protocolProvider.getConnection()
            .removePacketListener(thumbnailRequestListener);
    }

    /**
     * Listens for all <tt>StreamInitiation</tt> packets and adds a thumbnail
     * to them if a thumbnailed file is supported.
     *
     * @see PacketInterceptor#interceptPacket(Packet)
     */
    public void interceptPacket(Packet packet)
    {
        if (!(packet instanceof StreamInitiation))
            return;

        // If our file is not a thumbnailed file we have nothing to do here.
        if (!(file instanceof ThumbnailedFile))
            return;

        if (logger.isDebugEnabled())
            logger.debug("File transfer packet intercepted"
                    + " in order to add thumbnail.");

        StreamInitiation fileTransferPacket = (StreamInitiation) packet;

        ThumbnailedFile thumbnailedFile = (ThumbnailedFile) file;

        if (jabberTransfer.getStreamID()
                .equals(fileTransferPacket.getSessionID()))
        {
            StreamInitiation.File file = fileTransferPacket.getFile();

            thumbnailElement = new ThumbnailElement(
                StringUtils.parseServer(fileTransferPacket.getTo()),
                thumbnailedFile.getThumbnailData(),
                thumbnailedFile.getThumbnailMimeType(),
                thumbnailedFile.getThumbnailWidth(),
                thumbnailedFile.getThumbnailHeight());

            FileElement fileElement = new FileElement(file, thumbnailElement);

            fileTransferPacket.setFile(fileElement);

            if (logger.isDebugEnabled())
                logger.debug("The file transfer packet with thumbnail: "
                + fileTransferPacket.toXML());

            // Add the request listener in order to listen for requests coming
            // for the advertised thumbnail.
            if (protocolProvider.getConnection() != null)
            {
                protocolProvider.getConnection().addPacketListener(
                    thumbnailRequestListener,
                    new AndFilter(  new PacketTypeFilter(IQ.class),
                                    new IQTypeFilter(IQ.Type.GET)));
            }
        }
        // Remove this packet interceptor after we're done.
        protocolProvider.getConnection().removePacketInterceptor(this);
    }

    /**
     * The <tt>ThumbnailRequestListener</tt> listens for events triggered by
     * the reception of a <tt>ThumbnailIQ</tt> packet. The packet is examined
     * and a <tt>ThumbnailIQ</tt> is created to respond to the thumbnail
     * request received.
     */
    private class ThumbnailRequestListener implements PacketListener
    {
        public void processPacket(Packet packet)
        {
            // If this is not an IQ packet, we're not interested.
            if (!(packet instanceof ThumbnailIQ))
                return;

            ThumbnailIQ thumbnailIQ = (ThumbnailIQ) packet;
            String thumbnailIQCid = thumbnailIQ.getCid();
            XMPPConnection connection = protocolProvider.getConnection();

            if ((thumbnailIQCid != null)
                    && thumbnailIQCid.equals(thumbnailElement.getCid()))
            {
                ThumbnailedFile thumbnailedFile = (ThumbnailedFile) file;
                ThumbnailIQ thumbnailResponse = new ThumbnailIQ(
                    thumbnailIQ.getTo(),
                    thumbnailIQ.getFrom(),
                    thumbnailIQCid,
                    thumbnailedFile.getThumbnailMimeType(),
                    thumbnailedFile.getThumbnailData(),
                    IQ.Type.RESULT);

                if (logger.isDebugEnabled())
                    logger.debug("Send thumbnail response to the receiver: "
                        + thumbnailResponse.toXML());

                connection.sendPacket(thumbnailResponse);
            }
            else
            {
                // RETURN <item-not-found/>
            }

            if (connection != null)
                connection.removePacketListener(this);
        }
    }
}
