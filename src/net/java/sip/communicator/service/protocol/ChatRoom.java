/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.ChatRoomPropertyChangeListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomLocalUserStatusListener;
import net.java.sip.communicator.service.protocol.event.ChatRoomParticipantStatusListener;
import java.util.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents a chat channel/room/rendez-vous point/ where multiple chat users
 * could rally and communicate in a many-to-many fashion.
 *
 * @author Emil Ivov
 */
public interface ChatRoom
{

    /**
     * Returns the name of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the name of this <tt>ChatRoom</tt>.
     */
    public String getName();

    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     *
     * @param nickname the nickname to use.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void join()
        throws OperationFailedException;

    /**
     * Joins this chat room so that the user would start receiving events and
     * messages for it. The method uses the nickname of the local user and the
     * specified password in order to enter the chatroom.
     *
     * @param password the password to use when authenticating on the chatroom.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void join(byte[] password)
        throws OperationFailedException;


    /**
     * Joins this chat room with the specified nickname so that the user would
     * start receiving events and messages for it. If the chatroom already
     * contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * @param nickname the nickname to use.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void joinAs(String nickname)
        throws OperationFailedException;

    /**
     * Joins this chat room with the specified nickname and password so that the
     * user would start receiving events and messages for it. If the chatroom
     * already contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * @param nickname the nickname to use.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void joinAs(String nickname, byte[] password)
        throws OperationFailedException;

    /**
     * Returns true if the local user is currently in the multi user chat
     * (after calling one of the {@link #join(String)} methods).
     *
     * @return true if currently we're currently in this chat room and false
     * otherwise.
     */
    public boolean isJoined();

    /**
     * Leave this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered. Depending on the underlying protocol and
     * implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     */
    public void leave();

    /**
     * Adds <tt>listener</tt> to the list of listeners registered to receive
     * events upon modification of chat room properties such as its subject
     * for example.
     *
     * @param listener ChatRoomChangeListener
     */
    public void addChatRoomPropertyChangeListener(
                                    ChatRoomPropertyChangeListener listener);

    /**
     * Removes <tt>listener</tt> from the list of listeneres current registered
     * for chat room modification events.
     *
     * @param listener the <tt>ChatRoomChangeListener</tt> to remove.
     */
    public void removeChatRoomPropertyChangeListener(
                                    ChatRoomPropertyChangeListener listener);

    /**
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     * <p>
     * To be notified every time the room's subject change you should add a
     * <tt>ChatRoomChangelistener</tt> to this room.
     * {@link #addChatRoomChaneListener(ChatRoomChangeListener)}<p>
     *
     * To change the room's subject use {@link #setSubject(String)}.
     *
     * @return the room subject or <tt>null</tt> if the user hasn't joined the
     * room or the room does not have a subject yet.
     */
    public String getSubject();

    /**
     * Sets the subject of this chat room. If the user does not have the right
     * to change the room subject, or the protocol does not support this, or
     * the operation fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code.
     *
     * @param subject the new subject that we'd like this room to have
     * @throws OperationFailedException
     */
    public void setSubject(final String subject)
        throws OperationFailedException;

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     * context of the local chat room.
     */
    public String getNickname();

    /**
     * Changes the the local user's nickname in the context of this chatroom.
     * If the operation is not supported by the underlying implementation, the
     * method throws an OperationFailedException with the corresponding code.
     *
     * @param nickname the new nickname within the room.
     * @throws OperationFaileEexception if the setting the new nickname changes
     * for some reason.
     */
    public void setNickname(String nickname)
       throws OperationFailedException;

   /**
    * Adds a listener that will be notified of changes in our status in the room
    * such as us being kicked, banned, or granted admin permissions.
    *
    * @param listener a local user status listener.
    */
    public void addLocalUserStatusListener(
                                    ChatRoomLocalUserStatusListener listener);

   /**
    * Removes a listener that was being notified of changes in our status in
    * the room such as us being kicked, banned, or granted admin permissions.
    *
    * @param listener a local user status listener.
    */
    public void removeLocalUserStatusListener(
                                    ChatRoomLocalUserStatusListener listener);

   /**
    * Adds a listener that will be notified of changes in our status in the room
    * such as us being kicked, banned, or granted admin permissions.
    *
    * @param listener a participant status listener.
    */
    public void addParticipantStatusListener(
                                    ChatRoomParticipantStatusListener listener);

   /**
    * Removes a listener that was being notified of changes in the status of
    * other chat room participants such as users being kicked, banned, or
    * granted admin permissions.
    *
    * @param listener a participant status listener.
    */
    public void removeParticipantStatusListener(
                                    ChatRoomParticipantStatusListener listener);

    /**
     * Invites another user to this room.
     * <p>
     * If the room is password-protected, the invitee will receive a password to
     * use to join the room. If the room is members-only, the the invitee may
     * be added to the member list.
     *
     * @param userAddress the address of the user to invite to the room.(one
     * may also invite users not on their contact list).
     * @param reason a reason, subject, or welcome message that would tell the
     * the user why they are being invited.
     */
    public void invite(String userAddress, String reason);

    /**
     * Returns a <tt>List</tt> of <tt>Contact</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>Contact</tt> corresponding to all room
     * members.
     * @throws OperationFailedException if we fail retrieving the list of room
     * participants.
     */
    public List getMembers();

    /**
     * Returns the number of participants that are currently in this chat room.
     * @return int the number of <tt>Contact</tt>s, currently participating in
     * this room.
     */
    public int getMembersCount();

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this chat room.
     * @param listener a <tt>MessageListener</tt> that would be notified every
     * time a new message is received on this chat room.
     */
    public void addMessageListener(ChatRoomMessageListener listener);

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(ChatRoomMessageListener listener);

    //include - roominfo
    /** @todo include room info */
}
