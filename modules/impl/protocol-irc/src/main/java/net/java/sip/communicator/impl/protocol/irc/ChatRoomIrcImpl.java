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
package net.java.sip.communicator.impl.protocol.irc;

import java.beans.*;
import java.io.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.irc.exception.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a chat channel/room, where multiple chat users could rally and
 * communicate in a many-to-many fashion.
 *
 * @author Stephane Remy
 * @author Loic Kempf
 * @author Yana Stamcheva
 * @author Danny van Heumen
 */
public class ChatRoomIrcImpl
    extends AbstractChatRoom
{
    /**
     * Default channel prefix in case user forgot to include a valid channel
     * prefix in the chat room name.
     */
    private static final char DEFAULT_CHANNEL_PREFIX = '#';

    /**
     * Maximum length of an IRC channel name.
     */
    private static final int MAXIMUM_LENGTH_OF_CHANNEL_NAME = 200;

    /**
     * The object used for logging.
     */
    private static final Logger LOGGER
        = Logger.getLogger(ChatRoomIrcImpl.class);

    /**
     * The parent protocol service provider.
     */
    private final ProtocolProviderServiceIrcImpl parentProvider;

    /**
     * The name of the chat room.
     */
    private final String chatRoomName;

    /**
     * The subject of the chat room.
     */
    private String chatSubject = "";

    /**
     * list of members of this chatRoom.
     */
    private final Hashtable<String, ChatRoomMember> chatRoomMembers
        = new Hashtable<String, ChatRoomMember>();

    /**
     * Listeners that will be notified of changes in member status in the
     * room such as member joined, left or being kicked or dropped.
     */
    private final Vector<ChatRoomMemberPresenceListener>
        memberListeners = new Vector<ChatRoomMemberPresenceListener>();

    /**
     * Listeners that will be notified of changes in member role in the
     * room such as member being granted admin permissions, or revoked admin
     * permissions.
     */
    private final Vector<ChatRoomMemberRoleListener> memberRoleListeners
        = new Vector<ChatRoomMemberRoleListener>();

    /**
     * Listeners that will be notified of changes in local user role in the
     * room such as member being granted administrator permissions, or revoked
     * administrator permissions.
     */
    private final Vector<ChatRoomLocalUserRoleListener> localUserRoleListeners
        = new Vector<ChatRoomLocalUserRoleListener>();

    /**
     * Listeners that will be notified every time
     * a new message is received on this chat room.
     */
    private final Vector<ChatRoomMessageListener> messageListeners
        = new Vector<ChatRoomMessageListener>();

    /**
     * Listeners that will be notified every time
     * a chat room property has been changed.
     */
    private final Vector<ChatRoomPropertyChangeListener> propertyChangeListeners
        = new Vector<ChatRoomPropertyChangeListener>();

    /**
     * Listeners that will be notified every time
     * a chat room member property has been changed.
     */
    private final Vector<ChatRoomMemberPropertyChangeListener>
        memberPropChangeListeners
            = new Vector<ChatRoomMemberPropertyChangeListener>();

    /**
     * The table containing all banned members.
     */
    private ArrayList<ChatRoomMember> bannedMembers
        = new ArrayList<ChatRoomMember>();

    /**
     * Indicates if this chat room is a system one (i.e. corresponding to the
     * server channel).
     */
    private boolean isSystem = false;

    /**
     * Instance of chat room member that represents the user.
     */
    private ChatRoomMemberIrcImpl user = null;

    /**
     * Creates an instance of <tt>ChatRoomIrcImpl</tt>, by specifying the room
     * name and the protocol provider.
     *
     * @param chatRoomName the name of the chat room
     * @param parentProvider the protocol provider
     */
    public ChatRoomIrcImpl(final String chatRoomName,
        final ProtocolProviderServiceIrcImpl parentProvider)
    {
        this(chatRoomName, parentProvider, false);
    }

    /**
     * Creates an instance of <tt>ChatRoomIrcImpl</tt>, by specifying the room
     * name, the protocol provider and the isPrivate property. Private chat
     * rooms are one-to-one chat rooms.
     *
     * @param chatRoomName the name of the chat room (cannot be null or empty
     *            string)
     * @param parentProvider the protocol provider
     * @param isSystem indicates if this chat room is a system room
     */
    public ChatRoomIrcImpl(final String chatRoomName,
        final ProtocolProviderServiceIrcImpl parentProvider,
        final boolean isSystem)
    {
        if (parentProvider == null)
        {
            throw new IllegalArgumentException("parentProvider cannot be null");
        }
        this.parentProvider = parentProvider;
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        this.chatRoomName =
            verifyName(connection.getChannelManager().getChannelTypes(),
                chatRoomName);
        this.isSystem = isSystem;
    }

    /**
     * Verify if the chat room name/identifier meets all the criteria.
     *
     * @param name chat room name/identifier
     * @return returns the chat room name if it is valid
     * @throws IllegalArgumentException if name/identifier contains invalid
     *             characters
     */
    private static String verifyName(final Set<Character> channelTypes,
        final String name)
    {
        if (name == null || name.isEmpty()
            || name.length() > MAXIMUM_LENGTH_OF_CHANNEL_NAME)
        {
            throw new IllegalArgumentException("Invalid chat room name.");
        }
        final char prefix = name.charAt(0);
        // Check for default channel prefix explicitly just in case it isn't
        // listed as a channel type.
        if (channelTypes.contains(prefix) || prefix == DEFAULT_CHANNEL_PREFIX)
        {
            for (char c : IrcConnection.SPECIAL_CHARACTERS)
            {
                if (name.contains("" + c))
                {
                    throw new IllegalArgumentException(
                       "chat room identifier contains illegal character: " + c);
                }
            }
            return name;
        }
        else
        {
            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Automatically added " + DEFAULT_CHANNEL_PREFIX
                    + " channel prefix.");
            }
            return verifyName(channelTypes, DEFAULT_CHANNEL_PREFIX + name);
        }
    }

    /**
     * hashCode implementation for Chat Room.
     *
     * @return returns hash code for this instance
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + chatRoomName.hashCode();
        result = prime * result + parentProvider.hashCode();
        return result;
    }

    /**
     * equals implementation for Chat Room.
     *
     * @param obj other instance
     * @return returns true if equal or false if not
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        ChatRoomIrcImpl other = (ChatRoomIrcImpl) obj;
        if (!parentProvider.equals(other.parentProvider))
        {
            return false;
        }
        if (!chatRoomName.equals(other.chatRoomName))
        {
            return false;
        }
        return true;
    }

    /**
     * Returns the name of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the name of this <tt>ChatRoom</tt>.
     */
    public String getName()
    {
        return chatRoomName;
    }

    /**
     * Returns the identifier of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the identifier of this
     * <tt>ChatRoom</tt>.
     */
    public String getIdentifier()
    {
        return chatRoomName;
    }

    /**
     * Adds a <tt>ChatRoomMember</tt> to the list of members of this chat room.
     *
     * @param memberID the identifier of the member
     * @param member the <tt>ChatRoomMember</tt> to add.
     */
    protected void addChatRoomMember(final String memberID,
        final ChatRoomMember member)
    {
        chatRoomMembers.put(memberID, member);
    }

    /**
     * Removes a <tt>ChatRoomMember</tt> from the list of members of this chat
     * room.
     *
     * @param memberID the name of the <tt>ChatRoomMember</tt> to remove.
     */
    protected void removeChatRoomMember(final String memberID)
    {
        chatRoomMembers.remove(memberID);
    }

    /**
     * Joins this chat room with the nickname of the local user so that the user
     * would start receiving events and messages for it.
     *
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void join() throws OperationFailedException
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null || !connection.isConnected())
        {
            throw new OperationFailedException(
                "We are currently not connected to the server.",
                OperationFailedException.NETWORK_FAILURE);
        }

        if (connection.getChannelManager().isJoined(this))
        {
            throw new OperationFailedException("Channel is already joined.",
                OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
        }

        try
        {
            connection.getChannelManager().join(this);
        }
        catch (final IllegalArgumentException e)
        {
            throw new OperationFailedException(e.getMessage(),
                OperationFailedException.CHAT_ROOM_NOT_JOINED, e);
        }
    }

    /**
     * Joins this chat room so that the user would start receiving events and
     * messages for it. The method uses the nickname of the local user and the
     * specified password in order to enter the chatroom.
     *
     * @param password the password to use when authenticating on the chatroom.
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void join(final byte[] password) throws OperationFailedException
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new OperationFailedException(
                "We are currently not connected to the server.",
                OperationFailedException.NETWORK_FAILURE);
        }

        if (connection.getChannelManager().isJoined(this))
        {
            throw new OperationFailedException("Channel is already joined.",
                OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
        }

        try
        {
            connection.getChannelManager().join(this, password.toString());
        }
        catch (final IllegalArgumentException e)
        {
            throw new OperationFailedException(e.getMessage(),
                OperationFailedException.CHAT_ROOM_NOT_JOINED, e);
        }
    }

    /**
     * Joins this chat room with the specified nickname so that the user would
     * start receiving events and messages for it. If the chat room already
     * contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * The provided nick name is ignored, since IRC does not support nick
     * changes limited to a single chat room.
     *
     * @param nickname the nickname to use.
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void joinAs(final String nickname) throws OperationFailedException
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Not setting nick name upon chat room join, since a "
                + "nick change is not limited to a single chat room.");
        }
        this.join();
    }

    /**
     * Joins this chat room with the specified nickname and password so that the
     * user would start receiving events and messages for it. If the chatroom
     * already contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     *
     * The provided nick name is ignored, since IRC does not support nick
     * changes limited to a single chat room.
     *
     * @param nickname the nickname to use.
     * @param password a password necessary to authenticate when joining the
     *            room.
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void joinAs(final String nickname, final byte[] password)
        throws OperationFailedException
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Not setting nick name upon chat room join, since a "
                + "nick change is not limited to a single chat room.");
        }
        this.join(password);
    }

    /**
     * Returns true if the local user is currently in the multi user chat (after
     * calling one of the {@link #join()} methods).
     *
     * @return true if currently we're currently in this chat room and false
     *         otherwise.
     */
    public boolean isJoined()
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        return connection != null
            && connection.getChannelManager().isJoined(this);
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
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            return;
        }
        connection.getChannelManager().leave(this);
        this.chatRoomMembers.clear();
    }

    /**
     * Returns the list of banned chat room members.
     * @return the list of banned chat room members.
     *
     * @throws OperationFailedException if we are not joined or we don't have
     * enough privileges to obtain the ban list.
     */
    public Iterator<ChatRoomMember> getBanList()
        throws OperationFailedException
    {
        return bannedMembers.iterator();
    }

    /**
     * Bans the given <tt>ChatRoomMember</tt>.
     *
     * @param chatRoomMember the chat room member to ban
     * @param reason the reason of the ban
     * @throws OperationFailedException if we are not joined or we don't have
     * enough privileges to ban a participant.
     */
    public void banParticipant(final ChatRoomMember chatRoomMember,
        final String reason) throws OperationFailedException
    {
        if (!(chatRoomMember instanceof ChatRoomMemberIrcImpl))
        {
            LOGGER
                .trace("Cannot ban chat room member that is not an instance of "
                    + ChatRoomMemberIrcImpl.class.getCanonicalName());
            return;
        }
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().banParticipant(this,
            (ChatRoomMemberIrcImpl) chatRoomMember, reason);
    }

    /**
     * Kicks the given <tt>ChatRoomMember</tt>.
     *
     * @param chatRoomMember the chat room member to kick
     * @param reason the reason of the kick
     * @throws OperationFailedException if we are not joined or we don't have
     * enough privileges to kick a participant.
     */
    public void kickParticipant(final ChatRoomMember chatRoomMember,
        final String reason) throws OperationFailedException
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().kickParticipant(this, chatRoomMember,
            reason);
    }

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
        throws OperationFailedException
    {
        throw new OperationFailedException(
            "The configuration form is not yet implemented for irc.",
            OperationFailedException.GENERAL_ERROR);
    }

    /**
     * Adds <tt>listener</tt> to the list of listeners registered to receive
     * events upon modification of chat room properties such as its subject for
     * example.
     *
     * @param listener ChatRoomChangeListener
     */
    public void addPropertyChangeListener(
        final ChatRoomPropertyChangeListener listener)
    {
        synchronized (propertyChangeListeners)
        {
            if (!propertyChangeListeners.contains(listener))
            {
                propertyChangeListeners.add(listener);
            }
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of listeners current
     * registered for chat room modification events.
     *
     * @param listener the <tt>ChatRoomChangeListener</tt> to remove.
     */
    public void removePropertyChangeListener(
        final ChatRoomPropertyChangeListener listener)
    {
        synchronized (propertyChangeListeners)
        {
            propertyChangeListeners.remove(listener);
        }
    }

    /**
     * Adds the given <tt>listener</tt> to the list of listeners registered to
     * receive events upon modification of chat room member properties such as
     * its nickname being changed for example.
     *
     * @param listener the <tt>ChatRoomMemberPropertyChangeListener</tt>
     * that is to be registered for <tt>ChatRoomMemberPropertyChangeEvent</tt>s.
     */
    public void addMemberPropertyChangeListener(
        final ChatRoomMemberPropertyChangeListener listener)
    {
        synchronized (memberPropChangeListeners)
        {
            if (!memberPropChangeListeners.contains(listener))
            {
                memberPropChangeListeners.add(listener);
            }
        }
    }

    /**
     * Removes the given <tt>listener</tt> from the list of listeners currently
     * registered for chat room member property change events.
     *
     * @param listener the <tt>ChatRoomMemberPropertyChangeListener</tt> to
     * remove.
     */
    public void removeMemberPropertyChangeListener(
        final ChatRoomMemberPropertyChangeListener listener)
    {
        synchronized (memberPropChangeListeners)
        {
            memberPropChangeListeners.remove(listener);
        }
    }

    /**
     * Adds a listener that will be notified of changes of a member role in the
     * room such as being granted operator.
     *
     * @param listener a member role listener.
     */
    public void addMemberRoleListener(final ChatRoomMemberRoleListener listener)
    {
        synchronized (memberRoleListeners)
        {
            if (!memberRoleListeners.contains(listener))
            {
                memberRoleListeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener that was being notified of changes of a member role in
     * this chat room such as us being granded operator.
     *
     * @param listener a member role listener.
     */
    public void removeMemberRoleListener(
        final ChatRoomMemberRoleListener listener)
    {
        synchronized (memberRoleListeners)
        {
            if (memberRoleListeners.contains(listener))
            {
                memberRoleListeners.remove(listener);
            }
        }
    }

    /**
     * Adds a listener that will be notified of changes in our role in the room
     * such as us being granded operator.
     *
     * @param listener a local user role listener.
     */
    public void addLocalUserRoleListener(
        final ChatRoomLocalUserRoleListener listener)
    {
        synchronized (localUserRoleListeners)
        {
            if (!localUserRoleListeners.contains(listener))
            {
                localUserRoleListeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener that was being notified of changes in our role in this
     * chat room such as us being granted operator.
     *
     * @param listener a local user role listener.
     */
    public void removelocalUserRoleListener(
        final ChatRoomLocalUserRoleListener listener)
    {
        synchronized (localUserRoleListeners)
        {
            if (localUserRoleListeners.contains(listener))
            {
                localUserRoleListeners.remove(listener);
            }
        }
    }

    /**
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     * <p>
     * To be notified every time the room's subject change you should add a
     * <tt>ChatRoomPropertyChangelistener</tt> to this room.
     * <p>
     *
     * To change the room's subject use {@link #setSubject(String)}.
     *
     * @return the room subject or <tt>null</tt> if the user hasn't joined the
     *         room or the room does not have a subject yet.
     */
    public String getSubject()
    {
        return chatSubject;
    }

    /**
     * Sets the subject of this chat room. If the user does not have the right
     * to change the room subject, or the protocol does not support this, or the
     * operation fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code.
     *
     * @param subject the new subject that we'd like this room to have
     * @throws OperationFailedException thrown if the user is not joined to the
     * channel or if he/she doesn't have enough privileges to change the
     * topic or if the topic is null.
     */
    public void setSubject(final String subject)
        throws OperationFailedException
    {
        try
        {
            final IrcConnection connection =
                this.parentProvider.getIrcStack().getConnection();
            if (connection == null)
            {
                throw new IllegalStateException("Connection is not available.");
            }
            connection.getChannelManager().setSubject(this, subject);
        }
        catch (RuntimeException e)
        {
            if (e.getCause() instanceof IOException)
            {
                throw new OperationFailedException("Failed to change subject.",
                    OperationFailedException.NETWORK_FAILURE, e.getCause());
            }

            throw new OperationFailedException("Failed to change subject.",
                OperationFailedException.GENERAL_ERROR, e);
        }
    }

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user
     */
    public String getUserNickname()
    {
        // User's nick name is determined by the server connection, not the
        // individual chat rooms.
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        return connection.getIdentityManager().getNick();
    }

    /**
     * Changes the the local user's nickname in the context of this chat room.
     * If the operation is not supported by the underlying implementation, the
     * method throws an OperationFailedException with the corresponding code.
     *
     * @param nickName the new nickname within the room.
     *
     * @throws OperationFailedException if the setting the new nickname changes
     *             for some reason.
     */
    @Override
    public void setUserNickname(final String nickName)
        throws OperationFailedException
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new OperationFailedException(
                "IRC connection is not established.",
                OperationFailedException.NETWORK_FAILURE);
        }
        connection.getIdentityManager().setNick(nickName);
    }

    /**
     * Adds a listener that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     *
     * @param listener a participant status listener.
     */
    public void addMemberPresenceListener(
        final ChatRoomMemberPresenceListener listener)
    {
        synchronized (memberListeners)
        {
            if (!memberListeners.contains(listener))
            {
                memberListeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener that was being notified of changes in the status of
     * other chat room participants such as users being kicked, banned, or
     * granted admin permissions.
     *
     * @param listener a participant status listener.
     */
    public void removeMemberPresenceListener(
        final ChatRoomMemberPresenceListener listener)
    {
        synchronized (memberListeners)
        {
            memberListeners.remove(listener);
        }
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time
     * a new message is received on this chat room.
     *
     * @param listener a <tt>MessageListener</tt> that would be notified every
     *            time a new message is received on this chat room.
     */
    public void addMessageListener(final ChatRoomMessageListener listener)
    {
        synchronized (messageListeners)
        {
            if (!messageListeners.contains(listener))
            {
                messageListeners.add(listener);
            }
        }
    }

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     *
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(final ChatRoomMessageListener listener)
    {
        synchronized (messageListeners)
        {
            if (messageListeners.contains(listener))
            {
                messageListeners.remove(messageListeners.indexOf(listener));
            }
        }
    }


    /**
     * Returns the <tt>ChatRoomMember</tt> corresponding to the given member id.
     * If no member is found for the given id, returns NULL.
     *
     * @param memberID the identifier of the member
     * @return the <tt>ChatRoomMember</tt> corresponding to the given member id.
     */
    public ChatRoomMember getChatRoomMember(final String memberID)
    {
        return chatRoomMembers.get(memberID);
    }

    /**
     * Removes all chat room members from the list.
     */
    protected void clearChatRoomMemberList()
    {
        synchronized (chatRoomMembers)
        {
            chatRoomMembers.clear();
        }
    }

    /**
     * Invites another user to this room. If we're not joined nothing will
     * happen.
     *
     * @param userAddress the address of the user to invite to the room.(one may
     *            also invite users not on their contact list).
     * @param reason a reason, subject, or welcome message that would tell the
     *            the user why they are being invited.
     */
    @Override
    public void invite(final String userAddress, final String reason)
    {
        // TODO Check if channel status is invite-only (+i). If this is the
        // case, user has to be channel operator in order to be able to invite
        // some-one.
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().invite(userAddress, this);
    }

    /**
     * Returns a <tt>List</tt> of <tt>ChatRoomMembers</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>Contact</tt> corresponding to all room
     *         members.
     */
    public List<ChatRoomMember> getMembers()
    {
        return new ArrayList<ChatRoomMember>(chatRoomMembers.values());
    }

    /**
     * Returns the number of participants that are currently in this chat room.
     *
     * @return the number of <tt>Contact</tt>s, currently participating in this
     * room.
     */
    public int getMembersCount()
    {
        return chatRoomMembers.size();
    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now
     *            subject.
     * @return the newly created message.
     */
    @Override
    public Message createMessage(final byte[] content, final String contentType,
        final String contentEncoding, final String subject)
    {
        Message msg =
            new MessageIrcImpl(new String(content), contentType,
                contentEncoding, subject);

        return msg;
    }

    /**
     * Create a Message instance for sending a simple text messages with default
     * (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    @Override
    public Message createMessage(final String messageText)
    {
        Message mess = new MessageIrcImpl(
            messageText,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING,
            null);

        return mess;
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param message the <tt>Message</tt> to send.
     * @throws OperationFailedException if the underlying stack is not
     * registered or initialized or if the chat room is not joined.
     */
    @Override
    public void sendMessage(final Message message)
        throws OperationFailedException
    {
        assertConnected();

        String[] splitMessages = message.getContent().split("\n");

        String messagePortion = null;
        for (int i = 0; i < splitMessages.length; i++)
        {
            messagePortion = splitMessages[i];

            // As we only send one message per line, we ignore empty lines in
            // the incoming multi line message.
            if (messagePortion.equals("\n") || messagePortion.matches("[\\ ]*"))
            {
                continue;
            }

            final IrcConnection connection =
                this.parentProvider.getIrcStack().getConnection();
            if (connection == null)
            {
                throw new IllegalStateException("Connection is not available.");
            }
            if (((MessageIrcImpl) message).isCommand())
            {
                try
                {
                    connection.getMessageManager()
                        .command(this, messagePortion);
                    this.fireMessageReceivedEvent(message, this.user,
                        new Date(),
                        ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
                }
                catch (final UnsupportedCommandException e)
                {
                    this.fireMessageDeliveryFailedEvent(
                        ChatRoomMessageDeliveryFailedEvent
                            .UNSUPPORTED_OPERATION,
                        e.getMessage(), new Date(), message);
                }
                catch (BadCommandException e)
                {
                    LOGGER.error("An error occurred while constructing "
                        + "the command. This is most likely due to a bug "
                        + "in the implementation of the command. Message: "
                        + message + "'", e);
                    this.fireMessageDeliveryFailedEvent(
                        ChatRoomMessageDeliveryFailedEvent.INTERNAL_ERROR,
                        "Command cannot be executed. This is most likely due "
                            + "to a bug in the implementation.", new Date(),
                        message);
                }
                catch (BadCommandInvocationException e)
                {
                    StringBuilder helpText = new StringBuilder();
                    if (e.getCause() != null) {
                        helpText.append(e.getCause().getMessage());
                        helpText.append('\n');
                    }
                    helpText.append(e.getHelp());
                    MessageIrcImpl helpMessage =
                        new MessageIrcImpl(
                            helpText.toString(),
                            OperationSetBasicInstantMessaging
                                .DEFAULT_MIME_TYPE,
                            OperationSetBasicInstantMessaging
                                .DEFAULT_MIME_ENCODING,
                            "Command usage:");
                    this.fireMessageReceivedEvent(helpMessage, this.user,
                        new Date(),
                        MessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED);
                }
            }
            else
            {
                connection.getMessageManager().message(this, messagePortion);
                this.fireMessageDeliveredEvent(new MessageIrcImpl(
                    messagePortion, message.getContentType(), message
                        .getEncoding(), message.getSubject()));
            }
        }
    }

    /**
     * Returns the protocol provider service that created us.
     *
     * @return the protocol provider service that created us.
     */
    public ProtocolProviderService getParentProvider()
    {
        return parentProvider;
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     *
     * @throws java.lang.IllegalStateException if the underlying stack is not
     *             registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (parentProvider == null)
        {
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                + "service before being able to communicate.");
        }
        if (!parentProvider.isRegistered())
        {
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                + "being able to communicate.");
        }
    }

    /**
     * Notifies all interested listeners that a
     * <tt>ChatRoomMessageDeliveredEvent</tt> has been fired.
     *
     * @param message the delivered message
     */
    private void fireMessageDeliveredEvent(final Message message)
    {
        int eventType
            = ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED;

        MessageIrcImpl msg = (MessageIrcImpl) message;

        if (msg.isAction())
        {
            eventType = ChatRoomMessageDeliveredEvent.ACTION_MESSAGE_DELIVERED;

            if (msg.getContent().indexOf(' ') != -1)
            {
                msg.setContent(
                    msg.getContent()
                        .substring(message.getContent().indexOf(' ')));
            }
        }

        ChatRoomMessageDeliveredEvent msgDeliveredEvt
            = new ChatRoomMessageDeliveredEvent(this,
                                                new Date(),
                                                msg,
                                                eventType);

        Iterable<ChatRoomMessageListener> listeners;
        synchronized (messageListeners)
        {
            listeners
                = new ArrayList<ChatRoomMessageListener>(messageListeners);
        }

        for (ChatRoomMessageListener listener : listeners)
        {
            try
            {
                listener.messageDelivered(msgDeliveredEvt);
            }
            catch (RuntimeException e)
            {
                LOGGER.error(String.format(
                    "Listener '%s' threw a runtime exception during execution."
                        + " This is probably due to a bug in the listener's "
                        + "implementation.",
                    listener.getClass().getCanonicalName()),
                    e);
            }
        }
    }

    /**
     * Notifies all interested listeners that a
     * <tt>ChatRoomMessageReceivedEvent</tt> has been fired.
     *
     * @param message the received message
     * @param fromMember the <tt>ChatRoomMember</tt>, which is the sender of the
     * message
     * @param date the time at which the message has been received
     * @param eventType the type of the received event. One of the
     * XXX_MESSAGE_RECEIVED constants declared in the
     * <tt>ChatRoomMessageReceivedEvent</tt> class.
     */
    public void fireMessageReceivedEvent(final Message message,
        final ChatRoomMember fromMember, final Date date, final int eventType)
    {
        ChatRoomMessageReceivedEvent event =
            new ChatRoomMessageReceivedEvent(this, fromMember, date, message,
                eventType);

        Iterable<ChatRoomMessageListener> listeners;
        synchronized (messageListeners)
        {
            listeners
                = new ArrayList<ChatRoomMessageListener>(messageListeners);
        }

        for (ChatRoomMessageListener listener : listeners)
        {
            try
            {
                listener.messageReceived(event);
            }
            catch (RuntimeException e)
            {
                LOGGER.error(String.format(
                    "Listener '%s' threw a runtime exception during execution."
                        + " This is probably due to a bug in the listener's "
                        + "implementation.",
                    listener.getClass().getCanonicalName()),
                    e);
            }
        }
    }

    /**
     * Notifies interested listeners that a message delivery has failed.
     *
     * @param errorCode the type of error that occurred
     * @param reason the reason of delivery failure
     * @param date the date the event was received
     * @param message the message that was failed to be delivered
     */
    public void fireMessageDeliveryFailedEvent(final int errorCode,
        final String reason, final Date date, final Message message)
    {
        final ChatRoomMessageDeliveryFailedEvent event =
            new ChatRoomMessageDeliveryFailedEvent(this, null, errorCode,
                reason, date, message);

        final Iterable<ChatRoomMessageListener> listeners;
        synchronized (messageListeners)
        {
            listeners
                = new ArrayList<ChatRoomMessageListener>(messageListeners);
        }

        for (final ChatRoomMessageListener listener : listeners)
        {
            try
            {
                listener.messageDeliveryFailed(event);
            }
            catch (RuntimeException e)
            {
                LOGGER.error(String.format(
                    "Listener '%s' threw a runtime exception during execution."
                        + " This is probably due to a bug in the listener's "
                        + "implementation.",
                    listener.getClass().getCanonicalName()),
                    e);
            }
        }
    }

    /**
     * Delivers the specified event to all registered property change listeners.
     *
     * @param evt the <tt>PropertyChangeEvent</tt> that we'd like delivered to
     * all registered property change listeners.
     */
    public void firePropertyChangeEvent(final PropertyChangeEvent evt)
    {
        Iterable<ChatRoomPropertyChangeListener> listeners;
        synchronized (propertyChangeListeners)
        {
            listeners
                = new ArrayList<ChatRoomPropertyChangeListener>(
                        propertyChangeListeners);
        }

        for (ChatRoomPropertyChangeListener listener : listeners)
        {
            if (evt instanceof ChatRoomPropertyChangeEvent)
            {
                listener.chatRoomPropertyChanged(
                    (ChatRoomPropertyChangeEvent) evt);
            }
            else if (evt instanceof ChatRoomPropertyChangeFailedEvent)
            {
                listener.chatRoomPropertyChangeFailed(
                    (ChatRoomPropertyChangeFailedEvent) evt);
            }
        }
    }

    /**
     * Delivers the specified event to all registered property change listeners.
     *
     * @param evt the <tt>ChatRoomMemberPropertyChangeEvent</tt> that we'd like
     * deliver to all registered member property change listeners.
     */
    public void fireMemberPropertyChangeEvent(
        final ChatRoomMemberPropertyChangeEvent evt)
    {
        Iterable<ChatRoomMemberPropertyChangeListener> listeners;
        synchronized (memberPropChangeListeners)
        {
            listeners
                = new ArrayList<ChatRoomMemberPropertyChangeListener>(
                        memberPropChangeListeners);
        }

        for (ChatRoomMemberPropertyChangeListener listener : listeners)
        {
            listener.chatRoomPropertyChanged(evt);
        }
    }

    /**
     * Creates the corresponding ChatRoomMemberPresenceChangeEvent and notifies
     * all <tt>ChatRoomMemberPresenceListener</tt>s that a ChatRoomMember has
     * joined or left this <tt>ChatRoom</tt>.
     *
     * @param member the <tt>ChatRoomMember</tt> that this event is about
     * @param actorMember a member that act in the event (for example the kicker
     * in a member kicked event)
     * @param eventID the identifier of the event
     * @param eventReason the reason of the event
     */
    public void fireMemberPresenceEvent(final ChatRoomMember member,
        final ChatRoomMember actorMember, final String eventID,
        final String eventReason)
    {
        // First update local state w.r.t. member presence change
        if (eventID == ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED)
        {
            addChatRoomMember(member.getContactAddress(), member);
        }
        else
        {
            removeChatRoomMember(member.getContactAddress());
        }

        ChatRoomMemberPresenceChangeEvent evt;
        if (actorMember != null)
        {
            evt = new ChatRoomMemberPresenceChangeEvent(
                this, member, actorMember, eventID, eventReason);
        }
        else
        {
            evt = new ChatRoomMemberPresenceChangeEvent(
                this, member, eventID, eventReason);
        }

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Will dispatch the following ChatRoom event: " + evt);
        }

        Iterable<ChatRoomMemberPresenceListener> listeners;
        synchronized (memberListeners)
        {
            listeners
                = new ArrayList<ChatRoomMemberPresenceListener>(
                        memberListeners);
        }
        for (ChatRoomMemberPresenceListener listener : listeners)
        {
            listener.memberPresenceChanged(evt);
        }
    }

    /**
     * Creates the corresponding ChatRoomMemberRoleChangeEvent and notifies
     * all <tt>ChatRoomMemberRoleListener</tt>s that a ChatRoomMember has
     * changed his role in this <tt>ChatRoom</tt>.
     *
     * @param member the <tt>ChatRoomMember</tt> that this event is about
     * @param newRole the new role of the given member
     */
    public void fireMemberRoleEvent(final ChatRoomMember member,
        final ChatRoomMemberRole newRole)
    {
        member.setRole(newRole);
        ChatRoomMemberRole previousRole = member.getRole();

        ChatRoomMemberRoleChangeEvent evt
            = new ChatRoomMemberRoleChangeEvent(this,
                                                member,
                                                previousRole,
                                                newRole);

        if (LOGGER.isTraceEnabled())
        {
            LOGGER.trace("Will dispatch the following ChatRoom event: " + evt);
        }

        Iterable<ChatRoomMemberRoleListener> listeners;
        synchronized (memberRoleListeners)
        {
            listeners
                = new ArrayList<ChatRoomMemberRoleListener>(
                        memberRoleListeners);
        }

        for (ChatRoomMemberRoleListener listener : listeners)
        {
            listener.memberRoleChanged(evt);
        }
    }

    /**
     * Notify all <tt>ChatRoomLocalUserRoleListener</tt>s that the local user's
     * role has been changed in this <tt>ChatRoom</tt>.
     *
     * @param event the event that describes the local user's role change
     */
    public void fireLocalUserRoleChangedEvent(
        final ChatRoomLocalUserRoleChangeEvent event)
    {
        ArrayList<ChatRoomLocalUserRoleListener> listeners;
        synchronized (localUserRoleListeners)
        {
            listeners =
                new ArrayList<ChatRoomLocalUserRoleListener>(
                    localUserRoleListeners);
        }

        for (ChatRoomLocalUserRoleListener listener : listeners)
        {
            listener.localUserRoleChanged(event);
        }
    }

    /**
     * Indicates whether or not this chat room is corresponding to a server
     * channel.
     *
     * @return <code>true</code> to indicate that this chat room is
     * corresponding to a server channel, <code>false</code> - otherwise.
     */
    @Override
    public boolean isSystem()
    {
        return isSystem;
    }

    /**
     * Sets whether or not this chat room is corresponding to a server
     * channel.
     *
     * @param isSystem <code>true</code> to indicate that this chat room is
     * corresponding to a server channel, <code>false</code> - otherwise.
     */
    protected void setSystem(final boolean isSystem)
    {
        this.isSystem = isSystem;
    }

    /**
     * Sets the subject obtained from the server once we're connected.
     *
     * @param subject the subject to set
     */
    protected void setSubjectFromServer(final String subject)
    {
        this.chatSubject = subject;
    }

    /**
     * Determines whether this chat room should be stored in the configuration
     * file or not. If the chat room is persistent it still will be shown after
     * a restart in the chat room list. A non-persistent chat room will be only
     * in the chat room list until the the program is running.
     *
     * @return true if this chat room is persistent, false otherwise
     */
    @Override
    public boolean isPersistent()
    {
        return true;
    }

    /**
     * Returns the local user role.
     * @return the local user role
     */
    @Override
    public ChatRoomMemberRole getUserRole()
    {
        if (this.user == null)
        {
            LOGGER.trace("User's chat room member instance is not set yet. "
                + "Assuming default role SILENT_MEMBER.");
            return ChatRoomMemberRole.SILENT_MEMBER;
        }
        return this.user.getRole();
    }

    /**
     * Method for setting chat room member instance representing the user.
     *
     * @param user instance representing the user. This instance cannot be null.
     */
    void setLocalUser(final ChatRoomMemberIrcImpl user)
    {
        if (user == null)
        {
            throw new IllegalArgumentException("user cannot be null");
        }
        this.user = user;
    }

    /**
     * Sets the local user role.
     *
     * No implementation is necessary for this. IRC server manages permissions.
     * If a new chat room is created then user will automatically receive the
     * appropriate role.
     *
     * @param role the role to set
     * @throws OperationFailedException if the operation don't succeed
     */
    @Override
    public void setLocalUserRole(final ChatRoomMemberRole role)
        throws OperationFailedException
    {
    }

    /**
     * Grants admin role to the participant given by <tt>address</tt>.
     * @param address the address of the participant to grant admin role to
     */
    @Override
    public void grantAdmin(final String address)
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().grant(this, address, Mode.OPERATOR);
    }

    /**
     * Grants membership role to the participant given by <tt>address</tt>.
     * @param address the address of the participant to grant membership role to
     */
    @Override
    public void grantMembership(final String address)
    {
        // TODO currently Voice == Membership.
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().grant(this, address, Mode.VOICE);
    }

    /**
     * Grants moderator role to the participant given by <tt>address</tt>.
     * @param address the address of the participant to grant moderator role to
     */
    @Override
    public void grantModerator(final String address)
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().grant(this, address, Mode.HALFOP);
    }

    /**
     * Grants ownership role to the participant given by <tt>address</tt>.
     * @param address the address of the participant to grant ownership role to
     */
    @Override
    public void grantOwnership(final String address)
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().grant(this, address, Mode.OWNER);
    }

    /**
     * Grants voice to the participant given by <tt>address</tt>.
     * @param address the address of the participant to grant voice to
     */
    @Override
    public void grantVoice(final String address)
    {
        // TODO currently Voice == Membership.
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().grant(this, address, Mode.VOICE);
    }

    /**
     * Revokes the admin role for the participant given by <tt>address</tt>.
     * @param address the address of the participant to revoke admin role for
     */
    @Override
    public void revokeAdmin(final String address)
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().revoke(this, address, Mode.OPERATOR);
    }

    /**
     * Revokes the membership role for the participant given by <tt>address</tt>
     * .
     *
     * @param address the address of the participant to revoke membership role
     *            for
     */
    @Override
    public void revokeMembership(final String address)
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().revoke(this, address, Mode.VOICE);
    }

    /**
     * Revokes the moderator role for the participant given by <tt>address</tt>.
     * @param address the address of the participant to revoke moderator role
     * for
     */
    @Override
    public void revokeModerator(final String address)
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().revoke(this, address, Mode.HALFOP);
    }

    /**
     * Revokes the ownership role for the participant given by <tt>address</tt>.
     * @param address the address of the participant to revoke ownership role
     * for
     */
    @Override
    public void revokeOwnership(final String address)
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().revoke(this, address, Mode.OWNER);
    }

    /**
     * Revokes the voice for the participant given by <tt>address</tt>.
     * @param address the address of the participant to revoke voice for
     */
    @Override
    public void revokeVoice(final String address)
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        connection.getChannelManager().revoke(this, address, Mode.VOICE);
    }

    /**
     * {@inheritDoc}
     *
     * Not implemented.
     */
    @Override
    public ConferenceDescription publishConference(
        final ConferenceDescription cd, final String name)
    {
        return null;
    }

    /**
     * Find the Contact instance corresponding to the specified chat room
     * member. Since every chat room member is also a private contact, we will
     * create an instance if it cannot be found.
     *
     * @param name nick name of the chat room member
     * @return returns Contact instance corresponding to specified chat room
     *         member
     */
    @Override
    public Contact getPrivateContactByNickname(final String name)
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Getting private contact for nick name '" + name
                + "'.");
        }
        // TODO Also register contact address as interesting contact for
        // presence status updates at IrcConnection.PresenceManager.
        return this.parentProvider.getPersistentPresence()
            .findOrCreateContactByID(name);
    }

    /**
     * IRC does not provide continuous presence status updates, so no
     * implementation is necessary.
     *
     * @param nickname nick name to look up
     */
    @Override
    public void updatePrivateContactPresenceStatus(final String nickname)
    {
    }

    /**
     * IRC does not provide continuous presence status updates, so no
     * implementation is necessary.
     *
     * @param sourceContact contact to look up
     */
    @Override
    public void updatePrivateContactPresenceStatus(final Contact sourceContact)
    {
    }

    /**
     * IRC chat rooms cannot be destroyed. That is the way IRC works and there
     * is no need to cause a panic, so just return true.
     *
     * @param reason the reason for destroying.
     * @param alternateAddress the alternate address
     * @return <tt>true</tt> if the room is destroyed.
     */
    public boolean destroy(final String reason, final String alternateAddress)
    {
        return true;
    }

    /**
     * Returns the ids of the users that has the member role in the room. When
     * the room is member only, this are the users allowed to join.
     *
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
    public void setMembersWhiteList(final List<String> members)
    {
    }

    /**
     * Update the subject for this chat room.
     *
     * @param subject the subject
     */
    void updateSubject(final String subject)
    {
        if (this.chatSubject.equals(subject))
        {
            return;
        }
        final String previous =
            this.chatSubject == null ? "" : this.chatSubject;
        this.chatSubject = subject;
        ChatRoomPropertyChangeEvent topicChangeEvent =
            new ChatRoomPropertyChangeEvent(this,
                ChatRoomPropertyChangeEvent.CHAT_ROOM_SUBJECT, previous,
                subject);
        firePropertyChangeEvent(topicChangeEvent);
    }

    /**
     * Update the ChatRoomMember instance. When the nick changes, the chat room
     * member is still stored under the old nick. Find the instance under its
     * old nick and reinsert it into the map according to the current nick name.
     *
     * @param oldName The old nick name under which the member instance is
     *            currently stored.
     */
    void updateChatRoomMemberName(final String oldName)
    {
        synchronized (this.chatRoomMembers)
        {
            ChatRoomMember member = this.chatRoomMembers.remove(oldName);
            if (member != null)
            {
                this.chatRoomMembers.put(member.getContactAddress(), member);
            }
        }
    }
}
