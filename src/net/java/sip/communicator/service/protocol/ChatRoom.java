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
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * Represents a chat channel/room/rendez-vous point/ where multiple chat users
 * could rally and communicate in a many-to-many fashion.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 * @author Valentin Martinet
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
     * Returns the identifier of this <tt>ChatRoom</tt>. The identifier of the
     * chat room would have the following syntax:
     * [chatRoomName]@[chatRoomServer]@[accountID]
     *
     * @return a <tt>String</tt> containing the identifier of this
     * <tt>ChatRoom</tt>.
     */
    public String getIdentifier();

    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     *
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
     * @param password a password necessary to authenticate when joining the
     * room.
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void joinAs(String nickname, byte[] password)
        throws OperationFailedException;

    /**
     * Returns true if the local user is currently in the multi user chat
     * (after calling one of the {@link #join()} methods).
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
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     * <p>
     * To be notified every time the room's subject change you should add a
     * <tt>ChatRoomChangelistener</tt> to this room.
     * {@link #addPropertyChangeListener(ChatRoomPropertyChangeListener)}
     * <p>
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
     * @throws OperationFailedException if the user doesn't have the right to
     * change this property.
     */
    public void setSubject(String subject)
        throws OperationFailedException;

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     * context of the local chat room.
     */
    public String getUserNickname();

    /**
     * Returns the local user's role in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the role currently being used by the local user in the context of
     * the chat room.
     */
    public ChatRoomMemberRole getUserRole();

    /**
     * Changes the the local user's nickname in the context of this chatroom.
     *
     * @param role the new role to set for the local user.
     *
     * @throws OperationFailedException if an error occurs.
     */
    public void setLocalUserRole(ChatRoomMemberRole role)
        throws OperationFailedException;

    /**
     * Changes the the local user's nickname in the context of this chatroom.
     *
     * @param nickname the new nickname within the room.
     *
     * @throws OperationFailedException if the new nickname already exist in
     * this room
     */
    public void setUserNickname(String nickname)
       throws OperationFailedException;

    /**
     * Adds a listener that will be notified of changes in our participation in
     * the room such as us being kicked, join, left...
     *
     * @param listener a member participation listener.
     */
    public void addMemberPresenceListener(
        ChatRoomMemberPresenceListener listener);

    /**
     * Removes a listener that was being notified of changes in the
     * participation of other chat room participants such as users being kicked,
     * join, left.
     *
     * @param listener a member participation listener.
     */
    public void removeMemberPresenceListener(
        ChatRoomMemberPresenceListener listener);

    /**
     * Adds a listener that will be notified of changes in our role in the room
     * such as us being granded operator.
     *
     * @param listener a local user role listener.
     */
    public void addLocalUserRoleListener(ChatRoomLocalUserRoleListener listener);

    /**
     * Removes a listener that was being notified of changes in our role in this
     * chat room such as us being granded operator.
     *
     * @param listener a local user role listener.
     */
    public void removelocalUserRoleListener(
        ChatRoomLocalUserRoleListener listener);

    /**
     * Adds a listener that will be notified of changes of a member role in the
     * room such as being granded operator.
     *
     * @param listener a member role listener.
     */
    public void addMemberRoleListener(ChatRoomMemberRoleListener listener);

    /**
     * Removes a listener that was being notified of changes of a member role in
     * this chat room such as us being granded operator.
     *
     * @param listener a member role listener.
     */
    public void removeMemberRoleListener(ChatRoomMemberRoleListener listener);

    /**
     * Adds a listener that will be notified of changes in the property of the
     * room such as the subject being change or the room state being changed.
     *
     * @param listener a property change listener.
     */
    public void addPropertyChangeListener(
        ChatRoomPropertyChangeListener listener);

    /**
     * Removes a listener that was being notified of changes in the property of
     * the chat room such as the subject being change or the room state being
     * changed.
     *
     * @param listener a property change listener.
     */
    public void removePropertyChangeListener(
        ChatRoomPropertyChangeListener listener);

    /**
     * Adds a listener that will be notified of changes in the property of a
     * room member such as the nickname being changed.
     *
     * @param listener a room member property change listener.
     */
    public void addMemberPropertyChangeListener(
        ChatRoomMemberPropertyChangeListener listener);

    /**
     * Removes a listener that was being notified of changes in the property of
     * a chat room member such as the nickname being changed.
     *
     * @param listener a room member property change listener.
     */
    public void removeMemberPropertyChangeListener(
        ChatRoomMemberPropertyChangeListener listener);

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
     * Returns a <tt>List</tt> of <tt>ChatRoomMember</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>ChatRoomMember</tt> instances
     * corresponding to all room members.
     */
    public List<ChatRoomMember> getMembers();

    /**
     * Returns the number of participants that are currently in this chat room.
     * @return the number of <tt>Contact</tt>s, currently participating in
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

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject);

    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText);

    /**
     * Sends the <tt>message</tt> to this chat room.
     *
     * @param message the <tt>Message</tt> to send.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    public void sendMessage(Message message)
        throws OperationFailedException;

    /**
     * Returns a reference to the provider that created this room.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this room.
     */
    public ProtocolProviderService getParentProvider();

    /**
     * Returns an Iterator over a set of ban masks for this chat room. The ban
     * mask defines a group of users that will be banned. The ban list is a list
     * of all such ban masks defined for this chat room.
     *
     * @return an Iterator over a set of ban masks for this chat room
     * @throws OperationFailedException if an error occured while performing the
     * request to the server or you don't have enough privileges to get this
     * information
     */
    public Iterator<ChatRoomMember> getBanList()
        throws OperationFailedException;

    /**
     * Bans a user from the room. An administrator or owner of the room can ban
     * users from a room. A banned user will no longer be able to join the room
     * unless the ban has been removed. If the banned user was present in the
     * room then he/she will be removed from the room and notified that he/she
     * was banned along with the reason (if provided) and the user who initiated
     * the ban.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to be banned.
     * @param reason the reason why the user was banned.
     * @throws OperationFailedException if an error occurs while banning a user.
     * In particular, an error can occur if a moderator or a user with an
     * affiliation of "owner" or "admin" was tried to be banned or if the user
     * that is banning have not enough permissions to ban.
     */
    public void banParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException;

    /**
     * Kicks a visitor or participant from the room.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to kick from the room
     * @param reason the reason why the participant is being kicked from the
     * room
     * @throws OperationFailedException if an error occurs while kicking the
     * participant. In particular, an error can occur if a moderator or a user
     * with an affiliation of "owner" or "administrator" was intended to be
     * kicked; or if the participant that intended to kick another participant
     * does not have kicking privileges;
     */
    public void kickParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException;

    /**
     * Returns the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room. If the user doesn't have
     * permissions to see and change chat room configuration an
     * <tt>OperationFailedException</tt> is thrown.
     *
     * @return the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room
     * @throws OperationFailedException if the user doesn't have
     * permissions to see and change chat room configuration
     */
    public ChatRoomConfigurationForm getConfigurationForm()
        throws OperationFailedException;

    /**
     * Returns <code>true</code> if this chat room is a system room and
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if this chat room is a system room and
     * <code>false</code> otherwise.
     */
    public boolean isSystem();

    /**
     * Determines whether this chat room should be stored in the configuration
     * file or not. If the chat room is persistent it still will be shown after a
     * restart in the chat room list. A non-persistent chat room will be only in
     * the chat room list until the the program is running.
     *
     * @return true if this chat room is persistent, false otherwise
     */
    public boolean isPersistent();

    /**
     * Finds private messaging contact by nickname. If the contact doesn't 
     * exists a new volatile contact is created.
     * 
     * @param name the nickname of the contact.
     * @return the contact instance.
     */
    public Contact getPrivateContactByNickname(String name);

    /**
    * Grants administrator privileges to another user. Room owners may grant
    * administrator privileges to a member or unaffiliated user. An
    * administrator is allowed to perform administrative functions such as
    * banning users and edit moderator list.
    *
    * @param address the user address of the user to grant administrator
    * privileges (e.g. "user@host.org").
    */
    public void grantAdmin(String address);

    /**
    * Grants membership to a user. Only administrators are able to grant
    * membership. A user that becomes a room member will be able to enter a room
    * of type Members-Only (i.e. a room that a user cannot enter without being
    * on the member list).
    *
    * @param address the user address of the user to grant membership
    * privileges (e.g. "user@host.org").
    */
    public void grantMembership(String address);

    /**
    * Grants moderator privileges to a participant or visitor. Room
    * administrators may grant moderator privileges. A moderator is allowed to
    * kick users, grant and revoke voice, invite other users, modify room's
    * subject plus all the partcipants privileges.
    *
    * @param nickname the nickname of the occupant to grant moderator
    * privileges.
    */
    public void grantModerator(String nickname);

    /**
    * Grants ownership privileges to another user. Room owners may grant
    * ownership privileges. Some room implementations will not allow to grant
    * ownership privileges to other users. An owner is allowed to change
    * defining room features as well as perform all administrative functions.
    *
    * @param address the user address of the user to grant ownership
    * privileges (e.g. "user@host.org").
    */
    public void grantOwnership(String address);

    /**
    * Grants voice to a visitor in the room. In a moderated room, a moderator
    * may want to manage who does and does not have "voice" in the room. To have
    * voice means that a room occupant is able to send messages to the room
    * occupants.
    *
    * @param nickname the nickname of the visitor to grant voice in the room
    * (e.g. "john").
    */
    public void grantVoice(String nickname);

    /**
    * Revokes administrator privileges from a user. The occupant that loses
    * administrator privileges will become a member. Room owners may revoke
    * administrator privileges from a member or unaffiliated user.
    *
    * @param address the user address of the user to grant administrator
    * privileges (e.g. "user@host.org").
    */
    public void revokeAdmin(String address);

    /**
    * Revokes a user's membership. Only administrators are able to revoke
    * membership. A user that becomes a room member will be able to enter a room
    * of type Members-Only (i.e. a room that a user cannot enter without being
    * on the member list). If the user is in the room and the room is of type
    * members-only then the user will be removed from the room.
    *
    * @param address the user address of the user to revoke membership
    * (e.g. "user@host.org").
    */
    public void revokeMembership(String address);

    /**
    * Revokes moderator privileges from another user. The occupant that loses
    * moderator privileges will become a participant. Room administrators may
    * revoke moderator privileges only to occupants whose affiliation is member
    * or none. This means that an administrator is not allowed to revoke
    * moderator privileges from other room administrators or owners.
    *
    * @param nickname the nickname of the occupant to revoke moderator
    * privileges.
    */
    public void revokeModerator(String nickname);

    /**
    * Revokes ownership privileges from another user. The occupant that loses
    * ownership privileges will become an administrator. Room owners may revoke
    * ownership privileges. Some room implementations will not allow to grant
    * ownership privileges to other users.
    *
    * @param address the user address of the user to revoke ownership
    * (e.g. "user@host.org").
    */
    public void revokeOwnership(String address);

    /**
    * Revokes voice from a participant in the room. In a moderated room, a
    * moderator may want to revoke an occupant's privileges to speak. To have
    * voice means that a room occupant is able to send messages to the room
    * occupants.
    * @param nickname the nickname of the participant to revoke voice
    * (e.g. "john").
    */
    public void revokeVoice(String nickname);

    /**
     * Publishes a <tt>ConferenceDescription</tt> to the chat room.
     *
     * @param cd the description to publish
     * @param name the name of the conference
     * @return the published conference
     */
    public ConferenceDescription publishConference(ConferenceDescription cd,
        String name);

    /**
     * Updates the presence status of private messaging contact.
     *
     * @param nickname the nickname of the contact.
     */
    public void updatePrivateContactPresenceStatus(String nickname);

    /**
     * Updates the presence status of private messaging contact.
     *
     * @param contact the contact.
     */
    public void updatePrivateContactPresenceStatus(Contact contact);

    /**
     * Adds a listener that will be notified when a member of this chat room
     * has published a <tt>ConferenceDescription</tt> to the room.
     *
     * @param listener the listener to add.
     */
    public void addConferencePublishedListener(
            ChatRoomConferencePublishedListener listener);

    /**
     * Removes a listener that was being notified when a member of this chat
     * room had published a <tt>ConferenceDescription</tt> to the room.
     *
     * @param listener the listener to remove.
     */
    public void removeConferencePublishedListener(
            ChatRoomConferencePublishedListener listener);

    /**
     * Returns cached <tt>ConferenceDescription</tt> instances.
     * @return the cached <tt>ConferenceDescription</tt> instances.
     */
    public Map<String, ConferenceDescription> getCachedConferenceDescriptions();

    /**
     * Returns the number of cached <tt>ConferenceDescription</tt> instances.
     * @return the number of cached <tt>ConferenceDescription</tt> instances.
     */
    public int getCachedConferenceDescriptionSize();

    /**
     * Destroys the chat room.
     * @param reason the reason for destroying.
     * @param alternateAddress the alternate address
     * @return <tt>true</tt> if the room is destroyed.
     */
    public boolean destroy(String reason, String alternateAddress);

    /**
     * Returns the ids of the users that has the member role in the room.
     * When the room is member only, this are the users allowed to join.
     * @return the ids of the users that has the member role in the room.
     */
    public List<String> getMembersWhiteList();

    /**
     * Changes the list of users that has role member for this room.
     * When the room is member only, this are the users allowed to join.
     * @param members the ids of user to have member role.
     */
    public void setMembersWhiteList(List<String> members);
}
