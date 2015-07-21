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
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.oscar.service.chatrooms.*;

/**
 * Represents an ad-hoc chat room, where multiple chat users could communicate
 * in a many-to-many fashion.
 *
 * @author Valentin Martinet
 */
public class AdHocChatRoomIcqImpl
    implements AdHocChatRoom
{
    private static final Logger logger =
        Logger.getLogger(AdHocChatRoomIcqImpl.class);

    /**
     * Listeners that will be notified of changes in member status in the room
     * such as member joined, left or being kicked or dropped.
     */
    private Vector<AdHocChatRoomParticipantPresenceListener> memberListeners =
        new Vector<AdHocChatRoomParticipantPresenceListener>();

    /**
     * Listeners that will be notified every time a new message is received on
     * this chat room.
     */
    private Vector<AdHocChatRoomMessageListener> messageListeners =
        new Vector<AdHocChatRoomMessageListener>();

    /**
     * Chat room invitation from the icq provider, needed for joining a chat
     * room.
     */
    private ChatInvitation chatInvitation = null;

    /**
     * Chat room session from the icq provider, we get this after joining a chat
     * room. Provides most of the function we need for multi user chatting.
     */
    private ChatRoomSession chatRoomSession = null;

    /**
     * The list of participants of this ad-hoc chat room.
     */
    private Hashtable<String, Contact> participants =
        new Hashtable<String, Contact>();

    /**
     * The operation set that created us.
     */
    private OperationSetAdHocMultiUserChatIcqImpl opSetMuc = null;

    /**
     * The protocol provider that created us
     */
    private ProtocolProviderServiceIcqImpl provider = null;

    /**
     * List with invitations.
     */
    private Hashtable<String, String> inviteUserList =
        new Hashtable<String, String>();

    /**
     * HTML mime type
     */
    private static final String HTML_MIME_TYPE = "text/html";

    private static final String defaultHtmlStartTag = "<HTML>";

    private static final String defaultHtmlEndTag = "</HTML>";

    /**
     * Chat room name.
     */

    private String chatRoomName = "";

    /**
     * The nick name of the user.
     */
    private String nickName = "";

    /**
     * Chat room subject. Note: ICQ does not support chat room subjects.
     */
    private String chatSubject = "";

    /**
     * Constructor for chat room instances, with a given chat room invitation.
     * If this constructor is used the user was invited to a chat room.
     *
     * @param chatInvitation Chat room invitation that the user received from
     *            the ICQ network
     * @param icqProvider The ICQ provider
     */
    public AdHocChatRoomIcqImpl(ChatInvitation chatInvitation,
        ProtocolProviderServiceIcqImpl icqProvider)
    {

        chatRoomName = chatInvitation.getRoomName();
        this.chatInvitation = chatInvitation;
        this.provider = icqProvider;

        this.opSetMuc =
            (OperationSetAdHocMultiUserChatIcqImpl) provider
                .getOperationSet(OperationSetAdHocMultiUserChat.class);
    }

    /**
     * Constructor for chat room instances.
     *
     * @param roomName The name of the chat room.
     * @param chatRoomSession Chat room session from the icq network
     * @param icqProvider The icq provider
     */

    public AdHocChatRoomIcqImpl(String roomName,
        ChatRoomSession chatRoomSession,
        ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.chatRoomSession = chatRoomSession;
        chatRoomName = roomName;
        this.provider = icqProvider;

        this.opSetMuc =
            (OperationSetAdHocMultiUserChatIcqImpl) provider
                .getOperationSet(OperationSetAdHocMultiUserChat.class);

        this.chatRoomSession.addListener(new AdHocChatRoomSessionListenerImpl(
            this));

    }

    /**
     * Adds a listener that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     *
     * @param listener a participant status listener.
     */
    public void addParticipantPresenceListener(
        AdHocChatRoomParticipantPresenceListener listener)
    {
        synchronized (memberListeners)
        {
            if (!memberListeners.contains(listener))
                memberListeners.add(listener);
        }
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this chat room.
     *
     * @param listener a <tt>MessageListener</tt> that would be notified every
     *            time a new message is received on this chat room.
     */
    public void addMessageListener(AdHocChatRoomMessageListener listener)
    {
        synchronized (messageListeners)
        {
            if (!messageListeners.contains(listener))
                messageListeners.add(listener);
        }
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now
     *            subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
        String contentEncoding, String subject)
    {
        return new MessageIcqImpl(new String(content), contentType,
            contentEncoding, subject);
    }

    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        Message msg =
            new MessageIcqImpl(messageText,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);

        return msg;
    }

    /**
     * Returns the identifier of this <tt>AdHocChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the identifier of this
     *         <tt>AdHocChatRoom</tt>.
     */
    public String getIdentifier()
    {
        return chatRoomName;
    }

    /**
     * Returns a <tt>List</tt> of <tt>Contact</tt>s corresponding to all
     * participants currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>Contact</tt> corresponding to all room
     *         participants.
     */
    public List<Contact> getParticipants()
    {
        return new LinkedList<Contact>(participants.values());
    }

    /**
     * Returns the number of participants that are currently in this chat room.
     *
     * @return the number of <tt>Contact</tt>s, currently participating in this
     *         room.
     */
    public int getParticipantsCount()
    {
        return participants.size();
    }

    /**
     * Returns the name of this <tt>AdHocChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the name of this <tt>ChatRoom</tt>.
     */
    public String getName()
    {
        return chatRoomName;
    }

    /**
     * Returns the protocol provider service that created us.
     *
     * @return the protocol provider service that created us.
     */
    public ProtocolProviderService getParentProvider()
    {
        return provider;
    }

    /**
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     * <p>
     * To be notified every time the room's subject change you should add a
     * <tt>ChatRoomPropertyChangelistener</tt> to this room.
     * <p>
     *
     * @return the room subject or <tt>null</tt> if the user hasn't joined the
     *         room or the room does not have a subject yet.
     */
    public String getSubject()
    {
        return chatSubject;
    }

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     *         context of the local chat room.
     */
    public String getUserNickname()
    {
        if (nickName == null)
            nickName =
                provider.getInfoRetreiver().getNickName(
                    provider.getAccountID().getUserID());

        return nickName;
    }

    /**
     * Invites another user to this room. If we're not joined nothing will
     * happen.
     *
     * @param userAddress the address of the user (email address) to invite to
     *            the room.(one may also invite users not on their contact
     *            list).
     * @param reason invitation message
     */
    public void invite(String userAddress, String reason)
    {
        assertConnected();

        if (logger.isInfoEnabled())
            logger.info("Inviting " + userAddress + " for reason: " + reason);

        if (chatRoomSession.getState().equals(ChatSessionState.INROOM))
            chatRoomSession.invite(new Screenname(userAddress), reason);
        else
            inviteUserList.put(userAddress, reason);
    }

    /**
     * Joins this chat room with the nickname of the local user so that the user
     * would start receiving events and messages for it.
     *
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void join() throws OperationFailedException
    {
        if (chatRoomSession == null && chatInvitation == null)
        { // the session is not set and we don't have a chatInvitatoin
            // so we try to join the chatRoom again
            ChatRoomManager chatRoomManager =
                provider.getAimConnection().getChatRoomManager();
            chatRoomSession = chatRoomManager.joinRoom(this.getName());
            chatRoomSession.addListener(new AdHocChatRoomSessionListenerImpl(
                this));
        }
        else if (chatInvitation != null)
        {
            chatRoomSession = chatInvitation.accept();
            chatRoomSession.addListener(new AdHocChatRoomSessionListenerImpl(
                this));
        }

        // We don't specify a reason.
        opSetMuc.fireLocalUserPresenceEvent(this,
            LocalUserAdHocChatRoomPresenceChangeEvent.LOCAL_USER_JOINED, null);
    }

    /**
     * Leave this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered. Depending on the underlying protocol and
     * implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     */
    public void leave()
    {
        if (chatRoomSession != null)
        { // manually close the chat room session
            // and set the chat room session to null.
            chatRoomSession.close();
            chatRoomSession = null;
        }

        Iterator<Entry<String, Contact>> membersSet =
            participants.entrySet().iterator();

        while (membersSet.hasNext())
        {
            Map.Entry<String, Contact> memberEntry = membersSet.next();

            Contact participant = memberEntry.getValue();

            fireParticipantPresenceEvent(participant,
                AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT,
                "Local user has left the chat room.");
        }

        // Delete the list of members
        participants.clear();
    }

    /**
     * Removes a listener that was being notified of changes in the status of
     * other ad-hoc chat room participants.
     *
     * @param listener a participant status listener.
     */
    public void removeParticipantPresenceListener(
        AdHocChatRoomParticipantPresenceListener listener)
    {
        synchronized (memberListeners)
        {
            memberListeners.remove(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     *
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(AdHocChatRoomMessageListener listener)
    {
        synchronized (messageListeners)
        {
            messageListeners.remove(listener);
        }
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param message The <tt>Message</tt> to send.
     * @throws OperationFailedException if the underlying stack is not
     *             registered or initialized or if the chat room is not joined.
     */
    public void sendMessage(Message message) throws OperationFailedException
    {
        assertConnected();
        try
        {
            chatRoomSession.sendMessage(message.getContent());

            // we don't need to fire a message delivered event, because
            // we will receive this message again.
            AdHocChatRoomMessageDeliveredEvent msgDeliveredEvt =
                new AdHocChatRoomMessageDeliveredEvent(
                    this,
                    new Date(),
                    message,
                    AdHocChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED);

            fireMessageEvent(msgDeliveredEvt);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to send a conference message.");
            throw new OperationFailedException(
                "Failed to send a conference message.",
                OperationFailedException.GENERAL_ERROR);
        }
    }

    /**
     * Notifies all interested listeners that a
     * <tt>ChatRoomMessageDeliveredEvent</tt>,
     * <tt>ChatRoomMessageReceivedEvent</tt> or a
     * <tt>ChatRoomMessageDeliveryFailedEvent</tt> has been fired.
     *
     * @param evt The specific event
     */
    public void fireMessageEvent(EventObject evt)
    {
        Iterator<AdHocChatRoomMessageListener> listeners = null;
        synchronized (messageListeners)
        {
            listeners =
                new ArrayList<AdHocChatRoomMessageListener>(messageListeners)
                    .iterator();
        }

        while (listeners.hasNext())
        {
            AdHocChatRoomMessageListener listener = listeners.next();

            if (evt instanceof AdHocChatRoomMessageDeliveredEvent)
            {
                listener
                    .messageDelivered((AdHocChatRoomMessageDeliveredEvent) evt);
            }
            else if (evt instanceof AdHocChatRoomMessageReceivedEvent)
            {
                listener
                    .messageReceived((AdHocChatRoomMessageReceivedEvent) evt);
            }
            else if (evt instanceof AdHocChatRoomMessageDeliveryFailedEvent)
            {
                listener
                    .messageDeliveryFailed((AdHocChatRoomMessageDeliveryFailedEvent) evt);
            }
        }
    }

    /**
     * Creates the corresponding AdHocChatRoomParticipantPresenceChangeEvent and
     * notifies all <tt>AdHocChatRoomParticipantPresenceListener</tt>s that a
     * Contact has joined or left this <tt>AdHocChatRoom</tt>.
     *
     * @param member the <tt>Contact</tt> that this
     * @param eventID the identifier of the event
     * @param eventReason the reason of the event
     */
    private void fireParticipantPresenceEvent(Contact member, String eventID,
        String eventReason)
    {
        AdHocChatRoomParticipantPresenceChangeEvent evt =
            new AdHocChatRoomParticipantPresenceChangeEvent(this, member,
                eventID, eventReason);

        if (logger.isTraceEnabled())
            logger.trace("Will dispatch the following ChatRoom event: " + evt);

        Iterator<AdHocChatRoomParticipantPresenceListener> listeners = null;
        synchronized (memberListeners)
        {
            listeners =
                new ArrayList<AdHocChatRoomParticipantPresenceListener>(
                    memberListeners).iterator();
        }

        while (listeners.hasNext())
        {
            AdHocChatRoomParticipantPresenceListener listener =
                listeners.next();

            listener.participantPresenceChanged(evt);
        }
    }

    /**
     * Our listener for all events for this chat room, e.g. incoming messages or
     * users that leave or join the chat room.
     *
     */

    private class AdHocChatRoomSessionListenerImpl
        implements ChatRoomSessionListener
    {
        /**
         * The chat room this listener is for.
         */
        private AdHocChatRoomIcqImpl chatRoom = null;

        /**
         * Constructor for this listener, needed to set the chatRoom.
         *
         * @param room The containing chat room.
         */
        public AdHocChatRoomSessionListenerImpl(AdHocChatRoomIcqImpl room)
        {
            chatRoom = room;
        }

        /**
         * Handles incoming messages for the specified chat room.
         *
         * @param chatRoomSession Specific chat room session
         * @param chatRoomUser The User who sends the message
         * @param chatMessage The message
         */

        public void handleIncomingMessage(ChatRoomSession chatRoomSession,
            ChatRoomUser chatRoomUser, ChatMessage chatMessage)
        {
            if (logger.isDebugEnabled())
                logger.debug("Incoming multi user chat message received: "
                + chatMessage.getMessage());

            String msgBody = chatMessage.getMessage();

            String msgContent;
            if (msgBody.startsWith(defaultHtmlStartTag))
            {
                msgContent =
                    msgBody.substring(msgBody.indexOf(defaultHtmlStartTag)
                        + defaultHtmlStartTag.length(), msgBody
                        .indexOf(defaultHtmlEndTag));
            }
            else
                msgContent = msgBody;

            Message newMessage =
                createMessage(
                    msgContent.getBytes(),
                    HTML_MIME_TYPE,
                    OperationSetBasicInstantMessagingIcqImpl
                        .DEFAULT_MIME_ENCODING,
                    null);

            String participantUID = chatRoomUser.getScreenname().getFormatted();

            if (participantUID.equals(nickName))
                return;

            AdHocChatRoomMessageReceivedEvent msgReceivedEvent =
                new AdHocChatRoomMessageReceivedEvent(
                    chatRoom,
                    participants.get(participantUID),
                    new Date(),
                    newMessage,
                    AdHocChatRoomMessageReceivedEvent
                        .CONVERSATION_MESSAGE_RECEIVED);

            fireMessageEvent(msgReceivedEvent);
        }

        public void handleStateChange(ChatRoomSession chatRoomSession,
            ChatSessionState oldChatSessionState,
            ChatSessionState newChatSessionState)
        {
            if (logger.isDebugEnabled())
                logger.debug("ChatRoomSessionState changed to: "
                + newChatSessionState);

            if (chatInvitation == null
                && newChatSessionState.equals(ChatSessionState.INROOM))
            {
                try
                {
                    chatRoom.join();
                }
                catch (Exception e)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Failed to join the chat room: " + e);
                }
            }

            if (inviteUserList != null
                && newChatSessionState.equals(ChatSessionState.INROOM))
            {
                Iterator<Map.Entry<String, String>> invitesIter =
                    inviteUserList.entrySet().iterator();

                while (invitesIter.hasNext())
                {
                    Map.Entry<String, String> entry = invitesIter.next();

                    chatRoom.invite(entry.getKey(), entry.getValue());
                }
            }

            if (newChatSessionState.equals(ChatSessionState.CLOSED)
                || newChatSessionState.equals(ChatSessionState.FAILED))
            {
                // the chatRoom is closed or we failed to join, so we remove all
                // chat room user from the chat room member list
                updateMemberList(chatRoomSession.getUsers(), true);
            }
        }

        public void handleUsersJoined(ChatRoomSession chatRoomSession,
            Set<ChatRoomUser> chatRoomUserSet)
        {
            // add the new members to the member list
            updateMemberList(chatRoomUserSet, false);
        }

        public void handleUsersLeft(ChatRoomSession chatRoomSession,
            Set<ChatRoomUser> chatRoomUserSet)
        {
            // remove the given members from the member list
            updateMemberList(chatRoomUserSet, true);
        }
    }

    /**
     * Updates the member list, if the given boolean is true given members will
     * be added, if it is false the given members will be removed.
     *
     * @param chatRoomUserSet New members or members to remove
     * @param removeMember True if members should be removed, False if members
     *            should be added.
     */
    private void updateMemberList(Set<ChatRoomUser> chatRoomUserSet,
        boolean removeMember)
    {
        Iterator<ChatRoomUser> it = chatRoomUserSet.iterator();

        while (it.hasNext())
        {
            ChatRoomUser user = it.next();
            String uid = user.getScreenname().getFormatted();

            // we want to add a member and he/she is not in our member list
            if (!removeMember && !participants.containsKey(uid)
                && !uid.equals(provider.getAccountID().getUserID()))
            {
                OperationSetPersistentPresenceIcqImpl presenceOpSet =
                    (OperationSetPersistentPresenceIcqImpl) getParentProvider()
                        .getOperationSet(OperationSetPersistentPresence.class);

                Contact participant =
                    presenceOpSet.getServerStoredContactList()
                        .findContactByScreenName(uid);

                participants.put(uid, participant);

                fireParticipantPresenceEvent(participant,
                    AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED,
                    null);
            }
            // we want to remove a member and found him/her in our member list
            if (removeMember && participants.containsKey(uid))
            {
                Contact participant = participants.get(uid);

                participants.remove(uid);

                fireParticipantPresenceEvent(participant,
                    AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT,
                    null);
            }
        }
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws java.lang.IllegalStateException if the underlying stack is not
     *             registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (provider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                    + "service before being able to communicate.");
        if (!provider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                    + "being able to communicate.");
    }

    /**
     * Finds the member of this chat room corresponding to the given nick name.
     *
     * @param nickName the nick name to search for.
     * @return the member of this chat room corresponding to the given nick
     *         name.
     */
    public Contact findParticipantForNickName(String nickName)
    {
        return participants.get(nickName);
    }
}
