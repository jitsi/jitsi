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
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.muc.*;
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
 * @author Danny van Heumen
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
    private final Map<String, ChatRoomIrcImpl> chatRoomCache
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
        final ProtocolProviderServiceIrcImpl provider)
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
        final IrcConnection connection =
            this.ircProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        return connection.getServerChannelLister().getList();
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
        synchronized (chatRoomCache)
        {
            List<ChatRoom> joinedRooms
                = new LinkedList<ChatRoom>(this.chatRoomCache.values());

            Iterator<ChatRoom> joinedRoomsIter = joinedRooms.iterator();

            while (joinedRoomsIter.hasNext())
            {
                if (!joinedRoomsIter.next().isJoined())
                {
                    joinedRoomsIter.remove();
                }
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
    public List<String> getCurrentlyJoinedChatRooms(
        final ChatRoomMember chatRoomMember)
    {
        // Implement "who is" for the IRC stack.
        // (currently not in use)
        /*
         * According to the RFC:
         *
         * 311 RPL_WHOISUSER "<nick> <user> <host> * :<real name>"
         *
         * 312 RPL_WHOISSERVER "<nick> <server> :<server info>"
         *
         * 313 RPL_WHOISOPERATOR "<nick> :is an IRC operator"
         *
         * 317 RPL_WHOISIDLE "<nick> <integer> :seconds idle"
         *
         * 318 RPL_ENDOFWHOIS "<nick> :End of /WHOIS list"
         *
         * 319 RPL_WHOISCHANNELS "<nick> :{[@|+]<channel><space>}"
         *
         * - Replies 311 - 313, 317 - 319 are all replies generated in response
         * to a WHOIS message. Given that there are enough parameters present,
         * the answering server must either formulate a reply out of the above
         * numerics (if the query nick is found) or return an error reply. The
         * '*' in RPL_WHOISUSER is there as the literal character and not as a
         * wild card. For each reply set, only RPL_WHOISCHANNELS may appear more
         * than once (for long lists of channel names). The '@' and '+'
         * characters next to the channel name indicate whether a client is a
         * channel operator or has been granted permission to speak on a
         * moderated channel. The RPL_ENDOFWHOIS reply is used to mark the end
         * of processing a WHOIS message.
         */
        return Collections.emptyList();
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
            final String roomName,
            final Map<String, Object> roomProperties)
        throws OperationFailedException,
               OperationNotSupportedException
    {
        try
        {
            return findOrCreateRoom(roomName);
        }
        catch (IllegalArgumentException e)
        {
            String message =
                IrcActivator.getResources().getI18NString(
                    "service.gui.CREATE_CHAT_ROOM_ERROR", new String[]
                    {roomName});
            throw new OperationFailedException(message,
                OperationFailedException.ILLEGAL_ARGUMENT, e);
        }
    }

    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt>.
     *
     * Originally, this method would create the room if it doesn't exist. This
     * is not acceptable anymore, since rebuilding the chat room list would
     * create new instances without the IRC stack being prepared for this or
     * having corresponding instances.
     *
     * @param roomName the name of the <tt>ChatRoom</tt> that we're looking for.
     * @return the <tt>ChatRoom</tt> named <tt>roomName</tt>.
     */
    public ChatRoomIrcImpl findRoom(final String roomName)
    {
        return chatRoomCache.get(roomName);
    }

    /**
     * Find an existing room with the provided name, or create a new room with
     * this name.
     *
     * @param roomName name of the chat room
     * @return returns a chat room
     */
    public ChatRoomIrcImpl findOrCreateRoom(final String roomName)
    {
        synchronized (this.chatRoomCache)
        {
            ChatRoomIrcImpl room = chatRoomCache.get(roomName);
            if (room == null)
            {
                room = createLocalChatRoomInstance(roomName);
            }
            return room;
        }
    }

    /**
     * There is no such thing as a rejection to an invitatation. The notion of
     * an invite in IRC is just an addition to a white list. There is nothing to
     * reject.
     *
     * @param invitation the invitation we are rejecting.
     * @param reason the reason of rejecting
     */
    public void rejectInvitation(final ChatRoomInvitation invitation,
        final String reason)
    {
    }

    /**
     * Returns true if <tt>contact</tt> supports multi-user chat sessions.
     *
     * @param contact reference to the contact whose support for chat rooms
     * we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports chat
     * rooms.
     */
    public boolean isMultiChatSupportedByContact(final Contact contact)
    {
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
    protected ChatRoomIrcImpl getChatRoom(final String chatRoomName)
    {
        return (ChatRoomIrcImpl) this.chatRoomCache.get(chatRoomName);
    }

    /**
     * Creates a <tt>ChatRoom</tt> from the specified chat room name.
     *
     * Must be used in SYNCHRONIZED context.
     *
     * @param chatRoomName the name of the chat room to add
     *
     * @return ChatRoom the chat room that we've just created.
     */
    private ChatRoomIrcImpl createLocalChatRoomInstance(
        final String chatRoomName)
    {
        ChatRoomIrcImpl chatRoom =
            new ChatRoomIrcImpl(chatRoomName, ircProvider);

        this.chatRoomCache.put(chatRoom.getName(), chatRoom);

        return chatRoom;
    }

    /**
     * Register chat room instance in case it is not yet registered.
     *
     * @param chatroom the chatroom
     */
    public void registerChatRoomInstance(final ChatRoomIrcImpl chatroom)
    {
        synchronized (this.chatRoomCache)
        {
            this.chatRoomCache.put(chatroom.getIdentifier(), chatroom);
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
    protected void fireInvitationEvent(final ChatRoom targetChatRoom,
                                    final String inviter,
                                    final String reason,
                                    final byte[] password)
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
        if (serverChatRoom == null)
        {
            serverChatRoom = new ChatRoomIrcImpl(
                ircProvider.getAccountID().getService(),
                ircProvider,
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
        {
            return (ChatRoomMemberIrcImpl) serverChatRoom.getMembers().get(0);
        }
        else
        {
            return new ChatRoomMemberIrcImpl(ircProvider, serverChatRoom,
                ircProvider.getAccountID().getService(), "", ircProvider
                    .getAccountID().getServerAddress(),
                ChatRoomMemberRole.GUEST, IrcStatusEnum.ONLINE);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Always returns <tt>true</tt>.
     */
    @Override
    public boolean isPrivateMessagingContact(final String contactAddress)
    {
        return true;
    }

    /**
     * Open a chat room window.
     *
     * In IRC a situation may occur where the user gets joined to a channel
     * without Jitsi initiating the joining activity. This "unannounced" join
     * event, must also be handled and we should display the chat room window in
     * that case, to alert the user that this happened.
     *
     * @param chatRoom the chat room
     */
    void openChatRoomWindow(final ChatRoomIrcImpl chatRoom)
    {
        MUCService mucService = IrcActivator.getMUCService();
        UIService uiService = IrcActivator.getUIService();
        ChatRoomWrapper wrapper =
            mucService.getChatRoomWrapperByChatRoom(chatRoom, true);
        uiService.openChatRoomWindow(wrapper);
    }
}
