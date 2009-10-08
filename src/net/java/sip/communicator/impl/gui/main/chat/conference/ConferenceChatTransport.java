/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.io.*;

import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The conference implementation of the <tt>ChatTransport</tt> interface that
 * provides abstraction to access to protocol providers.
 * 
 * @author Yana Stamcheva
 */
public class ConferenceChatTransport
    implements  ChatTransport
{
    private final ChatSession chatSession;

    private final ChatRoom chatRoom;

    /**
     * Creates an instance of <tt>ConferenceChatTransport</tt> by specifying the
     * parent chat session and the chat room associated with this transport.
     * 
     * @param chatSession the parent chat session.
     * @param chatRoom the chat room associated with this conference transport.
     */
    public ConferenceChatTransport( ChatSession chatSession,
                                    ChatRoom chatRoom)
    {
        this.chatSession = chatSession;
        this.chatRoom = chatRoom;
    }

    /**
     * Returns the contact address corresponding to this chat transport.
     * 
     * @return The contact address corresponding to this chat transport.
     */
    public String getName()
    {
        return chatRoom.getName();
    }

    /**
     * Returns the display name corresponding to this chat transport.
     * 
     * @return The display name corresponding to this chat transport.
     */
    public String getDisplayName()
    {
        return chatRoom.getName();
    }

    /**
     * Returns the presence status of this transport.
     * 
     * @return the presence status of this transport.
     */
    public PresenceStatus getStatus()
    {
        return null;
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
        return chatRoom.getParentProvider();
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
        return chatRoom.isJoined();
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
        Object smsOpSet = chatRoom.getParentProvider()
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
        Object tnOpSet = chatRoom.getParentProvider()
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
     * @param messageText The message to send.
     * @param mimeType The mime type of the message to send: text/html or
     * text/plain.
     */
    public void sendInstantMessage(String messageText, String mimeType)
        throws Exception
    {
        // If this chat transport does not support instant messaging we do
        // nothing here.
        if (!allowsInstantMessage())
            return;

        Message message = chatRoom.createMessage(messageText);

        chatRoom.sendMessage(message);
    }

    /**
     * Sending sms messages is not supported by this chat transport
     * implementation.
     */
    public void sendSmsMessage(String phoneNumber, String message)
        throws Exception
    {}

    /**
     * Sending typing notifications is not supported by this chat transport
     * implementation.
     */
    public int sendTypingNotification(int typingState)
    {
        return 0;
    }

    /**
     * Sending files through a chat room is not yet supported by this chat
     * transport implementation.
     */
    public FileTransfer sendFile(File file)
        throws Exception
    { 
        return null;
    }

    /**
     * Returns the maximum file length supported by the protocol in bytes.
     * @return the file length that is supported.
     */
    public long getMaximumFileLength()
    {
        return -1;
    }

    /**
     * Invites the given contact in this chat conference.
     * 
     * @param contactAddress the address of the contact to invite
     * @param reason the reason for the invitation
     */
    public void inviteChatContact(String contactAddress, String reason)
    {
        if(chatRoom != null)
            chatRoom.invite(contactAddress, reason);
    }

    /**
     * Returns the parent session of this chat transport. A <tt>ChatSession</tt>
     * could contain more than one transports.
     * 
     * @return the parent session of this chat transport
     */
    public ChatSession getParentChatSession()
    {
        return chatSession;
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
            = chatRoom
                .getParentProvider()
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
            = chatRoom
                .getParentProvider()
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
            = chatRoom
                .getParentProvider()
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
            = chatRoom
                .getParentProvider()
                    .getOperationSet(OperationSetBasicInstantMessaging.class);

        imOpSet.removeMessageListener(l);
    }

    public void dispose()
    {}

    /**
     * Returns the descriptor of this chat transport.
     * 
     * @return the descriptor of this chat transport
     */
    public Object getDescriptor()
    {
        return chatRoom;
    }
}
