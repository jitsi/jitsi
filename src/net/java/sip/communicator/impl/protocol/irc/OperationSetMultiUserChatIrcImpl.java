/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Allows creating, configuring, joining and administering of individual
 * text-based conference rooms.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class OperationSetMultiUserChatIrcImpl
    extends AbstractOperationSetMultiUserChat
{
    /**
     * A call back to the IRC provider that created us.
     */
    private ProtocolProviderServiceIrcImpl ircProvider = null;

    /**
     * A list of the rooms that are currently open by this account. Note that
     * we have not necessarily joined these rooms, we might have simply been
     * searching through them.
     */
    private final Map<String, ChatRoom> chatRoomCache
        = new Hashtable<String, ChatRoom>();

    /**
     * A list of all private rooms opened by user on this server. These rooms
     * are a result of exchange of private messages between the local user and
     * some of the other chat room members.
     */
    private final Map<String, ChatRoomIrcImpl> privateRoomCache
        = new Hashtable<String, ChatRoomIrcImpl>();

    /**
     * The <tt>ChatRoom</tt> corresponding to the IRC server channel. This chat
     * room is not returned by any of methods getExistingChatRooms(),
     * getCurrentlyJoinedChatRooms, etc.
     */
    private ChatRoomIrcImpl serverChatRoom;

    /**
     * Instantiates the user operation set with a currently valid instance of
     * the irc protocol provider.
     * @param provider a currently valid instance of
     * ProtocolProviderServiceIrcImpl.
     */
    public OperationSetMultiUserChatIrcImpl(
        ProtocolProviderServiceIrcImpl provider)
    {
        this.ircProvider = provider;
    }

    /**
     * Returns the <tt>List</tt> of <tt>ChatRoom</tt>s currently available on
     * the server that this protocol provider is connected to.
     *
     * @return a <tt>java.util.List</tt> of <tt>ChatRoom</tt>s that are
     * currently available on the server that this protocol provider is
     * connected to.
     *
     * @throws OperationFailedException if we failed retrieving this list from
     * the server.
     */
    public List<String> getExistingChatRooms() throws OperationFailedException
    {
        return ircProvider.getIrcStack().getServerChatRoomList();
    }

    /**
     * Returns a list of the chat rooms that we have joined and are currently
     * active in.
     *
     * @return a <tt>List</tt> of the rooms where the user has joined using a
     * given connection.
     */
    public List<ChatRoom> getCurrentlyJoinedChatRooms()
    {
        synchronized(chatRoomCache)
        {
            List<ChatRoom> joinedRooms
                = new LinkedList<ChatRoom>(this.chatRoomCache.values());

            Iterator<ChatRoom> joinedRoomsIter = joinedRooms.iterator();

            while (joinedRoomsIter.hasNext())
            {
                if (!joinedRoomsIter.next().isJoined())
                    joinedRoomsIter.remove();
            }

            return joinedRooms;
        }
    }

    /**
     * Returns a list of the chat rooms that <tt>chatRoomMember</tt> has joined
     * and is currently active in.
     *
     * @param chatRoomMember the chat room member whose current ChatRooms we
     * will be querying.
     * @return a list of the chat rooms that <tt>chatRoomMember</tt> has joined
     * and is currently active in.
     */
    public List<String> getCurrentlyJoinedChatRooms(ChatRoomMember chatRoomMember)
    {
        //TODO: Implement "who is" for the IRC stack.
        return null;
    }

    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to. When the method returns the room the
     * local user will not have joined it and thus will not receive messages on
     * it until the <tt>ChatRoom.join()</tt> method is called.
     * <p>
     * @param roomName the name of the <tt>ChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     * created.
     * @throws OperationFailedException if the room couldn't be created for some
     * reason (e.g. room already exists; user already joined to an existent
     * room or user has no permissions to create a chat room).
     * @throws OperationNotSupportedException if chat room creation is not
     * supported by this server
     *
     * @return the newly created <tt>ChatRoom</tt> named <tt>roomName</tt>.
     */
    public ChatRoom createChatRoom(
            String roomName,
            Map<String, Object> roomProperties)
        throws OperationFailedException,
               OperationNotSupportedException
    {
        return findRoom(roomName);
    }

    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt>. The room is
     * created if it doesn't exists
     * <p>
     * @param roomName the name of the <tt>ChatRoom</tt> that we're looking for.
     * @return the <tt>ChatRoom</tt> named <tt>roomName</tt>.
     */
    public ChatRoom findRoom(String roomName)
    {
        //first check whether we have already initialized the room.
        ChatRoom room = chatRoomCache.get(roomName);

        //if yes - we return it. if not, we create it.
        return (room != null) ? room : createLocalChatRoomInstance(roomName);
    }

    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param invitation the invitation we are rejecting.
     * @param reason the reason of rejecting
     */
    public void rejectInvitation(ChatRoomInvitation invitation, String reason)
    {
        //TODO: Implement reject invitation.
    }

    /**
     * Returns true if <tt>contact</tt> supports multi-user chat sessions.
     *
     * @param contact reference to the contact whose support for chat rooms
     * we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports chat
     * rooms.
     */
    public boolean isMultiChatSupportedByContact(Contact contact)
    {
        //TODO: Implement isMultiChatSupportedByContact.
        return true;
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
    protected ChatRoomIrcImpl getChatRoom(String chatRoomName)
    {
        return (ChatRoomIrcImpl)this.chatRoomCache.get(chatRoomName);
    }

    /**
     * Creates a <tt>ChatRoom</tt> from the specified chat room name.
     *
     * @param chatRoomName the name of the chat room to add
     *
     * @return ChatRoom the chat room that we've just created.
     */
    private ChatRoom createLocalChatRoomInstance(String chatRoomName)
    {
        synchronized(chatRoomCache)
        {
            ChatRoomIrcImpl chatRoom
                = new ChatRoomIrcImpl(chatRoomName, ircProvider);

            this.chatRoomCache.put(chatRoom.getName(), chatRoom);

            return chatRoom;
        }
    }

    /**
     * Returns the private room corresponding to the given nick name.
     * 
     * @param nickIdentifier the nickName of the person for which the private
     * room is.
     * @return the private room corresponding to the given nick name
     */
    protected ChatRoomIrcImpl findPrivateChatRoom(String nickIdentifier)
    {
        ChatRoomIrcImpl chatRoom;

        synchronized(privateRoomCache)
        {
            if(privateRoomCache.containsKey(nickIdentifier))
                return privateRoomCache.get(nickIdentifier);

            chatRoom =
                new ChatRoomIrcImpl(
                        nickIdentifier,
                        ircProvider,
                        true,
                        false);
            privateRoomCache.put(nickIdentifier, chatRoom);
        }

        /*
         * As a rule of thumb, firing inside synchronized blocks increases the
         * chances of creating deadlocks.
         */
        fireLocalUserPresenceEvent(
            chatRoom,
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
            "Private conversation initiated.");

        return chatRoom;
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
    protected void fireInvitationEvent(ChatRoom targetChatRoom,
                                    String inviter,
                                    String reason,
                                    byte[] password)
    {
        ChatRoomInvitationIrcImpl invitation
            = new ChatRoomInvitationIrcImpl(targetChatRoom,
                                            inviter,
                                            reason,
                                            password);

        fireInvitationReceived(invitation);
    }

    /**
     * Returns the room corresponding to the server channel.
     * 
     * @return the room corresponding to the server channel
     */
    protected ChatRoomIrcImpl findSystemRoom()
    {
        if(serverChatRoom == null)
        {
            serverChatRoom = new ChatRoomIrcImpl(
                ircProvider.getAccountID().getService(),
                ircProvider,
                false, // is private room
                true); // is system room

            this.fireLocalUserPresenceEvent(
                serverChatRoom,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                "Connected to the server.");
        }

        return serverChatRoom;
    }

    /**
     * Returns the system room member.
     * 
     * @return the system room member.
     */
    protected ChatRoomMemberIrcImpl findSystemMember()
    {
        if (serverChatRoom.getMembers().size() > 0)
            return (ChatRoomMemberIrcImpl) serverChatRoom.getMembers().get(0);
        else
            return new ChatRoomMemberIrcImpl(
                ircProvider,
                serverChatRoom,
                ircProvider.getAccountID().getService(),
                "", // We don't specify a login.
                "", // We don't specify a hostname.
                ChatRoomMemberRole.GUEST);
    }
}
