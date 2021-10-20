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

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.XMPPException.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smackx.muc.MultiUserChatException.*;
import org.jivesoftware.smackx.muc.packet.*;
import org.jivesoftware.smackx.xdata.*;
import org.jivesoftware.smackx.xdata.form.*;
import org.jivesoftware.smackx.xdata.packet.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.jxmpp.jid.parts.*;
import org.jxmpp.stringprep.*;

/**
 * A jabber implementation of the multi user chat operation set.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class OperationSetMultiUserChatJabberImpl
    extends AbstractOperationSetMultiUserChat
    implements SubscriptionListener
{
    /**
     * This class logger.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperationSetMultiUserChatJabberImpl.class);

    /**
     * The currently valid Jabber protocol provider service implementation.
     */
    private final ProtocolProviderServiceJabberImpl jabberProvider;

    /**
     * A list of the rooms that are currently open by this account. Note that
     * we have not necessarily joined these rooms, we might have simply been
     * searching through them.
     */
    private final Hashtable<BareJid, ChatRoomJabberImpl> chatRoomCache
        = new Hashtable<>();

    /**
     * A reference to the persistent presence operation set that we use
     * to match incoming messages to <tt>Contact</tt>s and vice versa.
     */
    private OperationSetPersistentPresenceJabberImpl opSetPersPresence = null;

    /**
     * A listener that is fired anytime an invitation to join a MUC room is
     * received.
     */
    private SmackInvitationListener smackInvitationListener = null;

    /**
     * Instantiates the user operation set with a currently valid instance of
     * the Jabber protocol provider.
     * @param jabberProvider a currently valid instance of
     * ProtocolProviderServiceJabberImpl.
     */
    OperationSetMultiUserChatJabberImpl(
                        ProtocolProviderServiceJabberImpl jabberProvider)
    {
        this.jabberProvider = jabberProvider;

        //The registration listener that would get notified when the underlying
        //Jabber provider gets registered.
        RegistrationStateListener providerRegListener
            = new RegistrationStateListener();
        jabberProvider.addRegistrationStateChangeListener(providerRegListener);

        opSetPersPresence
            = (OperationSetPersistentPresenceJabberImpl)jabberProvider
                .getOperationSet(OperationSetPersistentPresence.class);

        opSetPersPresence.addSubscriptionListener(this);
    }

    /**
     * Add SmackInvitationRejectionListener to <tt>MultiUserChat</tt> instance
     * which will dispatch all rejection events.
     *
     * @param muc the smack MultiUserChat instance that we're going to wrap our
     * chat room around.
     * @param chatRoom the associated chat room instance
     */
    public void addSmackInvitationRejectionListener(MultiUserChat muc,
        ChatRoom chatRoom)
    {
        muc.addInvitationRejectionListener(
            new SmackInvitationRejectionListener(chatRoom));
    }

    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to.
     *
     * @param roomName the name of the <tt>ChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     *   created.
     *
     * @throws OperationFailedException if the room couldn't be created for
     * some reason (e.g. room already exists; user already joined to an
     * existent room or user has no permissions to create a chat room).
     * @throws OperationNotSupportedException if chat room creation is not
     * supported by this server
     *
     * @return ChatRoom the chat room that we've just created.
     */
    public ChatRoom createChatRoom(
            String roomName,
            Map<String, Object> roomProperties)
        throws OperationFailedException,
               OperationNotSupportedException
    {
        //first make sure we are connected and the server supports multichat
        assertSupportedAndConnected();

        ChatRoom room = null;
        if (roomName == null)
            roomName = "chatroom-" + RandomStringUtils.random(4);
        else
            room = findRoom(roomName);

        if (room == null)
        {
            if (logger.isInfoEnabled())
                logger.info("Find room returns null.");

            // rooms using google servers needs a special name.
            // in the form private-chat-UUID@groupchat.google.com
            if(getXmppConnection() != null &&
                getXmppConnection().getHost().toLowerCase().contains("google"))
            {
                roomName = "private-chat-" +
                        UUID.randomUUID().toString() + "@groupchat.google.com";
            }

            MultiUserChat muc;
            try
            {
                MultiUserChatManager manager = MultiUserChatManager
                    .getInstanceFor(getXmppConnection());
                muc = manager.getMultiUserChat(getCanonicalRoomName(roomName));

                Resourcepart nick = Resourcepart.from(JabberActivator.getGlobalDisplayDetailsService()
                    .getDisplayName(jabberProvider));
                muc.create(nick);
            }
            catch (Exception ex)
            {
                logger.error("Failed to create chat room.", ex);
                throw new OperationFailedException(
                    "Failed to create chat room",
                    OperationFailedException.GENERAL_ERROR,
                    ex
                );
            }

            boolean isPrivate = false;
            if(roomProperties != null)
            {
                Object isPrivateObject = roomProperties.get("isPrivate");
                if(isPrivateObject != null)
                {
                    isPrivate = isPrivateObject.equals(true);
                }
            }

            try
            {
                FillableForm form = null;
                if(isPrivate)
                {
                    Form initForm;
                    try
                    {
                        initForm = muc.getConfigurationForm();
                    }
                    catch (NoResponseException
                        | NotConnectedException
                        | InterruptedException e)
                    {
                        throw new OperationFailedException(
                            "Could not get config form",
                            OperationFailedException.GENERAL_ERROR,
                            e
                        );
                    }
                    form = initForm.getFillableForm();
                    //TODO is this init field stuff really required?
                    for (FormField initField : initForm.getDataForm().getFields())
                    {
                        if( initField == null ||
                            initField.getFieldName() == null ||
                            initField.getType() == FormField.Type.fixed ||
                            initField.getType() == FormField.Type.hidden)
                            continue;
                        form.setAnswer(initField.getFieldName(), initField.getValues());
                    }
                    form.setAnswer("muc#roomconfig_membersonly", true);
                    form.setAnswer("muc#roomconfig_allowinvites", true);
                    form.setAnswer("muc#roomconfig_publicroom", false);
                }
                muc.sendConfigurationForm(form);
            }
            catch (XMPPException
                | InterruptedException
                | NotConnectedException
                | NoResponseException e)
            {
                logger.error("Failed to send config form.", e);
            }

            room = createLocalChatRoomInstance(muc);
            // as we are creating the room we are the owner of it
            // at least that's what MultiUserChat.create says
            room.setLocalUserRole(ChatRoomMemberRole.OWNER);
        }
        return room;
    }

    /**
     * Creates a <tt>ChatRoom</tt> from the specified smack
     * <tt>MultiUserChat</tt>.
     *
     * @param muc the smack MultiUserChat instance that we're going to wrap our
     * chat room around.
     *
     * @return ChatRoom the chat room that we've just created.
     */
    private ChatRoom createLocalChatRoomInstance(MultiUserChat muc)
    {
        synchronized(chatRoomCache)
        {
            ChatRoomJabberImpl chatRoom
                = new ChatRoomJabberImpl(muc, jabberProvider);
            this.chatRoomCache.put(muc.getRoom(), chatRoom);

            // Add the contained in this class SmackInvitationRejectionListener
            // which will dispatch all rejection events to the
            // ChatRoomInvitationRejectionListener.
            addSmackInvitationRejectionListener(muc, chatRoom);

            return chatRoom;
        }
    }

    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt>. If the room
     * doesn't exists in the cache it creates it.
     *
     * @param roomName the name of the <tt>ChatRoom</tt> that we're looking
     *   for.
     * @return the <tt>ChatRoom</tt> named <tt>roomName</tt>
     * @throws OperationFailedException if an error occurs while trying to
     * discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi user chat
     */
    public synchronized ChatRoom findRoom(String roomName)
        throws OperationFailedException, OperationNotSupportedException
    {
        //make sure we are connected and multichat is supported.
        assertSupportedAndConnected();

        EntityBareJid canonicalRoomName = getCanonicalRoomName(roomName);
        ChatRoomJabberImpl room = chatRoomCache.get(canonicalRoomName);

        if (room != null)
            return room;

        MultiUserChatManager manager = MultiUserChatManager
            .getInstanceFor(getXmppConnection());
        MultiUserChat muc = manager.getMultiUserChat(canonicalRoomName);
        room = new ChatRoomJabberImpl(muc, jabberProvider);
        chatRoomCache.put(canonicalRoomName, room);
        return room;
    }

    /**
     * Returns a list of the chat rooms that we have joined and are currently
     * active in.
     *
     * @return a <tt>List</tt> of the rooms where the user has joined using
     *   a given connection.
     */
    public List<ChatRoom> getCurrentlyJoinedChatRooms()
    {
        synchronized(chatRoomCache)
        {
            List<ChatRoom> joinedRooms = new LinkedList<>();
            for (ChatRoom cr : this.chatRoomCache.values())
            {
                joinedRooms.add(cr);
            }

            Iterator<ChatRoom> joinedRoomsIter = joinedRooms.iterator();

            while (joinedRoomsIter.hasNext())
                if (!joinedRoomsIter.next().isJoined())
                    joinedRoomsIter.remove();
            return joinedRooms;
        }
    }

//     **this method is not used**
//
//    /**
//     * Returns a list of the names of all chat rooms that <tt>contact</tt> is
//     * currently a  member of.
//     *
//     * @param contact the contact whose current ChatRooms we will be
//     *   querying.
//     * @return a list of <tt>String</tt> indicating the names of  the chat rooms
//     * that <tt>contact</tt> has joined and is currently active in.
//     *
//     * @throws OperationFailedException if an error occurs while trying to
//     * discover the room on the server.
//     * @throws OperationNotSupportedException if the server does not support
//     * multi user chat
//     */
//    public List getCurrentlyJoinedChatRooms(Contact contact)
//        throws OperationFailedException, OperationNotSupportedException
//    {
//        assertSupportedAndConnected();
//
//        Iterator joinedRoomsIter
//            = MultiUserChat.getJoinedRooms( getXmppConnection()
//                                            , contact.getAddress());
//
//        List joinedRoomsForContact = new LinkedList();
//
//        while ( joinedRoomsIter.hasNext() )
//        {
//            MultiUserChat muc = (MultiUserChat)joinedRoomsIter.next();
//            joinedRoomsForContact.add(muc.getRoom());
//        }
//
//        return joinedRoomsForContact;
//    }

    /**
     * Returns the <tt>List</tt> of <tt>String</tt>s indicating chat rooms
     * currently available on the server that this protocol provider is
     * connected to.
     *
     * @return a <tt>java.util.List</tt> of the name <tt>String</tt>s for chat
     * rooms that are currently available on the server that this protocol
     * provider is connected to.
     *
     * @throws OperationFailedException if we faile retrieving this list from
     * the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi user chat
     */
    public List<String> getExistingChatRooms()
        throws  OperationFailedException,
                OperationNotSupportedException
    {
        assertSupportedAndConnected();

        List<String> list = new LinkedList<>();

        //first retrieve all conference service names available on this server
        MultiUserChatManager manager = MultiUserChatManager
            .getInstanceFor(getXmppConnection());
        List<DomainBareJid> serviceNames;
        try
        {
            serviceNames = manager.getMucServiceDomains();
        }
        catch (XMPPException
            | InterruptedException
            | NotConnectedException
            | NoResponseException ex)
        {
            throw new OperationFailedException(
                "Failed to retrieve Jabber conference service names"
                , OperationFailedException.GENERAL_ERROR
                , ex);
        }

        //now retrieve all chat rooms currently available for every service name
        for (DomainBareJid serviceName : serviceNames)
        {
            Map<EntityBareJid, HostedRoom> roomsOnThisService;
            try
            {
                roomsOnThisService =
                    new HashMap<>(manager.getRoomsHostedBy(serviceName));
            }
            catch (XMPPException
                | InterruptedException
                | NotConnectedException
                | NoResponseException
                | NotAMucServiceException ex)
            {
                logger.error("Failed to retrieve rooms for serviceName="
                             + serviceName, ex);
                //continue bravely with other service names
                continue;
            }

            //now go through all rooms available on this service
            for (HostedRoom hr : roomsOnThisService.values())
            {
                list.add(hr.getJid().toString());
            }
        }

        // todo maybe we should add a check here and fail if retrieving chat
        // rooms failed for all service names
        return list;
    }

    /**
     * Returns true if <tt>contact</tt> supports multi user chat sessions.
     *
     * @param contact reference to the contact whose support for chat rooms
     *   we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports
     *   chatrooms.
     */
    public boolean isMultiChatSupportedByContact(Contact contact)
    {
        return contact.getProtocolProvider()
            .getOperationSet(OperationSetMultiUserChat.class) != null;
    }

    /**
     * Checks if the contact address is associated with private messaging
     * contact or not.
     *
     * @return <tt>true</tt> if the contact address is associated with private
     * messaging contact and <tt>false</tt> if not.
     */
    public boolean isPrivateMessagingContact(String contactAddress)
    {
        try
        {
            Jid jid = JidCreate.from(contactAddress);
            return opSetPersPresence.isPrivateMessagingContact(jid);
        }
        catch (XmppStringprepException e)
        {
            logger.error(contactAddress + " is not a valid JID", e);
            return false;
        }
    }

    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param invitation the connection to use for sending the rejection.
     * @param rejectReason the reason to reject the given invitation
     */
    public void rejectInvitation(ChatRoomInvitation invitation,
        String rejectReason)
        throws OperationFailedException
    {
        MultiUserChatManager manager = MultiUserChatManager
            .getInstanceFor(getXmppConnection());
        try
        {
            manager.decline(
                JidCreate.entityBareFrom(invitation.getTargetChatRoom().getName()),
                JidCreate.entityBareFrom(invitation.getInviter()),
                rejectReason
            );
        }
        catch (NotConnectedException
            | XmppStringprepException
            | InterruptedException e)
        {
            throw new OperationFailedException(
                "Could not reject invitation",
                OperationFailedException.GENERAL_ERROR,
                e
            );
        }
    }

    /**
     * Almost all <tt>MultiUserChat</tt> methods require an xmpp connection
     * param so I added this method only for the sake of utility.
     *
     * @return the XMPP connection currently in use by the jabber provider or
     * null if jabber provider has yet to be initialized.
     */
    private XMPPConnection getXmppConnection()
    {
        return (jabberProvider == null)
            ? null
            : jabberProvider.getConnection();
    }

    /**
     * Makes sure that we are properly connected and that the server supports
     * multi user chats.
     *
     * @throws OperationFailedException if the provider is not registered or
     * the xmpp connection not connected.
     * @throws OperationNotSupportedException if the service is not supported
     * by the server.
     */
    private void assertSupportedAndConnected()
        throws OperationFailedException, OperationNotSupportedException
    {
        //throw an exception if the provider is not registered or the xmpp
        //connection not connected.
        if(!jabberProvider.isRegistered()
            || getXmppConnection() == null
            || !getXmppConnection().isConnected())
        {
            throw new OperationFailedException(
                "Provider not connected to jabber server"
                , OperationFailedException.NETWORK_FAILURE);
        }

        // isServiceEnabled(...) requires the subdomain providing the
        // muc service, it doesn't search recursively. We do that in ourselves
        // in getCanonicalRoomName
//        MultiUserChatManager manager = MultiUserChatManager
//            .getInstanceFor(getXmppConnection());
//        try
//        {
//            Jid jid = JidCreate.domainBareFrom(jabberProvider.getAccountID().getService());
//            if (!manager.isServiceEnabled(jid))
//            {
//                throw new OperationNotSupportedException(
//                    "Chat rooms not supported on server "
//                    + jabberProvider.getAccountID().getService()
//                    + " for user "
//                    + jabberProvider.getAccountID().getUserID());
//            }
//        }
//        catch (XMPPErrorException
//            | NotConnectedException
//            | XmppStringprepException
//            | NoResponseException
//            | InterruptedException e)
//        {
//            throw new OperationFailedException(
//                "Could not determine MUC support feature",
//                OperationFailedException.GENERAL_ERROR,
//                e
//            );
//        }
    }

    /**
     * In case <tt>roomName</tt> does not represent a complete room id, the
     * method returns a canonincal chat room name in the following form:
     * roomName@muc-servicename.jabserver.com. In case <tt>roomName</tt> is
     * already a canonical room name, the method simply returns it without
     * changing it.
     *
     * @param roomName the name of the room that we'd like to "canonize".
     *
     * @return the canonincal name of the room (which might be equal to
     * roomName in case it was already in a canonical format).
     *
     * @throws OperationFailedException if we fail retrieving the conference
     * service name
     */
    private EntityBareJid getCanonicalRoomName(String roomName)
        throws OperationFailedException
    {
        try
        {
            return JidCreate.entityBareFrom(roomName);
        }
        catch (XmppStringprepException e)
        {
            // try to append to domain part of our own JID
        }

        List<DomainBareJid> serviceNames;
        try
        {
            MultiUserChatManager manager = MultiUserChatManager
                .getInstanceFor(getXmppConnection());
            serviceNames = manager.getXMPPServiceDomains();
        }
        catch (Exception ex)
        {
            logger.error("Failed to retrieve conference service name for user: "
                + jabberProvider.getAccountID().getUserID()
                + " on server: "
                + jabberProvider.getAccountID().getService()
                , ex);
            throw new OperationFailedException(
                "Failed to retrieve conference service name for user: "
                + jabberProvider.getAccountID().getUserID()
                + " on server: "
                + jabberProvider.getAccountID().getService()
                , OperationFailedException.GENERAL_ERROR
                , ex);

        }

        if (serviceNames != null)
        {
            try
            {
                return JidCreate.entityBareFrom(
                    Localpart.from(roomName),
                    serviceNames.get(0));
            }
            catch (XmppStringprepException e)
            {
                throw new OperationFailedException(
                    roomName + " is not a valid JID local part",
                    OperationFailedException.GENERAL_ERROR,
                    e
                );
            }
        }

        //hmmmm strange.. no service name returned. we should probably throw an
        //exception
        throw new OperationFailedException(
            "Failed to retrieve MultiUserChat service names."
            , OperationFailedException.GENERAL_ERROR);
    }

    /**
     * Returns a reference to the chat room named <tt>chatRoomName</tt> or
     * null if the room hasn't been cached yet.
     *
     * @param chatRoomName the name of the room we're looking for.
     *
     * @return the <tt>ChatRoomJabberImpl</tt> instance that has been cached
     * for <tt>chatRoomName</tt> or null if no such room has been cached so far.
     */
    public ChatRoomJabberImpl getChatRoom(BareJid chatRoomName)
    {
        return this.chatRoomCache.get(chatRoomName);
    }

    /**
     * Returns the list of currently joined chat rooms for
     * <tt>chatRoomMember</tt>.
     * @param chatRoomMember the member we're looking for
     * @return a list of all currently joined chat rooms
     * @throws OperationFailedException if the operation fails
     * @throws OperationNotSupportedException if the operation is not supported
     */
    public List<String> getCurrentlyJoinedChatRooms(ChatRoomMember chatRoomMember)
        throws OperationFailedException, OperationNotSupportedException
    {
        assertSupportedAndConnected();

        MultiUserChatManager manager = MultiUserChatManager
            .getInstanceFor(getXmppConnection());

        try
        {
            EntityFullJid memberJid = JidCreate.from(
                chatRoomMember.getContactAddress()).asEntityFullJidOrThrow();
            List<String> joinedRooms = new ArrayList<>();
            for (Jid jid : manager.getJoinedRooms(memberJid))
            {
                joinedRooms.add(jid.toString());
            }

            return joinedRooms;
        }
        catch (XmppStringprepException
            | NoResponseException
            | InterruptedException
            | NotConnectedException
            | XMPPErrorException e)
        {
            throw new OperationFailedException(
                "Could not get list of joined rooms",
                OperationFailedException.GENERAL_ERROR,
                e
            );
        }
    }

    /**
     * Delivers a <tt>ChatRoomInvitationReceivedEvent</tt> to all
     * registered <tt>ChatRoomInvitationListener</tt>s.
     *
     * @param targetChatRoom the room that invitation refers to
     * @param inviter the inviter that sent the invitation
     * @param reason the reason why the inviter sent the invitation
     * @param password the password to use when joining the room
     */
    public void fireInvitationEvent(
        ChatRoom targetChatRoom,
        EntityJid inviter,
        String reason,
        byte[] password)
    {
        ChatRoomInvitationJabberImpl invitation
            = new ChatRoomInvitationJabberImpl( targetChatRoom,
                                                inviter,
                                                reason,
                                                password);

        fireInvitationReceived(invitation);
    }

    /**
     * A listener that is fired anytime an invitation to join a MUC room is
     * received.
     */
    private class SmackInvitationListener
        implements InvitationListener
    {
        /**
         * Called when the an invitation to join a MUC room is received.<p>
         *
         * If the room is password-protected, the invitee will receive a
         * password to use to join the room. If the room is members-only, the
         * the invitee may be added to the member list.
         *
         * @param conn the XMPPConnection that received the invitation.
         * @param room the room that invitation refers to.
         * @param inviter the inviter that sent the invitation.
         * (e.g. crone1@shakespeare.lit).
         * @param reason the reason why the inviter sent the invitation.
         * @param password the password to use when joining the room.
         * @param message the message used by the inviter to send the invitation.
         */
        @Override
        public void invitationReceived(XMPPConnection conn,
            MultiUserChat room, EntityJid inviter, String reason,
            String password, Message message, MUCUser.Invite invite)
        {
            ChatRoomJabberImpl chatRoom;
            try
            {
                chatRoom = (ChatRoomJabberImpl) findRoom(room.getRoom().toString());
                if (password != null)
                    fireInvitationEvent(
                        chatRoom, inviter, reason, password.getBytes());
                else
                    fireInvitationEvent(
                        chatRoom, inviter, reason, null);
            }
            catch (OperationFailedException | OperationNotSupportedException e)
            {
                logger.error("Failed to find room with name: " + room, e);
            }
        }
    }

    /**
     * A listener that is fired anytime an invitee declines or rejects an
     * invitation.
     */
    private class SmackInvitationRejectionListener
        implements InvitationRejectionListener
    {
        /**
         * The chat room for this listener.
         */
        private ChatRoom chatRoom;

        /**
         * Creates an instance of <tt>SmackInvitationRejectionListener</tt> and
         * passes to it the chat room for which it will listen for rejection
         * events.
         *
         * @param chatRoom chat room for which this intance will listen for
         * rejection events
         */
        public SmackInvitationRejectionListener(ChatRoom chatRoom)
        {
            this.chatRoom = chatRoom;
        }

        /**
         * Called when the invitee declines the invitation.
         *
         * @param invitee the invitee that declined the invitation.
         *                (e.g. hecate@shakespeare.lit).
         * @param reason the reason why the invitee declined the invitation.
         * @param message the message used to decline the invitation.
         * @param rejection the raw decline found in the message.
         */
        @Override
        public void invitationDeclined(EntityBareJid invitee,
                                       String reason,
                                       Message message,
                                       MUCUser.Decline rejection)
        {
            fireInvitationRejectedEvent(chatRoom, invitee.toString(), reason);
        }
    }

    /**
     * Our listener that will tell us when we're registered to Jabber and the
     * smack MultiUserChat is ready to accept us as a listener.
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenever
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                if (logger.isDebugEnabled())
                    logger.debug("adding an Invitation listener to the smack muc");


                MultiUserChatManager manager = MultiUserChatManager
                    .getInstanceFor(getXmppConnection());
                smackInvitationListener = new SmackInvitationListener();
                manager.addInvitationListener(smackInvitationListener);
            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERED
                || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                // clear cached chatrooms as there are no longer valid
                chatRoomCache.clear();

                XMPPConnection connection = getXmppConnection();
                if (smackInvitationListener != null && connection != null)
                {
                    MultiUserChatManager.getInstanceFor(connection)
                        .removeInvitationListener(smackInvitationListener);
                    smackInvitationListener = null;
                }
            }
            else if (evt.getNewState() == RegistrationState.UNREGISTERING)
            {
                XMPPConnection connection = getXmppConnection();
                if (smackInvitationListener != null && connection != null)
                {
                    MultiUserChatManager.getInstanceFor(connection)
                        .removeInvitationListener(smackInvitationListener);
                    smackInvitationListener = null;
                }

                // lets check for joined rooms and leave them
                List<ChatRoom> joinedRooms = getCurrentlyJoinedChatRooms();
                for(ChatRoom room : joinedRooms)
                {
                    room.leave();
                }
            }
        }
    }

    /**
     * Updates corresponding chat room members when a contact has been modified
     * in our contact list.
     * @param evt the <tt>SubscriptionEvent</tt> that notified us
     */
     public void contactModified(ContactPropertyChangeEvent evt)
     {
         Contact modifiedContact = evt.getSourceContact();

         this.updateChatRoomMembers(modifiedContact);
     }

     /**
      * Updates corresponding chat room members when a contact has been created
      * in our contact list.
      * @param evt the <tt>SubscriptionEvent</tt> that notified us
      */
     public void subscriptionCreated(SubscriptionEvent evt)
     {
         Contact createdContact = evt.getSourceContact();

         this.updateChatRoomMembers(createdContact);
     }

     /**
      * Not interested in this event for our member update purposes.
      * @param evt the <tt>SubscriptionEvent</tt> that notified us
      */
     public void subscriptionFailed(SubscriptionEvent evt) {}

     /**
      * Not interested in this event for our member update purposes.
      * @param evt the <tt>SubscriptionEvent</tt> that notified us
      */
     public void subscriptionMoved(SubscriptionMovedEvent evt) {}

     /**
      * Updates corresponding chat room members when a contact has been removed
      * from our contact list.
      * @param evt the <tt>SubscriptionEvent</tt> that notified us
      */
     public void subscriptionRemoved(SubscriptionEvent evt)
     {
     }

     /**
      * Not interested in this event for our member update purposes.
      * @param evt the <tt>SubscriptionEvent</tt> that notified us
      */
     public void subscriptionResolved(SubscriptionEvent evt) {}

     /**
      * Finds all chat room members, which name corresponds to the name of the
      * given contact and updates their contact references.
      *
      * @param contact the contact we're looking correspondences for.
      */
     private void updateChatRoomMembers(Contact contact)
     {
         for (ChatRoomJabberImpl chatRoom : chatRoomCache.values())
         {
             Resourcepart nick;
             try
             {
                 nick = Resourcepart.from(contact.getAddress());
             }
             catch (XmppStringprepException e)
             {
                 continue;
             }

             ChatRoomMemberJabberImpl member
                 = chatRoom.findMemberForNickName(nick);

             if (member != null)
             {
                 member.setContact(contact);
                 member.setAvatar(contact.getImage());
             }
         }
     }
}
