/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;
import java.util.*;

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
     * @throws OperationFailedException
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
     * Changes the the local user's nickname in the context of this chatroom.
     * If the operation is not supported by the underlying implementation, the
     * method throws an OperationFailedException with the corresponding code.
     *
     * @param nickname the new nickname within the room.
     *
     * @throws OperationFailedException if the setting the new nickname changes
     * for some reason.
     */
    public void setUserNickname(String nickname)
       throws OperationFailedException;

    /**
     * Indicates if this <tt>ChatRoom</tt> is visible to everyone and everyone
     * could join it. If the <tt>ChatRoom</tt> is NOT visible, it cannot be seen
     * without knowing the exact name.
     * 
     * @return <code>true</code> if this <tt>ChatRoom</tt> is visible to
     * everyone, otherwise - <code>false</code>.
     */
    public boolean isVisible();
    
    /**
     * Sets the property indicating if this <tt>ChatRoom</tt> is visible to
     * everyone and everyone could join it. If the <tt>ChatRoom</tt> is NOT
     * visible, it cannot be seen without knowing the exact name.
     *  
     * @param isVisible indicates if this <tt>ChatRoom</tt> should be visible or
     * not 
     */
    public void setVisible(boolean isVisible);
    
    /**
     * Indicates if this <tt>ChatRoom</tt> requires a password to be joined.
     * 
     * @return <code>true</code> if this <tt>ChatRoom</tt> requires a password
     * to be joined, otherwise - <code>false</code>
     */
    public boolean isPasswordRequired();
    
    /**
     * Sets the property indicating if this <tt>ChatRoom</tt> requires a
     * password to be joined.
     * 
     * @param isPasswordRequired indicates if this <tt>ChatRoom</tt> requires a
     * password to be joined.
     */
    public void setPasswordRequired(boolean isPasswordRequired);
    
    /**
     * Indicates if this <tt>ChatRoom</tt> requires an invitation. If the
     * chat room requires invitation, the user could not join it without being
     * invited.
     * 
     * @return <code>true</code> if this <tt>ChatRoom</tt> requires invitation
     * to joined, otherwise - <code>false</code>.
     */
    public boolean isInvitationRequired();
    
    /**
     * Sets the property indicating if this <tt>ChatRoom</tt> requires an
     * invitation to be joined. If the chat room requires invitation, the user
     * could not join it without being invited.
     * 
     * @param isInvitationRequired indicates if this <tt>ChatRoom</tt> requires
     * invitation to be joined
     */
    public void setInvitationRequired(boolean isInvitationRequired);
    
    /**
     * Indicates if this <tt>ChatRoom</tt> has a limit on number of users who
     * could join it.
     * 
     * @return <code>true</code> if the number of users who could join this
     * <tt>ChatRoom</tt> is limited, otherwise - <code>false</code> 
     */
    public boolean isMemberNumberLimited();
    
    /**
     * Sets the property inficating if this <tt>ChatRoom</tt> has a limit on
     * number of users who could join it.
     *  
     * @param isMemberNumberLimited indicates if this <tt>ChatRoom</tt> has a
     * limit on number of users who could join it
     */
    public void setMemberNumberLimited(boolean isMemberNumberLimited);
    
    /**
     * Indicates if this <tt>ChatRoom</tt> is in a mute mode. If a
     * <tt>ChatRoom</tt> is muted no messages could be obtained from it.
     * 
     * @return <code>true</code> if this <tt>ChatRoom</tt> is in a mute mode,
     * otherwise - <code>false</code>
     */
    public boolean isMute();
    
    /**
     * Sets the property indicating if this <tt>ChatRoom</tt> is in a mute mode.
     * If a <tt>ChatRoom</tt> is muted no messages could be obtained from it.
     * 
     * @param isMute indicates if this <tt>ChatRoom</tt> should be in a mute
     * mode or not
     */
    public void setMute(boolean isMute);
    
    /**
     * Indicates if it's possible for someone to send messages to this
     * <tt>ChatRoom</tt> even if they are not present inside the chat room.
     * 
     * @return <code>true</code> if this <tt>ChatRoom</tt> allows external
     * messages, otherwise - <code>false</code>
     */
    public boolean isAllowExternalMessages();
    
    /**
     * Sets the property indicating if it's possible for someone to send
     * messages to this <tt>ChatRoom</tt> even if they are not present inside
     * the chat room.
     * 
     * @param isAllowExternalMessages indicates if this <tt>ChatRoom</tt> should
     * allow external messages or not 
     */
    public void setAllowExternalMessages(boolean isAllowExternalMessages);
    
    /**
     * Indicates if this <tt>ChatRoom</tt> is registered.
     * 
     * @return <code>true</code> if this <tt>ChatRoom</tt> is registered,
     * otherwise - <code>false</code>
     */
    public boolean isRegistered();
    
    /**
     * Sets the property indicating if this <tt>ChatRoom</tt> is registered or
     * not.
     * 
     * @param isRegistered indicates if this <tt>ChatRoom</tt> is registered
     */
    public void setRegistered(boolean isRegistered);
    
    /**
     * Indicates if the subject of this <tt>ChatRoom</tt> could be changed by
     * non admin users. If the subject is locked only admin users could change
     * it.
     * 
     * @return <code>true</code> if only admin user could change the subject of
     * this <tt>ChatRoom</tt>, otherwise - <code>false</code>
     */
    public boolean isSubjectLocked();
    
    /**
     * Sets the property indicating if the subject of this <tt>ChatRoom</tt>
     * could be changed only by admin users.
     *  
     * @param isSubjectLocked indicates if the subject of this <tt>ChatRoom</tt>
     * is locked 
     */
    public void setSubjectLocked(boolean isSubjectLocked);
    
    /**
     * Indicates if the message format in this <tt>ChatRoom</tt> could be
     * modified. If the message formatting is allowed - we could have colored,
     * underlined, etc. messages.
     * 
     * @return <code>true</code> if message formatting in this <tt>ChatRoom</tt>
     * is allowed, otherwise - <code>false</code>
     */
    public boolean isAllowMessageFormatting();
    
    /**
     * Sets the property indicating if the message format in this
     * <tt>ChatRoom</tt> could be modified. If the message formatting is
     * allowed - we could have colored, underlined, etc. messages.
     *  
     * @param isAllowMessageFormatting indicates if the message formatting in
     * this <tt>ChatRoom</tt> is allowed
     */
    public void setAllowMessageFormatting(boolean isAllowMessageFormatting);
    
    /**
     * Indicates is this <tt>ChatRoom</tt> room is currently in a message
     * formatting filtered mode. The message formatting filtered mode means that
     * all formatted messages (colored, underlined, etc.) are seen in standard
     * format by other users.
     * 
     * @return <code>true</code> if this <tt>ChatRoom</tt> is in message
     * formatting filtered mode, otherwise - <code>false</code>
     */
    public boolean isFilterMessageFormatting();
    
    /**
     * Sets the property indicating if this <tt>ChatRoom</tt> room is currently
     * in a message formatting filtered mode. The message formatting filtered
     * mode means that all formatted messages (colored, underlined, etc.) are
     * seen in standard format by other users.
     *  
     * @param isFilterMessageFormatting indicates if this <tt>ChatRoom</tt>
     * is currently is a message formatting filtered mode
     */
    public void setFilterMessageFormatting(boolean isFilterMessageFormatting);
    
    /**
     * Indicates if users can only join this <tt>ChatRoom</tt> in a specified
     * interval of X seconds or the time of join is unlimited.
     * 
     * @return <code>true</code> if this <tt>ChatRoom</tt> could be joined only
     * in a specified interval of X seconds, otherwise - <code>false</code>
     */
    public boolean isJoinTimeLimited();
    
    /**
     * Sets the property indicating if users can only join this <tt>ChatRoom</tt>
     * in a specified interval of X seconds or the time of join is unlimited.
     * 
     * @param isJoinTimeLimited indicates if users can only join this
     * <tt>ChatRoom</tt> in a specified interval of X seconds or the time of
     * join is unlimited.
     */
    public void setJoinTimeLimited(boolean isJoinTimeLimited);
    
    /**
     * Indicates if sending invitation request is allowed in this
     * <tt>ChatRoom</tt>.
     * 
     * @return <code>true</code> if user is allowed to send invitation request
     * from this <tt>ChatRoom</tt>, otherwise - <code>false</code>
     */
    public boolean isAllowInvitationSend();
    
    /**
     * Sets the property indicating if sending invitation request is allowed in
     * this <tt>ChatRoom</tt>.
     * 
     * @param isAllowInvitationSend indicates if sending invitation request is
     * allowed in this <tt>ChatRoom</tt>
     */
    public void setAllowInvitationSend(boolean isAllowInvitationSend);
    
    /**
     * Indicates if receiving invitation request is allowed in this
     * <tt>ChatRoom</tt>.
     * 
     * @return <code>true</code> if user is allowed to receive invitation request
     * in this <tt>ChatRoom</tt>, otherwise - <code>false</code>
     */
    public boolean isAllowInvitationReceive();
    
    /**
     * Sets the property indicating if receiving invitation request is allowed
     * in this <tt>ChatRoom</tt>.
     * 
     * @param isAllowInvitationReceive indicates if receiving invitation request
     * is allowed in this <tt>ChatRoom</tt>
     */
    public void setAllowInvitationReceive(boolean isAllowInvitationReceive);
    
    /**
     * Indicates if users which join this <tt>ChatRoom</tt> are redirected to
     * another one.
     * 
     * @return <code>true</code> if users are redirected, otherwise -
     * <code>false</code>
     */
    public boolean isUserRedirected();
    
    /**
     * Sets the property indicating if users in this <tt>ChatRoom</tt> are
     * redirected to another one.
     * 
     * @param isRedirected indicates if users in this <tt>ChatRoom</tt> are
     * redirected to another one.
     */
    public void setUserRedirected(boolean isRedirected);
    
    /**
     * Indicates if users in this <tt>ChatRoom</tt> can or cannot change their
     * nickname.
     * 
     * @return <code>true</code> if users in this <tt>ChatRoom</tt> can NOT
     * change their nickname, otherwise - <code>false</code>
     */
    public boolean isUserNicknameLocked();
    
    /**
     * Sets the property indicating if users in this <tt>ChatRoom</tt> can or
     * cannot change their nickname.
     *  
     * @param isUserNicknameLocked indicates if nicknames in this
     * <tt>ChatRoom</tt> are locked and could not be changed by users or users
     * are free to modify their nick as they want
     */
    public void setUserNicknameLocked(boolean isUserNicknameLocked);
    
    /**
     * Indicates if kicks are allowed in this <tt>ChatRoom</tt>. A kick tells the
     * server to force a user to leave the chat room. Kick is a term inspired of
     * the IRC protocol.
     * 
     * @return <code>true</code> if kicks are allowed in this <tt>ChatRoom</tt>,
     * otherwise - <code>false</code>
     */
    public boolean isAllowKick();
    
    /**
     * Sets the property indicating if kicks are allowed in this
     * <tt>ChatRoom</tt>. A kick tells the server to force a user to leave the
     * chat room. Kick is a term inspired of the IRC protocol.
     * 
     * @param isAllowKick indicates if kicks are allowed in this
     * <tt>ChatRoom</tt> 
     */
    public void setAllowKick(boolean isAllowKick);
    
    /**
     * Indicates if only registered users can join this <tt>ChatRoom</tt> or
     * it's opened for everyone.
     * 
     * @return <code>true</code> if only registered users could join this
     * <tt>ChatRoom</tt>, otherwise - <code>false</code>
     */
    public boolean isRegisteredUserOnly();
    
    /**
     * Sets the property indicating if only registered users can join this
     * <tt>ChatRoom</tt> or it's opened for everyone.
     * 
     * @param isRegisteredUserOnly indicates if only registered users can join
     * this <tt>ChatRoom</tt> or it's opened for everyone.
     */
    public void setRegisteredUserOnly(boolean isRegisteredUserOnly);
    
    /**
     * Indicates if this <tt>ChatRoom</tt> allows special messages.
     * 
     * @return <code>true</code> if special messages are allowed in this
     * <tt>ChatRoom</tt>, otherwise - <code>false</code>
     */
    public boolean isAllowSpecialMessage();
    
    /**
     * Sets the property indicating if this <tt>ChatRoom</tt> allows special
     * messages.
     * 
     * @param isAllowSpecialMessage indicates this <tt>ChatRoom</tt> should
     * allow special messages
     */
    public void setAllowSpecialMessage(boolean isAllowSpecialMessage);
    
    /** 
     * Indicates if the list of nicknames in this chat room is currently
     * visible.
     * 
     * @return <code>true</code> if the list of nicknames in this
     * <tt>ChatRoom</tt> is visible, otherwise - <code>false</code>
     */
    public boolean isNicknameListVisible();
    
    /**
     * Sets the property indicating if the list of nicknames in this chat room
     * is currently visible.
     *  
     * @param isNicknameListVisible indicates if the list of nicknames in this
     * chat room should be visible.
     */
    public void setNicknameListVisible(boolean isNicknameListVisible);
    
    /**
     * Adds a listener that will be notified of changes in our participation in
     * the room such as us being kicked, join, left.
     *
     * @param listener a local user participation listener.
     */
    public void addLocalUserPresenceListener(
        ChatRoomLocalUserPresenceListener listener);

    /**
     * Removes a listener that was being notified of changes in our
     * participation in the room such as us being kicked, join, left...
     * 
     * @param listener a local user participation listener.
     */
    public void removeLocalUserPresenceListener(
        ChatRoomLocalUserPresenceListener listener);

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
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
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
     * Sets the password of this chat room. If the user does not have the right
     * to change the room password, or the protocol does not support this, or
     * the operation fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code.
     * 
     * @param password the new password that we'd like this room to have
     * @throws OperationFailedException if the user does not have the right to
     * change the room password, or the protocol does not support
     * this, or the operation fails for some other reason
     */
    public void setPassword(String password)
        throws OperationFailedException;

    /**
     * Returns the password of this chat room or null if the room doesn't have
     * password.
     * @return the password of this chat room or null if the room doesn't have
     * password
     */
    public String getPassword();

    /**
     * Adds a ban mask to the list of ban masks of this chat room. The ban mask
     * defines a group of users that will be banned. This property is meant to
     * be used mainly by IRC implementations. If the user does not have the
     * right to change the room ban list, or the protocol does not support this,
     * or the operation fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code.
     * 
     * @param banMask the new ban mask that we'd like to add to the room ban
     * list
     * @throws OperationFailedException if the user does not have the right to
     * change the room ban list, or the protocol does not support this, or the
     * operation fails for some other reason
     */
    public void addBanMask(String banMask)
        throws OperationFailedException;

    /**
     * Remove a ban mask from the list of ban masks of this chat room. If the
     * user does not have the right to change the room ban list, or the protocol
     * does not support this, or the operation fails for some other reason, the
     * method throws an <tt>OperationFailedException</tt> with the
     * corresponding code.
     * 
     * @param banMask the ban mask that we'd like to remove from this room ban
     * list
     * @throws OperationFailedException if the user does not have the right to
     * change the room ban list, or the protocol does not support this, or the
     * operation fails for some other reason
     */
    public void removeBanMask(String banMask)
        throws OperationFailedException;

    /**
     * Sets the user limit of this chat room. The user limit is the maximum
     * number of users, who could enter this chat room at a time. If the user
     * does not have the right to change the room user limit, or the protocol
     * does not support this, or the operation fails for some other reason, the
     * method throws an <tt>OperationFailedException</tt> with the
     * corresponding code.
     * 
     * @param userLimit the new user limit that we'd like this room to have
     * @throws OperationFailedException if the user does not have the right to
     * change the room user limit, or the protocol does not support this, or the
     * operation fails for some other reason
     */
    public void setUserLimit(int userLimit)
        throws OperationFailedException;

    /**
     * Returns the limit of user for this chat room. The user limit is the
     * maximum number of users, who could enter this chat room at a time.
     * 
     * @return int the limit of user for this chat room
     */
    public int getUserLimit();

    /**
     * Adds a configuration property to the configuration list of this chat
     * room. If the user does not have the right to change the room
     * configuration, or the protocol does not support this, or the operation
     * fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code.
     * 
     * @param propertyName the name of the configuration property to set
     * @param propertyValue the value to set to the given configuration property
     * @throws OperationFailedException if the user does not have the right to
     * change the room configuration, or the protocol does not support this, or
     * the operation fails for some other reason
     */
    public void addAdvancedConfigProperty(String propertyName,
        String propertyValue)
        throws OperationFailedException;

    /**
     * Removes a configuration property from this chat room configuration list.
     * If the user does not have the right to change the room state, or the
     * protocol does not support this, or the operation fails for some other
     * reason, the method throws an <tt>OperationFailedException</tt> with the
     * corresponding code.
     * 
     * @param propertyName the name of the configuration property to be removed
     * 
     * @throws OperationFailedException if the user does not have the right to
     * change the room configuration, or the protocol does not support this, or
     * the operation fails for some other reason
     */
    public void removeAdvancedConfigProperty(String propertyName)
        throws OperationFailedException;

    /**
     * Returns an Iterator over a set of <tt>ChatRoomConfigParams</tt>,
     * containing the current configuration of this chat room. This method is
     * meant to be used by bundles interested in what are the specific chat room
     * configurations.
     * 
     * @return a Map of (Property_Name, Property_Value) pairs, containing the
     * current configuration of this chat room
     */
    public Map getAdvancedConfigurationSet();

    /**
     * Returns an Iterator over a set of ban masks for this chat room. The ban
     * mask defines a group of users that will be banned. The ban list is a list
     * of all such ban masks defined for this chat room.
     * 
     * @return an Iterator over a set of ban masks for this chat room
     */
    public Iterator getBanList();

}
