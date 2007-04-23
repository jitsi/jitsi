/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;
import org.jivesoftware.smackx.muc.*;

/**
 * A Jabber implementation of the chat room member.
 *
 * @author Emil Ivov
 */
public class ChatRoomMemberJabberImpl
    implements ChatRoomMember
{
    /**
     * The chat room that we are a member of.
     */
    private ChatRoomJabberImpl containingRoom = null;
    /**
     * The role that this member has in its member room.
     */
    private ChatRoomMemberRole  role = null;

    /**
     * The jabber id of the member (will only be visible to members with
     * necessary permissions)
     */
    private String jabberID = null;

    /**
     * The nick name that this member is using inside its containing chat room.
     */
    private String nickName = null;

    /**
     * Creates a jabber chat room member with the specified containing chat
     * room parent.
     * @param containingChatRoom the room that this
     * <tt>ChatRoomMemberJabberImpl</tt> is a member of.
     * @param nickName the nick name that the member is using to participate
     * in the chat room
     * @param jabberID the jabber id, if available, of the member or null
     * otherwise.
     * @param role the role that the member has in this room.
     */
    public ChatRoomMemberJabberImpl(ChatRoomJabberImpl containingChatRoom,
                                    String             nickName,
                                    String             jabberID,
                                    ChatRoomMemberRole role)
    {
        this.jabberID = jabberID;
        this.nickName = nickName;
        this.containingRoom = containingChatRoom;

        this.role = role;
    }
    /**
     * Returns the chat room that this member is participating in.
     *
     * @return the <tt>ChatRoom</tt> instance that this member belongs to.
     */
    public ChatRoom getChatRoom()
    {
        return containingRoom;
    }

    /**
     * Returns the contact identifier representing this contact.
     *
     * @return a String (contact address), uniquely representing the contact
     *   over the service the service being used by the associated protocol
     *   provider instance/
     */
    public String getContactAddress()
    {
        return jabberID;
    }

    /**
     * Returns the name of this member as it is known in its containing
     * chatroom (aka a nickname).
     *
     * @return the name of this member as it is known in the containing chat
     *   room (aka a nickname).
     */
    public String getName()
    {
        return nickName;
    }

    /**
     * Returns the protocol provider instance that this member has originated
     * in.
     *
     * @return the <tt>ProtocolProviderService</tt> instance that created
     *   this member and its containing cht room
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return containingRoom.getParentProvider();
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
}
