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
package net.java.sip.communicator.impl.gui.main.chat;

import java.io.*;
import java.net.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The single chat implementation of the <tt>ChatTransport</tt> interface that
 * provides abstraction to protocol provider access.
 *
 * @author Yana Stamcheva
 */
public class MetaContactChatTransport
    implements  ChatTransport,
                ContactPresenceStatusListener
{
    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(MetaContactChatTransport.class);

    /**
     * The parent <tt>ChatSession</tt>, where this transport is available.
     */
    private final MetaContactChatSession parentChatSession;

    /**
     * The associated protocol <tt>Contact</tt>.
     */
    private final Contact contact;

    /**
     * The resource associated with this contact.
     */
    private ContactResource contactResource;

    /**
     * The protocol presence operation set associated with this transport.
     */
    private final OperationSetPresence presenceOpSet;

    /**
     * The thumbnail default width.
     */
    private static final int THUMBNAIL_WIDTH = 64;

    /**
     * The thumbnail default height.
     */
    private static final int THUMBNAIL_HEIGHT = 64;

    /**
     * Indicates if only the resource name should be displayed.
     */
    private boolean isDisplayResourceOnly = false;

    /**
     * Creates an instance of <tt>MetaContactChatTransport</tt> by specifying
     * the parent <tt>chatSession</tt> and the <tt>contact</tt> associated with
     * the transport.
     *
     * @param chatSession the parent <tt>ChatSession</tt>
     * @param contact the <tt>Contact</tt> associated with this transport
     */
    public MetaContactChatTransport(MetaContactChatSession chatSession,
                                    Contact contact)
    {
        this(chatSession, contact, null, false);
    }

    /**
     * Creates an instance of <tt>MetaContactChatTransport</tt> by specifying
     * the parent <tt>chatSession</tt> and the <tt>contact</tt> associated with
     * the transport.
     *
     * @param chatSession the parent <tt>ChatSession</tt>
     * @param contact the <tt>Contact</tt> associated with this transport
     * @param contactResource the <tt>ContactResource</tt> associated with the
     * contact
     * @param isDisplayResourceOnly indicates if only the resource name should
     * be displayed
     */
    public MetaContactChatTransport(MetaContactChatSession chatSession,
                                    Contact contact,
                                    ContactResource contactResource,
                                    boolean isDisplayResourceOnly)
    {
        this.parentChatSession = chatSession;
        this.contact = contact;
        this.contactResource = contactResource;
        this.isDisplayResourceOnly = isDisplayResourceOnly;

        presenceOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetPresence.class);

        if (presenceOpSet != null)
            presenceOpSet.addContactPresenceStatusListener(this);

        // checking this can be slow so make
        // sure its out of our way
        new Thread(new Runnable()
            {
                public void run()
                {
                    checkImCaps();
                }
            }).start();
    }

    /**
     * If sending im is supported check it for supporting html messages
     * if a font is set.
     * As it can be slow make sure its not on our way
     */
    private void checkImCaps()
    {
        if (ConfigurationUtils.getChatDefaultFontFamily() != null
            && ConfigurationUtils.getChatDefaultFontSize() > 0)
        {
            OperationSetBasicInstantMessaging imOpSet
                = contact.getProtocolProvider()
                    .getOperationSet(OperationSetBasicInstantMessaging.class);

            if(imOpSet != null)
                imOpSet.isContentTypeSupported(
                    OperationSetBasicInstantMessaging.HTML_MIME_TYPE, contact);
        }
    }

    /**
     * Returns the contact associated with this transport.
     *
     * @return the contact associated with this transport
     */
    public Contact getContact()
    {
        return contact;
    }

    /**
     * Returns the contact address corresponding to this chat transport.
     *
     * @return The contact address corresponding to this chat transport.
     */
    public String getName()
    {
        return contact.getAddress();
    }

    /**
     * Returns the display name corresponding to this chat transport.
     *
     * @return The display name corresponding to this chat transport.
     */
    public String getDisplayName()
    {
        return contact.getDisplayName();
    }

    /**
     * Returns the resource name of this chat transport. This is for example the
     * name of the user agent from which the contact is logged.
     *
     * @return The display name of this chat transport resource.
     */
    public String getResourceName()
    {
        if (contactResource != null)
            return contactResource.getResourceName();

        return null;
    }

    public boolean isDisplayResourceOnly()
    {
        return isDisplayResourceOnly;
    }

    /**
     * Returns the presence status of this transport.
     *
     * @return the presence status of this transport.
     */
    public PresenceStatus getStatus()
    {
        if (contactResource != null)
            return contactResource.getPresenceStatus();
        else
            return contact.getPresenceStatus();
    }

    /**
     * Returns the <tt>ProtocolProviderService</tt>, corresponding to this chat
     * transport.
     *
     * @return the <tt>ProtocolProviderService</tt>, corresponding to this chat
     * transport.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return contact.getProtocolProvider();
    }

    /**
     * Returns <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports instant
     * messaging, otherwise returns <code>false</code>.
     */
    public boolean allowsInstantMessage()
    {
        // First try to ask the capabilities operation set if such is
        // available.
        OperationSetContactCapabilities capOpSet = getProtocolProvider()
            .getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet != null)
        {
            if (capOpSet.getOperationSet(
                contact, OperationSetBasicInstantMessaging.class) != null)
            {
                return true;
            }
        }
        else if (contact.getProtocolProvider()
            .getOperationSet(OperationSetBasicInstantMessaging.class) != null)
            return true;

        return false;
    }

    /**
     * Returns <code>true</code> if this chat transport supports message
     * corrections and false otherwise.
     *
     * @return <code>true</code> if this chat transport supports message
     * corrections and false otherwise.
     */
    public boolean allowsMessageCorrections()
    {
        OperationSetContactCapabilities capOpSet = getProtocolProvider()
                .getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet != null)
        {
            return capOpSet.getOperationSet(
                    contact, OperationSetMessageCorrection.class) != null;
        }
        else
        {
            return contact.getProtocolProvider().getOperationSet(
                    OperationSetMessageCorrection.class) != null;
        }
    }

    /**
     * Returns <code>true</code> if this chat transport supports sms
     * messaging, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports sms
     * messaging, otherwise returns <code>false</code>.
     */
    public boolean allowsSmsMessage()
    {
        // First try to ask the capabilities operation set if such is
        // available.
        OperationSetContactCapabilities capOpSet = getProtocolProvider()
            .getOperationSet(OperationSetContactCapabilities.class);

        if (capOpSet != null)
        {
            if (capOpSet.getOperationSet(
                contact, OperationSetSmsMessaging.class) != null)
            {
                return true;
            }
        }
        else if (contact.getProtocolProvider()
            .getOperationSet(OperationSetSmsMessaging.class) != null)
            return true;

        return false;
    }


    /**
     * Returns <code>true</code> if this chat transport supports typing
     * notifications, otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports typing
     * notifications, otherwise returns <code>false</code>.
     */
    public boolean allowsTypingNotifications()
    {
        Object tnOpSet = contact.getProtocolProvider()
            .getOperationSet(OperationSetTypingNotifications.class);

        if (tnOpSet != null)
            return true;
        else
            return false;
    }

    /**
     * Returns <code>true</code> if this chat transport supports file transfer,
     * otherwise returns <code>false</code>.
     *
     * @return <code>true</code> if this chat transport supports file transfer,
     * otherwise returns <code>false</code>.
     */
    public boolean allowsFileTransfer()
    {
        Object ftOpSet = contact.getProtocolProvider()
            .getOperationSet(OperationSetFileTransfer.class);

        if (ftOpSet != null)
            return true;
        else
            return false;
    }

    /**
     * Sends the given instant message through this chat transport,
     * by specifying the mime type (html or plain text).
     *
     * @param message The message to send.
     * @param mimeType The mime type of the message to send: text/html or
     * text/plain.
     * @throws Exception if the send operation is interrupted
     */
    public void sendInstantMessage( String message,
                                    String mimeType)
        throws Exception
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetBasicInstantMessaging.class);

        Message msg;
        if (mimeType.equals(OperationSetBasicInstantMessaging.HTML_MIME_TYPE)
            && imOpSet.isContentTypeSupported(
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE))
        {
            msg = imOpSet.createMessage(message,
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE, "utf-8", "");
        }
        else
        {
            msg = imOpSet.createMessage(message);
        }

        if (contactResource != null)
            imOpSet.sendInstantMessage(contact, contactResource, msg);
        else
            imOpSet.sendInstantMessage(contact,
                    ContactResource.BASE_RESOURCE, msg);
    }

    /**
     * Sends <tt>message</tt> as a message correction through this transport,
     * specifying the mime type (html or plain text) and the id of the
     * message to replace.
     *
     * @param message The message to send.
     * @param mimeType The mime type of the message to send: text/html or
     * text/plain.
     * @param correctedMessageUID The ID of the message being corrected by
     * this message.
     */
    public void correctInstantMessage(String message, String mimeType,
            String correctedMessageUID)
    {
        if (!allowsMessageCorrections())
        {
            return;
        }

        OperationSetMessageCorrection mcOpSet = contact.getProtocolProvider()
                .getOperationSet(OperationSetMessageCorrection.class);

        Message msg;
        if (mimeType.equals(OperationSetBasicInstantMessaging.HTML_MIME_TYPE)
                && mcOpSet.isContentTypeSupported(
                OperationSetBasicInstantMessaging.HTML_MIME_TYPE))
        {
            msg = mcOpSet.createMessage(message,
                    OperationSetBasicInstantMessaging.HTML_MIME_TYPE,
                    "utf-8", "");
        }
        else
        {
            msg = mcOpSet.createMessage(message);
        }

        mcOpSet.correctMessage(
            contact, contactResource, msg, correctedMessageUID);
    }

    /**
     * Determines whether this chat transport supports the supplied content type
     *
     * @param contentType the type we want to check
     * @return <tt>true</tt> if the chat transport supports it and
     * <tt>false</tt> otherwise.
     */
    public boolean isContentTypeSupported(String contentType)
    {
        OperationSetBasicInstantMessaging imOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetBasicInstantMessaging.class);

        if(imOpSet != null)
            return imOpSet.isContentTypeSupported(contentType);
        else
            return false;
    }

    /**
     * Sends the given sms message trough this chat transport.
     *
     * @param phoneNumber phone number of the destination
     * @param messageText The message to send.
     * @throws Exception if the send operation is interrupted
     */
    public void sendSmsMessage(String phoneNumber, String messageText)
        throws Exception
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return;

        SMSManager.sendSMS(
            contact.getProtocolProvider(),
            phoneNumber,
            messageText);
    }

    /**
     * Whether a dialog need to be opened so the user can enter the destination
     * number.
     * @return <tt>true</tt> if dialog needs to be open.
     */
    public boolean askForSMSNumber()
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return false;

        OperationSetSmsMessaging smsOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetSmsMessaging.class);

        return smsOpSet.askForNumber(contact);
    }

    /**
     * Sends the given sms message trough this chat transport.
     *
     * @param message the message to send
     * @throws Exception if the send operation is interrupted
     */
    public void sendSmsMessage(String message)
        throws Exception
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return;

        SMSManager.sendSMS(contact, message);
    }

    /**
     * Sends a typing notification state.
     *
     * @param typingState the typing notification state to send
     * @return the result of this operation. One of the TYPING_NOTIFICATION_XXX
     * constants defined in this class
     */
    public int sendTypingNotification(int typingState)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsTypingNotifications())
            return -1;

        ProtocolProviderService protocolProvider
            = contact.getProtocolProvider();
        OperationSetTypingNotifications tnOperationSet
            = protocolProvider
                .getOperationSet(OperationSetTypingNotifications.class);

        // if protocol is not registered or contact is offline don't
        // try to send typing notifications
        if(protocolProvider.isRegistered()
            && contact.getPresenceStatus().getStatus()
                    >= PresenceStatus.ONLINE_THRESHOLD)
        {
            try
            {
                tnOperationSet.sendTypingNotification(
                    contact, typingState);

                return ChatPanel.TYPING_NOTIFICATION_SUCCESSFULLY_SENT;
            }
            catch (Exception ex)
            {
                logger.error("Failed to send typing notifications.", ex);

                return ChatPanel.TYPING_NOTIFICATION_SEND_FAILED;
            }
        }

        return ChatPanel.TYPING_NOTIFICATION_SEND_FAILED;
    }

    /**
     * Sends the given file through this chat transport file transfer operation
     * set.
     * @param file the file to send
     * @return the <tt>FileTransfer</tt> object charged to transfer the file
     * @throws Exception if anything goes wrong
     */
    public FileTransfer sendFile(File file)
        throws Exception
    {
        return sendFile(file, false);
    }

    /**
     * Sends the given file through this chat transport file transfer operation
     * set.
     * @param file the file to send
     * @return the <tt>FileTransfer</tt> object charged to transfer the file
     * @throws Exception if anything goes wrong
     */
    private FileTransfer sendFile(File file, boolean isMultimediaMessage)
        throws Exception
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsFileTransfer())
            return null;

        if (FileUtils.isImage(file.getName()))
        {
            // Create a thumbnailed file if possible.
            OperationSetThumbnailedFileFactory tfOpSet
                = contact
                    .getProtocolProvider()
                        .getOperationSet(
                            OperationSetThumbnailedFileFactory.class);

            if (tfOpSet != null)
            {
                byte[] thumbnail = getFileThumbnail(file);

                if (thumbnail != null && thumbnail.length > 0)
                {
                    file = tfOpSet.createFileWithThumbnail(
                        file, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT,
                        "image/png", thumbnail);
                }
            }
        }

        if(isMultimediaMessage)
        {
            OperationSetSmsMessaging smsOpSet = contact.getProtocolProvider()
                .getOperationSet(OperationSetSmsMessaging.class);

            if(smsOpSet == null)
                return null;

            return smsOpSet.sendMultimediaFile(contact, file);
        }
        else
        {
            OperationSetFileTransfer ftOpSet = contact.getProtocolProvider()
                .getOperationSet(OperationSetFileTransfer.class);
            return ftOpSet.sendFile(contact, file);
        }
    }

    /**
     * Sends the given SMS multimedia message trough this chat transport,
     * leaving the transport to choose the destination.
     *
     * @param file the file to send
     * @throws Exception if the send doesn't succeed
     */
    public FileTransfer sendMultimediaFile(File file)
        throws Exception
    {
        return sendFile(file, true);
    }

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        OperationSetFileTransfer ftOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetFileTransfer.class);

        return ftOpSet.getMaximumFileLength();
    }

    public void inviteChatContact(String contactAddress, String reason) {}

    /**
     * Returns the parent session of this chat transport. A <tt>ChatSession</tt>
     * could contain more than one transports.
     *
     * @return the parent session of this chat transport
     */
    public ChatSession getParentChatSession()
    {
        return parentChatSession;
    }

    /**
     * Adds an SMS message listener to this chat transport.
     * @param l The message listener to add.
     */
    public void addSmsMessageListener(MessageListener l)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return;

        OperationSetSmsMessaging smsOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetSmsMessaging.class);

        smsOpSet.addMessageListener(l);
    }

    /**
     * Adds an instant message listener to this chat transport.
     * @param l The message listener to add.
     */
    public void addInstantMessageListener(MessageListener l)
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetBasicInstantMessaging.class);

        imOpSet.addMessageListener(l);
    }

    /**
     * Removes the given sms message listener from this chat transport.
     * @param l The message listener to remove.
     */
    public void removeSmsMessageListener(MessageListener l)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return;

        OperationSetSmsMessaging smsOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetSmsMessaging.class);

        smsOpSet.removeMessageListener(l);
    }

    /**
     * Removes the instant message listener from this chat transport.
     * @param l The message listener to remove.
     */
    public void removeInstantMessageListener(MessageListener l)
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetBasicInstantMessaging.class);

        imOpSet.removeMessageListener(l);
    }

    /**
     * Indicates that a contact has changed its status.
     * @param evt The presence event containing information about the
     * contact status change.
     */
    public void contactPresenceStatusChanged(
                                        ContactPresenceStatusChangeEvent evt)
    {
        if (evt.getSourceContact().equals(contact)
            && !evt.getOldStatus().equals(evt.getNewStatus())
            && contactResource == null) // If the contact source is set then the
                                        // status will be updated from the
                                        // MetaContactChatSession.
        {
            this.updateContactStatus();
        }
    }

    /**
     * Updates the status of this contact with the new given status.
     */
    private void updateContactStatus()
    {
        // Update the status of the given contact in the "send via" selector
        // box.
        parentChatSession.getChatSessionRenderer()
            .updateChatTransportStatus(this);
    }

    /**
     * Removes all previously added listeners.
     */
    public void dispose()
    {
        if (presenceOpSet != null)
            presenceOpSet.removeContactPresenceStatusListener(this);
    }

    /**
     * Returns the descriptor of this chat transport.
     * @return the descriptor of this chat transport
     */
    public Object getDescriptor()
    {
        return contact;
    }

    /**
     * Sets the icon for the given file.
     *
     * @param file the file to set an icon for
     * @return the byte array containing the thumbnail
     */
    private byte[] getFileThumbnail(File file)
    {
        byte[] bytes = null;
        if (FileUtils.isImage(file.getName()))
        {
            try
            {
                ImageIcon image = new ImageIcon(file.toURI().toURL());
                int width = image.getIconWidth();
                int height = image.getIconHeight();

                if (width > THUMBNAIL_WIDTH)
                    width = THUMBNAIL_WIDTH;
                if (height > THUMBNAIL_HEIGHT)
                    height = THUMBNAIL_HEIGHT;

                bytes
                    = ImageUtils
                        .getScaledInstanceInBytes(
                            image.getImage(),
                            width,
                            height);
            }
            catch (MalformedURLException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Could not locate image.", e);
            }
        }
        return bytes;
    }
}
