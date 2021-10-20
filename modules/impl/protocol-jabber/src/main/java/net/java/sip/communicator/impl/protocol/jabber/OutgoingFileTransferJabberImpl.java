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
import org.jitsi.xmpp.extensions.thumbnail.*;
import net.java.sip.communicator.service.protocol.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.filetransfer.*;
import org.jivesoftware.smackx.bob.*;
import org.jivesoftware.smackx.si.packet.*;
import org.jxmpp.stringprep.*;

/**
 * The Jabber protocol extension of the <tt>AbstractFileTransfer</tt>.
 *
 * @author Yana Stamcheva
 */
public class OutgoingFileTransferJabberImpl
    extends AbstractFileTransfer
    implements StanzaListener
{
    /**
     * The logger of this class.
     */
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OutgoingFileTransferJabberImpl.class);

    private final String id;

    private final Contact receiver;

    private final File file;

    /**
     * The jabber outgoing file transfer.
     */
    private final OutgoingFileTransfer jabberTransfer;

    private final ProtocolProviderServiceJabberImpl protocolProvider;

    private BoBInfo bobInfo;

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
        this.id = UUID.randomUUID().toString();

        // Add this outgoing transfer as a packet interceptor in
        // order to manage thumbnails.
        if (file instanceof ThumbnailedFile
             && ((ThumbnailedFile) file).getThumbnailData() != null
             && ((ThumbnailedFile) file).getThumbnailData().length > 0)
        {
            try
            {
                if (protocolProvider.isFeatureListSupported(
                                protocolProvider.getFullJid(receiver),
                    "urn:xmpp:thumbs:0",
                    "urn:xmpp:bob"))
                {
                    protocolProvider.getConnection().addStanzaInterceptor(
                        this,
                        new AndFilter(
                            IQTypeFilter.SET,
                            new StanzaTypeFilter(StreamInitiation.class)));
                }
            }
            catch (XmppStringprepException e)
            {
                logger.error("Failed to parse receiver address " + receiver
                    + " into a Jid");
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
    public void removeThumbnailHandler()
    {
        if (bobInfo == null)
        {
            return;
        }

        BoBManager bobManager = BoBManager.getInstanceFor(
            protocolProvider.getConnection());
        for (ContentId hash : bobInfo.getHashes())
        {
            bobManager.removeBoB(hash);
        }
    }

    /**
     * Listens for all <tt>StreamInitiation</tt> packets and adds a thumbnail
     * to them if a thumbnailed file is supported.
     */
    @Override
    public void processStanza(Stanza packet)
    {
        // If our file is not a thumbnailed file we have nothing to do here.
        if (!(file instanceof ThumbnailedFile))
            return;

        logger.debug("File transfer packet intercepted to add thumbnail");

        StreamInitiation fileTransferPacket = (StreamInitiation) packet;
        ThumbnailedFile thumbnailedFile = (ThumbnailedFile) file;

        if (jabberTransfer.getStreamID()
                .equals(fileTransferPacket.getSessionID()))
        {
            StreamInitiation.File file = fileTransferPacket.getFile();

            BoBData bobData = new BoBData(
                thumbnailedFile.getThumbnailMimeType(),
                thumbnailedFile.getThumbnailData());
            BoBManager bobManager = BoBManager.getInstanceFor(protocolProvider.getConnection());
            bobInfo = bobManager.addBoB(bobData);
            Thumbnail thumbnailElement = new Thumbnail(
                thumbnailedFile.getThumbnailData(),
                thumbnailedFile.getThumbnailMimeType(),
                thumbnailedFile.getThumbnailWidth(),
                thumbnailedFile.getThumbnailHeight());

            ThumbnailFile fileElement = new ThumbnailFile(file,
                thumbnailElement);

            fileTransferPacket.setFile(fileElement);

            if (logger.isDebugEnabled())
                logger.debug("The file transfer packet with thumbnail: "
                + fileTransferPacket.toXML());
        }
        // Remove this packet interceptor after we're done.
        protocolProvider.getConnection().removeStanzaInterceptor(this);
    }
}
