/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.io.*;
import java.net.*;

import javax.swing.*;

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
    private final ChatSession parentChatSession;

    /**
     * The associated protocol <tt>Contact</tt>.
     */
    private final Contact contact;

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
     * Creates an instance of <tt>MetaContactChatTransport</tt> by specifying
     * the parent <tt>chatSession</tt> and the <tt>contact</tt> associated with
     * the transport.
     *
     * @param chatSession the parent <tt>ChatSession</tt>
     * @param contact the <tt>Contact</tt> associated with this transport
     */
    public MetaContactChatTransport(ChatSession chatSession,
                                    Contact contact)
    {
        this.parentChatSession = chatSession;
        this.contact = contact;

        presenceOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetPresence.class);

        if (presenceOpSet != null)
            presenceOpSet.addContactPresenceStatusListener(this);
    }

    /**
     * Returns the contact associated with this transport.
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
     * Returns the presence status of this transport.
     * 
     * @return the presence status of this transport.
     */
    public PresenceStatus getStatus()
    {
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
        Object imOpSet = contact.getProtocolProvider()
            .getOperationSet(OperationSetBasicInstantMessaging.class);

        if (imOpSet != null)
            return true;
        else
            return false;
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
        Object smsOpSet = contact.getProtocolProvider()
            .getOperationSet(OperationSetSmsMessaging.class);

        if (smsOpSet != null)
            return true;
        else
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
     * Sends the given instant message trough this chat transport, by specifying
     * the mime type (html or plain text).
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

        imOpSet.sendInstantMessage(contact, msg);
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

        OperationSetSmsMessaging smsOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetSmsMessaging.class);

        Message smsMessage = smsOpSet.createMessage(messageText);

        smsOpSet.sendSmsMessage(phoneNumber, smsMessage);
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

        if(protocolProvider.isRegistered())
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
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsFileTransfer())
            return null;

        OperationSetFileTransfer ftOpSet
            = contact
                .getProtocolProvider()
                    .getOperationSet(OperationSetFileTransfer.class);

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
        return ftOpSet.sendFile(contact, file);
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
        Contact sourceContact = evt.getSourceContact();

        if (sourceContact.equals(contact)
            && (evt.getOldStatus() != evt.getNewStatus()))
        {
            this.updateContactStatus(evt.getNewStatus());
        }
    }

    /**
     * Updates the status of this contact with the new given status.
     * @param newStatus The new status.
     */
    private void updateContactStatus(PresenceStatus newStatus)
    {
        // Update the status of the given contact in the "send via" selector
        // box.
        parentChatSession.getChatSessionRenderer()
            .updateChatTransportStatus(this);

        //TODO: Update the status of the chat session.
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
