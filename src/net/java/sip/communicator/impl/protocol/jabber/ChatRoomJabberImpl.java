/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Implements chat rooms for jabber. The class encapsulates instances of the
 * jive software <tt>MultiUserChat</tt>.
 *
 * @author Emil Ivov
 */
public class ChatRoomJabberImpl
    implements ChatRoom
{
    private static final Logger logger
        = Logger.getLogger(ChatRoomJabberImpl.class);

    /**
     * The multi user chat smack object that we encapsulate in this room.
     */
    private MultiUserChat multiUserChat = null;

    /**
     * The list of listeners registered for chat room property change events.
     */
    private Vector chatRoomPropertyChangeListeners = new Vector();

    /**
     * Listeners that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     */
    private Vector localUserStatusListeners = new Vector();

    /**
     * Listeners that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     */
    private Vector memberListeners = new Vector();

    /**
     * The protocol provider that created us
     */
    private ProtocolProviderServiceJabberImpl provider = null;

    /**
     * The operation set that crated us.
     */
    OperationSetMultiUserChatJabberImpl opSetMuc = null;

    /**
     * Listeners that will be notified every time
     * a new message is received on this chat room.
     */
    private Vector messageListeners = new Vector();

    /**
     * The members of this chat room.
     */
    private Hashtable members = new Hashtable();

    /**
     * Creates an instance of a chat room that has been.
     *
     * @param multiUserChat MultiUserChat
     * @param provider a reference to the currently valid jabber protocol
     * provider.
     */
    public ChatRoomJabberImpl(MultiUserChat multiUserChat,
                              ProtocolProviderServiceJabberImpl provider)
    {
        this.multiUserChat = multiUserChat;
        this.provider = provider;
        this.opSetMuc = (OperationSetMultiUserChatJabberImpl)provider
            .getOperationSet(OperationSetMultiUserChat.class);

        /** @todo add listeners to the chat room */
        /** @todo multiUserChat.addMessageListener(); */
        /** @todo multiUserChat.addParticipantListener();*/
        /** @todo multiUserChat.addParticipantStatusListener(); */
        /** @todo multiUserChat.addPresenceInterceptor(); */
        /** @todo multiUserChat.addSubjectUpdatedListener(); */
        /** @todo multiUserChat.addUserStatusListener(); */
        //multiUserChat.addParticipantStatusListener(this);
    }

    /**
     * Adds <tt>listener</tt> to the list of listeners registered to receive
     * events upon modification of chat room properties such as its subject
     * for example.
     *
     * @param listener the <tt>ChatRoomChangeListener</tt> that is to be
     * registered for <tt>ChatRoomChangeEvent</tt>-s.
     */
    public void addPropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        synchronized(chatRoomPropertyChangeListeners)
        {
            if (!chatRoomPropertyChangeListeners.contains(listener))
                chatRoomPropertyChangeListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of listeneres current
     * registered for chat room modification events.
     *
     * @param listener the <tt>ChatRoomChangeListener</tt> to remove.
     */
    public void removePropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        synchronized(chatRoomPropertyChangeListeners)
        {
            chatRoomPropertyChangeListeners.remove(listener);
        }
    }

    /**
     * Adds a listener that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     *
     * @param listener a local user status listener.
     */
    public void addLocalUserPresenceListener(
                            ChatRoomLocalUserPresenceListener listener)
    {
        synchronized(localUserStatusListeners)
        {
            if (!localUserStatusListeners.contains(listener))
                localUserStatusListeners.add(listener);
        }
    }

    /**
     * Removes a listener that was being notified of changes in our status in
     * the room such as us being kicked, banned, or granted admin
     * permissions.
     *
     * @param listener a local user status listener.
     */
    public void removeLocalUserPresenceListener(
            ChatRoomLocalUserPresenceListener listener)
    {
        synchronized(localUserStatusListeners)
        {
            localUserStatusListeners.remove(listener);
        }
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time
     * a new message is received on this chat room.
     *
     * @param listener a <tt>MessageListener</tt> that would be notified
     *   every time a new message is received on this chat room.
     */
    public void addMessageListener(ChatRoomMessageListener listener)
    {
        synchronized(messageListeners)
        {
            if (!messageListeners.contains(listener))
                messageListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> so that it won't receive any further message
     * events from this room.
     *
     * @param listener the <tt>MessageListener</tt> to remove from this room
     */
    public void removeMessageListener(ChatRoomMessageListener listener)
    {
        synchronized(messageListeners)
        {
            messageListeners.remove(listener);
        }

    }

    /**
     * Adds a listener that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     *
     * @param listener a participant status listener.
     */
    public void addMemberPresenceListener(
        ChatRoomMemberPresenceListener listener)
    {
        synchronized(memberListeners)
        {
            if (!memberListeners.contains(listener))
                memberListeners.add(listener);
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
        ChatRoomMemberPresenceListener listener)
    {
        synchronized(memberListeners)
        {
            memberListeners.remove(listener);
        }

    }

    /**
     * Create a Message instance for sending arbitrary MIME-encoding content.
     *
     * @param content content value
     * @param contentType the MIME-type for <tt>content</tt>
     * @param contentEncoding encoding used for <tt>content</tt>
     * @param subject a <tt>String</tt> subject or <tt>null</tt> for now
     *   subject.
     * @return the newly created message.
     */
    public Message createMessage(byte[] content, String contentType,
                                 String contentEncoding, String subject)
    {
        return new MessageJabberImpl(
                new String(content)
                , contentType
                , contentEncoding
                , subject);
    }


    /**
     * Create a Message instance for sending a simple text messages with
     * default (text/plain) content type and encoding.
     *
     * @param messageText the string content of the message.
     * @return Message the newly created message
     */
    public Message createMessage(String messageText)
    {
        Message msg
            = new MessageJabberImpl(
                messageText
                , OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE
                , OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING
                , null);
        return msg;
    }

    /**
     * Returns a <tt>List</tt> of <tt>Member</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>Member</tt> corresponding to all room
     *   members.
     */
    public List getMembers()
    {
        return new LinkedList(members.entrySet());
    }

    /**
     * Returns the number of participants that are currently in this chat
     * room.
     *
     * @return int the number of <tt>Contact</tt>s, currently participating
     *   in this room.
     */
    public int getMembersCount()
    {
        return multiUserChat.getOccupantsCount();
    }

    /**
     * Returns the name of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the name of this
     *   <tt>ChatRoom</tt>.
     */
    public String getName()
    {
        return multiUserChat.getRoom();
    }

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     *   context of the local chat room.
     */
    public String getUserNickname()
    {
        return this.multiUserChat.getNickname();
    }

    /**
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     *
     * @return the room subject or <tt>null</tt> if the user hasn't joined
     *   the room or the room does not have a subject yet.
     */
    public String getSubject()
    {
        return this.multiUserChat.getNickname();
    }

    /**
     * Invites another user to this room.
     *
     * @param userAddress the address of the user to invite to the room.(one
     *   may also invite users not on their contact list).
     * @param reason a reason, subject, or welcome message that would tell
     *   the the user why they are being invited.
     */
    public void invite(String userAddress, String reason)
    {
        multiUserChat.invite(userAddress, reason);
    }

    /**
     * Returns true if the local user is currently in the multi user chat
     * (after calling one of the {@link #join()} methods).
     *
     * @return true if currently we're currently in this chat room and false
     *   otherwise.
     */
    public boolean isJoined()
    {
        return multiUserChat.isJoined();
    }

    /**
     * Joins this chat room so that the user would start receiving events and
     * messages for it.
     *
     * @param password the password to use when authenticating on the
     *   chatroom.
     * @throws OperationFailedException with the corresponding code if an
     *   error occurs while joining the room.
     */
    public void join(byte[] password)
        throws OperationFailedException
    {
        joinAs(provider.getAccountID().getUserID(), password);
    }

    /**
     * Joins this chat room with the nickname of the local user so that the
     * user would start receiving events and messages for it.
     *
     * @throws OperationFailedException with the corresponding code if an
     *   error occurs while joining the room.
     */
    public void join()
        throws OperationFailedException
    {
        joinAs(provider.getAccountID().getUserID());
    }

    /**
     * Joins this chat room with the specified nickname and password so that
     * the user would start receiving events and messages for it.
     *
     * @param nickname the nickname to use.
     * @param password a password necessary to authenticate when joining the
     *   room.
     * @throws OperationFailedException with the corresponding code if an
     *   error occurs while joining the room.
     */
    public void joinAs(String nickname, byte[] password)
        throws OperationFailedException
    {
        try
        {
            multiUserChat.join(nickname, new String(password));

            initMembers();
        }
        catch (XMPPException ex)
        {
            logger.error("Failed to join chat room "
                          + getName()
                          + " with nickname "
                          + nickname
                          , ex );
        }
    }

    /**
     * Joins this chat room with the specified nickname so that the user
     * would start receiving events and messages for it.
     *
     * @param nickname the nickname to use.
     * @throws OperationFailedException with the corresponding code if an
     *   error occurs while joining the room.
     */
    public void joinAs(String nickname)
        throws OperationFailedException
    {
        try
        {
            multiUserChat.join(nickname);

            initMembers();
        }
        catch (XMPPException ex)
        {
            logger.error("Failed to join room "
                         + getName()
                         + " with nickname: "
                         + nickname
                         , ex);
            throw new OperationFailedException(
                "Failed to join room "
                         + getName()
                         + " with nickname: "
                         + nickname
                         , OperationFailedException.GENERAL_ERROR
                         , ex);
        }
    }

    /**
     * Initialises the list of chat room members.
     *
     * @throws XMPPException if we fail retrieving the members.
     */
    private void initMembers()
        throws XMPPException
    {
        Iterator occupantsIter = multiUserChat.getOccupants();

        while (occupantsIter.hasNext())
        {
            String occupantStr = (String) occupantsIter.next();

            Occupant occupant = multiUserChat.getOccupant(occupantStr);

            ChatRoomMemberRole role = smackRoleToScRole(occupant.getRole());

            //smack returns fully qualified occupant names.
            ChatRoomMemberJabberImpl member = new ChatRoomMemberJabberImpl(
                  this
                , occupant.getNick()
                , occupant.getJid()
                , role);
            this.members.put(member.getContactAddress(), member);
        }
    }

    /**
     * Returns that <tt>ChatRoomJabberRole</tt> instance corresponding to the
     * <tt>smackRole</tt> string.
     *
     * @param smackRole the smack role as returned by
     * <tt>Occupant.getRole()</tt>.
     * @return ChatRoomMemberRole
     */
    private ChatRoomMemberRole smackRoleToScRole(String smackRole)
    {
        if (smackRole.equalsIgnoreCase("moderator"))
        {
            return ChatRoomMemberRole.MODERATOR;
        }
        else if (smackRole.equalsIgnoreCase("participant"))
        {
            return ChatRoomMemberRole.MEMBER;
        }
        else
        {
            return ChatRoomMemberRole.GUEST;
        }
    }

    /**
     * Leave this chat room.
     *
     */
    public void leave()
    {
        multiUserChat.leave();
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param message the <tt>Message</tt> to send.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    public void sendMessage(Message message)
        throws OperationFailedException
    {
        try
        {
            multiUserChat.sendMessage(message.getContent());
        }
        catch (XMPPException ex)
        {
            logger.error("Failed to send message " + message, ex);
            throw new OperationFailedException(
                "Failed to send message " + message
                , OperationFailedException.GENERAL_ERROR
                , ex);
        }
    }

    /**
     * Changes the the local user's nickname in the context of this chatroom.
     *
     * @param nickname the new nickname within the room.
     * @throws OperationFailedException if the setting the new nickname
     *   changes for some reason.
     */
    public void setNickname(String nickname)
        throws OperationFailedException
    {
        try
        {
            multiUserChat.changeNickname(nickname);
        }
        catch (XMPPException ex)
        {
            logger.error("Failed to changed subject for chat room" + getName()
                         , ex);
            throw new OperationFailedException(
                "Failed to changed nickname in chat room" + getName()
                , OperationFailedException.FORBIDDEN
                , ex);
        }
    }

    /**
     * Sets the subject of this chat room.
     *
     * @param subject the new subject that we'd like this room to have
     * @throws OperationFailedException
     */
    public void setSubject(String subject)
        throws OperationFailedException
    {
        try
        {
            multiUserChat.changeSubject(subject);
        }
        catch (XMPPException ex)
        {
            logger.error("Failed to change subject for chat room" + getName()
                         , ex);
            throw new OperationFailedException(
                "Failed to changed subject for chat room" + getName()
                , OperationFailedException.FORBIDDEN
                , ex);
        }

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
     * Instances of this class should be registered as
     * <tt>ParticipantStatusListener</tt> in smack and translates events .
     */
    private class MemberListener implements ParticipantStatusListener
    {
        /**
         * Called when an administrator or owner banned a participant from the
         * room. This means that banned participant will no longer be able to
         * join the room unless the ban has been removed.
         *
         * @param participant the participant that was banned from the room
         * (e.g. room@conference.jabber.org/nick).
         * @param actor the administrator that banned the occupant (e.g.
         * user@host.org).
         * @param reason the reason provided by the administrator to ban the
         * occupant.
         */
        public void banned(String participant, String actor, String reason)
        {
            /** @todo implement banned() */
        }

        /**
         * Called when an owner grants administrator privileges to a user. This
         * means that the user will be able to perform administrative functions
         * such as banning users and edit moderator list.
         *
         * @param participant the participant that was granted administrator
         * privileges (e.g. room@conference.jabber.org/nick).
         */
        public void adminGranted(String participant)
        {
            /** @todo implement banned() */
        }

        /**
         * Called when an owner revokes administrator privileges from a user.
         * This means that the user will no longer be able to perform
         * administrative functions such as banning users and edit moderator
         * list.
         *
         * @param participant the participant that was revoked administrator
         * privileges (e.g. room@conference.jabber.org/nick).
         */
        public void adminRevoked(String participant)
        {
            /** @todo implement banned() */
        }

        /**
         * Called when a new room occupant has joined the room. Note: Take in
         * consideration that when you join a room you will receive the list of
         * current occupants in the room. This message will be sent for each
         * occupant.
         *
         * @param participant the participant that has just joined the room
         * (e.g. room@conference.jabber.org/nick).
         */
        public void joined(String participant)
        {
            /** @todo implement joined() */
        }

        /**
         * Called when a room occupant has left the room on its own. This means
         * that the occupant was neither kicked nor banned from the room.
         *
         * @param participant the participant that has left the room on its own.
         * (e.g. room@conference.jabber.org/nick).
         */
        public void left(String participant)
        {
            /** @todo implement left() */
        }

        /**
         * Called when a participant changed his/her nickname in the room. The
         * new participant's nickname will be informed with the next available
         * presence.
         *
         * @param participant the participant that was revoked administrator
         * privileges (e.g. room@conference.jabber.org/nick).
         * @param newNickname the new nickname that the participant decided to
         * use.
         */
        public void nicknameChanged(String participant, String newNickname)
        {
            /** @todo implement nicknameChanged() */
        }

        /**
         * Called when an owner revokes a user ownership on the room. This
         * means that the user will no longer be able to change defining room
         * features as well as perform all administrative functions.
         *
         * @param participant the participant that was revoked ownership on the
         * room (e.g. room@conference.jabber.org/nick).
         */
        public void ownershipRevoked(String participant)
        {
            /** @todo implement ownershipRevoked() */
        }

        /**
         * Called when a room participant has been kicked from the room. This
         * means that the kicked participant is no longer participating in the
         * room.
         *
         * @param participant the participant that was kicked from the room
         * (e.g. room@conference.jabber.org/nick).
         * @param actor the moderator that kicked the occupant from the room
         * (e.g. user@host.org).
         * @param reason the reason provided by the actor to kick the occupant
         * from the room.
         */
        public void kicked(String participant, String actor, String reason)
        {
            /** @todo implement kicked() */
        }

        /**
         * Called when an administrator grants moderator privileges to a user.
         * This means that the user will be able to kick users, grant and
         * revoke voice, invite other users, modify room's subject plus all the
         * partcipants privileges.
         *
         * @param participant the participant that was granted moderator
         * privileges in the room (e.g. room@conference.jabber.org/nick).
         */
        public void moderatorGranted(String participant)
        {
            /** @todo implement moderatorGranted() */
        }

        /**
         * Called when a moderator revokes voice from a participant. This means
         * that the participant in the room was able to speak and now is a
         * visitor that can't send messages to the room occupants.
         *
         * @param participant the participant that was revoked voice from the
         * room (e.g. room@conference.jabber.org/nick).
         */
        public void voiceRevoked(String participant)
        {
            /** @todo implement voiceRevoked() */
        }

        /**
         * Called when an administrator grants a user membership to the room.
         * This means that the user will be able to join the members-only room.
         *
         * @param participant the participant that was granted membership in
         * the room (e.g. room@conference.jabber.org/nick).
         */
        public void membershipGranted(String participant)
        {
            /** @todo implement membershipGranted() */
        }

        /**
         * Called when an administrator revokes moderator privileges from a
         * user. This means that the user will no longer be able to kick users,
         * grant and revoke voice, invite other users, modify room's subject
         * plus all the partcipants privileges.
         *
         * @param participant the participant that was revoked moderator
         * privileges in the room (e.g. room@conference.jabber.org/nick).
         */
        public void moderatorRevoked(String participant)
        {
            /** @todo implement moderatorRevoked() */
        }

        /**
         * Called when a moderator grants voice to a visitor. This means that
         * the visitor can now participate in the moderated room sending
         * messages to all occupants.
         *
         * @param participant the participant that was granted voice in the room
         * (e.g. room@conference.jabber.org/nick).
         */
        public void voiceGranted(String participant)
        {
            /** @todo implement voiceGranted() */
        }

        /**
         * Called when an administrator revokes a user membership to the room.
         * This means that the user will not be able to join the members-only
         * room.
         *
         * @param participant the participant that was revoked membership from
         * the room
         * (e.g. room@conference.jabber.org/nick).
         */
        public void membershipRevoked(String participant)
        {
            /** @todo implement membershipRevoked() */
        }

        /**
         * Called when an owner grants a user ownership on the room. This means
         * that the user will be able to change defining room features as well
         * as perform all administrative functions.
         *
         * @param participant the participant that was granted ownership on the
         * room (e.g. room@conference.jabber.org/nick).
         */
        public void ownershipGranted(String participant)
        {
            /** @todo implement ownershipGranted() */
        }
    }

    public void addLocalUserRoleListener(
        ChatRoomLocalUserRoleListener listener)
    {
        /** @todo implement addLocalUserRoleListener() */
    }

    public void removelocalUserRoleListener(
        ChatRoomLocalUserRoleListener listener)
    {
        /** @todo implement removeLocalUserRoleListener() */
    }

    public void addMemberRoleListener(ChatRoomMemberRoleListener listener)
    {
        /** @todo implement addMemberRoleListener() */
    }

    public void removeMemberRoleListener(ChatRoomMemberRoleListener listener)
    {
        /** @todo implement removeMemberRoleListener() */
    }

    public void setPassword(String password)
        throws OperationFailedException
    {
        /** @todo implement setPassword */
    }

    public String getPassword()
    {
        /** @todo implement getPassword */
        return null;
    }

    public void addBanMask(String banMask) throws OperationFailedException
    {
        /** @todo implement addBanMask */
    }

    public void removeBanMask(String banMask) throws OperationFailedException
    {
        /** @todo implement removeBanMask */
    }

    public void setUserLimit(int userLimit) throws OperationFailedException
    {
        /** @todo implement setUserLimit */
    }

    public int getUserLimit()
    {
        /** @todo implement getUserLimit */
        return 0;
    }

    public Iterator getBanList()
    {
        /** @todo implement getBanList */
        return null;
    }

    public void setUserNickname(String nickname) throws OperationFailedException
    {
        /** @todo implement setUserNickname */
    }

    public boolean isVisible()
    {
        return true;
    }

    public void setVisible(boolean isVisible)
    {
        /** @todo implement setVisible */
    }

    public boolean isPasswordRequired()
    {
        return false;
    }

    public void setPasswordRequired(boolean isPasswordRequired)
    {
        /** @todo implement setPasswordRequired */
    }

    public boolean isInvitationRequired()
    {
        return false;
    }

    public void setInvitationRequired(boolean isInvitationRequired)
    {
        /** @todo implement setInvitationRequired */
    }

    public boolean isMemberNumberLimited()
    {
        return false;
    }

    public void setMemberNumberLimited(boolean isMemberNumberLimited)
    {
        /** @todo implement setMemberNumberLimited */
    }

    public boolean isMute()
    {
        return false;
    }

    public void setMute(boolean isMute)
    {
        /** @todo implement setMute */
    }

    public boolean isAllowExternalMessages()
    {
        return false;
    }

    public void setAllowExternalMessages(boolean isAllowExternalMessages)
    {
        /** @todo implement setAllowExternalMessages */
    }

    public boolean isRegistered()
    {
        return false;
    }

    public void setRegistered(boolean isRegistered)
    {
        /** @todo implement setRegistered */
    }

    public boolean isSubjectLocked()
    {
        return false;
    }

    public void setSubjectLocked(boolean isSubjectLocked)
    {
        /** @todo implement setSubjectLocked */
    }

    public boolean isAllowMessageFormatting()
    {
        return true;
    }

    public void setAllowMessageFormatting(boolean isAllowMessageFormatting)
    {
        /** @todo implement setAllowMessageFormatting */
    }

    public boolean isFilterMessageFormatting()
    {
        return false;
    }

    public void setFilterMessageFormatting(boolean isFilterMessageFormatting)
    {
        /** @todo implement setFilterMessageFormatting */
    }

    public boolean isJoinTimeLimited()
    {
        return false;
    }

    public void setJoinTimeLimited(boolean isJoinTimeLimited)
    {
        /** @todo implement setJoinTimeLimited */
    }

    public boolean isAllowInvitationSend()
    {
        return true;
    }

    public void setAllowInvitationSend(boolean isAllowInvitationSend)
    {
        /** @todo implement setAllowInvitationSend */
    }

    public boolean isAllowInvitationReceive()
    {
        return true;
    }

    public void setAllowInvitationReceive(boolean isAllowInvitationReceive)
    {
        /** @todo implement setAllowInvitationReceive */
    }

    public boolean isUserRedirected()
    {
        return false;
    }

    public void setUserRedirected(boolean isRedirected)
    {   
        /** @todo implement setUserRedirected */
    }

    public boolean isUserNicknameLocked()
    {
        return false;
    }

    public void setUserNicknameLocked(boolean isUserNicknameLocked)
    {   
        /** @todo implement setUserNicknameLocked */
    }

    public boolean isAllowKick()
    {
        return false;
    }

    public void setAllowKick(boolean isAllowKick)
    {   
        /** @todo implement setAllowKick */
    }

    public boolean isRegisteredUserOnly()
    {
        return false;
    }

    public void setRegisteredUserOnly(boolean isRegisteredUserOnly)
    {
        /** @todo implement setRegisteredUserOnly*/
    }

    public boolean isAllowSpecialMessage()
    {
        return false;
    }

    public void setAllowSpecialMessage(boolean isAllowSpecialMessage)
    {   
        /** @todo implement setAllowSpecialMessage() */
    }

    public boolean isNicknameListVisible()
    {
        return true;
    }

    public void setNicknameListVisible(boolean isNicknameListVisible)
    {   
        /** @todo implement setNicknameListVisible() */
    }

    public void addAdvancedConfigProperty(String propertyName,
        String propertyValue)
        throws OperationFailedException
    {
        /** @todo implement addAdvancedConfigProperty() */
    }

    public void removeAdvancedConfigProperty(String propertyName)
        throws OperationFailedException
    {   
        /** @todo implement removeAdvancedConfigProperty() */
    }

    public Map getAdvancedConfigurationSet()
    {
        /** @todo implement getAdvancedConfigurationSet() */
        return null;
    }
}
