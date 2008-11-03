/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class MetaContactChatTransport
    implements  ChatTransport,
                ContactPresenceStatusListener
{
    private Logger logger = Logger.getLogger(MetaContactChatTransport.class);

    private ChatSession parentChatSession;

    private Contact contact;

    private OperationSetPresence presenceOpSet;

    public MetaContactChatTransport(ChatSession chatSession,
                                    Contact contact)
    {
        this.parentChatSession = chatSession;
        this.contact = contact;

        presenceOpSet =
            (OperationSetPresence) contact.getProtocolProvider()
                .getOperationSet(OperationSetPresence.class);

        presenceOpSet.addContactPresenceStatusListener(this);
    }

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
     * Sends the given instant message trough this chat transport, by specifying
     * the mime type (html or plain text).
     * 
     * @param message The message to send.
     * @param mimeType The mime type of the message to send: text/html or
     * text/plain.
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
            = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
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
     * Sends the given sms message trough this chat transport.
     * 
     * @param message The message to send.
     */
    public void sendSmsMessage(String phoneNumber, String messageText)
        throws Exception
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return;

        OperationSetSmsMessaging smsOpSet
            = (OperationSetSmsMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetSmsMessaging.class);

        Message smsMessage = smsOpSet.createMessage(messageText);

        smsOpSet.sendSmsMessage(phoneNumber, smsMessage);
    }

    /**
     * Sends a typing notification state.
     * 
     * @param typingState the typing notification state to send
     * 
     * @return the result of this operation. One of the TYPING_NOTIFICATION_XXX
     * constants defined in this class
     */
    public int sendTypingNotification(int typingState)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsTypingNotifications())
            return -1;

        OperationSetTypingNotifications tnOperationSet
            = (OperationSetTypingNotifications) contact.getProtocolProvider()
                .getOperationSet(OperationSetTypingNotifications.class);

        if(contact.getProtocolProvider().isRegistered())
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

    public void inviteChatContact(String contactAddress, String reason)
    {
        
    }

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
     * Adds an sms message listener to this chat transport.
     * 
     * @param l The message listener to add.
     */
    public void addSmsMessageListener(MessageListener l)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return;

        OperationSetSmsMessaging smsOpSet
            = (OperationSetSmsMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetSmsMessaging.class);

        smsOpSet.addMessageListener(l);
    }

    /**
     * Adds an instant message listener to this chat transport.
     * 
     * @param l The message listener to add.
     */
    public void addInstantMessageListener(MessageListener l)
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet
            = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        imOpSet.addMessageListener(l);
    }

    /**
     * Removes the given sms message listener from this chat transport.
     * 
     * @param l The message listener to remove.
     */
    public void removeSmsMessageListener(MessageListener l)
    {
        // If this chat transport does not support sms messaging we do
        // nothing here.
        if (!allowsSmsMessage())
            return;

        OperationSetSmsMessaging smsOpSet
            = (OperationSetSmsMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetSmsMessaging.class);

        smsOpSet.removeMessageListener(l);
    }

    /**
     * Removes the instant message listener from this chat transport.
     * 
     * @param l The message listener to remove.
     */
    public void removeInstantMessageListener(MessageListener l)
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        OperationSetBasicInstantMessaging imOpSet
            = (OperationSetBasicInstantMessaging) contact.getProtocolProvider()
                .getOperationSet(OperationSetBasicInstantMessaging.class);

        imOpSet.removeMessageListener(l);
    }

    /**
     * Indicates that a contact has changed its status.
     *
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
     * 
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
        presenceOpSet.removeContactPresenceStatusListener(this);
    }

    public Object getDescriptor()
    {
        return contact;
    }
}
