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

import java.beans.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;

import org.apache.commons.lang3.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.*;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smackx.packet.*;

/**
 * Implements chat rooms for jabber. The class encapsulates instances of the
 * jive software <tt>MultiUserChat</tt>.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 * @author Valentin Martinet
 * @author Boris Grozev
 * @author Hristo Terezov
 */
public class ChatRoomJabberImpl
    extends AbstractChatRoom
{
    /**
     * The logger of this class.
     */
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
    private final Hashtable<String, ChatRoomMemberJabberImpl> members
        = new Hashtable<String, ChatRoomMemberJabberImpl>();

    /**
     * The list of banned members of this chat room.
     */
    private final Hashtable<String, ChatRoomMember> banList
        = new Hashtable<String, ChatRoomMember>();

    /**
     * The nickname of this chat room local user participant.
     */
    private String nickname;

    /**
     * The subject of this chat room. Keeps track of the subject changes.
     */
    private String oldSubject;

    /**
     * The role of this chat room local user participant.
     */
    private ChatRoomMemberRole role = null;

    /**
     * The corresponding configuration form.
     */
    private ChatRoomConfigurationFormJabberImpl configForm;

    /**
     * Packet listener waits for rejection of invitations to join room.
     */
    private InvitationRejectionListeners invitationRejectionListeners
        = new InvitationRejectionListeners();

    /**
     * The conference which we have announced in the room in our last sent
     * <tt>Presence</tt> update.
     */
    private ConferenceDescription publishedConference = null;

    /**
     * The <tt>ConferenceAnnouncementPacketExtension</tt> corresponding to
     * <tt>this.publishedConference</tt> which we add to all our presence
     * updates.
     * This MUST be kep in sync with <tt>this.publishedConference</tt>
     */
    private ConferenceDescriptionPacketExtension publishedConferenceExt = null;

    /**
     * The last <tt>Presence</tt> packet we sent to the MUC.
     */
    private Presence lastPresenceSent = null;

    /**
     *
     */
    private List<CallJabberImpl> chatRoomConferenceCalls
        = new ArrayList<CallJabberImpl>();

    /**
     * The Presence listener instance.
     */
    private PresenceListener presenceListener = null;

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
        multiUserChat.addUserStatusListener(new UserListener());
        multiUserChat.addPresenceInterceptor(new PresenceInterceptor());

        this.provider.getConnection().addPacketListener(
            invitationRejectionListeners,
            new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class));
    }

    /**
     * Returns the MUCUser packet extension included in the packet or <tt>null</tt> if none.
     *
     * @param packet the packet that may include the MUCUser extension.
     * @return the MUCUser found in the packet.
     */
    private MUCUser getMUCUserExtension(Packet packet)
    {
        if (packet != null)
        {
            // Get the MUC User extension
            return (MUCUser) packet.getExtension("x",
                "http://jabber.org/protocol/muc#user");
        }
        return null;
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
     * Adds a <tt>CallJabberImpl</tt> instance to the list of conference calls
     * associated with the room.
     *
     * @param call the call to add
     */
    public synchronized void addConferenceCall(CallJabberImpl call)
    {
        if(!chatRoomConferenceCalls.contains(call))
            chatRoomConferenceCalls.add(call);
    }

    /**
     * Removes a <tt>CallJabberImpl</tt> instance from the list of conference
     * calls associated with the room.
     *
     * @param call the call to remove.
     */
    public synchronized void removeConferenceCall(CallJabberImpl call)
    {
        if(chatRoomConferenceCalls.contains(call))
            chatRoomConferenceCalls.remove(call);
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
        synchronized (members)
        {
            return new LinkedList<ChatRoomMember>(members.values());
        }
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

    /**
     * Finds private messaging contact by nickname. If the contact doesn't
     * exists a new volatile contact is created.
     *
     * @param nickname the nickname of the contact.
     * @return the contact instance.
     */
    @Override
    public Contact getPrivateContactByNickname(String nickname)
    {
        OperationSetPersistentPresenceJabberImpl opSetPersPresence
            = (OperationSetPersistentPresenceJabberImpl) provider
                .getOperationSet(OperationSetPersistentPresence.class);
        String jid = getName() + "/" + nickname;
        Contact sourceContact = opSetPersPresence.findContactByID(jid);
        if(sourceContact == null)
        {
            sourceContact = opSetPersPresence.createVolatileContact(jid, true);
        }

        return sourceContact;

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
        joinAs(JabberActivator.getGlobalDisplayDetailsService()
            .getDisplayName(getParentProvider()), password);
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
        joinAs(JabberActivator.getGlobalDisplayDetailsService()
            .getDisplayName(getParentProvider()));
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

        this.nickname = getNickName(StringUtils.parseName(nickname));
        if(this.nickname.length() == 0)
            this.nickname = nickname;

        try
        {
            if (multiUserChat.isJoined())
            {
                if (!multiUserChat.getNickname().equals(nickname))
                    multiUserChat.changeNickname(nickname);
            }
            else
            {
                presenceListener = new PresenceListener(this);
                this.provider.getConnection().addPacketListener(
                    presenceListener,
                    new AndFilter(
                        new FromMatchesFilter(multiUserChat.getRoom()),
                        new PacketTypeFilter(
                            org.jivesoftware.smack.packet.Presence.class)));
                if(password == null)
                    multiUserChat.join(nickname);
                else
                    multiUserChat.join(nickname, new String(password));
            }

            ChatRoomMemberJabberImpl member
                = new ChatRoomMemberJabberImpl( this,
                                                nickname,
                                                provider.getAccountID()
                                                    .getAccountAddress());
            synchronized (members)
            {
                members.put(nickname, member);
            }

            // We don't specify a reason.
            opSetMuc.fireLocalUserPresenceEvent(this,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED, null);
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
        catch (Throwable ex)
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
        this.joinAs(nickname, null);
    }

    /**
     * Returns that <tt>ChatRoomJabberRole</tt> instance corresponding to the
     * <tt>smackRole</tt> string.
     *
     * @param smackRole the smack role as returned by
     * <tt>Occupant.getRole()</tt>.
     * @return ChatRoomMemberRole
     */
    public static ChatRoomMemberRole smackRoleToScRole(String smackRole,
                                                String affiliation)
    {
        if(affiliation != null)
        {
            if(affiliation.equals("admin"))
            {
                return ChatRoomMemberRole.ADMINISTRATOR;
            }
            else if(affiliation.equals("owner"))
            {
                return ChatRoomMemberRole.OWNER;
            }
        }

        if(smackRole != null)
        {
            if (smackRole.equalsIgnoreCase("moderator"))
            {
                return ChatRoomMemberRole.MODERATOR;
            }
            else if (smackRole.equalsIgnoreCase("participant"))
            {
                return ChatRoomMemberRole.MEMBER;
            }
        }

        return ChatRoomMemberRole.GUEST;
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
    public ChatRoomMemberJabberImpl smackParticipantToScMember(String participant)
    {
        String participantName = StringUtils.parseResource(participant);
        synchronized (members)
        {
            Iterator<ChatRoomMemberJabberImpl> chatRoomMembers =
                this.members.values().iterator();

            while(chatRoomMembers.hasNext())
            {
                ChatRoomMemberJabberImpl member = chatRoomMembers.next();

                if(participantName.equals(member.getName())
                    || participant.equals(member.getContactAddress())
                    || participantName.equals(member.getContactAddress()))
                    return member;
            }
        }
        return null;
    }

    /**
     * Destroys the chat room.
     * @param reason the reason for destroying.
     * @param alternateAddress the alternate address
     * @return <tt>true</tt> if the room is destroyed.
     */
    public boolean destroy(String reason, String alternateAddress)
    {
        try
        {
            multiUserChat.destroy(reason, alternateAddress);
        }
        catch (XMPPException e)
        {
            logger.warn("Error occured while destroying chat room", e);
            return false;
        }
        return true;
    }

    /**
     * Leave this chat room.
     */
    public void leave()
    {
        this.leave(null, null);
    }

    /**
     * Leave this chat room.
     */
    private void leave(String reason, String alternateAddress)
    {
        OperationSetBasicTelephonyJabberImpl basicTelephony
            = (OperationSetBasicTelephonyJabberImpl) provider
                .getOperationSet(OperationSetBasicTelephony.class);

        if(basicTelephony != null && this.publishedConference != null)
        {
            ActiveCallsRepositoryJabberGTalkImpl
                <CallJabberImpl, CallPeerJabberImpl> activeRepository
                    = basicTelephony.getActiveCallsRepository();

            String callid = publishedConference.getCallId();

            if (callid != null)
            {
                CallJabberImpl call = activeRepository.findCallId(callid);
                for(CallPeerJabberImpl peer : call.getCallPeerList())
                {
                    peer.hangup(false, null, null);
                }
            }
        }

        List<CallJabberImpl> tmpConferenceCalls;
        synchronized (chatRoomConferenceCalls)
        {
            tmpConferenceCalls
                = new ArrayList<CallJabberImpl>(chatRoomConferenceCalls);
            chatRoomConferenceCalls.clear();
        }

        for(CallJabberImpl call : tmpConferenceCalls)
        {
            for(CallPeerJabberImpl peer : call.getCallPeerList())
                peer.hangup(false, null, null);
        }

        clearCachedConferenceDescriptionList();

        XMPPConnection connection = this.provider.getConnection();
        try
        {
            // if we are already disconnected
            // leave maybe called from gui when closing chat window
            if(connection != null)
                multiUserChat.leave();
        }
        catch(Throwable e)
        {
            logger.warn("Error occured while leaving, maybe just " +
                "disconnected before leaving", e);
        }

        // FIXME Do we have to do the following when we leave the room?
        Hashtable<String, ChatRoomMemberJabberImpl> membersCopy;
        synchronized (members)
        {
            membersCopy
                = new Hashtable<String, ChatRoomMemberJabberImpl>(members);

            // Delete the list of members
            members.clear();
        }

        for (ChatRoomMember member : membersCopy.values())
            fireMemberPresenceEvent(
                member,
                ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT,
                "Local user has left the chat room.");

        // connection can be null if we are leaving cause connection failed
        if(connection != null)
        {
            connection.removePacketListener(invitationRejectionListeners);
            if(presenceListener != null)
            {
                connection.removePacketListener(presenceListener);
                presenceListener = null;
            }
        }

        opSetMuc.fireLocalUserPresenceEvent(
            this,
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT,
            reason,
            alternateAddress);
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
             //msg.addExtension(new Version());

             MessageEventManager.
                 addNotificationsRequests(msg, true, false, false, true);

             // We send only the content because it doesn't work if we send the
             // Message object.
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
     * Returns local user role in the context of this chatroom.
     *
     * @return ChatRoomMemberRole
     */
    public ChatRoomMemberRole getUserRole()
    {
        if(this.role == null)
        {
            Occupant o = multiUserChat.getOccupant(
                multiUserChat.getRoom() + "/" + multiUserChat.getNickname());

            if(o == null)
                return ChatRoomMemberRole.GUEST;
            else
                this.role = smackRoleToScRole(o.getRole(), o.getAffiliation());
        }

        return this.role;
    }

    /**
     * Sets the new rolefor the local user in the context of this chatroom.
     *
     * @param role the new role to be set for the local user
     */
    public void setLocalUserRole(ChatRoomMemberRole role)
    {
        setLocalUserRole(role, false);
    }

    /**
     * Sets the new rolefor the local user in the context of this chatroom.
     *
     * @param role the new role to be set for the local user
     * @param isInitial if <tt>true</tt> this is initial role set.
     */
    public void setLocalUserRole(ChatRoomMemberRole role, boolean isInitial)
    {
        fireLocalUserRoleEvent(getUserRole(), role, isInitial);
        this.role = role;
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
            if (logger.isInfoEnabled())
                logger.info(participant + " has been banned from "
                + getName() + " chat room.");

            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            String participantName = StringUtils.parseResource(participant);

            synchronized (members)
            {
                members.remove(participantName);
            }

            banList.put(participant, member);

            fireMemberRoleEvent(member, member.getCurrentRole(),
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
            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            fireMemberRoleEvent(member, member.getCurrentRole(),
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
            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            fireMemberRoleEvent(member, member.getCurrentRole(),
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
            if (logger.isInfoEnabled())
                logger.info(participant + " has joined the "
                + getName() + " chat room.");

            String participantName = StringUtils.parseResource(participant);

            // We try to get the nickname of the participantName in case it's
            // in the form john@servicename.com, because the nickname we keep
            // in the nickname property is just the user name like "john".
            if (nickname.equals(participantName)
                || members.containsKey(participantName))
                return;

            // when somebody changes its nickname we first receive
            // event for its nickname changed and after that that has joined
            // we check is this already joined and if so we skip it
            if(members.contains(participantName))
                return;

            Occupant occupant = multiUserChat.getOccupant(participant);

            //smack returns fully qualified occupant names.
            ChatRoomMemberJabberImpl member = new ChatRoomMemberJabberImpl(
                  ChatRoomJabberImpl.this,
                  StringEscapeUtils.unescapeXml(occupant.getNick()),
                  occupant.getJid());

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
            if (logger.isInfoEnabled())
                logger.info(participant + " has left the "
                + getName() + " chat room.");

            ChatRoomMember member
                = smackParticipantToScMember(participant);

            if(member == null)
                return;

            String participantName = StringUtils.parseResource(participant);
            synchronized (members)
            {
                members.remove(participantName);
            }

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

            if(nickname.equals(getNickName(member.getName())))
                nickname = getNickName(newNickname);

            ((ChatRoomMemberJabberImpl) member).setName(newNickname);

            String participantName = StringUtils.parseResource(participant);

            synchronized (members)
            {
                // chnage the member key
                ChatRoomMemberJabberImpl mem = members.remove(participantName);
                members.put(newNickname, mem);
            }

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
            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            fireMemberRoleEvent(member, member.getCurrentRole(),
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
            synchronized (members)
            {
                members.remove(participantName);
            }

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
            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            fireMemberRoleEvent(member, member.getCurrentRole(),
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
            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            fireMemberRoleEvent(member, member.getCurrentRole(),
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
            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            fireMemberRoleEvent(member, member.getCurrentRole(),
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
            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            fireMemberRoleEvent(member, member.getCurrentRole(),
                ChatRoomMemberRole.MEMBER);
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
            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            fireMemberRoleEvent(member, member.getCurrentRole(),
                ChatRoomMemberRole.MEMBER);
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
            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            fireMemberRoleEvent(member, member.getCurrentRole(),
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
            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            fireMemberRoleEvent(member, member.getCurrentRole(),
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
     * @return a list of all banned participants
     * @throws OperationFailedException if we could not obtain the ban list
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
            int atIndex = nickname.lastIndexOf("@");
            if(atIndex <= 0)
                this.nickname = nickname;
            else
                this.nickname = nickname.substring(0, atIndex);
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
            multiUserChat.banUser(
                ((ChatRoomMemberJabberImpl)chatRoomMember).getJabberID(),
                reason);
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

        if (logger.isTraceEnabled())
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

        if (logger.isTraceEnabled())
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
        member.setRole(newRole);
        ChatRoomMemberRoleChangeEvent evt
            = new ChatRoomMemberRoleChangeEvent(
                this, member, previousRole, newRole);

        if (logger.isTraceEnabled())
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
    void fireMessageEvent(EventObject evt)
    {
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
                if (evt instanceof ChatRoomMessageDeliveredEvent)
                {
                    listener.messageDelivered(
                        (ChatRoomMessageDeliveredEvent)evt);
                }
                else if (evt instanceof ChatRoomMessageReceivedEvent)
                {
                    listener.messageReceived(
                        (ChatRoomMessageReceivedEvent)evt);
                }
                else if (evt instanceof ChatRoomMessageDeliveryFailedEvent)
                {
                    listener.messageDeliveryFailed(
                        (ChatRoomMessageDeliveryFailedEvent)evt);
                }
            } catch (Throwable e)
            {
                logger.error("Error delivering multi chat message for " +
                    listener, e);
            }
        }
    }

    /**
     * Publishes a conference to the room by sending a <tt>Presence</tt> IQ
     * which contains a <tt>ConferenceDescriptionPacketExtension</tt>
     *
     * @param cd the description of the conference to announce
     * @param name the name of the conference
     * @return the <tt>ConferenceDescription</tt> that was announced (e.g.
     * <tt>cd</tt> on success or <tt>null</tt> on failure)
     */
    public ConferenceDescription publishConference(ConferenceDescription cd,
        String name)
    {
        if (publishedConference != null)
        {
            cd = publishedConference;
            cd.setAvailable(false);
        }
        else
        {
            String displayName;
            if(name == null)
            {
                displayName = JabberActivator.getResources()
                    .getI18NString("service.gui.CHAT_CONFERENCE_ITEM_LABEL",
                        new String[]{nickname});
            }
            else
            {
                displayName = name;
            }
            cd.setDisplayName(displayName);
        }

        ConferenceDescriptionPacketExtension ext
                = new ConferenceDescriptionPacketExtension(cd);
        if (lastPresenceSent != null)
        {
            setPacketExtension(
                lastPresenceSent, ext,
                ConferenceDescriptionPacketExtension.NAMESPACE);
            provider.getConnection().sendPacket(lastPresenceSent);
        }
        else
        {
            logger.warn("Could not publish conference," +
                    " lastPresenceSent is null.");
            publishedConference = null;
            publishedConferenceExt = null;
            return null;
        }

        /*
         * Save the extensions to set to other outgoing Presence packets
         */
        publishedConference
                = (cd == null || !cd.isAvailable())
                ? null
                : cd;
        publishedConferenceExt
                = (publishedConference == null)
                ? null
                : ext;

        fireConferencePublishedEvent(members.get(nickname), cd,
            ChatRoomConferencePublishedEvent.CONFERENCE_DESCRIPTION_SENT);
        return cd;
    }

    /**
     * Sets <tt>ext</tt> as the only <tt>PacketExtension</tt> that belongs to
     * given <tt>namespace</tt> of the <tt>packet</tt>.
     *
     * @param packet the <tt>Packet<tt> to be modified.
     * @param extension the <tt>ConferenceDescriptionPacketExtension<tt> to set,
     * or <tt>null</tt> to not set one.
     * @param namespace the namespace of <tt>PacketExtension</tt>.
     */
    private static void setPacketExtension(
            Packet packet,
            PacketExtension extension,
            String namespace)
    {
        if (org.jitsi.util.StringUtils.isNullOrEmpty(namespace))
        {
            return;
        }

        //clear previous announcements
        PacketExtension pe;
        while (null != (pe = packet.getExtension(namespace)))
        {
            packet.removeExtension(pe);
        }

        if (extension != null)
        {
            packet.addExtension(extension);
        }
    }

    /**
     * Publishes new status message in chat room presence.
     * @param newStatus the new status message to be published in the MUC.
     */
    public void publishPresenceStatus(String newStatus)
    {
        if (lastPresenceSent != null)
        {
            lastPresenceSent.setStatus(newStatus);

            provider.getConnection().sendPacket(lastPresenceSent);
        }
    }

    /**
     * Adds given <tt>PacketExtension</tt> to the MUC presence and publishes it
     * immediately.
     * @param extension the <tt>PacketExtension</tt> to be included in MUC
     *                  presence.
     */
    public void sendPresenceExtension(PacketExtension extension)
    {
        if (lastPresenceSent != null)
        {
            setPacketExtension(
                lastPresenceSent, extension, extension.getNamespace());

            provider.getConnection().sendPacket(lastPresenceSent);
        }
    }

    /**
     * Returns the ids of the users that has the member role in the room.
     * When the room is member only, this are the users allowed to join.
     * @return the ids of the users that has the member role in the room.
     */
    public List<String> getMembersWhiteList()
    {
        List<String> res = new ArrayList<String>();
        try
        {

            for(Affiliate a : multiUserChat.getMembers())
            {
                res.add(a.getJid());
            }
        }
        catch(XMPPException e)
        {
            logger.error("Cannot obtain members list", e);
        }

        return res;
    }

    /**
     * Changes the list of users that has role member for this room.
     * When the room is member only, this are the users allowed to join.
     * @param members the ids of user to have member role.
     */
    public void setMembersWhiteList(List<String> members)
    {
        try
        {
            List<String> membersToRemove = getMembersWhiteList();
            membersToRemove.removeAll(members);

            if(membersToRemove.size() > 0)
                multiUserChat.revokeMembership(membersToRemove);

            if(members.size() > 0)
                multiUserChat.grantMembership(members);
        }
        catch(XMPPException e)
        {
            logger.error("Cannot modify members list", e);
        }
    }

    /**
     * A listener that listens for packets of type Message and fires an event
     * to notifier interesting parties that a message was received.
     */
    private class SmackMessageListener
        implements PacketListener
    {
        /**
         * The timestamp of the last history message sent to the UI.
         * Do not send earlier or messages with the same timestamp.
         */
        private Date lastSeenDelayedMessage = null;

        /**
         * The property to store the timestamp.
         */
        private static final String LAST_SEEN_DELAYED_MESSAGE_PROP
            = "lastSeenDelayedMessage";

        /**
         * Process a packet.
         * @param packet to process.
         */
        public void processPacket(Packet packet)
        {
            if(!(packet instanceof org.jivesoftware.smack.packet.Message))
                return;

            org.jivesoftware.smack.packet.Message msg
                = (org.jivesoftware.smack.packet.Message) packet;

            Date timeStamp;

            DelayInformation delay =
                (DelayInformation)msg.getExtension("x", "jabber:x:delay");

            if(delay != null)
            {
                timeStamp = delay.getStamp();

                // This is a delayed chat room message, a history message for
                // the room coming from server. Lets check have we already
                // shown this message and if this is the case skip it
                // otherwise save it as last seen delayed message
                if(lastSeenDelayedMessage == null)
                {
                    // initialise this from configuration
                    String timestamp =
                        ConfigurationUtils.getChatRoomProperty(
                            provider,
                            getIdentifier(),
                            LAST_SEEN_DELAYED_MESSAGE_PROP);

                    try
                    {
                        lastSeenDelayedMessage =
                            new Date(Long.parseLong(timestamp));
                    }
                    catch(Throwable t)
                    {}
                }

                if(lastSeenDelayedMessage != null
                    && !timeStamp.after(lastSeenDelayedMessage))
                    return;

                // save it in configuration
                ConfigurationUtils.updateChatRoomProperty(
                    provider,
                    getIdentifier(),
                    LAST_SEEN_DELAYED_MESSAGE_PROP,
                    String.valueOf(timeStamp.getTime()));

                lastSeenDelayedMessage = timeStamp;
            }
            else
            {
                timeStamp = new Date();
            }

            String msgBody = msg.getBody();

            if(msgBody == null)
                return;

            int messageReceivedEventType =
                ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED;

            String msgFrom = msg.getFrom();
            ChatRoomMember member = null;

            String fromUserName = StringUtils.parseResource(msgFrom);

            // when the message comes from the room itself its a system message
            if(msgFrom.equals(getName()))
            {
                messageReceivedEventType =
                    ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED;
                member = new ChatRoomMemberJabberImpl(
                    ChatRoomJabberImpl.this, getName(), getName());
            }
            else
            {
                member = smackParticipantToScMember(msgFrom);
            }

            // sometimes when connecting to rooms they send history
            // when the member is no longer available we create
            // a fake one so the messages to be displayed.
            if(member == null)
            {
                member = new ChatRoomMemberJabberImpl(
                    ChatRoomJabberImpl.this, fromUserName, msgFrom);
            }

            if(logger.isDebugEnabled())
            {
                if (logger.isDebugEnabled())
                    logger.debug("Received from "
                             + fromUserName
                             + " the message "
                             + msg.toXML());
            }

            Message newMessage = createMessage(msgBody);

            // if we are sending this message, this either a delivery report
            // or if there is a delay extension this is a history coming from
            // the chat room
            if(getUserNickname().equals(fromUserName))
            {
                // message delivered
                ChatRoomMessageDeliveredEvent msgDeliveredEvt
                     = new ChatRoomMessageDeliveredEvent(
                         ChatRoomJabberImpl.this,
                         timeStamp,
                         newMessage,
                         ChatRoomMessageDeliveredEvent
                            .CONVERSATION_MESSAGE_DELIVERED);

                if(delay != null)
                    msgDeliveredEvt.setHistoryMessage(true);

                fireMessageEvent(msgDeliveredEvt);
                return;
            }

            if(msg.getType() == org.jivesoftware.smack.packet.Message.Type.error)
            {
                if (logger.isInfoEnabled())
                    logger.info("Message error received from " + fromUserName);

                XMPPError error = packet.getError();
                int errorCode = error.getCode();
                int errorResultCode
                    = ChatRoomMessageDeliveryFailedEvent.UNKNOWN_ERROR;
                String errorReason = error.getMessage();

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
                        errorReason,
                        new Date(),
                        newMessage);

                fireMessageEvent(evt);
                return;
            }

            ChatRoomMessageReceivedEvent msgReceivedEvt
                = new ChatRoomMessageReceivedEvent(
                    ChatRoomJabberImpl.this,
                    member,
                    timeStamp,
                    newMessage,
                    messageReceivedEventType);

            if(delay != null)
                msgReceivedEvt.setHistoryMessage(true);

            if(messageReceivedEventType
                == ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED
                && newMessage.getContent().contains(getUserNickname() + ":"))
            {
                msgReceivedEvt.setImportantMessage(true);
            }

            fireMessageEvent(msgReceivedEvt);
        }
    }

    /**
     * A listener that is fired anytime a MUC room changes its subject.
     */
    private class SmackSubjectUpdatedListener implements SubjectUpdatedListener
    {
        /**
         * Notification that subject has changed
         * @param subject the new subject
         * @param from
         */
        public void subjectUpdated(String subject, String from)
        {
            if (logger.isInfoEnabled())
                logger.info("Subject updated to " + subject);

            // only fire event if subject has really changed, not for new one
            if(subject != null && !subject.equals(oldSubject))
            {
                ChatRoomPropertyChangeEvent evt
                    = new ChatRoomPropertyChangeEvent(
                        ChatRoomJabberImpl.this,
                        ChatRoomPropertyChangeEvent.CHAT_ROOM_SUBJECT,
                        oldSubject,
                        subject);

                firePropertyChangeEvent(evt);
            }

            // Keeps track of the subject.
            oldSubject = subject;
        }
    }

    /**
     * A listener that is fired anytime your participant's status in a room
     * is changed, such as the user being kicked, banned, or granted admin
     * permissions.
     */
    private class UserListener implements UserStatusListener
    {
        /**
         * Called when a moderator kicked your user from the room. This
         * means that you are no longer participating in the room.
         *
         * @param actor the moderator that kicked your user from the room
         * (e.g. user@host.org).
         * @param reason the reason provided by the actor to kick you from
         * the room.
         */
        public void kicked(String actor, String reason)
        {
            opSetMuc.fireLocalUserPresenceEvent(
                ChatRoomJabberImpl.this,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_KICKED,
                reason);
            leave();
        }

        /**
         * Called when a moderator grants voice to your user. This means that
         * you were a visitor in the moderated room before and now you can
         * participate in the room by sending messages to all occupants.
         */
        public void voiceGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when a moderator revokes voice from your user. This means that
        * you were a participant in the room able to speak and now you are a
        * visitor that can't send messages to the room occupants.
        */
        public void voiceRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.SILENT_MEMBER);
        }

        /**
        * Called when an administrator or owner banned your user from the room.
        * This means that you will no longer be able to join the room unless the
        * ban has been removed.
        *
        * @param actor the administrator that banned your user
        * (e.g. user@host.org).
        * @param reason the reason provided by the administrator to banned you.
        */
        public void banned(String actor, String reason)
        {
            opSetMuc.fireLocalUserPresenceEvent(ChatRoomJabberImpl.this,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED, reason);
            leave();
        }

        /**
        * Called when an administrator grants your user membership to the room.
        * This means that you will be able to join the members-only room.
        */
        public void membershipGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when an administrator revokes your user membership to the room.
        * This means that you will not be able to join the members-only room.
        */
        public void membershipRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.GUEST);
        }

        /**
        * Called when an administrator grants moderator privileges to your user.
        * This means that you will be able to kick users, grant and revoke
        * voice, invite other users, modify room's subject plus all the
        * participants privileges.
        */
        public void moderatorGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.MODERATOR);
        }

        /**
        * Called when an administrator revokes moderator privileges from your
        * user. This means that you will no longer be able to kick users, grant
        * and revoke voice, invite other users, modify room's subject plus all
        * the participants privileges.
        */
        public void moderatorRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when an owner grants to your user ownership on the room. This
        * means that you will be able to change defining room features as well
        * as perform all administrative functions.
        */
        public void ownershipGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.OWNER);
        }

        /**
        * Called when an owner revokes from your user ownership on the room.
        * This means that you will no longer be able to change defining room
        * features as well as perform all administrative functions.
        */
        public void ownershipRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when an owner grants administrator privileges to your user.
        * This means that you will be able to perform administrative functions
        * such as banning users and edit moderator list.
        */
        public void adminGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.ADMINISTRATOR);
        }

        /**
        * Called when an owner revokes administrator privileges from your user.
        * This means that you will no longer be able to perform administrative
        * functions such as banning users and edit moderator list.
        */
        public void adminRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }
    }

    /**
     * Creates the corresponding ChatRoomLocalUserRoleChangeEvent and notifies
     * all <tt>ChatRoomLocalUserRoleListener</tt>s that local user's role has
     * been changed in this <tt>ChatRoom</tt>.
     *
     * @param previousRole the previous role that local user had
     * @param newRole the new role the local user gets
     * @param isInitial if <tt>true</tt> this is initial role set.
     */
    private void fireLocalUserRoleEvent(ChatRoomMemberRole previousRole,
                                        ChatRoomMemberRole newRole,
                                        boolean isInitial)
    {
        ChatRoomLocalUserRoleChangeEvent evt
            = new ChatRoomLocalUserRoleChangeEvent(
                    this, previousRole, newRole, isInitial);

        if (logger.isTraceEnabled())
            logger.trace("Will dispatch the following ChatRoom event: " + evt);

        Iterable<ChatRoomLocalUserRoleListener> listeners;
        synchronized (localUserRoleListeners)
        {
            listeners = new ArrayList<ChatRoomLocalUserRoleListener>(
                            localUserRoleListeners);
        }

        for (ChatRoomLocalUserRoleListener listener : listeners)
            listener.localUserRoleChanged(evt);
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
        }
        catch (Exception ex)
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
        synchronized (members)
        {
            return members.get(jabberID);
        }
    }

   /**
    * Grants administrator privileges to another user. Room owners may grant
    * administrator privileges to a member or un-affiliated user. An
    * administrator is allowed to perform administrative functions such as
    * banning users and edit moderator list.
    *
    * @param jid the bare XMPP user ID of the user to grant administrator
    * privileges (e.g. "user@host.org").
    */
    public void grantAdmin(String jid)
    {
        try
        {
            multiUserChat.grantAdmin(jid);
        }
        catch (XMPPException ex)
        {
            logger.error("An error occurs granting administrator " +
                "privileges to a user.", ex);
        }
    }

   /**
    * Grants membership to a user. Only administrators are able to grant
    * membership. A user that becomes a room member will be able to enter a room
    * of type Members-Only (i.e. a room that a user cannot enter without being
    * on the member list).
    *
    * @param jid the bare XMPP user ID of the user to grant membership
    * privileges (e.g. "user@host.org").
    */
    public void grantMembership(String jid)
    {
        try
        {
            multiUserChat.grantMembership(jid);
        }
        catch (XMPPException ex)
        {
            logger.error("An error occurs granting membership to a user", ex);
        }
    }

   /**
    * Grants moderator privileges to a participant or visitor. Room
    * administrators may grant moderator privileges. A moderator is allowed to
    * kick users, grant and revoke voice, invite other users, modify room's
    * subject plus all the partcipants privileges.
    *
    * @param nickname the nickname of the occupant to grant moderator
    * privileges.
    */
    public void grantModerator(String nickname)
    {
        try
        {
            multiUserChat.grantModerator(nickname);
        }
        catch (XMPPException ex)
        {
            logger.error("An error occurs granting moderator " +
                "privileges to a user", ex);
        }
    }

   /**
    * Grants ownership privileges to another user. Room owners may grant
    * ownership privileges. Some room implementations will not allow to grant
    * ownership privileges to other users. An owner is allowed to change
    * defining room features as well as perform all administrative functions.
    *
    * @param jid the bare XMPP user ID of the user to grant ownership
    * privileges (e.g. "user@host.org").
    */
    public void grantOwnership(String jid)
    {
        try
        {
            multiUserChat.grantOwnership(jid);
        }
        catch (XMPPException ex)
        {
            logger.error("An error occurs granting ownership " +
                "privileges to a user", ex);
        }
    }

   /**
    * Grants voice to a visitor in the room. In a moderated room, a moderator
    * may want to manage who does and does not have "voice" in the room. To have
    * voice means that a room occupant is able to send messages to the room
    * occupants.
    *
    * @param nickname the nickname of the visitor to grant voice in the room
    * (e.g. "john").
    *
    * XMPPException if an error occurs granting voice to a visitor. In
    * particular, a 403 error can occur if the occupant that intended to grant
    * voice is not a moderator in this room (i.e. Forbidden error); or a 400
    * error can occur if the provided nickname is not present in the room.
    */
    public void grantVoice(String nickname)
    {
        try
        {
            multiUserChat.grantVoice(nickname);
        }
        catch (XMPPException ex)
        {
            logger.error("An error occurs granting voice to a visitor", ex);
        }
    }

   /**
    * Revokes administrator privileges from a user. The occupant that loses
    * administrator privileges will become a member. Room owners may revoke
    * administrator privileges from a member or unaffiliated user.
    *
    * @param jid the bare XMPP user ID of the user to grant administrator
    * privileges (e.g. "user@host.org").
    */
    public void revokeAdmin(String jid)
    {
        try
        {
            multiUserChat.revokeAdmin(jid);
        }
        catch (XMPPException ex)
        {
            logger.error("n error occurs revoking administrator " +
                "privileges to a user", ex);
        }
    }

   /**
    * Revokes a user's membership. Only administrators are able to revoke
    * membership. A user that becomes a room member will be able to enter a room
    * of type Members-Only (i.e. a room that a user cannot enter without being
    * on the member list). If the user is in the room and the room is of type
    * members-only then the user will be removed from the room.
    *
    * @param jid the bare XMPP user ID of the user to revoke membership
    * (e.g. "user@host.org").
    */
    public void revokeMembership(String jid)
    {
        try
        {
            multiUserChat.revokeMembership(jid);
        }
        catch (XMPPException ex)
        {
            logger.error("An error occurs revoking membership to a user", ex);
        }
    }

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
    public void revokeModerator(String nickname)
    {
        try
        {
            multiUserChat.revokeModerator(nickname);
        }
        catch (XMPPException ex)
        {
            logger.error("n error occurs revoking moderator " +
                "privileges from a user", ex);
        }
    }

   /**
    * Revokes ownership privileges from another user. The occupant that loses
    * ownership privileges will become an administrator. Room owners may revoke
    * ownership privileges. Some room implementations will not allow to grant
    * ownership privileges to other users.
    *
    * @param jid the bare XMPP user ID of the user to revoke ownership
    * (e.g. "user@host.org").
    */
    public void revokeOwnership(String jid)
    {
        try
        {
            multiUserChat.revokeOwnership(jid);
        }
        catch (XMPPException ex)
        {
            logger.error("An error occurs revoking ownership " +
                "privileges from a user", ex);
        }
    }

    /**
    * Revokes voice from a participant in the room. In a moderated room, a
    * moderator may want to revoke an occupant's privileges to speak. To have
    * voice means that a room occupant is able to send messages to the room
    * occupants.
    * @param nickname the nickname of the participant to revoke voice
    * (e.g. "john").
    *
    * XMPPException if an error occurs revoking voice from a participant.
    * In particular, a 405 error can occur if a moderator or a user
    * with an affiliation of "owner" or "admin" was tried to revoke his voice
    * (i.e. Not Allowed error); or a 400 error can occur if the provided
    * nickname is not present in the room.
    */
    public void revokeVoice(String nickname)
    {
        try
        {
            multiUserChat.revokeVoice(nickname);
        }
        catch (XMPPException ex)
        {
            logger.info("An error occurs revoking voice from a participant", ex);
        }
    }

    /**
     * Returns the nickname of the given participant name. For example, for
     * the address "john@xmppservice.com", "john" would be returned. If no @
     * is found in the address we return the given name.
     * @param participantAddress the address of the participant
     * @return the nickname part of the given participant address
     */
    static String getNickName(String participantAddress)
    {
        if (participantAddress == null)
            return null;

        int atIndex = participantAddress.lastIndexOf("@");
        if (atIndex <= 0)
            return participantAddress;
        else
            return participantAddress.substring(0, atIndex);
    }

    /**
     * Returns the internal stack used chat room instance.
     * @return the chat room used in the protocol stack.
     */
    MultiUserChat getMultiUserChat()
    {
        return multiUserChat;
    }

    /**
     * Listens for presence packets.
     */
    private class PresenceListener
    implements PacketListener
    {
        /**
         * Chat room associated with the listener.
         */
        private ChatRoom chatRoom;

        /**
         * Creates an instance of a listener of presence packets.
         *
         * @param chatRoom the chat room associated with the listener
         */
        public PresenceListener(ChatRoom chatRoom)
        {
            super();
            this.chatRoom = chatRoom;
        }

        /**
         * Processes an incoming presence packet.
         * @param packet the incoming packet.
         */
        @Override
        public void processPacket(Packet packet)
        {
            if (packet == null
                    || !(packet instanceof Presence)
                    || packet.getError() != null)
            {
                logger.warn("Unable to handle packet: " + packet);
                return;
            }

            Presence presence = (Presence) packet;
            String ourOccupantJid
                    = multiUserChat.getRoom() + "/" + multiUserChat.getNickname();
            if (ourOccupantJid.equals(presence.getFrom()))
                processOwnPresence(presence);
            else
                processOtherPresence(presence);
        }

        /**
         * Processes a <tt>Presence</tt> packet addressed to our own occupant
         * JID.
         * @param presence the packet to process.
         */
        private void processOwnPresence(Presence presence)
        {
            MUCUser mucUser = getMUCUserExtension(presence);

            if (mucUser != null)
            {
                String affiliation = mucUser.getItem().getAffiliation();
                String role = mucUser.getItem().getRole();

                // if status 201 is available means that
                // room is created and locked till we send
                // the configuration
                if (mucUser.getStatus() != null
                    && "201".equals(mucUser.getStatus().getCode()))
                {
                    try
                    {
                        multiUserChat.sendConfigurationForm(
                            new Form(Form.TYPE_SUBMIT));
                    } catch (XMPPException e)
                    {
                        logger.error("Failed to send config form.", e);
                    }

                    opSetMuc.addSmackInvitationRejectionListener(multiUserChat,
                        chatRoom);

                    if(affiliation.equalsIgnoreCase(ChatRoomMemberRole.OWNER
                            .getRoleName().toLowerCase()))
                    {
                        setLocalUserRole(ChatRoomMemberRole.OWNER, true);
                    }
                    else
                        setLocalUserRole(ChatRoomMemberRole.MODERATOR, true);
                }
                else
                {
                    // this is the presence for our member initial role and
                    // affiliation, as smack do not fire any initial
                    // events lets check it and fire events
                    ChatRoomMemberRole jitsiRole =
                        ChatRoomJabberImpl.smackRoleToScRole(role, affiliation);

                    if(jitsiRole == ChatRoomMemberRole.MODERATOR
                        || jitsiRole == ChatRoomMemberRole.OWNER
                        || jitsiRole == ChatRoomMemberRole.ADMINISTRATOR)
                    {
                        setLocalUserRole(jitsiRole, true);
                    }

                    if(!presence.isAvailable()
                        && "none".equalsIgnoreCase(affiliation)
                        && "none".equalsIgnoreCase(role))
                    {
                        MUCUser.Destroy destroy = mucUser.getDestroy();
                        if(destroy == null)
                        {
                            // the room is unavailable to us, there is no
                            // message we will just leave
                            leave();
                        }
                        else
                        {
                            leave(destroy.getReason(), destroy.getJid());
                        }
                    }
                }
            }
        }

        /**
         * Process a <tt>Presence</tt> packet sent by one of the other room
         * occupants.
         */
        private void processOtherPresence(Presence presence)
        {
            PacketExtension ext
                    = presence.getExtension(
                    ConferenceDescriptionPacketExtension.NAMESPACE);
            if(presence.isAvailable() && ext != null)
            {
                ConferenceDescriptionPacketExtension cdExt
                        = (ConferenceDescriptionPacketExtension) ext;

                ConferenceDescription cd = cdExt.toConferenceDescription();

                String from = presence.getFrom();
                String participantName = null;
                if (from != null)
                {
                    participantName = StringUtils.parseResource(from);
                }
                ChatRoomMember member = members.get(participantName);

                if(!processConferenceDescription(cd, participantName))
                    return;

                if (member != null)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("Received " + cd + " from " +
                                participantName + "in " +
                                multiUserChat.getRoom());
                    fireConferencePublishedEvent(member, cd,
                        ChatRoomConferencePublishedEvent
                            .CONFERENCE_DESCRIPTION_RECEIVED);
                }
                else
                {
                    logger.warn("Received a ConferenceDescription from an " +
                            "unknown member ("+participantName+") in " +
                            multiUserChat.getRoom());
                }
            }
        }
    }

    /**
     * Listens for rejection message and delivers system message when received.
     */
    private class InvitationRejectionListeners
        implements PacketListener
    {
        /**
         * Process incoming packet, checking for muc extension.
         * @param packet the incoming packet.
         */
        public void processPacket(Packet packet)
        {
            MUCUser mucUser = getMUCUserExtension(packet);

            // Check if the MUCUser informs that the invitee
            // has declined the invitation
            if (mucUser != null
                && mucUser.getDecline() != null
                && ((org.jivesoftware.smack.packet.Message) packet).getType()
                    != org.jivesoftware.smack.packet.Message.Type.error)
            {
                int messageReceivedEventType =
                    ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED;
                ChatRoomMemberJabberImpl member = new ChatRoomMemberJabberImpl(
                    ChatRoomJabberImpl.this, getName(), getName());

                String from = mucUser.getDecline().getFrom();

                OperationSetPersistentPresenceJabberImpl presenceOpSet
                    = (OperationSetPersistentPresenceJabberImpl) provider
                        .getOperationSet(OperationSetPersistentPresence.class);
                if(presenceOpSet != null)
                {
                    Contact c = presenceOpSet.findContactByID(
                        StringUtils.parseBareAddress(from));
                    if(c != null)
                    {
                        if(!from.contains(c.getDisplayName()))
                            from = c.getDisplayName() + " (" + from + ")";
                    }
                }

                String msgBody =
                    JabberActivator.getResources().getI18NString(
                        "service.gui.INVITATION_REJECTED",
                        new String[]{from, mucUser.getDecline().getReason()});

                ChatRoomMessageReceivedEvent msgReceivedEvt
                    = new ChatRoomMessageReceivedEvent(
                        ChatRoomJabberImpl.this,
                        member,
                        new Date(),
                        createMessage(msgBody),
                        messageReceivedEventType);

                fireMessageEvent(msgReceivedEvt);
            }
        }
    }

    /**
     * The <tt>PacketInterceptor</tt> we use to make sure that our outgoing
     * <tt>Presence</tt> packets contain the correct
     * <tt>ConferenceAnnouncementPacketExtension</tt>.
     */
    private class PresenceInterceptor
            implements PacketInterceptor
    {
        /**
         * {@inheritDoc}
         *
         * Adds <tt>this.publishedConferenceExt</tt> as the only
         * <tt>ConferenceAnnouncementPacketExtension</tt> of <tt>packet</tt>.
         */
        @Override
        public void interceptPacket(Packet packet)
        {
            if (packet instanceof Presence)
            {
                setPacketExtension(
                    packet,
                    publishedConferenceExt,
                    ConferenceDescriptionPacketExtension.NAMESPACE);

                lastPresenceSent = (Presence) packet;
            }
        }
    }


    /**
     * Updates the presence status of private messaging contact.
     *
     * @param nickname the nickname of the contact.
     */
    public void updatePrivateContactPresenceStatus(String nickname)
    {
        OperationSetPersistentPresenceJabberImpl presenceOpSet
            = (OperationSetPersistentPresenceJabberImpl) provider
                .getOperationSet(OperationSetPersistentPresence.class);
        ContactJabberImpl sourceContact
            = (ContactJabberImpl)presenceOpSet.findContactByID(getName() + "/"
                    + nickname);

        updatePrivateContactPresenceStatus(sourceContact);
    }

    /**
     * Updates the presence status of private messaging contact.
     *
     * @param contact the contact.
     */
    public void updatePrivateContactPresenceStatus(Contact contact)
    {
        OperationSetPersistentPresenceJabberImpl presenceOpSet
            = (OperationSetPersistentPresenceJabberImpl) provider
                .getOperationSet(OperationSetPersistentPresence.class);

        if(contact == null)
            return;

        PresenceStatus oldContactStatus
            = contact.getPresenceStatus();
        String nickname = StringUtils.parseResource(contact.getAddress());
        boolean isOffline = !members.containsKey(nickname);

        PresenceStatus offlineStatus =
            provider.getJabberStatusEnum().getStatus(
                isOffline
                    ? JabberStatusEnum.OFFLINE : JabberStatusEnum.AVAILABLE);

        // When status changes this may be related to a change in the
        // available resources.
        ((ContactJabberImpl)contact).updatePresenceStatus(offlineStatus);

        presenceOpSet.fireContactPresenceStatusChangeEvent(contact,
            contact.getParentContactGroup(),
            oldContactStatus, offlineStatus);
    }
}
