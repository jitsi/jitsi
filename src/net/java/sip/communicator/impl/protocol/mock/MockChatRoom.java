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
import net.java.sip.communicator.service.protocol.event.*;

/**
 *
 * @author Damian Minkov
 */
public class MockChatRoom
    extends AbstractChatRoom
{
    private MockProvider provider;

    private MockMultiUserChat parentOpSet = null;

    private String name;

    private String subject;

    private String nickname;

    private boolean joined = false;

    private List<ChatRoomMember> members = new Vector<ChatRoomMember>();

    /**
     * Currently registered member presence listeners.
     */
    private final List<ChatRoomMemberPresenceListener> memberPresenceListeners
        = new Vector<ChatRoomMemberPresenceListener>();

    /**
     * Currently registered local user role listeners.
     */
    private final List<ChatRoomLocalUserRoleListener> localUserRoleListeners
        = new Vector<ChatRoomLocalUserRoleListener>();

    /**
     * Currently registered member role listeners.
     */
    private final List<ChatRoomMemberRoleListener> memberRoleListeners
        = new Vector<ChatRoomMemberRoleListener>();

    /**
     * Currently registered property change listeners.
     */
    private final List<ChatRoomPropertyChangeListener> propertyChangeListeners
        = new Vector<ChatRoomPropertyChangeListener>();

    /**
     * Currently registered message listeners.
     */
    private final List<ChatRoomMessageListener> messageListeners
        = new Vector<ChatRoomMessageListener>();

    /**
     * Creates an instance of <tt>MockChatRoom</tt> by specifying the
     * corresponding protocol provider, the multi user chat operation set
     * and the room name.
     * @param provider the corresponding <tt>ProtocolProviderService</tt>
     * @param parentOpSet the corresponding <tt>OperationSetMultiUserChat</tt>
     * @param roomName the name of the room
     */
    public MockChatRoom(MockProvider provider,
                        MockMultiUserChat parentOpSet,
                        String roomName)
    {
        this.provider = provider;
        this.name = roomName;
        this.parentOpSet = parentOpSet;
    }

     /**
     * Returns the name of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the name of this <tt>ChatRoom</tt>.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     *
     * @throws OperationFailedException with the corresponding code if an error
     * occurs while joining the room.
     */
    public void join()
        throws OperationFailedException
    {
        joinAs(null, null);
    }

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
        throws OperationFailedException
    {
        joinAs(null, password);
    }

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
        throws OperationFailedException
    {
        joinAs(nickname, null);
    }

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
        throws OperationFailedException
    {
        if(nickname == null)
            nickname = getParentProvider().getAccountID().getUserID();

        this.nickname = nickname;
        this.joined = true;

        Contact mockContact = new MockContact(nickname,
            (MockProvider) getParentProvider());

        MockChatRoomMember newMember =
            new MockChatRoomMember( nickname,
                                    this,
                                    ChatRoomMemberRole.MEMBER,
                                    mockContact,
                                    null);

        members.add(newMember);

        parentOpSet
            .fireLocalUserPresenceEvent(
                this,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                null);
    }

    /**
     * Returns true if the local user is currently in the multi user chat
     * (after calling one of the {@link #join()} methods).
     *
     * @return true if currently we're currently in this chat room and false
     * otherwise.
     */
    public boolean isJoined()
    {
        return joined;
    }

    /**
     * Leave this chat room. Once this method is called, the user won't be
     * listed as a member of the chat room any more and no further chat events
     * will be delivered. Depending on the underlying protocol and
     * implementation leave() might cause the room to be destroyed if it has
     * been created by the local user.
     */
    public void leave()
    {

    }

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
    public String getSubject()
    {
        return subject;
    }

    /**
     * Sets the subject of this chat room. If the user does not have the right
     * to change the room subject, or the protocol does not support this, or
     * the operation fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code.
     *
     * @param subject the new subject that we'd like this room to have
     * @throws OperationFailedException
     */
    public void setSubject(String subject)
        throws OperationFailedException
    {
        this.subject = subject;
    }

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     * context of the local chat room.
     */
    public String getUserNickname()
    {
        return nickname;
    }

    /**
     * Changes the the local user's nickname in the context of this chatroom.
     * If the operation is not supported by the underlying implementation, the
     * method throws an OperationFailedException with the corresponding code.
     *
     * @param nickname the new nickname within the room.
     *
     * @throws OperationFailedException if the setting the new nickname changes
     * for some reason.
     */
    public void setNickname(String nickname)
       throws OperationFailedException
    {
        this.nickname = nickname;
    }

    /**
     * Adds a listener that will be notified of changes in our participation in
     * the room such as us being kicked, join, left...
     *
     * @param listener a member participation listener.
     */
    public void addMemberPresenceListener(
        ChatRoomMemberPresenceListener listener)
    {
        if(!memberPresenceListeners.contains(listener))
            memberPresenceListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes in the
     * participation of other chat room participants such as users being kicked,
     * join, left.
     *
     * @param listener a member participation listener.
     */
    public void removeMemberPresenceListener(
        ChatRoomMemberPresenceListener listener)
    {
        memberPresenceListeners.remove(listener);
    }

    /**
     * Adds a listener that will be notified of changes in our role in the room
     * such as us being granded operator.
     *
     * @param listener a local user role listener.
     */
    public void addLocalUserRoleListener(ChatRoomLocalUserRoleListener listener)
    {
        if(!localUserRoleListeners.contains(listener))
            localUserRoleListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes in our role in this
     * chat room such as us being granded operator.
     *
     * @param listener a local user role listener.
     */
    public void removelocalUserRoleListener(
        ChatRoomLocalUserRoleListener listener)
    {
        localUserRoleListeners.remove(listener);
    }

    /**
     * Adds a listener that will be notified of changes of a member role in the
     * room such as being granded operator.
     *
     * @param listener a member role listener.
     */
    public void addMemberRoleListener(ChatRoomMemberRoleListener listener)
    {
        if(!memberRoleListeners.contains(listener))
            memberRoleListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes of a member role in
     * this chat room such as us being granded operator.
     *
     * @param listener a member role listener.
     */
    public void removeMemberRoleListener(ChatRoomMemberRoleListener listener)
    {
        memberRoleListeners.remove(listener);
    }

    /**
     * Adds a listener that will be notified of changes in the property of the
     * room such as the subject being change or the room state being changed.
     *
     * @param listener a property change listener.
     */
    public void addPropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        if(!propertyChangeListeners.contains(listener))
            propertyChangeListeners.add(listener);
    }

    /**
     * Removes a listener that was being notified of changes in the property of
     * the chat room such as the subject being change or the room state being
     * changed.
     *
     * @param listener a property change listener.
     */
    public void removePropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        propertyChangeListeners.remove(listener);
    }

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
    public void invite(String userAddress, String reason)
    {

    }

    /**
     * Returns a <tt>List</tt> of <tt>ChatRoomMember</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>ChatRoomMember</tt> instances
     * corresponding to all room members.
     */
    public List<ChatRoomMember> getMembers()
    {
        return members;
    }

    /**
     * Returns the number of participants that are currently in this chat room.
     * @return int the number of <tt>Contact</tt>s, currently participating in
     * this room.
     */
    public int getMembersCount()
    {
        return members.size();
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this chat room.
     * @param listener a <tt>MessageListener</tt> that would be notified every
     * time a new message is received on this chat room.
     */
    public void addMessageListener(ChatRoomMessageListener listener)
    {
        if(!messageListeners.contains(listener))
            messageListeners.add(listener);
    }

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(ChatRoomMessageListener listener)
    {
        messageListeners.remove(listener);
    }

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
                                 String contentEncoding, String subject)
    {
        return new MockMessage(new String(content), contentType,
            contentEncoding, subject);
    }

    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        return new MockMessage(messageText);
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     * @param message the <tt>Message</tt> to send.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    public void sendMessage(Message message)
        throws OperationFailedException
    {
        ChatRoomMessageDeliveredEvent evt =
            new ChatRoomMessageDeliveredEvent(
                    this,
                    new Date(),
                    message,
                    ChatRoomMessageDeliveredEvent
                        .CONVERSATION_MESSAGE_DELIVERED);

        for (ChatRoomMessageListener elem : messageListeners)
            elem.messageDelivered(evt);
    }

    /**
     * Returns a reference to the provider that created this room.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> instance
     * that created this room.
     */
    public ProtocolProviderService getParentProvider()
    {
        return provider;
    }

    /**
     * Returns an Iterator over a set of ban masks for this chat room. The ban
     * mask defines a group of users that will be banned. The ban list is a list
     * of all such ban masks defined for this chat room.
     *
     * @return an Iterator over a set of ban masks for this chat room
     */
    public Iterator<ChatRoomMember> getBanList()
    {
        return new Vector<ChatRoomMember>().iterator();
    }

    /**
     * Methods for manipulating mock operation set as
     * deliver(receive) messageop
     *
     * @param msg the message that we are to deliver.
     * @param from delivered from
     */
    public void deliverMessage(Message msg, String from)
    {
        ChatRoomMember fromMember = null;
        for (ChatRoomMember elem : members)
        {
            if(elem.getName().equals(from))
            {
                fromMember = elem;
                break;
            }
        }

        if(fromMember == null)
            return;

        ChatRoomMessageReceivedEvent evt =
            new ChatRoomMessageReceivedEvent(
                    this,
                    fromMember,
                    new Date(),
                    msg,
                    ChatRoomMessageReceivedEvent
                        .CONVERSATION_MESSAGE_RECEIVED);

        for (ChatRoomMessageListener elem : messageListeners)
            elem.messageReceived(evt);
    }

    /**
     * Changes the the local user's nickname in the context of this chatroom.
     * If the operation is not supported by the underlying implementation, the
     * method throws an OperationFailedException with the corresponding code.
     *
     * @param nickname the new nickname within the room.
     *
     * @throws OperationFailedException if the setting the new nickname changes
     * for some reason.
     */
    public void setUserNickname(String nickname) throws OperationFailedException
    {
        this.nickname = nickname;
    }

    /**
     * Returns the identifier of this <tt>ChatRoom</tt>. The identifier of the
     * chat room would have the following syntax:
     * [chatRoomName]@[chatRoomServer]@[accountID]
     *
     * @return a <tt>String</tt> containing the identifier of this
     * <tt>ChatRoom</tt>.
     */
    public String getIdentifier()
    {
        return name;
    }

    /**
     * Bans a user from the room. Not implemented for mock chat rooms.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to be banned
     * @param reason the reason why the user was banned
     * @throws OperationFailedException if an error occurs while banning a user
     */
    public void banParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException {}

    /**
     * Kicks a visitor or participant from the room. Not implemented for mock
     * chat rooms.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to kick from the room
     * @param reason the reason why the participant is being kicked from the
     * room
     * @throws OperationFailedException if an error occurs while kicking the
     * participant
     */
    public void kickParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException {}

    /**
     * Returns the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room. Not implemented for mock
     * chat rooms.
     *
     * @return the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room
     * @throws OperationFailedException if the user doesn't have
     * permissions to see and change chat room configuration
     */
    public ChatRoomConfigurationForm getConfigurationForm()
        throws OperationFailedException
    {
        return null;
    }

    /**
     * Adds a listener that will be notified of changes in the property of a
     * room member such as the nickname being changed.
     *
     * @param listener a room member property change listener.
     */
    public void addMemberPropertyChangeListener(
        ChatRoomMemberPropertyChangeListener listener)
    {
        // TODO Implement the addMemberPropertyChangeListener
    }

    /**
     * Removes a listener that was being notified of changes in the property of
     * a chat room member such as the nickname being changed.
     *
     * @param listener a room member property change listener.
     */
    public void removeMemberPropertyChangeListener(
        ChatRoomMemberPropertyChangeListener listener)
    {
        // TODO Implement the removeMemberPropertyChangeListener
    }

    /**
     * Returns <code>true</code> if this chat room is a system room and
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if this chat room is a system room and
     * <code>false</code> otherwise.
     */
    public boolean isSystem()
    {
        return false;
    }

    /**
     * Determines whether this chat room should be stored in the configuration
     * file or not. If the chat room is persistent it still will be shown after a
     * restart in the chat room list. A non-persistent chat room will be only in
     * the chat room list until the the program is running.
     *
     * @return true if this chat room is persistent, false otherwise
     */
    public boolean isPersistent()
    {
        return true;
    }

    /**
     * Grants admin role to the participant given by <tt>address</tt>.
     * @param address the address of the participant to grant admin role to
     */
    public void grantAdmin(String address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Grants membership role to the participant given by <tt>address</tt>.
     * @param address the address of the participant to grant membership role to
     */
    public void grantMembership(String address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Grants moderator role to the participant given by <tt>address</tt>.
     * @param address the address of the participant to grant moderator role to
     */
    public void grantModerator(String address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Grants ownership role to the participant given by <tt>address</tt>.
     * @param address the address of the participant to grant ownership role to
     */
    public void grantOwnership(String address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Grants voice to the participant given by <tt>address</tt>.
     * @param address the address of the participant to grant voice to
     */
    public void grantVoice(String address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Revokes the admin role for the participant given by <tt>address</tt>.
     * @param address the address of the participant to revoke admin role for
     */
    public void revokeAdmin(String address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Revokes the membership role for the participant given by <tt>address</tt>.
     * @param address the address of the participant to revoke membership role
     * for
     */
    public void revokeMembership(String address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Revokes the moderator role for the participant given by <tt>address</tt>.
     * @param address the address of the participant to revoke moderator role
     * for
     */
    public void revokeModerator(String address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Revokes the ownership role for the participant given by <tt>address</tt>.
     * @param address the address of the participant to revoke ownership role
     * for
     */
    public void revokeOwnership(String address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Revokes the voice for the participant given by <tt>address</tt>.
     * @param address the address of the participant to revoke voice for
     */
    public void revokeVoice(String address)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns the local user role.
     * @return the local user role
     */
    public ChatRoomMemberRole getUserRole()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Sets the local user role.
     * @param role the role to set
     * @throws OperationFailedException if the operation don't succeed
     */
    public void setLocalUserRole(ChatRoomMemberRole role)
        throws OperationFailedException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc}
     *
     * Not implemented.
     */
    public ConferenceDescription publishConference(ConferenceDescription cd,
        String name)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * Not implemented.
     */
    @Override
    public Contact getPrivateContactByNickname(String name)
    {
        return null;
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     *
     * Not implemented.
     */
    @Override
    public void updatePrivateContactPresenceStatus(String nickname) { }

    /**
     * {@inheritDoc}
     *
     * Not implemented.
     */
    @Override
    public void updatePrivateContactPresenceStatus(Contact sourceContact) { }

    /**
     * Destroys the chat room.
     * @param reason the reason for destroying.
     * @param alternateAddress the alternate address
     * @return <tt>true</tt> if the room is destroyed.
     */
    public boolean destroy(String reason, String alternateAddress)
    {
        return true;
    }

    /**
     * Returns the ids of the users that has the member role in the room.
     * When the room is member only, this are the users allowed to join.
     * @return the ids of the users that has the member role in the room.
     */
    @Override
    public List<String> getMembersWhiteList()
    {
        return new ArrayList<String>();
    }

    /**
     * Changes the list of users that has role member for this room.
     * When the room is member only, this are the users allowed to join.
     * @param members the ids of user to have member role.
     */
    @Override
    public void setMembersWhiteList(List<String> members)
    {}
}
