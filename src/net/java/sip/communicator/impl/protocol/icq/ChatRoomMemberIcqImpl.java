/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents a chat room member.
 * 
 * @author Rupert Burchardi
 */

public class ChatRoomMemberIcqImpl implements ChatRoomMember
{
   /**
    * The ChatRoom, where the member is joined.
    */
   private ChatRoomIcqImpl containingRoom = null;
   /**
    * The email address of the user.
    */
   private String userAddress = null;

   /**
    * The role of this member.
    */
   private ChatRoomMemberRole memberRole = null;
   /**
    * The nickname of the user.
    */
   private String nickName = null;

   /**
    * The contact from our server stored contact list corresponding to this
    * member.
    */
   private Contact contact;

   /**
    * The avatar of this chat room member.
    */
   private byte[] avatar;

   /**
    * Creates an instance of <tt>ChatRoomMemberIcqImpl</tt>, by specifying
    * the corresponding chat room, where this member is joined, the nickname
    * (display name) and the user address (email address) and finally the role
    * that this contact has in the chat room.
    * 
    * @param chatRoom The chat room, where this member is joined
    * @param nickName The nick of the member (display name)
    * @param userAddress The identifier of the member (email address)
    * @param role The role of the member inside the chat room
    */

   public ChatRoomMemberIcqImpl(ChatRoomIcqImpl chatRoom,
                               String nickName,
                               String userAddress,
                               ChatRoomMemberRole role)
   {
       this.containingRoom = chatRoom;
       this.nickName = nickName;
       this.userAddress = userAddress;
       this.memberRole = role;

       OperationSetPersistentPresenceIcqImpl presenceOpSet
           = (OperationSetPersistentPresenceIcqImpl)chatRoom.getParentProvider()
               .getOperationSet(OperationSetPersistentPresence.class);

       this.contact = presenceOpSet.getServerStoredContactList()
           .findContactByScreenName(userAddress);

       // If we have found a contact we set also its avatar.
       if (contact != null)
           this.avatar = contact.getImage();
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
    *         over the service being used by the associated protocol provider
    *         instance
    */
   public String getContactAddress()
   {
       return userAddress;
   }

   /**
    * Returns the name of the member as he/she is known in its containing chat
    * room (display name).
    * 
    * @return The users display name.
    */
   public String getName()
   {
       return nickName;
   }

   /**
    * Returns the protocol provider instance that this member has originated
    * in.
    * 
    * @return the <tt>ProtocolProviderService</tt> instance that created this
    *         member and its containing chat room
    */
   public ProtocolProviderService getProtocolProvider()
   {
       return containingRoom.getParentProvider();
   }

   /**
    * Returns the role of this chat room member in its containing room.
    * 
    * @return a <tt>ChatRoomMemberRole</tt> instance indicating the role the
    *         this member in its containing chat room.
    */
   public ChatRoomMemberRole getRole()
   {
       return memberRole;
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
     * Sets the avatar for this member.
     * 
     * @param avatar the avatar to set.
     */
    public void setAvatar(byte[] avatar)
    {
        this.avatar = avatar;
    }

   /**
    * Returns the protocol contact corresponding to this member in our contact
    * list. The contact returned here could be used by the user interface to
    * check if this member is contained in our contact list and in function of
    * this to show additional information add additional functionality.
    * 
    * @return the protocol contact corresponding to this member in our contact
    * list.
    */
    public Contact getContact()
    {
        return contact;
    }

   /**
    * Sets the given contact to this member.
    * 
    * @param contact the contact to set.
    */
    public void setContact(Contact contact)
    {
        this.contact = contact;
    }
}
