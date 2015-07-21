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
package net.java.sip.communicator.impl.protocol.yahoo;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import ymsg.network.*;

/**
 * Represents a Yahoo ad-hoc chat room, where multiple chat users could
 * communicate in a many-to-many fashion.
 *
 * @author Rupert Burchardi
 * @author Valentin Martinet
 */
public class AdHocChatRoomYahooImpl
    implements AdHocChatRoom
{
    private static final Logger logger = Logger
        .getLogger(AdHocChatRoomYahooImpl.class);

    /**
     * Listeners that will be notified of changes in member status in the room
     *   such as member joined, left or being kicked or dropped.
     */
    private Vector<AdHocChatRoomParticipantPresenceListener> memberListeners
        = new Vector<AdHocChatRoomParticipantPresenceListener>();

    /**
     * Listeners that will be notified every time a new message is received on
     * this ad-hoc chat room.
     */
    private Vector<AdHocChatRoomMessageListener> messageListeners
        = new Vector<AdHocChatRoomMessageListener>();

    /**
     * The protocol provider that created us
     */
    private ProtocolProviderServiceYahooImpl provider = null;

    /**
     * The operation set that created us.
     */
    private OperationSetAdHocMultiUserChatYahooImpl opSetMuc = null;

    /**
     * The list of participants of this chat room.
     */
    private Hashtable<String, Contact> participants
        = new Hashtable<String, Contact>();

    /**
     * The nickname of this chat room local user participant.
     */
    private String nickname;

    /**
     * The yahoo conference model of this ad-hoc chat room, its the
     * representation of an ad-hoc chat room in the lib for this protocol.
     */
    private YahooConference yahooConference = null;

    /**
     * Creates an instance of a chat room that has been.
     *
     * @param multiUserChat
     *            MultiUserChat
     * @param provider
     *            a reference to the currently valid jabber protocol provider.
     */
    public AdHocChatRoomYahooImpl(  YahooConference multiUserChat,
                                    ProtocolProviderServiceYahooImpl provider)
    {
        this.yahooConference = multiUserChat;
        this.provider = provider;
        this.opSetMuc = (OperationSetAdHocMultiUserChatYahooImpl) provider
            .getOperationSet(OperationSetAdHocMultiUserChat.class);
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this chat room.
     *
     * @param listener A <tt>MessageListener</tt> that would be notified every
     *                 time a new message is received on this chat room.
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
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     *
     * @param listener The <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(AdHocChatRoomMessageListener listener)
    {
        synchronized (messageListeners)
        {
            messageListeners.remove(listener);
        }
    }

    /**
     * Adds a listener that will be notified of changes in our status in the
     * room.
     *
     * @param listener A participant status listener.
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
     * Removes a listener that was being notified of changes in the status of
     * other chat room participants.
     *
     * @param listener A participant status listener.
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
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText
     *            the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        Message msg = new MessageYahooImpl(messageText,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);
        return msg;
    }

    /**
     * Returns a <tt>List</tt> of <tt>Contact</tt>s corresponding to all members
     * currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>Contact</tt> corresponding to all room
     *         members.
     */
    public List<Contact> getParticipants()
    {
        return new LinkedList<Contact>(participants.values());
    }

    /**
     * Updates the member list of the chat room.
     *
     */
    public void updateParticipantsList()
    {
        Iterator<?> it = yahooConference.getMembers().iterator();

        while (it.hasNext())
        {
            YahooUser user = (YahooUser) it.next();
            Contact contact;
            OperationSetPersistentPresenceYahooImpl presenceOpSet
                = (OperationSetPersistentPresenceYahooImpl) this
                .getParentProvider().getOperationSet(
                    OperationSetPersistentPresence.class);

            contact = presenceOpSet.findContactByID(user.getId());

            if(!participants.containsKey(contact.getDisplayName()))
            {
                participants.put(contact.getDisplayName(), contact);
            }
        }
    }

    /**
     * Returns the identifier of this <tt>AdHocChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the identifier of this
     * <tt>AdHocChatRoom</tt>.
     */
    public String getIdentifier()
    {
        return yahooConference.getName();
    }

    /**
     * Returns the number of participants that are currently in this ad-hoc chat
     * room.
     *
     * @return the number of <tt>Contact</tt>s, currently participating in
     * this ad-hoc room.
     */
    public int getParticipantsCount()
    {
        return yahooConference.getMembers().size();
    }

    /**
     * Returns the name of this <tt>AdHocChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the name of this
     * <tt>AdHocChatRoom</tt>.
     */
    public String getName()
    {
        return yahooConference.getName();
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
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     * context of the local ad-hoc chat room.
     */

    public String getUserNickname()
    {
        if(nickname == null)
            nickname = provider.getYahooSession().getLoginIdentity().getId();

        return nickname;
    }

    /**
     * Invites another user to this room. If we're not joined nothing will
     * happen.
     *
     * @param userAddress The identifier of the contact (email address or yahoo
     * id)
     * @param reason The invite reason, which is send to the invitee.
     */
    public void invite(String userAddress, String reason)
    {
        try
        {
                provider.getYahooSession().extendConference(yahooConference,
                    userAddress, reason);
        }
        catch (IOException ioe)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to invite the user: " + userAddress
                + " Error: " + ioe);
        }
    }

    /**
     * Indicates whether or not this chat room is corresponding to a server
     * channel. Note: Returns always <code>false</code>.
     *
     * @return Always <code>false</code> since system chat room can't be joined
     * with current yahoo library.
     */
    public boolean isSystem()
    {
        return false;
    }

    /**
     * Joins this chat room with the nickname of the local user so that the user
     * would start receiving events and messages for it.
     *
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void join() throws OperationFailedException
    {
        this.nickname = provider.getAccountID().getUserID();
        try
        {
            provider.getYahooSession().acceptConferenceInvite(yahooConference);

            // We don't specify a reason.
            opSetMuc.fireLocalUserPresenceEvent(this,
                LocalUserAdHocChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                null);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Couldn't join the chat room: "
                + yahooConference.getName() + e);
        }
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
        try
        {
            provider.getYahooSession().leaveConference(yahooConference);

            Iterator< Map.Entry<String, Contact>> membersSet
                = participants.entrySet().iterator();

            while (membersSet.hasNext())
            {
                Map.Entry<String, Contact> memberEntry = membersSet.next();
                Contact participant = memberEntry.getValue();

                fireParticipantPresenceEvent(participant,
                    AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT,
                    "Local user has left the chat room.");
            }
        }
        catch (IOException ioe)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to leave the chat room: "
                + yahooConference.getName() + " Error: " + ioe);
        }

        participants.clear();
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param message The <tt>Message</tt> to send.
     * @throws OperationFailedException if the underlying stack is not
     * registered or initialized or if the chat room is not joined.
     */
    public void sendMessage(Message message) throws OperationFailedException
    {
        assertConnected();

        try
        {
            provider.getYahooSession().sendConferenceMessage(yahooConference,
                message.getContent());

            AdHocChatRoomMessageDeliveredEvent msgDeliveredEvt
                = new AdHocChatRoomMessageDeliveredEvent(
                    this,
                    new Date(),
                    message,
                    ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED);

            fireMessageEvent(msgDeliveredEvt);
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to send a conference message.");
        }
    }

    /**
     * Notifies all interested listeners that a
     * <tt>AdHocChatRoomMessageDeliveredEvent</tt>,
     * <tt>AdHocChatRoomMessageReceivedEvent</tt> or a
     * <tt>AdHocChatRoomMessageDeliveryFailedEvent</tt> has been fired.
     * @param evt The specific event
     */
    public void fireMessageEvent(EventObject evt)
    {
        Iterator<AdHocChatRoomMessageListener> listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList<AdHocChatRoomMessageListener>(
                messageListeners).iterator();
        }

        while (listeners.hasNext())
        {
            AdHocChatRoomMessageListener listener = listeners.next();

            if (evt instanceof AdHocChatRoomMessageDeliveredEvent)
            {
                listener.messageDelivered(
                    (AdHocChatRoomMessageDeliveredEvent) evt);
            }
            else if (evt instanceof AdHocChatRoomMessageReceivedEvent)
            {
                listener.messageReceived(
                    (AdHocChatRoomMessageReceivedEvent) evt);
            }
            else if (evt instanceof AdHocChatRoomMessageDeliveryFailedEvent)
            {
                listener.messageDeliveryFailed(
                    (AdHocChatRoomMessageDeliveryFailedEvent) evt);
            }
        }
    }

    /**
     * Creates the corresponding AdHocChatRoomParticipantPresenceChangeEvent and
     * notifies all <tt>AdHocChatRoomParticipantPresenceListener</tt>s that a
     * Contact has joined or left this <tt>AdHocChatRoom</tt>.
     *
     * @param participant the <tt>Contact</tt> that this
     * @param eventID the identifier of the event
     * @param eventReason the reason of the event
     */
    public void fireParticipantPresenceEvent(Contact participant, String eventID,
           String eventReason)
    {
        AdHocChatRoomParticipantPresenceChangeEvent evt
            = new AdHocChatRoomParticipantPresenceChangeEvent(this,
                participant,
                eventID,
                eventReason);

        if (logger.isTraceEnabled())
            logger.trace("Will dispatch the following ChatRoom event: " + evt);

        Iterator<AdHocChatRoomParticipantPresenceListener> listeners = null;
        synchronized (memberListeners)
        {
            listeners = new ArrayList<AdHocChatRoomParticipantPresenceListener>
                (memberListeners).iterator();
        }

        while (listeners.hasNext())
        {
            AdHocChatRoomParticipantPresenceListener listener = listeners.next();

            listener.participantPresenceChanged(evt);
        }
    }

    /**
     * Finds the participant of this ad-hoc chat room corresponding to the
     * given address.
     *
     * @param address the address to search for.
     * @return the participant of this chat room corresponding to the given
     * nick name.
     */
    public Contact findParticipantForAddress(String address)
    {
        Iterator<Contact> participantsIter
            = this.participants.values().iterator();

        while (participantsIter.hasNext())
        {
            Contact contact = participantsIter.next();

            if (contact.getAddress().equals(address))
            {
                return contact;
            }
        }

        return null;
    }

    /**
     * Removes the specified ad-hoc chat room participant from the participants
     * list of this ad-hoc chat room.
     * @param participant The member, who should be removed from the ad-hoc chat room
     * participants list.
     */
    public void removeChatRoomParticipant(Contact participant)
    {
        if(participant == null)
            return;

        participants.remove(participant.getDisplayName());

        fireParticipantPresenceEvent(participant,
            AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_LEFT, null);
    }

    /**
     * Adds a participant to the ad-hoc chat room participant list.
     * @param participant The participant, who should be added to the ad-hoc
     * chat room participant list.
     */
    public void addChatRoomParticipant(Contact participant)
    {
        if (participant == null)
            return;

        if (!participants.containsKey(participant.getDisplayName()))
        {
            participants.put(participant.getDisplayName(), participant);

            fireParticipantPresenceEvent(participant,
                AdHocChatRoomParticipantPresenceChangeEvent.CONTACT_JOINED,
                null);
        }
    }

    /**
     * Returns the yahoo conference model of this chat room.
     * @return The yahoo conference.
     */
    public YahooConference getYahooConference()
    {
        return yahooConference;
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (provider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                +"service before being able to communicate.");
        if (!provider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to communicate.");
    }

    /**
     * Determines whether this chat room should be stored in the configuration
     * file or not. If the chat room is persistent it still will be shown after a
     * restart in the chat room list. A non-persistent chat room will be only in
     * the chat room list until the the program is running.
     *
     * @return true if this chat room is persistent, false otherwise
     */
    public boolean isPersistent()
    {
       return false;
    }
}
