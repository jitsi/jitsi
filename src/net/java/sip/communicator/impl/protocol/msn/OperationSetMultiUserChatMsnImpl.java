/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;
import net.sf.jml.event.*;
import net.sf.jml.message.*;

/**
 * A MSN implementation of the multi user chat operation set.
 * 
 * @author Rupert Burchardi
 */
public class OperationSetMultiUserChatMsnImpl
    extends AbstractOperationSetMultiUserChat
    implements SubscriptionListener
{
    private static final Logger logger =
        Logger.getLogger(OperationSetMultiUserChatMsnImpl.class);

    /**
     * A list of the rooms that are currently open by this account. Note that
     * this list only contains chat rooms where the user is not the initiator.
     */
    private Hashtable chatRoomCache = new Hashtable();

    /**
     * A list of the rooms that are currently open and created by this account.
     */
    private Hashtable userCreatedChatRoomList = new Hashtable();

    /**
     * The currently valid MSN protocol provider service implementation.
     */
    private ProtocolProviderServiceMsnImpl msnProvider = null;

    /**
     * Instantiates the user operation set with a currently valid instance of
     * the MSN protocol provider.
     * 
     * @param msnProvider a currently valid instance of
     *            ProtocolProviderServiceMsnImpl.
     */
    OperationSetMultiUserChatMsnImpl(ProtocolProviderServiceMsnImpl msnProvider)
    {
        this.msnProvider = msnProvider;

        msnProvider
            .addRegistrationStateChangeListener(new RegistrationStateListener());

        OperationSetPersistentPresence presenceOpSet
            = (OperationSetPersistentPresence) msnProvider
                .getOperationSet(OperationSetPersistentPresence.class);

        presenceOpSet.addSubscriptionListener(this);
    }

    /**
     * Returns true if <tt>contact</tt> supports multi user chat sessions.
     * 
     * @param contact reference to the contact whose support for chat rooms we
     *            are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports chatrooms.
     */
    public boolean isMultiChatSupportedByContact(Contact contact)
    {
        // if (contact.getProtocolProvider().getOperationSet(
        // OperationSetMultiUserChat.class) != null)
        // return true;
        //     
        // return false;

        return true;
    }

    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to.
     * 
     * @param roomName the name of the <tt>ChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     *            created.
     * 
     * @throws OperationFailedException if the room couldn't be created for some
     *             reason (e.g. room already exists; user already joined to an
     *             existent room or user has no permissions to create a chat
     *             room).
     * @throws OperationNotSupportedException if chat room creation is not
     *             supported by this server
     * 
     * @return ChatRoom the chat room that we've just created.
     */
    public ChatRoom createChatRoom( String roomName,
                                    Map<String, Object> roomProperties)
        throws  OperationFailedException,
                OperationNotSupportedException
    {
        return findRoom(roomName);
    }

    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt> or creates a
     * new chat room and puts it into the userCreatedChatRoomList. Note: Only
     * called by user.
     * 
     * @param roomName the name of the <tt>ChatRoom</tt> that we're looking for.
     * @return the <tt>ChatRoom</tt> named <tt>roomName</tt> or null if no such
     *         room exists on the server that this provider is currently
     *         connected to.
     * @throws OperationFailedException if an error occurs while trying to
     *             discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     *             multi user chat
     */

    public ChatRoom findRoom(String roomName)
        throws OperationFailedException,
        OperationNotSupportedException
    {
        assertConnected();

        ChatRoom room = (ChatRoom) chatRoomCache.get(roomName);

        if (room == null)
        { // when the room hasn't been created, we create it.
            room = createLocalChatRoomInstance(roomName);

            // we create an identifier object and create a new switchboard
            // we need to track this object to identify this chatRoom
            Object id = new Object();
            msnProvider.getMessenger().newSwitchboard(id);
            // we put it into a hash table
            userCreatedChatRoomList.put(id, room);
        }

        return room;
    }

    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt>. If the chat
     * room doesn't exist, a new chat room is created for the given
     * MsnSwitchboard.
     * 
     * @param switchboard The specific switchboard for the chat room.
     * 
     * @return the corresponding chat room
     * 
     * @throws OperationFailedException if an error occurs while trying to
     *             discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     *             multi user chat
     */

    public ChatRoom findRoom(MsnSwitchboard switchboard)
        throws OperationFailedException,
        OperationNotSupportedException
    {
        assertConnected();

        ChatRoomMsnImpl room =
            (ChatRoomMsnImpl) chatRoomCache.get(String.valueOf(switchboard
                .hashCode()));

        if (room == null)
        {
            String name = String.valueOf(switchboard.hashCode());
            room = createChatRoom(name, switchboard);
            room.setSwitchboard(switchboard);
            room.updateMemberList(switchboard);

            chatRoomCache.put(name, room);

            // fireInvitationEvent(room,
            // switchboard.getMessenger().getOwner().getDisplayName(),
            // "You have been invited to a group chat", null);
            room.join();
        }

        return room;
    }

    /**
     * Creates a <tt>ChatRoom</tt> from the specified chatRoomName.
     * 
     * @param chatRoomName the specific chat room name.
     * 
     * @return ChatRoom the chat room that we've just created.
     */
    private ChatRoom createLocalChatRoomInstance(String chatRoomName)
    {
        synchronized (chatRoomCache)
        {
            ChatRoomMsnImpl chatRoom =
                new ChatRoomMsnImpl(chatRoomName, msnProvider);

            this.chatRoomCache.put(chatRoom.getName(), chatRoom);
            return chatRoom;
        }
    }

    /**
     * Creates a <tt>ChatRoom</tt> from the specified chatRoomName and the
     * corresponding switchboard.
     * 
     * @param chatRoomName the specific chat room name.
     * @param switchboard The corresponding switchboard.
     * 
     * @return ChatRoom the chat room that we've just created.
     */
    private ChatRoomMsnImpl createChatRoom(String chatRoomName,
        MsnSwitchboard switchboard)
    {
        synchronized (chatRoomCache)
        {
            ChatRoomMsnImpl chatRoom =
                new ChatRoomMsnImpl(chatRoomName, msnProvider, switchboard);

            this.chatRoomCache.put(chatRoom.getName(), chatRoom);
            return chatRoom;
        }

    }

    /**
     * Makes sure that we are properly connected.
     * 
     * @throws OperationFailedException if the provider is not connected.
     * @throws OperationNotSupportedException if the service is not supported by
     *             the server.
     */
    private void assertConnected()
        throws OperationFailedException,
        OperationNotSupportedException
    {
        if (msnProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                    + "service before being able to communicate.");
        if (!msnProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                    + "being able to communicate.");
    }

    /**
     * Returns a list of the chat rooms that we have joined and are currently
     * active in.
     * 
     * @return a <tt>List</tt> of the rooms where the user has joined using a
     *         given connection.
     */
    public List getCurrentlyJoinedChatRooms()
    {
        synchronized (chatRoomCache)
        {
            List joinedRooms = new LinkedList(this.chatRoomCache.values());

            Iterator joinedRoomsIter = joinedRooms.iterator();

            while (joinedRoomsIter.hasNext())
            {
                if (!((ChatRoom) joinedRoomsIter.next()).isJoined())
                    joinedRoomsIter.remove();
            }

            return joinedRooms;
        }
    }

    /**
     * Returns a list of the names of all chat rooms that <tt>contact</tt> is
     * currently a member of.
     * 
     * @param contact the contact whose current ChatRooms we will be querying.
     * @return a list of <tt>String</tt> indicating the names of the chat rooms
     *         that <tt>contact</tt> has joined and is currently active in.
     * 
     * @throws OperationFailedException if an error occurs while trying to
     *             discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     *             multi user chat
     */
    public List getCurrentlyJoinedChatRooms(ChatRoomMember chatRoomMember)
        throws OperationFailedException,
        OperationNotSupportedException
    {
        synchronized (chatRoomCache)
        {
            List joinedRooms = new LinkedList(this.chatRoomCache.values());

            Iterator joinedRoomsIter = joinedRooms.iterator();

            while (joinedRoomsIter.hasNext())
            {
                if (!((ChatRoom) joinedRoomsIter.next()).isJoined())
                    joinedRoomsIter.remove();
            }

            return joinedRooms;
        }
    }

    /**
     * Note: This is not supported inside the MSN, so we just return an empty
     * list.
     */
    public List getExistingChatRooms()
        throws OperationFailedException,
        OperationNotSupportedException
    {
        // we dont have any available chat rooms on the server.

        return new LinkedList();
    }

    /**
     * Note: Not supported inside the MSN.
     */

    public void rejectInvitation(ChatRoomInvitation invitation,
        String rejectReason)
    {
        // there is no way to block invitations, because there arn't any
        // invitations.
        // the only way would be to block the Friend and that shouldn't be done
        // here.
        return;
    }

    /**
     * Creates a message by a given message text.
     * 
     * @param messageText The message text.
     * @return the newly created message.
     */

    public Message createMessage(String messageText)
    {
        return new MessageMsnImpl(messageText,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);
    }

    /**
     * Checks if an incoming message is a multi user chat message. This is done
     * by the switchboard, if it is not created by the user, its an active file
     * transfer switchboard or the user count is too low then this method return
     * false.
     * 
     * @param switchboard The corresponding MSNswitchboard.
     * @return True if it is a group chat message or false in the other case.
     */

    public boolean isGroupChatMessage(MsnSwitchboard switchboard)
    {
        // //fileTransfer??
        // if (switchboard.getActiveFileTransfers() != null)
        // return false;

        Object attachment = switchboard.getAttachment();
        if (attachment == null)
        { // the user did not created the chat room by him/her self,
            // the only way to figure out if this is a group chat message
            // is to check the user count
            return (switchboard.getAllContacts().length > 1);

        }

        return userCreatedChatRoomList.containsKey(attachment);
    }

    protected void fireInvitationEvent(ChatRoom targetChatRoom, String inviter,
        String reason, byte[] password)
    {
        ChatRoomInvitationMsnImpl invitation
            = new ChatRoomInvitationMsnImpl(
                    targetChatRoom,
                    inviter,
                    reason,
                    password);

        fireInvitationReceived(invitation);
    }

    /**
     * Our listener that will tell us when we're registered to msn.
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
                msnProvider.getMessenger().addSwitchboardListener(
                    new MsnSwitchboardListener());
                msnProvider.getMessenger().addMessageListener(
                    new MsnMessageListener());
            }
        }

    }

    /**
     * Our group chat message listener, it extends the MsnMessageAdapter from
     * the the jml library.
     * 
     */
    private class MsnMessageListener
        extends MsnMessageAdapter
        implements MsnEmailListener
    {
        public void instantMessageReceived( MsnSwitchboard switchboard,
                                            MsnInstantMessage message,
                                            MsnContact contact)
        {
            if (!isGroupChatMessage(switchboard))
                return;

            Message newMessage = createMessage(message.getContent());

            logger.debug("Group chat message received.");
            Object attachment = switchboard.getAttachment();
            try
            {
                ChatRoomMsnImpl chatRoom = null;

                if (attachment == null) // chat room session NOT created by
                                        // yourself
                    chatRoom = (ChatRoomMsnImpl) findRoom(switchboard);

                // user created chat room session?
                if (attachment != null
                    && userCreatedChatRoomList.containsKey(attachment))
                    chatRoom =
                        (ChatRoomMsnImpl) userCreatedChatRoomList
                            .get(attachment);

                if (chatRoom == null)
                    return;

                ChatRoomMemberMsnImpl member =
                    chatRoom.getChatRoomMember(contact.getId());

                ChatRoomMessageReceivedEvent msgReceivedEvent =
                    new ChatRoomMessageReceivedEvent(
                        chatRoom,
                        member,
                        System.currentTimeMillis(),
                        newMessage,
                        ChatRoomMessageReceivedEvent
                            .CONVERSATION_MESSAGE_RECEIVED);

                chatRoom.fireMessageEvent(msgReceivedEvent);

            }
            catch (OperationFailedException e)
            {
                logger.error("Failed to find room with name: ", e);
            }
            catch (OperationNotSupportedException e)
            {
                logger.error("Failed to find room with name: ", e);
            }

        }

        public void initialEmailNotificationReceived(
            MsnSwitchboard switchboard, MsnEmailInitMessage message,
            MsnContact contact)
        {
        }

        public void initialEmailDataReceived(MsnSwitchboard switchboard,
            MsnEmailInitEmailData message, MsnContact contact)
        {
        }

        public void newEmailNotificationReceived(MsnSwitchboard switchboard,
            MsnEmailNotifyMessage message, MsnContact contact)
        {

        }

        public void activityEmailNotificationReceived(
            MsnSwitchboard switchboard, MsnEmailActivityMessage message,
            MsnContact contact)
        {
        }
    }

    /**
     * The Switchboard Listener, listens to all four switchboard events:
     * Switchboard started/closed and User joins/left.
     * 
     */
    private class MsnSwitchboardListener
        extends MsnSwitchboardAdapter
    {
        public void contactJoinSwitchboard(MsnSwitchboard switchboard,
            MsnContact contact)
        {
            logger.debug(contact.getDisplayName()
                + " has joined the Switchboard");
            if (!isGroupChatMessage(switchboard))
                return;

            Object attachment = switchboard.getAttachment();
            try
            {
                ChatRoomMsnImpl chatRoom = null;
                if (attachment == null) // chat room session NOT created by
                                        // yourself
                    chatRoom = (ChatRoomMsnImpl) findRoom(switchboard);

                // user created chat room session?
                if (attachment != null
                    && userCreatedChatRoomList.containsKey(attachment))
                    chatRoom =
                        (ChatRoomMsnImpl) userCreatedChatRoomList
                            .get(attachment);

                if (chatRoom == null)
                    return;

                String memberId = contact.getId();

                ChatRoomMemberMsnImpl member =
                    chatRoom.getChatRoomMember(memberId);

                if (member == null)
                {
                    member =
                        new ChatRoomMemberMsnImpl(chatRoom, contact
                            .getDisplayName(), contact.getEmail().toString(),
                            ChatRoomMemberRole.MEMBER);

                    chatRoom.addChatRoomMember(memberId, member);
                }
            }
            catch (Exception e)
            {

            }

        }

        public void contactLeaveSwitchboard(MsnSwitchboard switchboard,
                                            MsnContact contact)
        {
            logger
                .debug(contact.getDisplayName() + " has left the Switchboard");

            Object attachment = switchboard.getAttachment();

            try
            {
                ChatRoomMsnImpl chatRoom = null;
                if (attachment == null)// chat room session NOT created by
                                       // yourself
                    chatRoom = (ChatRoomMsnImpl) findRoom(switchboard);

                // user created chat room session?
                if (attachment != null
                    && userCreatedChatRoomList.containsKey(attachment))
                    chatRoom =
                        (ChatRoomMsnImpl) userCreatedChatRoomList
                            .get(attachment);

                if (chatRoom == null)
                    return;

                String memberId = contact.getId();

                ChatRoomMemberMsnImpl member =
                    chatRoom.getChatRoomMember(memberId);

                if (member != null)
                {
                    chatRoom.removeChatRoomMember(memberId);
                }
            }
            catch (OperationFailedException e)
            {
                logger.debug(   "Could not find a chat room corresponding" +
                                "to the given switchboard.", e);
            }
            catch (OperationNotSupportedException e)
            {
                logger.debug(   "Could not find a chat room corresponding" +
                                "to the given switchboard.", e);
            }
        }

        public void switchboardClosed(MsnSwitchboard switchboard)
        {
            logger.debug("Switchboard closed.");

            Object attachment = switchboard.getAttachment();
            try
            {
                ChatRoomMsnImpl chatRoom = null;
                if (attachment == null)// chat room session NOT created by
                                       // yourself
                    chatRoom = (ChatRoomMsnImpl) findRoom(switchboard);
                // user created chat room session?
                if (attachment != null
                    && userCreatedChatRoomList.containsKey(attachment))
                    chatRoom =
                        (ChatRoomMsnImpl) userCreatedChatRoomList
                            .get(attachment);

                if (chatRoom == null)
                    return;

                chatRoom.setSwitchboard(null);

                // chatRoom.leave();
                // fireLocalUserPresenceEvent(chatRoom,
                // LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED ,
                // "Switchboard closed.");
            }
            catch (Exception e)
            {
            }
        }

        public void switchboardStarted(MsnSwitchboard switchboard)
        {
            logger.debug("Switchboard started.");
            Object switchboardID = switchboard.getAttachment();
            try
            {
                ChatRoomMsnImpl chatRoom = null;
                if (switchboardID != null
                    && userCreatedChatRoomList.containsKey(switchboardID))
                {
                    chatRoom =
                        (ChatRoomMsnImpl) userCreatedChatRoomList
                            .get(switchboardID);

                    chatRoom.setSwitchboard(switchboard);
                    chatRoom.updateMemberList(switchboard);
                    chatRoom.join();
                }

            }
            catch (OperationFailedException ofe)
            {
                logger.debug("Could not join the ChatRoom: " + ofe);
            }
        }
    }

    /**
     * Updates corresponding chat room members when a contact has been modified
     * in our contact list.
     */
     public void contactModified(ContactPropertyChangeEvent evt)
     {
         Contact modifiedContact = evt.getSourceContact();

         this.updateChatRoomMembers(modifiedContact);
     }

     /**
      * Updates corresponding chat room members when a contact has been created
      * in our contact list.
      */
     public void subscriptionCreated(SubscriptionEvent evt)
     {
         Contact createdContact = evt.getSourceContact();

         this.updateChatRoomMembers(createdContact);
     }

     /**
      * Not interested in this event for our member update purposes.
      */
     public void subscriptionFailed(SubscriptionEvent evt)
     {}

     /**
      * Not interested in this event for our member update purposes.
      */
     public void subscriptionMoved(SubscriptionMovedEvent evt)
     {}

     /**
      * Updates corresponding chat room members when a contact has been removed
      * from our contact list.
      */
     public void subscriptionRemoved(SubscriptionEvent evt)
     {
         // Set to null the contact reference in all corresponding chat room
         // members.
         this.updateChatRoomMembers(null);
     }

     /**
      * Not interested in this event for our member update purposes.
      */
     public void subscriptionResolved(SubscriptionEvent evt)
     {}

     /**
      * Finds all chat room members, which name corresponds to the name of the
      * given contact and updates their contact references.
      * 
      * @param contact the contact we're looking correspondences for.
      */
     private void updateChatRoomMembers(Contact contact)
     {
         Enumeration<ChatRoomMsnImpl> chatRooms = chatRoomCache.elements();

         while (chatRooms.hasMoreElements())
         {
             ChatRoomMsnImpl chatRoom = chatRooms.nextElement();

             ChatRoomMemberMsnImpl member
                 = chatRoom.findMemberForAddress(contact.getAddress());

             if (member != null)
             {
                 member.setContact(contact);
                 member.setAvatar(contact.getImage());
             }
         }
     }
}
