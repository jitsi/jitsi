/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents a chat room member.
 *
 * @author Stephane Remy
 * @author Lubomir Marinov
 */
public class ChatRoomMemberIrcImpl
    implements ChatRoomMember
{
    /**
     * The ChatRoom.
     */
    private final ChatRoom chatRoom;

    /**
     * The id of the contact.
     */
    private final String contactID;

    /**
     * The provider that created us.
     */
    private final ProtocolProviderServiceIrcImpl parentProvider;

    /**
     * The role of this member.
     */
    private ChatRoomMemberRole chatRoomMemberRole;

    /**
     * Creates an instance of <tt>ChatRoomMemberIrcImpl</tt>, by specifying the
     * protocol provider, the corresponding chat room, where this member is
     * joined, the identifier of the contact (the nickname), the login, the
     * host name and finally the role that this contact has in the chat room. 
     * 
     * @param parentProvider the protocol provider, to which the corresponding
     * chat room belongs 
     * @param chatRoom the chat room, where this member is joined
     * @param contactID the nickname of the member
     * @param chatRoomMemberRole the role that this member has in the
     * corresponding chat room
     */
    public ChatRoomMemberIrcImpl(ProtocolProviderServiceIrcImpl parentProvider,
                                 ChatRoom chatRoom,
                                 String contactID,
                                 ChatRoomMemberRole chatRoomMemberRole)
    {
        this.parentProvider = parentProvider;
        this.chatRoom = chatRoom;
        this.contactID = contactID;
        this.chatRoomMemberRole = chatRoomMemberRole;
    }

    /**
     * Returns the chat room that this member is participating in.
     *
     * @return the <tt>ChatRoom</tt> instance that this member belongs to.
     */
    public ChatRoom getChatRoom()
    {
        return this.chatRoom;
    }

    /**
     * Returns the protocol provider instance that this member has originated
     * in.
     *
     * @return the <tt>ProtocolProviderService</tt> instance that created this
     * member and its containing chat room
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return this.parentProvider;
    }

    /**
     * Returns the contact identifier representing this contact. For IRC 
     * this method returns the same as getName().
     *
     * @return a String (contact address), uniquely representing the contact
     * over the service being used by the associated protocol provider instance/
     */
    public String getContactAddress()
    {
        return this.contactID;
    }

    /**
     * Returns the name of this member as it is known in its containing
     * chat room (i.e. a nickname). The name returned by this method, may
     * sometimes match the string returned by getContactID() which is actually
     * the address of  a contact in the realm of the corresponding protocol.
     *
     * @return the name of this member as it is known in the containing chat
     * room (i.e. a nickname).
     */
    public String getName()
    {
        return this.contactID;
    }

    /**
     * Returns the role of this chat room member in its containing room.
     *
     * @return a <tt>ChatRoomMemberRole</tt> instance indicating the role
     * the this member in its containing chat room.
     */
    public ChatRoomMemberRole getRole()
    {
        return this.chatRoomMemberRole;
    }

    /**
     * Sets a new member role to this <tt>ChatRoomMember</tt>.
     * 
     * @param chatRoomMemberRole the role to be set
     */
    public void setRole(ChatRoomMemberRole chatRoomMemberRole)
    {
        this.chatRoomMemberRole = chatRoomMemberRole;
    }

    /**
     * Returns null to indicate that there's no avatar attached to the IRC
     * member.
     * 
     * @return null
     */
     public byte[] getAvatar()
     {
         return null;
     }

     /**
      * Returns null to indicate that there's no contact corresponding to the
      * IRC member.
      * 
      * @return null
      */
     public Contact getContact()
     {
         return null;
     }
}
