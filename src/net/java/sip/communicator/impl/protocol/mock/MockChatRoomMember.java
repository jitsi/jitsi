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

import net.java.sip.communicator.service.protocol.*;

/**
 *
 * @author Damian Minkov
 */
public class MockChatRoomMember
    implements ChatRoomMember
{
    private ChatRoom chatRoom;
    private String name;
    private ChatRoomMemberRole role;
    private Contact contact;
    private byte[] avatar;

    /**
     * Creates an instance of <tt>MockChatRoomMember</tt> by specifying the
     * <tt>name</tt> of the member, the <tt>chatRoom</tt>, to which it belongs,
     * its <tt>role</tt> in the room, the <tt>contact</tt> corresponding to it
     * and its <tt>avatar</tt>.
     * @param name the name of the member
     * @param chatRoom the chat room to which the member belongs
     * @param role the role of the member in the room
     * @param contact the contact corresponding to this member in the local
     * contact list
     * @param avatar the avatar of the member
     */
    public MockChatRoomMember(String name, ChatRoom chatRoom,
        ChatRoomMemberRole role, Contact contact, byte[] avatar)
    {
        this.chatRoom = chatRoom;
        this.name =  name;
        this.role = role;
        this.contact = contact;
        this.avatar = avatar;
    }

    /**
     * Returns the chat room that this member is participating in.
     *
     * @return the <tt>ChatRoom</tt> instance that this member belongs to.
     */
    public ChatRoom getChatRoom()
    {
        return chatRoom;
    }

    /**
     * Returns the protocol provider instance that this member has originated
     * in.
     *
     * @return the <tt>ProtocolProviderService</tt> instance that created this
     * member and its containing cht room
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return chatRoom.getParentProvider();
    }

    /**
     * Returns the contact identifier representing this contact. In protocols
     * like IRC this method would return the same as getName() but in others
     * like Jabber, this method would return a full contact id uri.
     *
     * @return a String (contact address), uniquely representing the contact
     * over the service the service being used by the associated protocol
     * provider instance/
     */
    public String getContactAddress()
    {
        return name;
    }

    /**
     * Returns the name of this member as it is known in its containing
     * chatroom (aka a nickname). The name returned by this method, may
     * sometimes match the string returned by getContactID() which is actually
     * the address of  a contact in the realm of the corresponding protocol.
     *
     * @return the name of this member as it is known in the containing chat
     * room (aka a nickname).
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the role of this chat room member in its containing room.
     *
     * @return a <tt>ChatRoomMemberRole</tt> instance indicating the role
     * the this member in its containing chat room.
     */
    public ChatRoomMemberRole getRole()
    {
        return role;
    }

    /**
     * Sets the role of this member.
     * @param role the role to set
     */
    public void setRole(ChatRoomMemberRole role)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the avatar of this member, that can be used when including it in
     * user interface.
     *
     * @return an avatar (e.g. user photo) of this member.
     */
    public byte[] getAvatar()
    {
        return avatar;
    }

    /**
     * Returns the protocol contact corresponding to this member in our contact
     * list.
     * @return the protocol contact corresponding to this member in our contact
     * list.
     */
    public Contact getContact()
    {
        return contact;
    }

    /**
     * Returns status for fully available mock chat room member.
     */
    @Override
    public PresenceStatus getPresenceStatus()
    {
        // mock fully available
        return MockStatusEnum.MOCK_STATUS_100;
    }
}
