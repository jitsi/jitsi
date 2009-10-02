/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.beans.PropertyChangeEvent;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.version.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smackx.packet.DiscoverInfo;

/**
 * Implements chat rooms for jabber. The class encapsulates instances of the
 * jive software <tt>MultiUserChat</tt>.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
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
     * Listeners that will be notified of changes in member status in the
     * room such as member joined, left or being kicked or dropped.
     */
    private final Vector<ChatRoomMemberPresenceListener> memberListeners 
        = new Vector<ChatRoomMemberPresenceListener>();

    /**
     * Listeners that will be notified of changes in member role in the
     * room such as member being granted admin permissions, or revoked admin
     * permissions.
     */
    private final Vector<ChatRoomMemberRoleListener> memberRoleListeners 
        = new Vector<ChatRoomMemberRoleListener>();
    
    /**
     * Listeners that will be notified of changes in local user role in the
     * room such as member being granted admin permissions, or revoked admin
     * permissions.
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
     * The protocol provider that created us
     */
    private final ProtocolProviderServiceJabberImpl provider;

    /**
     * The operation set that created us.
     */
    private final OperationSetMultiUserChatJabberImpl opSetMuc;

    /**
     * The list of members of this chat room.
     */
    private final Hashtable<String, ChatRoomMember> members = new Hashtable<String, ChatRoomMember>();

    /**
     * The list of banned members of this chat room.
     */
    private final Hashtable<String, ChatRoomMember> banList = new Hashtable<String, ChatRoomMember>();
    
    /**
     * The nickname of this chat room local user participant.
     */
    private String nickname;
    
    /**
     * The subject of this chat room. Keeps track of the subject changes.
     */
    private String oldSubject;

    /**
     * The corresponding configuration form.
     */
    private ChatRoomConfigurationFormJabberImpl configForm;
    
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
        
        this.oldSubject = multiUserChat.getSubject();
        
        multiUserChat.addSubjectUpdatedListener(
            new SmackSubjectUpdatedListener());
        multiUserChat.addMessageListener(new SmackMessageListener());
        multiUserChat.addParticipantStatusListener(new MemberListener());
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
        synchronized(propertyChangeListeners)
        {
            if (!propertyChangeListeners.contains(listener))
                propertyChangeListeners.add(listener);
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
        synchronized(propertyChangeListeners)
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
        ChatRoomMemberPropertyChangeListener listener)
    {
        synchronized(memberPropChangeListeners)
        {
            if (!memberPropChangeListeners.contains(listener))
                memberPropChangeListeners.add(listener);
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
        ChatRoomMemberPropertyChangeListener listener)
    {
        synchronized(memberPropChangeListeners)
        {
            memberPropChangeListeners.remove(listener);
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
                messageText,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING,
                null);

        return msg;
    }

    /**
     * Returns a <tt>List</tt> of <tt>Member</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>Member</tt> corresponding to all room
     *   members.
     */
    public List<ChatRoomMember> getMembers()
    {
        return new LinkedList<ChatRoomMember>(members.values());
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
     * Returns the identifier of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the identifier of this
     *   <tt>ChatRoom</tt>.
     */
    public String getIdentifier()
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
        return multiUserChat.getNickname();
    }

    /**bje
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     *
     * @return the room subject or <tt>null</tt> if the user hasn't joined
     *   the room or the room does not have a subject yet.
     */
    public String getSubject()
    {
        return this.multiUserChat.getSubject();
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
        this.assertConnected();
        
        this.nickname = nickname;
        
        try
        {
            multiUserChat.join(nickname, new String(password));
            
            ChatRoomMemberJabberImpl member
                = new ChatRoomMemberJabberImpl( this,
                                                nickname,
                                                provider.getAccountID()
                                                    .getAccountAddress(),
                                                ChatRoomMemberRole.GUEST);

            members.put(nickname, member);
  
            // We don't specify a reason.
            opSetMuc.fireLocalUserPresenceEvent(this,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED, null);
        }
        catch (XMPPException ex)
        {
            String errorMessage;
            
            if(ex.getXMPPError().getCode() == 401)
            {
                errorMessage
                    = "Failed to join chat room "
                        + getName()
                        + " with nickname: "
                        + nickname
                        + ". The chat room requests a password.";
                
                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage,
                    OperationFailedException.AUTHENTICATION_FAILED,
                    ex);
            }
            else if(ex.getXMPPError().getCode() == 407)
            {
                errorMessage
                    = "Failed to join chat room "
                        + getName()
                        + " with nickname: "
                        + nickname
                        + ". The chat room requires registration.";
                
                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage,
                    OperationFailedException.REGISTRATION_REQUIRED,
                    ex);
            }
            else
            {
                errorMessage
                    = "Failed to join room "
                        + getName()
                        + " with nickname: "
                        + nickname;
                
                logger.error(errorMessage, ex);
       
                throw new OperationFailedException(
                    errorMessage, 
                    OperationFailedException.GENERAL_ERROR,
                    ex);
            }
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
        this.assertConnected();
        
        this.nickname = nickname;

        try
        {
            if (multiUserChat.isJoined())
            {
                if (!multiUserChat.getNickname().equals(nickname))
                    multiUserChat.changeNickname(nickname);
            }
            else
                multiUserChat.join(nickname);

            if (members.get(nickname) == null)
            {
                ChatRoomMemberJabberImpl member
                    = new ChatRoomMemberJabberImpl( this,
                                                    nickname,
                                                    provider.getAccountID()
                                                        .getAccountAddress(),
                                                    ChatRoomMemberRole.GUEST);

            members.put(nickname, member);

                //we don't specify a reason
                opSetMuc.fireLocalUserPresenceEvent(this,
                    LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED, null);
            }
        }
        catch (XMPPException ex)
        {
            String errorMessage;

            if(ex.getXMPPError() == null)
            {
                errorMessage
                    = "Failed to join room "
                        + getName()
                        + " with nickname: "
                        + nickname;

                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage, 
                    OperationFailedException.GENERAL_ERROR,
                    ex);
            }
            else if(ex.getXMPPError().getCode() == 401)
            {
                errorMessage
                    = "Failed to join chat room "
                        + getName()
                        + " with nickname: "
                        + nickname
                        + ". The chat room requests a password.";
                
                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage,
                    OperationFailedException.AUTHENTICATION_FAILED,
                    ex);
            }
            else if(ex.getXMPPError().getCode() == 407)
            {
                errorMessage
                    = "Failed to join chat room "
                        + getName()
                        + " with nickname: "
                        + nickname
                        + ". The chat room requires registration.";
                
                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage,
                    OperationFailedException.REGISTRATION_REQUIRED,
                    ex);
            }
            else
            {
                errorMessage
                    = "Failed to join room "
                        + getName()
                        + " with nickname: "
                        + nickname;
                
                logger.error(errorMessage, ex);

                throw new OperationFailedException(
                    errorMessage, 
                    OperationFailedException.GENERAL_ERROR,
                    ex);
            }
        }
        catch (Exception ex)
        {
            String errorMessage = "Failed to join room "
                                    + getName()
                                    + " with nickname: "
                                    + nickname;

            logger.error(errorMessage, ex);

            throw new OperationFailedException(
                errorMessage, 
                OperationFailedException.GENERAL_ERROR,
                ex);
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
     * Returns the <tt>ChatRoomMember</tt> corresponding to the given smack
     * participant.
     * 
     * @param participant the full participant name
     * (e.g. sc-testroom@conference.voipgw.u-strasbg.fr/testuser)
     * @return the <tt>ChatRoomMember</tt> corresponding to the given smack
     * participant
     */
    public ChatRoomMember smackParticipantToScMember(String participant)
    {
        String participantName = StringUtils.parseResource(participant);
        
        Iterator<ChatRoomMember> chatRoomMembers =
            this.members.values().iterator();
        
        while(chatRoomMembers.hasNext())
        {
            ChatRoomMember member = chatRoomMembers.next();
            
            if(participantName.equals(member.getName())
                || participant.equals(member.getContactAddress()))
                return member;
        }
        
        return null;
    }

    /**
     * Leave this chat room.
     */
    public void leave()
    {
        multiUserChat.leave();

// FIXME : do we have to do the following when we leave the room ?

        Iterator<Map.Entry<String, ChatRoomMember>> membersSet 
            = members.entrySet().iterator();
        
        while(membersSet.hasNext())
        {
            Map.Entry<String, ChatRoomMember> memberEntry = membersSet.next();
            
            ChatRoomMember member = memberEntry.getValue();
            
            fireMemberPresenceEvent(member,
                ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT,
                "Local user has left the chat room.");
        }
        
        // Delete the list of members
        members.clear();
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
             assertConnected();
             
             org.jivesoftware.smack.packet.Message msg = 
                new org.jivesoftware.smack.packet.Message();
             
             msg.setBody(message.getContent());
             msg.addExtension(new Version());

             MessageEventManager.
                 addNotificationsRequests(msg, true, false, false, true);

             // We send only the content because it doesn't work if we send the
             // Message object.
             multiUserChat.sendMessage(message.getContent());

             ChatRoomMessageDeliveredEvent msgDeliveredEvt
                 = new ChatRoomMessageDeliveredEvent(
                     this,
                     System.currentTimeMillis(),
                     message,
                     ChatRoomMessageDeliveredEvent
                         .CONVERSATION_MESSAGE_DELIVERED);

             fireMessageEvent(msgDeliveredEvt);
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
            logger.info(participant + " has been banned from "
                + getName() + " chat room.");
            
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;

            String participantName = StringUtils.parseResource(participant);

            members.remove(participantName);

            banList.put(participant, member);
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.OUTCAST);
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
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.ADMINISTRATOR);
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
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.MEMBER);
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
            logger.info(participant + " has joined the "
                + getName() + " chat room.");
            
            String participantName = StringUtils.parseResource(participant);

            if(participantName.equals(nickname)
                || members.containsKey(participantName))
                return;
            
            Occupant occupant = multiUserChat.getOccupant(participant);

            ChatRoomMemberRole role = smackRoleToScRole(occupant.getRole());

            //smack returns fully qualified occupant names.
            ChatRoomMemberJabberImpl member = new ChatRoomMemberJabberImpl(
                  ChatRoomJabberImpl.this,
                  occupant.getNick(),
                  occupant.getJid(),
                  role);
            
            members.put(participantName, member);
                        
            //we don't specify a reason
            fireMemberPresenceEvent(member,
                ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
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
            logger.info(participant + " has left the "
                + getName() + " chat room.");
            
            ChatRoomMember member
                = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            String participantName = StringUtils.parseResource(participant);
            
            members.remove(participantName);
            
            fireMemberPresenceEvent(member,
                ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT, null);
        }

        /**
         * Called when a participant changed his/her nickname in the room. The
         * new participant's nickname will be informed with the next available
         * presence.
         *
         * @param participant the participant that has changed his nickname
         * @param newNickname the new nickname that the participant decided to
         * use.
         */
        public void nicknameChanged(String participant, String newNickname)
        {
            ChatRoomMember member = smackParticipantToScMember(participant);

            if(member == null)
                return;

            ((ChatRoomMemberJabberImpl) member).setName(newNickname);

            String participantName = StringUtils.parseResource(participant);
            
            ChatRoomMemberPropertyChangeEvent evt
                = new ChatRoomMemberPropertyChangeEvent(
                    member,
                    ChatRoomJabberImpl.this,
                    ChatRoomMemberPropertyChangeEvent.MEMBER_NICKNAME,
                    participantName,
                    newNickname);

            fireMemberPropertyChangeEvent(evt);
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
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.MEMBER);
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
            ChatRoomMember member
                = smackParticipantToScMember(participant);
            
            ChatRoomMember actorMember
                = smackParticipantToScMember(actor);
            
            if(member == null)
                return;
            
            String participantName = StringUtils.parseResource(participant);
            
            members.remove(participantName);
            
            fireMemberPresenceEvent(member, actorMember,
                ChatRoomMemberPresenceChangeEvent.MEMBER_KICKED, reason);
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
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.MODERATOR);
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
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.SILENT_MEMBER);
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
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.MEMBER);
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
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.GUEST);
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
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.GUEST);
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
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.GUEST);
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
            ChatRoomMember member = smackParticipantToScMember(participant);
            
            if(member == null)
                return;
            
            fireMemberRoleEvent(member, member.getRole(),
                ChatRoomMemberRole.OWNER);
        }
    }

    /**
     * Adds a listener that will be notified of changes in our role in the room
     * such as us being granded operator.
     * 
     * @param listener a local user role listener.
     */
    public void addLocalUserRoleListener(
        ChatRoomLocalUserRoleListener listener)
    {
        synchronized(localUserRoleListeners)
        {
            if (!localUserRoleListeners.contains(listener))
                localUserRoleListeners.add(listener);
        }
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
        synchronized(localUserRoleListeners)
        {
            localUserRoleListeners.remove(listener);
        }
    }

    /**
     * Adds a listener that will be notified of changes of a member role in the
     * room such as being granded operator.
     * 
     * @param listener a member role listener.
     */
    public void addMemberRoleListener(ChatRoomMemberRoleListener listener)
    {
        synchronized(memberRoleListeners)
        {
            if (!memberRoleListeners.contains(listener))
                memberRoleListeners.add(listener);
        }
    }

    /**
     * Removes a listener that was being notified of changes of a member role in
     * this chat room such as us being granded operator.
     * 
     * @param listener a member role listener.
     */
    public void removeMemberRoleListener(ChatRoomMemberRoleListener listener)
    {
        synchronized(memberRoleListeners)
        {
            memberRoleListeners.remove(listener);
        }
    }

    /**
     * Returns the list of banned users.
     */
    public Iterator<ChatRoomMember> getBanList()
        throws OperationFailedException
    {   
        return banList.values().iterator();
    }

    /**
     * Changes the local user nickname. If the new nickname already exist in the
     * chat room throws an OperationFailedException.
     * 
     * @param nickname the new nickname within the room.
     *
     * @throws OperationFailedException if the new nickname already exist in
     * this room
     */
    public void setUserNickname(String nickname)
        throws OperationFailedException
    {
        try
        {
            multiUserChat.changeNickname(nickname);
        }
        catch (XMPPException e)
        {
            logger.error("Failed to change nickname for chat room: "
                + getName());
            
            throw new OperationFailedException("The " + nickname
                + "already exists in this chat room.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        }        
    }
        
    /**
     * Bans a user from the room. An admin or owner of the room can ban users
     * from a room.
     *
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to be banned.
     * @param reason the reason why the user was banned.
     * @throws OperationFailedException if an error occurs while banning a user.
     * In particular, an error can occur if a moderator or a user with an
     * affiliation of "owner" or "admin" was tried to be banned or if the user
     * that is banning have not enough permissions to ban.
     */
    public void banParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException
    {   
        try
        {
            multiUserChat.banUser(chatRoomMember.getContactAddress(), reason);
        }
        catch (XMPPException e)
        {
            logger.error("Failed to ban participant.", e);
            
            // If a moderator or a user with an affiliation of "owner" or "admin"
            // was intended to be kicked.
            if (e.getXMPPError().getCode() == 405)
            {
                throw new OperationFailedException(
                    "Kicking an admin user or a chat room owner is a forbidden "
                    + "operation.",
                    OperationFailedException.FORBIDDEN);
            }
            else
            {
                throw new OperationFailedException(
                    "An error occured while trying to kick the participant.",
                    OperationFailedException.GENERAL_ERROR);
            }
        }
    }

    /**
     * Kicks a participant from the room.
     *
     * @param member the <tt>ChatRoomMember</tt> to kick from the room
     * @param reason the reason why the participant is being kicked from the
     * room
     * @throws OperationFailedException if an error occurs while kicking the
     * participant. In particular, an error can occur if a moderator or a user
     * with an affiliation of "owner" or "admin" was intended to be kicked; or
     * if the participant that intended to kick another participant does not
     * have kicking privileges;
     */
    public void kickParticipant(ChatRoomMember member, String reason)
        throws OperationFailedException
    {
        try
        {
            multiUserChat.kickParticipant(member.getName(), reason);
        }
        catch (XMPPException e)
        {
            logger.error("Failed to kick participant.", e);
            
            // If a moderator or a user with an affiliation of "owner" or "admin"
            // was intended to be kicked.
            if (e.getXMPPError().getCode() == 405) //not allowed
            {
                throw new OperationFailedException(
                    "Kicking an admin user or a chat room owner is a forbidden "
                    + "operation.",
                    OperationFailedException.FORBIDDEN);
            }
            // If a participant that intended to kick another participant does
            // not have kicking privileges.
            else if (e.getXMPPError().getCode() == 403) //forbidden
            {
                throw new OperationFailedException(
                    "The user that intended to kick another participant does" +
                    " not have enough privileges to do that.",
                    OperationFailedException.NOT_ENOUGH_PRIVILEGES);
            }
            else
            {
                throw new OperationFailedException(
                    "An error occured while trying to kick the participant.",
                    OperationFailedException.GENERAL_ERROR);
            }
        }
    }
    
    /**
     * Creates the corresponding ChatRoomMemberPresenceChangeEvent and notifies
     * all <tt>ChatRoomMemberPresenceListener</tt>s that a ChatRoomMember has
     * joined or left this <tt>ChatRoom</tt>.
     *
     * @param member the <tt>ChatRoomMember</tt> that this  
     * @param eventID the identifier of the event
     * @param eventReason the reason of the event
     */
    private void fireMemberPresenceEvent(ChatRoomMember member,
        String eventID, String eventReason)
    {
        ChatRoomMemberPresenceChangeEvent evt
            = new ChatRoomMemberPresenceChangeEvent(
                this, member, eventID, eventReason);
        
        logger.trace("Will dispatch the following ChatRoom event: " + evt);
    
        Iterator<ChatRoomMemberPresenceListener> listeners = null;
        synchronized (memberListeners)
        {
            listeners = new ArrayList<ChatRoomMemberPresenceListener>(
                                memberListeners).iterator();
        }
    
        while (listeners.hasNext())
        {
            ChatRoomMemberPresenceListener listener = listeners.next();
    
            listener.memberPresenceChanged(evt);
        }
    }
    
    /**
     * Creates the corresponding ChatRoomMemberPresenceChangeEvent and notifies
     * all <tt>ChatRoomMemberPresenceListener</tt>s that a ChatRoomMember has
     * joined or left this <tt>ChatRoom</tt>.
     *
     * @param member the <tt>ChatRoomMember</tt> that changed its presence
     * status
     * @param actor the <tt>ChatRoomMember</tt> that participated as an actor
     * in this event
     * @param eventID the identifier of the event
     * @param eventReason the reason of this event
     */
    private void fireMemberPresenceEvent(ChatRoomMember member,
        ChatRoomMember actor, String eventID, String eventReason)
    {
        ChatRoomMemberPresenceChangeEvent evt
            = new ChatRoomMemberPresenceChangeEvent(
                this, member, actor, eventID, eventReason);
        
        logger.trace("Will dispatch the following ChatRoom event: " + evt);
    
        Iterable<ChatRoomMemberPresenceListener> listeners;
        synchronized (memberListeners)
        {
            listeners
                = new ArrayList<ChatRoomMemberPresenceListener>(
                        memberListeners);
        }
    
        for (ChatRoomMemberPresenceListener listener : listeners)
            listener.memberPresenceChanged(evt);
    }
    
    /**
     * Creates the corresponding ChatRoomMemberRoleChangeEvent and notifies
     * all <tt>ChatRoomMemberRoleListener</tt>s that a ChatRoomMember has
     * changed its role in this <tt>ChatRoom</tt>.
     *
     * @param member the <tt>ChatRoomMember</tt> that has changed its role  
     * @param previousRole the previous role that member had
     * @param newRole the new role the member get
     */
    private void fireMemberRoleEvent(ChatRoomMember member,
        ChatRoomMemberRole previousRole, ChatRoomMemberRole newRole)
    {
        ChatRoomMemberRoleChangeEvent evt
            = new ChatRoomMemberRoleChangeEvent(
                this, member, previousRole, newRole);
        
        logger.trace("Will dispatch the following ChatRoom event: " + evt);
    
        Iterable<ChatRoomMemberRoleListener> listeners;
        synchronized (memberRoleListeners)
        {
            listeners
                = new ArrayList<ChatRoomMemberRoleListener>(
                        memberRoleListeners);
        }
    
        for (ChatRoomMemberRoleListener listener : listeners)
            listener.memberRoleChanged(evt);
    }
    
    /**
     * Delivers the specified event to all registered message listeners.
     * @param evt the <tt>EventObject</tt> that we'd like delivered to all
     * registered message listeners.
     */
    private void fireMessageEvent(EventObject evt)
    {
        Iterable<ChatRoomMessageListener> listeners;
        synchronized (messageListeners)
        {
            listeners
                = new ArrayList<ChatRoomMessageListener>(messageListeners);
        }

        for (ChatRoomMessageListener listener : listeners)
        {
            if (evt instanceof ChatRoomMessageDeliveredEvent)
            {
                listener.messageDelivered( (ChatRoomMessageDeliveredEvent) evt);
            }
            else if (evt instanceof ChatRoomMessageReceivedEvent)
            {
                listener.messageReceived( (ChatRoomMessageReceivedEvent) evt);
            }
            else if (evt instanceof ChatRoomMessageDeliveryFailedEvent)
            {
                listener.messageDeliveryFailed(
                    (ChatRoomMessageDeliveryFailedEvent) evt);
            }
        }
    }
    
    /**
     * A listener that listens for packets of type Message and fires an event
     * to notifier interesting parties that a message was received.
     */
    private class SmackMessageListener
        implements PacketListener
    {   
        public void processPacket(Packet packet)
        {
            if(!(packet instanceof org.jivesoftware.smack.packet.Message))
                return;

            org.jivesoftware.smack.packet.Message msg
                = (org.jivesoftware.smack.packet.Message) packet;
            String msgBody = msg.getBody();

            if(msgBody == null)
                return;

            String msgFrom = msg.getFrom();
            String fromUserName = StringUtils.parseResource(msgFrom);

            if(fromUserName.equals(nickname))
                return;

            ChatRoomMember member = smackParticipantToScMember(msgFrom);

            if(logger.isDebugEnabled())
            {
                logger.debug("Received from "
                             + fromUserName
                             + " the message "
                             + msg.toXML());
            }

            Message newMessage = createMessage(msgBody);

            if(msg.getType() == org.jivesoftware.smack.packet.Message.Type.error)
            {
                logger.info("Message error received from " + fromUserName);

                int errorCode = packet.getError().getCode();
                int errorResultCode
                    = ChatRoomMessageDeliveryFailedEvent.UNKNOWN_ERROR;

                if(errorCode == 503)
                {
                    org.jivesoftware.smackx.packet.MessageEvent msgEvent =
                        (org.jivesoftware.smackx.packet.MessageEvent)
                            packet.getExtension("x", "jabber:x:event");
                    if(msgEvent != null && msgEvent.isOffline())
                    {
                        errorResultCode = ChatRoomMessageDeliveryFailedEvent
                            .OFFLINE_MESSAGES_NOT_SUPPORTED;
                    }
                }

                ChatRoomMessageDeliveryFailedEvent evt =
                    new ChatRoomMessageDeliveryFailedEvent(
                        ChatRoomJabberImpl.this,
                        member,
                        errorResultCode,
                        new Date(),
                        newMessage);

                fireMessageEvent(evt);
                return;
            }

            ChatRoomMessageReceivedEvent msgReceivedEvt
                = new ChatRoomMessageReceivedEvent(
                    ChatRoomJabberImpl.this,
                    member,
                    System.currentTimeMillis(),
                    newMessage,
                    ChatRoomMessageReceivedEvent
                        .CONVERSATION_MESSAGE_RECEIVED);

            fireMessageEvent(msgReceivedEvt);
        }
    }
    
    /**
     * A listener that is fired anytime a MUC room changes its subject.
     */
    private class SmackSubjectUpdatedListener implements SubjectUpdatedListener
    {
        public void subjectUpdated(String subject, String from)
        {   
            ChatRoomPropertyChangeEvent evt
                = new ChatRoomPropertyChangeEvent(
                    ChatRoomJabberImpl.this,
                    ChatRoomPropertyChangeEvent.CHAT_ROOM_SUBJECT,
                    oldSubject,
                    subject);

            firePropertyChangeEvent(evt);
            
            // Keeps track of the subject.
            oldSubject = subject;
        }
    }
    
    /**
     * Delivers the specified event to all registered property change listeners.
     * 
     * @param evt the <tt>PropertyChangeEvent</tt> that we'd like delivered to
     * all registered property change listeners.
     */
    private void firePropertyChangeEvent(PropertyChangeEvent evt)
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
        ChatRoomMemberPropertyChangeEvent evt)
    {
        Iterable<ChatRoomMemberPropertyChangeListener> listeners;
        synchronized (memberPropChangeListeners)
        {
            listeners
                = new ArrayList<ChatRoomMemberPropertyChangeListener>(
                        memberPropChangeListeners);
        }

        for (ChatRoomMemberPropertyChangeListener listener : listeners)
            listener.chatRoomPropertyChanged(evt);
    }

    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (provider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                +"service before being able to communicate.");
        if (!provider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to communicate.");
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
        Form smackConfigForm = null;
        
        try
        {
            smackConfigForm = multiUserChat.getConfigurationForm();
            
            this.configForm
                = new ChatRoomConfigurationFormJabberImpl(
                    multiUserChat, smackConfigForm);
        }
        catch (XMPPException e)
        {
            if(e.getXMPPError().getCode() == 403)
                throw new OperationFailedException(
                    "Failed to obtain smack multi user chat config form."
                    + "User doesn't have enough privileges to see the form.",
                    OperationFailedException.NOT_ENOUGH_PRIVILEGES,
                    e);
            else
                throw new OperationFailedException(
                    "Failed to obtain smack multi user chat config form.",
                    OperationFailedException.GENERAL_ERROR,
                    e);
        }
        
        return configForm;
    }

    /**
     * The Jabber multi user chat implementation doesn't support system rooms.
     * 
     * @return false to indicate that the Jabber protocol implementation doesn't
     * support system rooms.
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
        boolean persistent = false;
        String roomName = multiUserChat.getRoom();
        try
        {
            // Do not use getRoomInfo, as it has bug and 
            // throws NPE
            DiscoverInfo info =
                ServiceDiscoveryManager.getInstanceFor(provider.getConnection()).
                    discoverInfo(roomName);

            if (info != null)
                persistent = info.containsFeature("muc_persistent");
            
        } catch (Exception ex)
        {
            logger.warn("could not get persistent state for room :" +
                roomName + "\n", ex);
        }
        return persistent;
    }

    /**
     * Finds the member of this chat room corresponding to the given nick name.
     * 
     * @param jabberID the nick name to search for.
     * @return the member of this chat room corresponding to the given nick name.
     */
    public ChatRoomMemberJabberImpl findMemberForNickName(String jabberID)
    {
        return (ChatRoomMemberJabberImpl) members.get(jabberID);
    }
}
