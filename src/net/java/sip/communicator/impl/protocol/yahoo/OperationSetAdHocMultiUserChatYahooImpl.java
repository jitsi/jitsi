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
import ymsg.network.event.*;

/**
 * A Yahoo implementation of the ad-hoc multi user chat operation set.
 *
 * @author Rupert Burchardi
 * @author Valentin Martinet
 * @author Yana Stamcheva
 */
public class OperationSetAdHocMultiUserChatYahooImpl
implements OperationSetAdHocMultiUserChat
{
    private static final Logger logger =
        Logger.getLogger(OperationSetAdHocMultiUserChatYahooImpl.class);

    /**
     * A list of listeners subscribed for invitations multi user chat events.
     */
    private final List<AdHocChatRoomInvitationListener> invitationListeners
    = new Vector<AdHocChatRoomInvitationListener>();

    /**
     * A list of listeners subscribed for events indicating rejection of a multi
     * user chat invitation sent by us.
     */
    private final List<AdHocChatRoomInvitationRejectionListener>
    invitationRejectionListeners
    = new Vector<AdHocChatRoomInvitationRejectionListener>();

    /**
     * Listeners that will be notified of changes in our status in the room such
     * as us being kicked, banned, or granted admin permissions.
     */
    private final List<LocalUserAdHocChatRoomPresenceListener> presenceListeners
    = new Vector<LocalUserAdHocChatRoomPresenceListener>();

    /**
     * A list of the rooms that are currently open by this account.
     */
    private final Hashtable<String, AdHocChatRoomYahooImpl> chatRoomCache
    = new Hashtable<String, AdHocChatRoomYahooImpl>();

    /**
     * The currently valid Yahoo protocol provider service implementation.
     */
    private final ProtocolProviderServiceYahooImpl yahooProvider;

    /**
     * The operation set for the basic instant messaging, provides some message
     * format functions.
     */
    private final OperationSetBasicInstantMessagingYahooImpl opSetBasic;

    /**
     * Instantiates the user operation set with a currently valid instance of
     * the Yahoo protocol provider.
     *
     * @param yahooProvider a currently valid instance of
     *            ProtocolProviderServiceYahooImpl.
     */
    OperationSetAdHocMultiUserChatYahooImpl(
        ProtocolProviderServiceYahooImpl yahooProvider)
    {
        this.yahooProvider = yahooProvider;

        yahooProvider
        .addRegistrationStateChangeListener(new RegistrationStateListener());

        opSetBasic =
            (OperationSetBasicInstantMessagingYahooImpl) yahooProvider
            .getOperationSet(OperationSetBasicInstantMessaging.class);
    }

    /**
     * Adds a listener to invitation notifications.
     *
     * @param listener An invitation listener.
     */
    public void addInvitationListener(AdHocChatRoomInvitationListener listener)
    {
        synchronized (invitationListeners)
        {
            if (!invitationListeners.contains(listener))
                invitationListeners.add(listener);
        }
    }

    /**
     * Removes a listener that was being notified of changes in our status in a
     * room such as us being kicked, banned or dropped.
     *
     * @param listener the <tt>LocalUserAdHocChatRoomPresenceListener</tt>.
     */
    public void removeInvitationListener(
        AdHocChatRoomInvitationListener listener)
    {
        synchronized (invitationListeners)
        {
            invitationListeners.remove(listener);
        }
    }

    /**
     * Subscribes <tt>listener</tt> so that it would receive events indicating
     * rejection of a multi user chat invitation that we've sent earlier.
     *
     * @param listener the listener that we'll subscribe for invitation
     *            rejection events.
     */

    public void addInvitationRejectionListener(
        AdHocChatRoomInvitationRejectionListener listener)
    {
        synchronized (invitationRejectionListeners)
        {
            if (!invitationRejectionListeners.contains(listener))
                invitationRejectionListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation rejection events.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationRejectionListener(
        AdHocChatRoomInvitationRejectionListener listener)
    {
        synchronized (invitationRejectionListeners)
        {
            invitationRejectionListeners.remove(listener);
        }
    }

    /**
     * Adds a listener that will be notified of changes in our status in a chat
     * room such as us being kicked, banned or dropped.
     *
     * @param listener the <tt>LocalUserAdHocChatRoomPresenceListener</tt>.
     */
    public void addPresenceListener(
        LocalUserAdHocChatRoomPresenceListener listener)
    {
        synchronized (presenceListeners)
        {
            if (!presenceListeners.contains(listener))
                presenceListeners.add(listener);
        }
    }

    /**
     * Removes a listener that was being notified of changes in our status in a
     * room such as us being kicked, banned or dropped.
     *
     * @param listener the <tt>LocalUserChatRoomPresenceListener</tt>.
     */
    public void removePresenceListener(
        LocalUserAdHocChatRoomPresenceListener listener)
    {
        synchronized (presenceListeners)
        {
            presenceListeners.remove(listener);
        }
    }

    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to. Note the roomProperties also contain
     * users that we like to invite to the chatRoom, this is required in the
     * yahoo protocol.
     *
     * @param roomName the name of the <tt>AdHocChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     *            created.
     *
     * @throws OperationFailedException if the room couldn't be created for some
     *             reason (e.g. room already exists; user already joined to an
     *             existent room or user has no permissions to create a chat
     *             room).
     *
     * @return ChatRoom the chat room that we've just created.
     */
    public AdHocChatRoom createAdHocChatRoom(String roomName,
        Map<String, Object> roomProperties)
    throws  OperationFailedException
    {
        return createAdHocChatRoom(roomName, (String[]) null, "");
    }

    /**
     * Creates an ad-hoc room with the named <tt>adHocRoomName</tt> and in
     * including to the specified <tt>contacts</tt>. When the method returns the
     * ad-hoc room the local user will have joined it.
     *
     * @return the ad-hoc room that has been just created
     * @param adHocRoomName the name of the room to be created
     * @param contacts the list of contacts ID
     * @param reason the reason for contacts' invitation
     * @throws OperationFailedException if the room couldn't be created for
     * some reason
     */
    public AdHocChatRoom createAdHocChatRoom(   String adHocRoomName,
        List<String> contacts,
        String reason)
    throws OperationFailedException
    {
        String[] contactsToInvite = new String[contacts.size()];
        for(int i=0; i<contacts.size(); i++)
        {
            contactsToInvite[i] = contacts.get(i);
        }
        return createAdHocChatRoom(
            adHocRoomName, contactsToInvite, reason);
    }

    /**
     * Creates an ad-hoc room with the named <tt>adHocRoomName</tt> and in
     * including to the specified <tt>contacts</tt>. When the method returns the
     * ad-hoc room the local user will have joined it.
     *
     * @param roomName name of the chatroom
     * @param invitedContacts contacts to be invited to this room
     * @param reason reason of this invitation
     * @return AdHocChatRoom the ad-hoc room that has been just created
     * @throws OperationFailedException
     */
    private AdHocChatRoom createAdHocChatRoom(
        String roomName,
        String[] invitedContacts,
        String reason)
    throws OperationFailedException
    {
        if (invitedContacts == null)
            invitedContacts = new String[0];

        AdHocChatRoom chatRoom = null;

        try
        {
            YahooConference conference =
                yahooProvider.getYahooSession().createConference(
                    invitedContacts, // users invited to this conference
                    reason, // invite message / topic
                    yahooProvider.getYahooSession().getLoginIdentity());

            chatRoom = createLocalChatRoomInstance(conference);
        }
        catch (Exception e)
        {
            String errorMessage
            = "Failed to create chat room with name: " + roomName;

            if (logger.isDebugEnabled())
                logger.debug(errorMessage, e);
            throw new OperationFailedException(errorMessage,
                OperationFailedException.CHAT_ROOM_NOT_JOINED, e);
        }
        chatRoom.join();
        return chatRoom;
    }

    /**
     * Creates a <tt>AdHocChatRoom</tt> instance from the specified Yahoo
     * conference.
     *
     * @param yahooConference The chat room model from the yahoo lib.
     *
     * @return AdHocChatRoom the chat room that we've just created.
     */
    private AdHocChatRoomYahooImpl createLocalChatRoomInstance(
        YahooConference yahooConference)
    {
        synchronized (chatRoomCache)
        {
            AdHocChatRoomYahooImpl newChatRoom
            = new AdHocChatRoomYahooImpl(yahooConference, yahooProvider);

            chatRoomCache.put(yahooConference.getName(), newChatRoom);

            return newChatRoom;
        }
    }

    /**
     * Creates a <tt>AdHocChatRoom</tt> instance (where the inviter is
     * represented by inviterID parameter) from the specified Yahoo conference.
     *
     * @param yahooConference The chat room model from the yahoo lib.
     * @param inviterID inviter's Yahoo ID which has to be added as room member
     *
     * @return AdHocChatRoom the chat room that we've just created.
     */
    private AdHocChatRoomYahooImpl createLocalChatRoomInstance(
        YahooConference yahooConference, String inviterID)
    {
        synchronized (chatRoomCache)
        {
            AdHocChatRoomYahooImpl newChatRoom
            = new AdHocChatRoomYahooImpl(yahooConference, yahooProvider);

            OperationSetPersistentPresenceYahooImpl opSetPresence  =
                (OperationSetPersistentPresenceYahooImpl) yahooProvider
                .getOperationSet(OperationSetPersistentPresence.class);

            newChatRoom.addChatRoomParticipant(
                opSetPresence.findContactByID(inviterID));
            chatRoomCache.put(yahooConference.getName(), newChatRoom);

            return newChatRoom;
        }
    }

    /**
     * Returns the <tt>AdHocChatRoomYahooImpl</tt> corresponding to the given
     * <tt>conference</tt> if such exists, otherwise returns null.
     *
     * @param conference the <tt>YahooConference</tt>, for which we're searching
     * correspondence
     * @return the <tt>AdHocChatRoomYahooImpl</tt> corresponding to the given
     * <tt>conference</tt> if such exists, otherwise returns null
     */
    private AdHocChatRoomYahooImpl getLocalChatRoomInstance(
        YahooConference conference)
    {
        synchronized (chatRoomCache)
        {
            for (AdHocChatRoomYahooImpl chatRoom : chatRoomCache.values())
            {
                if (chatRoom.getYahooConference().equals(conference))
                    return chatRoom;
            }
        }

        return null;
    }

    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param invitation the connection to use for sending the rejection.
     * @param rejectReason the reason to reject the given invitation
     */
    public void rejectInvitation(AdHocChatRoomInvitation invitation,
        String rejectReason)
    {
        AdHocChatRoomYahooImpl chatRoom =
            (AdHocChatRoomYahooImpl) invitation.getTargetAdHocChatRoom();

        try
        {
            yahooProvider.getYahooSession().declineConferenceInvite(
                chatRoom.getYahooConference(), rejectReason);
        }
        catch (IOException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to reject Invitation: " + e);
        }
    }

    /**
     * Delivers a <tt>AdHocChatRoomInvitationReceivedEvent</tt> to all
     * registered <tt>AdHocChatRoomInvitationListener</tt>s.
     *
     * @param targetChatRoom the room that invitation refers to
     * @param inviter the inviter that sent the invitation
     * @param reason the reason why the inviter sent the invitation
     */
    public void fireInvitationEvent(AdHocChatRoom targetChatRoom,
        String inviter, String reason)
    {
        AdHocChatRoomInvitationYahooImpl invitation =
            new AdHocChatRoomInvitationYahooImpl(targetChatRoom, inviter,
                reason);

        AdHocChatRoomInvitationReceivedEvent evt =
            new AdHocChatRoomInvitationReceivedEvent(this, invitation,
                new Date(System.currentTimeMillis()));

        Iterable<AdHocChatRoomInvitationListener> listeners;
        synchronized (invitationListeners)
        {
            listeners
            = new ArrayList<AdHocChatRoomInvitationListener>(
                invitationListeners);
        }

        for (AdHocChatRoomInvitationListener listener : listeners)
            listener.invitationReceived(evt);
    }

    /**
     * Delivers a <tt>AdHocChatRoomInvitationRejectedEvent</tt> to all
     * registered <tt>AdHocChatRoomInvitationRejectionListener</tt>s.
     *
     * @param sourceChatRoom the room that invitation refers to
     * @param invitee the name of the invitee that rejected the invitation
     * @param reason the reason of the rejection
     */
    public void fireInvitationRejectedEvent(AdHocChatRoom sourceChatRoom,
        String invitee, String reason)
    {
        AdHocChatRoomInvitationRejectedEvent evt =
            new AdHocChatRoomInvitationRejectedEvent(
                this, sourceChatRoom, invitee,
                reason, new Date(System.currentTimeMillis()));

        Iterable<AdHocChatRoomInvitationRejectionListener> listeners;
        synchronized (invitationRejectionListeners)
        {
            listeners
            = new ArrayList<AdHocChatRoomInvitationRejectionListener>(
                invitationRejectionListeners);
        }

        for (AdHocChatRoomInvitationRejectionListener listener : listeners)
            listener.invitationRejected(evt);
    }

    /**
     * Delivers a <tt>LocalUserAdHocChatRoomPresenceChangeEvent</tt> to all
     * registered <tt>LocalUserAdHocChatRoomPresenceListener</tt>s.
     *
     * @param chatRoom the <tt>ChatRoom</tt> which has been joined, left, etc.
     * @param eventType the type of this event; one of LOCAL_USER_JOINED,
     *            LOCAL_USER_LEFT, etc.
     * @param reason the reason
     */
    public void fireLocalUserPresenceEvent(AdHocChatRoom chatRoom,
        String eventType, String reason)
    {
        LocalUserAdHocChatRoomPresenceChangeEvent evt =
            new LocalUserAdHocChatRoomPresenceChangeEvent(
                this, chatRoom, eventType,
                reason);

        Iterable<LocalUserAdHocChatRoomPresenceListener> listeners;
        synchronized (presenceListeners)
        {
            listeners =
                new ArrayList<LocalUserAdHocChatRoomPresenceListener>(
                    presenceListeners);
        }

        for (LocalUserAdHocChatRoomPresenceListener listener : listeners)
            listener.localUserAdHocPresenceChanged(evt);
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
     * @throws UnsupportedEncodingException missing utf-8 in platform we use.
     */
    private Message createMessage(byte[] content, String contentType,
        String contentEncoding, String subject)
        throws UnsupportedEncodingException
    {
        return new MessageYahooImpl(new String(content, "UTF-8"), contentType,
            contentEncoding, subject);
    }

    /**
     * Creates a message by a given message text.
     *
     * @param messageText The message text.
     * @return the newly created message.
     */
    public Message createMessage(String messageText)
    {
        return new MessageYahooImpl(messageText,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);
    }

    /**
     * Our listener that will tell us when we're registered to yahoo network.
     *
     */
    private class RegistrationStateListener
    implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever a
         * change in the registration state of the corresponding provider had
         * occurred.
         *
         * @param evt ProviderStatusChangeEvent the event describing the status
         *            change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                yahooProvider.getYahooSession().addSessionListener(
                    new YahooMessageListener());
            }
        }
    }

    /**
     * Our group chat message listener, it extends the SessionAdapter from the
     * the Yahoo library.
     *
     */
    private class YahooMessageListener
    extends SessionAdapter
    {

        @Override
        public void conferenceInviteDeclinedReceived(SessionConferenceEvent ev)
        {
            if (logger.isDebugEnabled())
                logger.debug("Group Chat invite declined received. "
                + ev.toString());
            try
            {
                AdHocChatRoom chatRoom = getLocalChatRoomInstance(ev.getRoom());

                fireInvitationRejectedEvent(chatRoom, ev.getFrom(), ev
                    .getMessage());
            }
            catch (Exception e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Error: " + e);
            }
        }

        @Override
        public void conferenceInviteReceived(SessionConferenceEvent ev)
        {
            if (logger.isDebugEnabled())
                logger.debug("Conference Invite Received: " + ev.toString());

            try
            {
                AdHocChatRoom chatRoom = getLocalChatRoomInstance(ev.getRoom());

                if (chatRoom == null)
                {
                    chatRoom =
                        createLocalChatRoomInstance(ev.getRoom(), ev.getFrom());

                    fireInvitationEvent(
                        chatRoom, ev.getFrom(), ev.getMessage());
                }

            }
            catch (Exception e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Error: " + e);
            }
        }

        @Override
        public void conferenceLogoffReceived(SessionConferenceEvent ev)
        {
            if (logger.isDebugEnabled())
                logger.debug("Conference Logoff Received: " + ev.toString());

            try
            {
                AdHocChatRoomYahooImpl chatRoom
                = getLocalChatRoomInstance(ev.getRoom());

                if (chatRoom != null)
                {
                    Contact participant =
                        chatRoom.findParticipantForAddress(ev.getFrom());

                    chatRoom.removeChatRoomParticipant(participant);
                }
            }
            catch (Exception e)
            {
                logger
                .debug("Failed to remove a user from the chat room. " + e);
            }
        }

        @Override
        public void conferenceLogonReceived(SessionConferenceEvent ev)
        {
            if (logger.isDebugEnabled())
                logger.debug("Conference Logon Received: " + ev.toString());

            try
            {
                AdHocChatRoomYahooImpl chatRoom
                = getLocalChatRoomInstance(ev.getRoom());

                if (chatRoom != null)
                {
                    OperationSetPersistentPresenceYahooImpl presenceOpSet =
                        (OperationSetPersistentPresenceYahooImpl) chatRoom
                        .getParentProvider().getOperationSet(
                            OperationSetPersistentPresence.class);

                    Contact participant =
                        presenceOpSet.findContactByID(ev.getFrom());

                    chatRoom.addChatRoomParticipant(participant);
                }
            }
            catch (Exception e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to add a user to the chat room. " + e);
            }
        }

        @Override
        public void conferenceMessageReceived(SessionConferenceEvent ev)
        {
            if (logger.isDebugEnabled())
                logger.debug("Conference Message Received: " + ev.toString());

            try
            {
                String formattedMessage = ev.getMessage();
                if (logger.isDebugEnabled())
                    logger.debug("original message received : " + formattedMessage);

                formattedMessage = opSetBasic.decodeMessage(formattedMessage);
                if (logger.isDebugEnabled())
                    logger.debug("formatted Message : " + formattedMessage);
                // As no indications in the protocol is it html or not. No harm
                // to set all messages html - doesn't affect the appearance of
                // the gui

                Message newMessage =
                    createMessage(
                        formattedMessage.getBytes("UTF-8"),
                        OperationSetBasicInstantMessaging.HTML_MIME_TYPE,
                        OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING,
                        null);

                AdHocChatRoomYahooImpl chatRoom =
                    getLocalChatRoomInstance(ev.getRoom());

                if (chatRoom != null)
                {
                    Contact member =
                        chatRoom.findParticipantForAddress(ev.getFrom());

                    AdHocChatRoomMessageReceivedEvent msgReceivedEvent =
                        new AdHocChatRoomMessageReceivedEvent(
                            chatRoom,
                            member,
                            new Date(),
                            newMessage,
                            AdHocChatRoomMessageReceivedEvent
                            .CONVERSATION_MESSAGE_RECEIVED);

                    chatRoom.fireMessageEvent(msgReceivedEvent);
                }
            }
            catch (Exception e)
            {
                logger
                .debug("Error while receiving a multi user chat message: "
                    + e);
            }

        }

        @Override
        public void connectionClosed(SessionEvent ev)
        {
            if (logger.isDebugEnabled())
                logger.debug("Connection Closed: " + ev.toString());
        }
    }

    public List<AdHocChatRoom> getAdHocChatRooms()
    {
        return new ArrayList<AdHocChatRoom>(chatRoomCache.values());
    }
}
