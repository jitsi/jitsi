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

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.jabber.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;

import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.muc.*;

/**
 * A Jabber implementation of the chat room member.
 *
 * @author Emil Ivov
 */
public class ChatRoomMemberJabberImpl
    implements JabberChatRoomMember
{
    /**
     * The chat room that we are a member of.
     */
    private final ChatRoomJabberImpl containingRoom;

    /**
     * The role that this member has in its member room.
     */
    private ChatRoomMemberRole  role;

    /**
     * The jabber id of the member (will only be visible to members with
     * necessary permissions)
     */
    private final String jabberID;

    /**
     * The nick name that this member is using inside its containing chat room.
     */
    private String nickName;

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
     * Creates a jabber chat room member with the specified containing chat
     * room parent.
     * @param containingChatRoom the room that this
     * <tt>ChatRoomMemberJabberImpl</tt> is a member of.
     * @param nickName the nick name that the member is using to participate
     * in the chat room
     * @param jabberID the jabber id, if available, of the member or null
     * otherwise.
     */
    public ChatRoomMemberJabberImpl(ChatRoomJabberImpl containingChatRoom,
                                    String             nickName,
                                    String             jabberID)
    {
        this.jabberID = jabberID;
        this.nickName = nickName;
        this.containingRoom = containingChatRoom;

        OperationSetPersistentPresenceJabberImpl presenceOpSet
            = (OperationSetPersistentPresenceJabberImpl) containingChatRoom
                .getParentProvider().getOperationSet(
                    OperationSetPersistentPresence.class);

        this.contact = presenceOpSet.findContactByID(
            StringUtils.parseBareAddress(jabberID));

        // If we have found a contact we set also its avatar.
        if (contact != null)
        {
            this.avatar = contact.getImage();
        }

        // just query the stack for role, if its present will be set
        getRole();
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
     * Returns the jabber id of the member.
     * @return the jabber id.
     */
    public String getJabberID()
    {
        return jabberID;
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
        return nickName;
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
     * Update the name of this parcipant
     * @param newNick the newNick of the participant
     */
    protected void setName(String newNick)
    {
        if ((newNick == null) || !(newNick.length() > 0))
            throw new IllegalArgumentException(
                "a room member nickname could not be null");
        nickName = newNick;
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
        if(role == null)
        {
            Occupant o =
                containingRoom.getMultiUserChat().getOccupant(
                    containingRoom.getIdentifier() + "/" + nickName);

            if(o == null)
            {
                return ChatRoomMemberRole.GUEST;
            }
            else
                role = ChatRoomJabberImpl.smackRoleToScRole(
                    o.getRole(), o.getAffiliation());
        }

        return role;
    }

    /**
     * Returns the current role without trying to query it in the stack.
     * Mostly used for event creating on member role change.
     *
     * @return the current role of this member.
     */
    ChatRoomMemberRole getCurrentRole()
    {
        return this.role;
    }

    /**
     * Sets the role of this member.
     * @param role the role to set
     */
    public void setRole(ChatRoomMemberRole role)
    {
        this.role = role;
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

     /**
      * Current presence status of chat room member.
      *
      * @return returns current presence status
      */
     @Override
     public PresenceStatus getPresenceStatus()
     {
         // TODO implement current presence status for chat room member
         return ((ProtocolProviderServiceJabberImpl) getProtocolProvider())
             .getJabberStatusEnum().getStatus(JabberStatusEnum.AVAILABLE);
     }
}
