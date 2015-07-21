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
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.io.*;

import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * The conference implementation of the <tt>ChatTransport</tt> interface that
 * provides abstraction to access to protocol providers.
 *
 * @author Valentin Martinet
 */
public class AdHocConferenceChatTransport
    implements  ChatTransport
{
    private final ChatSession chatSession;

    private final AdHocChatRoom adHocChatRoom;

    /**
     * Creates an instance of <tt>ConferenceChatTransport</tt> by specifying the
     * parent chat session and the ad-hoc chat room associated with this
     * transport.
     *
     * @param chatSession the parent chat session.
     * @param chatRoom the ad-hoc chat room associated with this conference
     * transport.
     */
    public AdHocConferenceChatTransport(ChatSession  chatSession,
                                        AdHocChatRoom chatRoom)
    {
        this.chatSession = chatSession;
        this.adHocChatRoom = chatRoom;
    }

    /**
     * Returns the contact address corresponding to this chat transport.
     *
     * @return The contact address corresponding to this chat transport.
     */
    public String getName()
    {
        return adHocChatRoom.getName();
    }

    /**
     * Returns the display name corresponding to this chat transport.
     *
     * @return The display name corresponding to this chat transport.
     */
    public String getDisplayName()
    {
        return adHocChatRoom.getName();
    }

    /**
     * Returns the resource name of this chat transport. This is for example the
     * name of the user agent from which the contact is logged.
     *
     * @return The display name of this chat transport resource.
     */
    public String getResourceName()
    {
        return null;
    }

    /**
     * Indicates if the display name should only show the resource.
     *
     * @return <tt>true</tt> if the display name shows only the resource,
     * <tt>false</tt> - otherwise
     */
    public boolean isDisplayResourceOnly()
    {
        return false;
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
        return adHocChatRoom.getParentProvider();
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
        return true;
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
        Object tnOpSet = adHocChatRoom.getParentProvider()
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

        Message message = adHocChatRoom.createMessage(messageText);

        adHocChatRoom.sendMessage(message);
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
        // we only support plain text for chat rooms for now
        return OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE
                .equals(contentType);
    }

    /**
     * Sending sms messages is not supported by this chat transport
     * implementation.
     */
    public void sendSmsMessage(String phoneNumber, String message)
        throws Exception
    {}

    /**
     * Sending sms messages is not supported by this chat transport
     * implementation.
     */
    public void sendSmsMessage(String message)
        throws Exception
    {}

    /**
     * Sending file in sms messages is not supported by this chat transport
     * implementation.
     */
    public FileTransfer sendMultimediaFile(File file)
        throws Exception
    {
        return null;
    }

    /**
     * Not used.
     * @return
     */
    public boolean askForSMSNumber()
    {
        return false;
    }

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
     * Invites the given contact in this chat conference.
     *
     * @param contactAddress the address of the contact to invite
     * @param reason the reason for the invitation
     */
    public void inviteChatContact(String contactAddress, String reason)
    {
        if(adHocChatRoom != null)
            adHocChatRoom.invite(contactAddress, reason);
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
            = adHocChatRoom
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
            = adHocChatRoom
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
            = adHocChatRoom
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
            = adHocChatRoom
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
        return adHocChatRoom;
    }

    public long getMaximumFileLength()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * Returns <tt>true</tt> if this chat transport supports message
     * corrections and false otherwise.
     *
     * @return <tt>true</tt> if this chat transport supports message
     * corrections and false otherwise.
     */
    public boolean allowsMessageCorrections()
    {
        return false;
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
    {}
}
