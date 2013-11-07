/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.muc;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The MUC service provides interface for the chat rooms. It connects the GUI 
 * with the protcol.
 * 
 * @author Hristo Terezov
 */
public abstract class MUCService
{
    /**
     * Creates a <tt>ChatRoomWrapper</tt> by specifying the corresponding chat
     * room.
     *
     * @param parentProvider the protocol provider to which the corresponding
     * chat room belongs
     * @param chatRoom the chat room to which this wrapper corresponds.
     */
    public abstract ChatRoomWrapper createChatRoomWrapper( 
        ChatRoomProviderWrapper parentProvider,
        ChatRoom chatRoom);

    /**
     * Returns the <tt>ChatRoomList</tt> instance.
     * 
     * @return the <tt>ChatRoomList</tt> instance.
     */
    public abstract ChatRoomList getChatRoomList();
    
    /**
     * Adds a change listener to the <tt>ChatRoomList</tt>.
     * 
     * @param l the listener.
     */
    public abstract void addChatRoomListChangeListener(ChatRoomListChangeListener l);
    
    /**
     * Removes a change listener to the <tt>ChatRoomList</tt>.
     * 
     * @param l the listener.
     */
    public abstract void removeChatRoomListChangeListener(ChatRoomListChangeListener l);
    
    /**
     * Fires a <tt>ChatRoomListChangedEvent</tt> event.
     * 
     * @param chatRoomWrapper the chat room.
     * @param eventID the id of the event.
     */
    public abstract void fireChatRoomListChangedEvent(
        ChatRoomWrapper chatRoomWrapper, int eventID);
    
    /**
    * Joins the given chat room with the given password and manages all the
    * exceptions that could occur during the join process.
    *
    * @param chatRoomWrapper the chat room to join.
    * @param nickName the nickname we choose for the given chat room.
    * @param password the password.
    */
    public abstract void joinChatRoom(   ChatRoomWrapper chatRoomWrapper,
            String nickName,
            byte[] password);
    
    /**
    * Joins the given chat room with the given password and manages all the
    * exceptions that could occur during the join process.
    *
    * @param chatRoomWrapper the chat room to join.
    * @param nickName the nickname we choose for the given chat room.
    * @param password the password.
    * @param subject the subject which will be set to the room after the user 
    * join successful.
    */
    public abstract void joinChatRoom(   ChatRoomWrapper chatRoomWrapper,
        String nickName, byte[] password, String subject);
    
    /**
    * Creates a chat room, by specifying the chat room name, the parent
    * protocol provider and eventually, the contacts invited to participate in
    * this chat room.
    *
    * @param roomName the name of the room
    * @param protocolProvider the parent protocol provider.
    * @param contacts the contacts invited when creating the chat room.
    * @param reason
    * @param persistent is the room persistent
    * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
    */
    public abstract ChatRoomWrapper createChatRoom(String roomName,
        ProtocolProviderService protocolProvider, Collection<String> contacts,
        String reason, boolean persistent);
    
    
    
    /**
    * Creates a private chat room, by specifying the parent
    * protocol provider and eventually, the contacts invited to participate in
    * this chat room.
    *
    * @param protocolProvider the parent protocol provider.
    * @param contacts the contacts invited when creating the chat room.
    * @param reason
    * @param persistent is the room persistent
    * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
    */
    public abstract ChatRoomWrapper createPrivateChatRoom(
        ProtocolProviderService protocolProvider, Collection<String> contacts,
        String reason, boolean persistent);
    
    
    /**
    * Creates a chat room, by specifying the chat room name, the parent
    * protocol provider and eventually, the contacts invited to participate in
    * this chat room.
    *
    * @param roomName the name of the room
    * @param protocolProvider the parent protocol provider.
    * @param contacts the contacts invited when creating the chat room.
    * @param reason
    * @param join whether we should join the room after creating it.
    * @param persistent whether the newly created room will be persistent.
    * @param isPrivate whether the room will be private or public.
    * @return the <tt>ChatRoomWrapper</tt> corresponding to the created room
    */
    public abstract ChatRoomWrapper createChatRoom(String roomName,
        ProtocolProviderService protocolProvider, Collection<String> contacts,
        String reason, boolean join, boolean persistent, boolean isPrivate);
    
    /**
    * Joins the room with the given name though the given chat room provider.
    *
    * @param chatRoomName the name of the room to join.
    * @param chatRoomProvider the chat room provider to join through.
    */
    public abstract void joinChatRoom(   String chatRoomName,
        ChatRoomProviderWrapper chatRoomProvider);
    
    /**
    * Returns existing chat rooms for the given <tt>chatRoomProvider</tt>.
    * @param chatRoomProvider the <tt>ChatRoomProviderWrapper</tt>, which
    * chat rooms we're looking for
    * @return  existing chat rooms for the given <tt>chatRoomProvider</tt>
    */
    public abstract List<String> getExistingChatRooms(
        ChatRoomProviderWrapper chatRoomProvider);
    
    
    /**
    * Called to accept an incoming invitation. Adds the invitation chat room
    * to the list of chat rooms and joins it.
    *
    * @param invitation the invitation to accept.
    */
    public abstract void acceptInvitation(ChatRoomInvitation invitation);
    
    /**
     * Rejects the given invitation with the specified reason.
     *
     * @param multiUserChatOpSet the operation set to use for rejecting the
     * invitation
     * @param invitation the invitation to reject
     * @param reason the reason for the rejection
     */
    public abstract void rejectInvitation(  OperationSetMultiUserChat multiUserChatOpSet,
                                   ChatRoomInvitation invitation,
                                   String reason);
    
    /**
     * Determines whether a specific <code>ChatRoom</code> is private i.e.
     * represents a one-to-one conversation which is not a channel. Since the
     * interface {@link ChatRoom} does not expose the private property, an
     * heuristic is used as a workaround: (1) a system <code>ChatRoom</code> is
     * obviously not private and (2) a <code>ChatRoom</code> is private if it
     * has only one <code>ChatRoomMember</code> who is not the local user.
     *
     * @param chatRoom
     *            the <code>ChatRoom</code> to be determined as private or not
     * @return <tt>true</tt> if the specified <code>ChatRoom</code> is private;
     *         otherwise, <tt>false</tt>
     */
    public static boolean isPrivate(ChatRoom chatRoom)
    {
        if (!chatRoom.isSystem()
            && chatRoom.isJoined()
            && (chatRoom.getMembersCount() == 1))
        {
            String nickname = chatRoom.getUserNickname();

            if (nickname != null)
            {
                for (ChatRoomMember member : chatRoom.getMembers())
                    if (nickname.equals(member.getName()))
                        return false;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Leaves the given chat room.
     *
     * @param chatRoomWrapper the chat room to leave.
     * @return <tt>ChatRoomWrapper</tt> instance associated with the chat room.
     */
    public abstract ChatRoomWrapper leaveChatRoom(ChatRoomWrapper chatRoomWrapper);

    public abstract ChatRoomWrapper findChatRoomWrapperFromSourceContact(
        SourceContact contact);
}
