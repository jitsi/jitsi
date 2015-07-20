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
package net.java.sip.communicator.impl.protocol.mock;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Multiuser chat functionalities for the mock protocol.
 * @author Damian Minkov
 */
public class MockMultiUserChat
    extends AbstractOperationSetMultiUserChat
{
    /**
     * The protocol provider that created us.
     */
    private MockProvider provider = null;

    private final List<ChatRoom> existingChatRooms = new Vector<ChatRoom>();
    private final List<ChatRoom> joinedChatRooms = new Vector<ChatRoom>();

    /**
     * Creates an instance of this operation set keeping a reference to the
     * parent protocol provider and presence operation set.
     *
     * @param provider The provider instance that creates us.
     */
    public MockMultiUserChat(MockProvider provider)
    {
        this.provider = provider;
    }

    /**
     * Returns the <tt>List</tt> of <tt>String</tt>s indicating chat rooms
     * currently available on the server that this protocol provider is
     * connected to.
     *
     * @return a <tt>java.util.List</tt> of the name <tt>String</tt>s for chat
     * rooms that are currently available on the server that this protocol
     * provider is connected to.
     *
     * @throws OperationFailedException if we failed retrieving this list from
     * the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi user chat
     */
    public List<String> getExistingChatRooms()
        throws OperationFailedException,
               OperationNotSupportedException
    {
        List<String> existingChatRoomNames
            = new ArrayList<String>(existingChatRooms.size());

        for (ChatRoom existingChatRoom : existingChatRooms)
            existingChatRoomNames.add(existingChatRoom.getName());
        return existingChatRoomNames;
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
        return joinedChatRooms;
    }

    /**
     * Returns a list of the chat rooms that <tt>chatRoomMember</tt> has joined
     * and is currently active in.
     *
     * @param chatRoomMember the chatRoomMember whose current ChatRooms we will
     * be querying.
     * @return a list of the chat rooms that <tt>chatRoomMember</tt> has
     * joined and is currently active in.
     *
     * @throws OperationFailedException if an error occurs while trying to
     * discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi user chat
     */
    public List<String> getCurrentlyJoinedChatRooms(ChatRoomMember chatRoomMember)
        throws OperationFailedException,
               OperationNotSupportedException
    {
        List<String> result = new Vector<String>();

        for (ChatRoom elem : joinedChatRooms)
            if (elem.getMembers().contains(chatRoomMember))
                result.add(elem.getName());
        return result;
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
     * reason (e.g. room already exists; user already joined to an existing
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
        MockChatRoom room = new MockChatRoom(provider, this, roomName);
        existingChatRooms.add(room);
        return room;
    }

    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt> or null if no
     * such room exists.
     * <p>
     * @param roomName the name of the <tt>ChatRoom</tt> that we're looking for.
     * @return the <tt>ChatRoom</tt> named <tt>roomName</tt> or null if no such
     * room exists on the server that this provider is currently connected to.
     *
     * @throws OperationFailedException if an error occurs while trying to
     * discover the room on the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi user chat
     */
    public ChatRoom findRoom(String roomName)
        throws OperationFailedException,
               OperationNotSupportedException
    {
        for (ChatRoom elem : existingChatRooms)
            if(elem.getName().equals(roomName))
                return elem;
        return null;
    }

    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param invitation the invitation we are rejecting.
     * @param reason the reason for rejecting.
     */
    public void rejectInvitation(ChatRoomInvitation invitation, String reason)
    {
        fireInvitationRejectedEvent(
            invitation.getTargetChatRoom(),
            provider.getAccountID().getUserID(),
            invitation.getReason());
    }

    /**
     * Returns true if <tt>contact</tt> supports multi user chat sessions.
     *
     * @param contact reference to the contact whose support for chat rooms
     * we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports chatrooms.
     */
    public boolean isMultiChatSupportedByContact(Contact contact)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Always returns <tt>false</tt>.
     */
    @Override
    public boolean isPrivateMessagingContact(String contactAddress)
    {
        return false;
    }
}
