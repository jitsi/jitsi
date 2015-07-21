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

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.*;
import net.kano.joustsim.oscar.oscar.service.chatrooms.*;

/**
 * A ICQ implementation of the ad-hoc multi user chat operation set.
 *
 * @author Valentin Martinet
 */
public class OperationSetAdHocMultiUserChatIcqImpl
    implements OperationSetAdHocMultiUserChat
{
    private static final Logger logger =
        Logger.getLogger(OperationSetAdHocMultiUserChatIcqImpl.class);

    /**
     * The currently valid ICQ protocol provider service implementation.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

    /**
     * A list of listeners subscribed for invitations multi user chat events.
     */
    private Vector<AdHocChatRoomInvitationListener> invitationListeners =
        new Vector<AdHocChatRoomInvitationListener>();

    /**
     * A list of listeners subscribed for events indicating rejection of a multi
     * user chat invitation sent by us.
     */
    private Vector<AdHocChatRoomInvitationRejectionListener>
        invitationRejectionListeners
            = new Vector<AdHocChatRoomInvitationRejectionListener>();

    /**
     * Listeners that will be notified of changes in our status in the room such
     * as us being kicked, banned, or granted admin permissions.
     */
    private Vector<LocalUserAdHocChatRoomPresenceListener> presenceListeners =
        new Vector<LocalUserAdHocChatRoomPresenceListener>();

    /**
     * A list of the rooms that are currently open by this account. Note that we
     * have not necessarily joined these rooms, we might have simply been
     * searching through them.
     */
    private Hashtable<String, AdHocChatRoom> chatRoomCache =
        new Hashtable<String, AdHocChatRoom>();

    /**
     * The registration listener that would get notified when the underlying ICQ
     * provider gets registered.
     */
    private RegistrationStateListener providerRegListener =
        new RegistrationStateListener();

    /**
     * A reference to the persistent presence operation set that we use to match
     * incoming messages to <tt>Contact</tt>s and vice versa.
     */
    protected OperationSetPersistentPresenceIcqImpl opSetPersPresence = null;

    /**
     * Hash table that contains all invitations, this is needed if the user
     * wants to reject an invitation.
     */
    private Hashtable<AdHocChatRoom, ChatInvitation> invitations =
        new Hashtable<AdHocChatRoom, ChatInvitation>();

    /**
     * Instantiates the user operation set with a currently valid instance of
     * the Icq protocol provider.
     *
     * @param icqProvider a currently valid instance of
     *            ProtocolProviderServiceIcqImpl.
     */
    OperationSetAdHocMultiUserChatIcqImpl(
        ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.icqProvider = icqProvider;
        icqProvider.addRegistrationStateChangeListener(providerRegListener);
    }

    /**
     * Adds a listener to invitation notifications.
     *
     * @param listener an invitation listener.
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
     * Returns a list of all currently joined <tt>AdHocChatRoom</tt>-s.
     *
     * @return a list of all currently joined <tt>AdHocChatRoom</tt>-s
     */
    public List<AdHocChatRoom> getAdHocChatRooms()
    {
        return new ArrayList<AdHocChatRoom>(chatRoomCache.values());
    }

    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to.
     *
     * @param roomName the name of the <tt>AdHocChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     *            created. Contains list of invitees and the invitation message.
     *
     * @throws OperationFailedException if the room couldn't be created for some
     *             reason (e.g. room already exists; user already joined to an
     *             existent room or user has no permissions to create an ad-hoc
     *             chat room).
     * @throws OperationNotSupportedException if ad-hoc chat room creation is
     *             not supported by this server
     *
     * @return AdHocChatRoom the ad-hoc chat room that we've just created.
     */
    public AdHocChatRoom createAdHocChatRoom(String roomName,
                                            Map<String, Object> roomProperties)
        throws  OperationFailedException,
                OperationNotSupportedException
    {
        AdHocChatRoom chatRoom = null;

        ChatRoomManager chatRoomManager
            = icqProvider.getAimConnection().getChatRoomManager();

        ChatRoomSession chatRoomSession = chatRoomManager.joinRoom(roomName);

        if (chatRoomSession != null)
        {
            chatRoom = createLocalChatRoomInstance( roomName,
                                                    chatRoomSession);
        }

        return chatRoom;
    }

    /**
     * Creates an ad-hoc room with the named <tt>adHocRoomName</tt> and inviting
     * the specified list of <tt>contacts</tt>.
     *
     * @param adHocRoomName the name of the ad-hoc room
     * (however, it won't/does not have to be used since the only way to create
     * an ICQ room is having a room name which begins by the "chat" prefix
     * followed by a random number (this is what is used in the ICQ 7 software).
     *
     * @param contacts the list of contacts ID
     * @param reason the reason to be sent with the invitation for contacts.
     *
     * @return the ad-hoc room that has been just created
     * @throws OperationFailedException
     * @throws OperationNotSupportedException
     */
    public AdHocChatRoom createAdHocChatRoom(String adHocRoomName,
        List<String> contacts, String reason)
        throws OperationFailedException,
        OperationNotSupportedException
    {
        // It's strongly recommended to don't change room's name below:
        AdHocChatRoom adHocChatRoom = createAdHocChatRoom(
            "chat"+new Date().getTime(), new Hashtable<String, Object>());

        if (adHocChatRoom != null && contacts != null)
        {
            for (String address : contacts)
            {
                adHocChatRoom.invite(address, reason);
            }
        }

        return adHocChatRoom;
    }

    /**
     * Creates an <tt>AdHocChatRoom</tt> from the specified
     * <tt>chatInvitation</tt>.
     *
     * @param chatInvitation the chat invitation we received from the
     * chatRoomManager
     *
     * @return the chat room that we've just created.
     */
    private AdHocChatRoom createLocalChatRoomInstance(
        ChatInvitation chatInvitation)
    {
        synchronized (chatRoomCache)
        {
            AdHocChatRoom newChatRoom =
                new AdHocChatRoomIcqImpl(chatInvitation, icqProvider);

            chatRoomCache.put(chatInvitation.getRoomName(), newChatRoom);

            return newChatRoom;
        }
    }

    /**
     * Creates an <tt>AdHocChatRoom</tt> from the specified <tt>roomName</tt>
     * and Icq <tt>chatRoomSession</tt>.
     *
     * @param roomName the name of the room
     * @param chatRoomSession the Icq chat room session corresponding to the
     * room to create
     * @return an <tt>AdHocChatRoom</tt> from the specified <tt>roomName</tt>
     * and Icq <tt>chatRoomSession</tt>
     */
    private AdHocChatRoom createLocalChatRoomInstance(
        String roomName,
        ChatRoomSession chatRoomSession)
    {
        synchronized (chatRoomCache)
        {
            AdHocChatRoom newChatRoom
                = new AdHocChatRoomIcqImpl(
                    roomName, chatRoomSession, icqProvider);

            chatRoomCache.put(roomName, newChatRoom);

            return newChatRoom;
        }
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
        ChatInvitation inv
            = invitations.get(invitation.getTargetAdHocChatRoom());

        if (inv != null)
        { // send the rejection
            inv.reject();
        }
        // remove the invitation
        invitations.remove(invitation.getTargetAdHocChatRoom());
    }

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation events.
     *
     * @param listener the invitation listener to remove.
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
     * Removes a listener that was being notified of changes in our status in a
     * room such as us being joined or dropped.
     *
     * @param listener the <tt>LocalUserAdHocChatRoomPresenceListener</tt>.
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
     * Delivers a <tt>AdHocChatRoomInvitationReceivedEvent</tt> to all
     * registered <tt>AdHocChatRoomInvitationListener</tt>s.
     *
     * @param targetChatRoom the ad-hoc room that invitation refers to
     * @param inviter the inviter that sent the invitation
     * @param reason the reason why the inviter sent the invitation
     * @param password the password to use when joining the room
     */
    public void fireInvitationEvent(AdHocChatRoom targetChatRoom,
        String inviter, String reason, byte[] password)
    {
        AdHocChatRoomInvitationIcqImpl invitation =
            new AdHocChatRoomInvitationIcqImpl(targetChatRoom, inviter, reason,
                password);
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
     * Delivers a <tt>LocalUserAdHocChatRoomPresenceChangeEvent</tt> to all
     * registered <tt>LocalUserAdHocChatRoomPresenceListener</tt>s.
     *
     * @param chatRoom the <tt>AdHocChatRoom</tt> which has been joined, left,
     *            etc.
     * @param eventType the type of this event; one of LOCAL_USER_JOINED,
     *            LOCAL_USER_LEFT, etc.
     * @param reason the reason
     */
    public void fireLocalUserPresenceEvent(AdHocChatRoom chatRoom,
        String eventType, String reason)
    {
        LocalUserAdHocChatRoomPresenceChangeEvent evt =
            new LocalUserAdHocChatRoomPresenceChangeEvent(this, chatRoom,
                eventType, reason);

        Iterable<LocalUserAdHocChatRoomPresenceListener> listeners = null;
        synchronized (presenceListeners)
        {
            listeners
                = new ArrayList<LocalUserAdHocChatRoomPresenceListener>(
                        presenceListeners);
        }

        for (LocalUserAdHocChatRoomPresenceListener listener : listeners)
            listener.localUserAdHocPresenceChanged(evt);
    }

    /**
     * Our listener that will tell us when we're registered to icq and joust sim
     * is ready to accept us as a listener.
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
            if (logger.isDebugEnabled())
                logger.debug("The ICQ provider changed state from: "
                + evt.getOldState() + " to: " + evt.getNewState());
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                String customMessageEncoding = null;
                if ((customMessageEncoding =
                    System.getProperty("icq.custom.message.charset")) != null)
                    OscarTools.setDefaultCharset(customMessageEncoding);

                opSetPersPresence =
                    (OperationSetPersistentPresenceIcqImpl) icqProvider
                        .getOperationSet(OperationSetPersistentPresence.class);

                // add ChatRoomMangagerListener
                icqProvider.getAimConnection().getChatRoomManager()
                    .addListener(new ChatRoomManagerListenerImpl());
            }
        }
    }

    /**
     * Our listener for chat room invitations.
     */
    private class ChatRoomManagerListenerImpl
        implements ChatRoomManagerListener
    {
        public void handleInvitation(ChatRoomManager chatRoomManager,
            ChatInvitation chatInvitation)
        {
            if (logger.isDebugEnabled())
                logger.debug(
                "Invitation received: " + chatInvitation.getRoomName());

            AdHocChatRoom chatRoom
                = createLocalChatRoomInstance(chatInvitation);

            // save chatInvitation, for a possible rejection
            invitations.put(chatRoom, chatInvitation);

            fireInvitationEvent(chatRoom, chatInvitation.getScreenname()
                .toString(), chatInvitation.getMessage(), null);
        }
    }
}
