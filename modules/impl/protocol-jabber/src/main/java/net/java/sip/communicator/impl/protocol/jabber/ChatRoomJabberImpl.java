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

import org.jitsi.xmpp.extensions.condesc.*;
import org.jitsi.xmpp.extensions.jitsimeet.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.jabberconstants.*;
import net.java.sip.communicator.util.*;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.XMPPException.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.StanzaError.*;
import org.jivesoftware.smack.packet.id.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.delay.packet.*;
import org.jivesoftware.smackx.disco.*;
import org.jivesoftware.smackx.disco.packet.*;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smackx.muc.MultiUserChatException.*;
import org.jivesoftware.smackx.muc.filter.*;
import org.jivesoftware.smackx.muc.packet.*;
import org.jivesoftware.smackx.nick.packet.Nick;
import org.jivesoftware.smackx.xdata.form.*;
import org.jivesoftware.smackx.xevent.packet.MessageEvent;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.jxmpp.jid.parts.*;
import org.jxmpp.stringprep.*;

import java.beans.*;
import java.lang.reflect.*;
import java.util.*;

import static org.jivesoftware.smack.packet.StanzaError.Condition.*;

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
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChatRoomJabberImpl.class);

    /**
     * The multi user chat smack object that we encapsulate in this room.
     */
    private final MultiUserChat multiUserChat;

    /**
     * Listeners that will be notified of changes in member status in the
     * room such as member joined, left or being kicked or dropped.
     */
    private final Vector<ChatRoomMemberPresenceListener> memberListeners
        = new Vector<>();

    /**
     * Listeners that will be notified of changes in member role in the
     * room such as member being granted admin permissions, or revoked admin
     * permissions.
     */
    private final Vector<ChatRoomMemberRoleListener> memberRoleListeners
        = new Vector<>();

    /**
     * Listeners that will be notified of changes in local user role in the
     * room such as member being granted admin permissions, or revoked admin
     * permissions.
     */
    private final Vector<ChatRoomLocalUserRoleListener> localUserRoleListeners
        = new Vector<>();

    /**
     * Listeners that will be notified every time
     * a new message is received on this chat room.
     */
    private final Vector<ChatRoomMessageListener> messageListeners
        = new Vector<>();

    /**
     * Listeners that will be notified every time
     * a chat room property has been changed.
     */
    private final Vector<ChatRoomPropertyChangeListener> propertyChangeListeners
        = new Vector<>();

    /**
     * Listeners that will be notified every time
     * a chat room member property has been changed.
     */
    private final Vector<ChatRoomMemberPropertyChangeListener>
        memberPropChangeListeners
            = new Vector<>();

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
    private final Hashtable<Resourcepart, ChatRoomMemberJabberImpl> members
        = new Hashtable<>();

    /**
     * The list of banned members of this chat room.
     */
    private final Hashtable<Resourcepart, ChatRoomMember> banList
        = new Hashtable<>();

    /**
     * The nickname of this chat room local user participant.
     */
    private Resourcepart nickname;

    /**
     * The subject of this chat room. Keeps track of the subject changes.
     */
    private String oldSubject;

    /**
     * The role of this chat room local user participant.
     */
    private ChatRoomMemberRole role = null;

    /**
     * Packet listener waits for rejection of invitations to join room.
     */
    private final InvitationRejectionListeners invitationRejectionListeners
        = new InvitationRejectionListeners();

    /**
     * Listens for the last presence that was sent and stores it.
     */
    private final LastPresenceListener lastPresenceListener = new LastPresenceListener();

    /**
     * Intercepts presences to set custom extensions.
     */
    private final Consumer<PresenceBuilder> presenceInterceptor = this::presenceIntercept;

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
    private ConferenceDescriptionExtension publishedConferenceExt = null;

    /**
     * List of packet extensions we need to add to every outgoing presence
     * we send.
     * Currently used from external components reusing the protocol provider
     * to permanently add extension to the outgoing stanzas.
     */
    private final List<ExtensionElement> presencePacketExtensions
        = new ArrayList<>();

    /**
     * The last <tt>Presence</tt> packet we sent to the MUC.
     */
    private Presence lastPresenceSent = null;

    private final List<CallJabberImpl> chatRoomConferenceCalls
        = new ArrayList<>();

    /**
     * The Presence listener instance.
     */
    private final ChatRoomPresenceListener presenceListener = new ChatRoomPresenceListener();

    /**
     * A listener that is fired anytime a MUC room changes its subject.
     */
    private final SmackSubjectUpdatedListener smackSubjectUpdatedListener = new SmackSubjectUpdatedListener();

    /**
     * A listener that listens for packets of type Message and fires an event
     * to notifier interesting parties that a message was received.
     */
    private final SmackMessageListener smackMessageListener = new SmackMessageListener();

    /**
     * Instances of this class should be registered as
     * <tt>ParticipantStatusListener</tt> in smack and translates events .
     */
    private final MemberListener memberListener = new MemberListener();

    /**
     * A listener that is fired anytime your participant's status in a room
     * is changed, such as the user being kicked, banned, or granted admin
     * permissions.
     */
    private final UserListener userListener = new UserListener();

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
    }

    /**
     * Returns a Jid for associated lobby room with this chat room.
     *
     * @return <tt>Jid</tt> lobby room Jid.
     */
    private Jid getLobbyJidFromPacket(Stanza packet)
    {
        Jid lobbyJid = null;

        // This method is used to get a Jid that represents the lobby room that the user joins when trying
        // to join a meeting with lobby enabled. The custom <lobbyroom></lobbyroom> field is added to the error
        // in case the user is not yet a member of the meeting that was joined initially.
        try
        {
            if (packet != null)
            {
                ExtensionElement lobbyExtension = packet.getError().getExtension(
                    "lobbyroom", "http://jitsi.org/jitmeet");

                // let's fallback to old code if this is missing, TODO: drop this at some point
                if (lobbyExtension == null)
                {
                    lobbyExtension = packet.getExtensionElement("lobbyroom", "jabber:client");
                }

                if (lobbyExtension instanceof StandardExtensionElement)
                {
                    StandardExtensionElement lobbyStandardExtension = (StandardExtensionElement) lobbyExtension;

                    String lobbyJidString = lobbyStandardExtension.getText();

                    lobbyJid = JidCreate.entityBareFrom(lobbyJidString);
                }
            }
        }
        catch(Exception ex)
        {
            logger.error("Failed to extract lobbyroom from stanza", ex);
        }

        return lobbyJid;
    }

    /**
     * Returns the MUCUser packet extension included in the packet or <tt>null</tt> if none.
     *
     * @param packet the packet that may include the MUCUser extension.
     * @return the MUCUser found in the packet.
     */
    private MUCUser getMUCUserExtension(Stanza packet)
    {
        if (packet != null)
        {
            // Get the MUC User extension
            return packet.getExtension(MUCUser.class);
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
        return new MessageJabberImpl(
            messageText,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING,
            null);
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
            return new LinkedList<>(members.values());
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
        return multiUserChat.getRoom().toString();
    }

    /**
     * Returns the identifier of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the identifier of this
     *   <tt>ChatRoom</tt>.
     */
    public String getIdentifier()
    {
        return multiUserChat.getRoom().toString();
    }

    public EntityBareJid getIdentifierAsJid()
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
        return multiUserChat.getNickname() == null ? null : multiUserChat.getNickname().toString();
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
        Jid jid;
        try
        {
            jid = JidCreate.fullFrom(
                multiUserChat.getRoom(),
                Resourcepart.from(nickname));
        }
        catch (XmppStringprepException e)
        {
            throw new IllegalArgumentException("Invalid XMPP nickname");
        }
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
        try
        {
            multiUserChat.invite(JidCreate.entityBareFrom(userAddress), reason);
        }
        catch (InterruptedException
                | NotConnectedException
                | XmppStringprepException e)
        {
            logger.error("Could not invite " + userAddress, e);
        }
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
        try
        {
            this.nickname = Resourcepart.from(nickname);
        }
        catch (XmppStringprepException e)
        {
            throw new OperationFailedException("Nickname is empty", 0, e);
        }

        try
        {
            if (multiUserChat.isJoined())
            {
                if (!multiUserChat.getNickname().equals(this.nickname))
                    multiUserChat.changeNickname(this.nickname);
            }
            else
            {
                multiUserChat.addSubjectUpdatedListener(smackSubjectUpdatedListener);
                multiUserChat.addMessageListener(smackMessageListener);
                multiUserChat.addParticipantStatusListener(memberListener);
                multiUserChat.addUserStatusListener(userListener);

                XMPPConnection connection = this.provider.getConnection();

                // stores the last sent presence
                connection.addStanzaSendingListener(
                    lastPresenceListener,
                    new AndFilter(ToMatchesFilter.create(multiUserChat.getRoom()), StanzaTypeFilter.PRESENCE));

                // intercepts the presence to add custom extensions
                connection.addPresenceInterceptor(
                    presenceInterceptor,
                    ToMatchesFilter.create(multiUserChat.getRoom()).asPredicate(Presence.class)
                );

                connection.addAsyncStanzaListener(invitationRejectionListeners, StanzaTypeFilter.MESSAGE);

                connection.addAsyncStanzaListener(
                    presenceListener,
                    new AndFilter(FromMatchesFilter.create(multiUserChat.getRoom()), StanzaTypeFilter.PRESENCE));

                if(password == null)
                    multiUserChat.join(this.nickname);
                else
                    multiUserChat.join(this.nickname, new String(password));
            }

            ChatRoomMemberJabberImpl member
                = new ChatRoomMemberJabberImpl( this,
                    this.nickname,
                    JidCreate.bareFrom(
                            provider.getAccountID().getAccountAddress()));
            synchronized (members)
            {
                members.put(this.nickname, member);
            }

            // We don't specify a reason.
            opSetMuc.fireLocalUserPresenceEvent(this,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED, null);
        }
        catch (XMPPErrorException ex)
        {
            String errorMessage;

            if(ex.getStanzaError() == null)
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
            else if(ex.getStanzaError().getCondition() == not_authorized)
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
            else if(ex.getStanzaError().getCondition() == registration_required)
            {
                errorMessage
                    = "Failed to join chat room "
                        + getName()
                        + " with nickname: "
                        + nickname
                        + ". The chat room requires registration.";

                if (logger.isDebugEnabled())
                {
                    logger.debug(errorMessage, ex);
                }
                else
                {
                    logger.error(errorMessage);
                }

                OperationFailedException operationFailedException = new OperationFailedException(
                    errorMessage,
                    OperationFailedException.REGISTRATION_REQUIRED,
                    ex);

                Jid lobbyJid = getLobbyJidFromPacket(ex.getStanza());
                if (lobbyJid != null)
                {
                    operationFailedException
                        .getDataObject()
                        .setData("lobbyroomjid", lobbyJid);
                }
                else
                {
                    logger.warn("No lobby Jid! But registration required!");
                }

                throw operationFailedException;
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
    public static ChatRoomMemberRole smackRoleToScRole(MUCRole smackRole,
                                                       MUCAffiliation affiliation)
    {
        if(affiliation != null)
        {
            if(affiliation == MUCAffiliation.admin)
            {
                return ChatRoomMemberRole.ADMINISTRATOR;
            }
            else if(affiliation == MUCAffiliation.owner)
            {
                return ChatRoomMemberRole.OWNER;
            }
        }

        if(smackRole != null)
        {
            if (smackRole == MUCRole.moderator)
            {
                return ChatRoomMemberRole.MODERATOR;
            }
            else if (smackRole == MUCRole.participant)
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
    public ChatRoomMemberJabberImpl smackParticipantToScMember(Jid participant)
    {
        if (participant == null)
        {
            return null;
        }

        Resourcepart participantName = participant.getResourceOrThrow();
        synchronized (members)
        {

            for (ChatRoomMemberJabberImpl member : this.members.values())
            {
                if (participantName.toString().equals(member.getName())
                    || participant.toString().equals(member.getContactAddress())
                    || participantName.toString().equals(member.getContactAddress()))
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
        EntityBareJid alternateJid = null;
        try
        {
            alternateJid = JidCreate.entityBareFrom(alternateAddress);
        }
        catch (XmppStringprepException e)
        {
            logger.warn("Alternate address is not valid, ignoring", e);
        }

        try
        {
            multiUserChat.destroy(reason, alternateJid);
        }
        catch (XMPPException
                | InterruptedException
                | NoResponseException
                | NotConnectedException e)
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
        OperationSetBasicTelephonyJabberImpl basicTelephony
            = (OperationSetBasicTelephonyJabberImpl) provider
                .getOperationSet(OperationSetBasicTelephony.class);

        if(basicTelephony != null && this.publishedConference != null)
        {
            ActiveCallsRepositoryJabberImpl activeRepository
                    = basicTelephony.getActiveCallsRepository();

            String callid = publishedConference.getCallId();

            if (callid != null)
            {
                CallJabberImpl call = activeRepository.findCallId(callid);
                for(CallPeerJabberImpl peer : call.getCallPeerList())
                {
                    try
                    {
                        peer.hangup(false, null, null);
                    }
                    catch (NotConnectedException | InterruptedException e)
                    {
                        logger.error("Could not hangup peer " + peer.getAddress(), e);
                    }
                }
            }
        }

        List<CallJabberImpl> tmpConferenceCalls;
        synchronized (chatRoomConferenceCalls)
        {
            tmpConferenceCalls = new ArrayList<>(chatRoomConferenceCalls);
            chatRoomConferenceCalls.clear();
        }

        for(CallJabberImpl call : tmpConferenceCalls)
        {
            for(CallPeerJabberImpl peer : call.getCallPeerList())
            {
                try
                {
                    peer.hangup(false, null, null);
                }
                catch (NotConnectedException | InterruptedException e)
                {
                    logger.error("Could not hangup peer " + peer.getAddress(), e);
                }
            }
        }

        clearCachedConferenceDescriptionList();

        XMPPConnection connection = this.provider.getConnection();
        try
        {
            multiUserChat.removeSubjectUpdatedListener(smackSubjectUpdatedListener);
            multiUserChat.removeMessageListener(smackMessageListener);
            multiUserChat.removeParticipantStatusListener(memberListener);
            multiUserChat.removeUserStatusListener(userListener);

            // if we are already disconnected
            // leave maybe called from gui when closing chat window
            if(connection != null && connection.isConnected())
            {
                // skip leave if not joined, this is in case of an error, but we call leave to clear listeners and such
                if (multiUserChat.isJoined())
                {
                    multiUserChat.leave();
                }
                else
                {
                    // We have detected cases where participant is joined the room, but multiUserChat.myRoomJid which
                    // is internal state of not joined to the room and so no leave room presence is sent
                    // this is a hack to always send it if enabled, worst-case a presence error is returned
                    AccountID accountID = this.provider.getAccountID();
                    if (accountID.getAccountPropertyBoolean(
                        "net.java.sip.communicator.impl.protocol.jabber.FORCE_PRESENCE_ON_LEAVE", true))
                    {
                        logger.warn("Force sending presence unavailable to "
                            + multiUserChat.getRoom() + " for " + this.nickname);
                        try
                        {
                            Presence leavePresence = connection.getStanzaFactory().buildPresenceStanza()
                                .ofType(Presence.Type.unavailable)
                                .to(JidCreate.fullFrom(multiUserChat.getRoom(), this.nickname))
                                .build();
                            connection.sendAsync(leavePresence, new StanzaIdFilter(leavePresence));
                        }
                        catch(Exception e1)
                        {}
                        // let's cleanup ...
                        Method cleanupMethod = MultiUserChat.class.getDeclaredMethod("userHasLeft");
                        cleanupMethod.setAccessible(true);
                        cleanupMethod.invoke(multiUserChat);
                    }
                }
            }
        }
        catch(Throwable e)
        {
            logger.warn("Error occurred while leaving, maybe just disconnected before leaving", e);
        }

        // FIXME Do we have to do the following when we leave the room?
        Hashtable<Resourcepart, ChatRoomMemberJabberImpl> membersCopy;
        synchronized (members)
        {
            membersCopy = new Hashtable<>(members);

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
            connection.removeStanzaSendingListener(lastPresenceListener);
            connection.removeAsyncStanzaListener(invitationRejectionListeners);
            connection.removePresenceInterceptor(presenceInterceptor);
            connection.removeAsyncStanzaListener(presenceListener);
        }

        opSetMuc.fireLocalUserPresenceEvent(this, LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_LEFT, null);
    }

    /**
     * Utility method to send a smack message packet received, to the
     * <tt>multiUserChat</tt>.
     *
     * @param msg the message to be sent.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    private void sendMessage(MessageBuilder msg)
        throws OperationFailedException
    {
        try
        {
            assertConnected();
            msg.ofType(org.jivesoftware.smack.packet.Message.Type.groupchat);
            MessageEvent event = new MessageEvent();
            event.setOffline(true);
            event.setComposing(true);
            msg.addExtension(event);
            multiUserChat.sendMessage(msg);
        }
        catch (NotConnectedException | InterruptedException ex)
        {
            throw new OperationFailedException(
                "Failed to send message " + msg
                , OperationFailedException.GENERAL_ERROR
                , ex);
        }
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
        MessageBuilder msg = MessageBuilder
            .buildMessage()
            .setBody(message.getContent());

        sendMessage(msg);
    }

    /**
     * Sends the <tt>message</tt> with the json-message extension to the
     * destination indicated by the <tt>to</tt> contact.
     *
     * @param json the json message to be sent.
     * @throws OperationFailedException if sending the message fails for some
     * reason.
     */
    public void sendJsonMessage(String json)
            throws OperationFailedException
    {
        MessageBuilder msg = MessageBuilder.buildMessage();
        msg.addExtension(new JsonMessageExtension(json));
        sendMessage(msg);
    }

    /**
     * Sets the subject of this chat room.
     *
     * @param subject the new subject that we'd like this room to have
     */
    public void setSubject(String subject)
        throws OperationFailedException
    {
        try
        {
            multiUserChat.changeSubject(subject);
        }
        catch (NoResponseException
                | NotConnectedException
                | XMPPErrorException
                | InterruptedException ex)
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
            // If there is no nickname, there is no way to match anything.
            // Sometimes while joining we receive the presence and dispatch
            // the user role before nickname is stored in multiUserChat
            // returning guest here is just for the event reporting the new
            // role and that old is guest
            if (multiUserChat.getNickname() == null)
            {
                return ChatRoomMemberRole.GUEST;
            }

            Occupant o = multiUserChat.getOccupant(JidCreate.entityFullFrom(
                multiUserChat.getRoom(), multiUserChat.getNickname()));

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
     * Updates the last presence for a ChatRoomMember. Extracts known elements if available.
     * @param member The member to update.
     * @param presence The presence of the participant that will be assigned as last received Presence stanza.
     */
    private void updateMemberLastPresence(ChatRoomMemberJabberImpl member, Presence presence)
    {
        Nick nickExtension = presence.getExtension(Nick.class);
        if (nickExtension != null)
        {
            member.setDisplayName(nickExtension.getName());
        }

        Email emailExtension = presence.getExtension(Email.class);
        if (emailExtension != null)
        {
            member.setEmail(emailExtension.getAddress());
        }

        AvatarUrl avatarUrl = presence.getExtension(AvatarUrl.class);
        if (avatarUrl != null)
        {
            member.setAvatarUrl(avatarUrl.getAvatarUrl());
        }

        StatsId statsId = presence.getExtension(StatsId.class);
        if (statsId != null)
        {
            member.setStatisticsID(statsId.getStatsId());
        }

        member.setLastPresence(presence);
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
        @Override
        public void banned(EntityFullJid participant, Jid actor, String reason)
        {
            if (logger.isInfoEnabled())
                logger.info(participant + " has been banned from "
                + getName() + " chat room.");

            ChatRoomMemberJabberImpl member =
                smackParticipantToScMember(participant);

            if(member == null)
                return;

            synchronized (members)
            {
                members.remove(participant.getResourceOrThrow());
            }

            banList.put(participant.getResourceOrThrow(), member);

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
        @Override
        public void adminGranted(EntityFullJid participant)
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
        @Override
        public void adminRevoked(EntityFullJid participant)
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
        @Override
        public void joined(EntityFullJid participant)
        {
            if (logger.isInfoEnabled())
                logger.info(participant + " has joined the "
                + getName() + " chat room.");

            Resourcepart participantName = participant.getResourceOrThrow();

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
                  occupant.getNick(),
                  occupant.getJid());

            // let's update the participant last presence
            updateMemberLastPresence(member, multiUserChat.getOccupantPresence(participant));

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
        @Override
        public void left(EntityFullJid participant)
        {
            if (logger.isInfoEnabled())
                logger.info(participant + " has left the "
                + getName() + " chat room.");

            ChatRoomMember member
                = smackParticipantToScMember(participant);

            if(member == null)
                return;

            synchronized (members)
            {
                members.remove(participant.getResourceOrThrow());
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
        @Override
        public void nicknameChanged(EntityFullJid participant, Resourcepart newNickname)
        {
            ChatRoomMemberJabberImpl member = smackParticipantToScMember(participant);

            if(member == null)
                return;

            member.setName(newNickname);

            synchronized (members)
            {
                // change the member key
                ChatRoomMemberJabberImpl mem = members.remove(participant.getResourceOrThrow());
                members.put(newNickname, mem);
            }

            ChatRoomMemberPropertyChangeEvent evt
                = new ChatRoomMemberPropertyChangeEvent(
                    member,
                    ChatRoomJabberImpl.this,
                    ChatRoomMemberPropertyChangeEvent.MEMBER_NICKNAME,
                    participant.getResourceOrThrow(),
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
        @Override
        public void ownershipRevoked(EntityFullJid participant)
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
        @Override
        public void kicked(EntityFullJid participant, Jid actor, String reason)
        {
            ChatRoomMember member
                = smackParticipantToScMember(participant);

            ChatRoomMember actorMember
                = smackParticipantToScMember(actor);

            if(member == null)
                return;

            synchronized (members)
            {
                members.remove(participant.getResourceOrThrow());
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
        @Override
        public void moderatorGranted(EntityFullJid participant)
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
        @Override
        public void voiceRevoked(EntityFullJid participant)
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
        @Override
        public void membershipGranted(EntityFullJid participant)
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
        @Override
        public void moderatorRevoked(EntityFullJid participant)
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
        @Override
        public void voiceGranted(EntityFullJid participant)
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
        @Override
        public void membershipRevoked(EntityFullJid participant)
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
        @Override
        public void ownershipGranted(EntityFullJid participant)
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
    @Override
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
    @Override
    public void removelocalUserRoleListener(
        ChatRoomLocalUserRoleListener listener)
    {
        synchronized(localUserRoleListeners)
        {
            localUserRoleListeners.remove(listener);
        }
    }

    /**
     * Adds a packet extension which will be added to every presence we sent.
     *
     * @param ext the extension we want to add.
     */
    public void addPresencePacketExtensions(ExtensionElement ext)
    {
        synchronized(presencePacketExtensions)
        {
            if (!presencePacketExtensions.contains(ext))
                presencePacketExtensions.add(ext);
        }
    }

    /**
     * Removes a packet extension from the list of extensions we add to every
     * presence we send.
     *
     * @param ext the extension we want to remove.
     */
    public void removePresencePacketExtensions(
        ExtensionElement ext)
    {
        synchronized(presencePacketExtensions)
        {
            presencePacketExtensions.remove(ext);
        }
    }

    /**
     * Adds a listener that will be notified of changes of a member role in the
     * room such as being granded operator.
     *
     * @param listener a member role listener.
     */
    @Override
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
    @Override
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
     */
    @Override
    public Iterator<ChatRoomMember> getBanList()
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
    @Override
    public void setUserNickname(String nickname)
        throws OperationFailedException
    {
        try
        {
            Resourcepart resourceNick = Resourcepart.from(nickname);
            multiUserChat.changeNickname(resourceNick);
            this.nickname = resourceNick;
        }
        catch (XMPPException e)
        {
            logger.error("Failed to change nickname for chat room: "
                + getName());

            throw new OperationFailedException("The " + nickname
                + "already exists in this chat room.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        }
        catch (MucNotJoinedException
                | InterruptedException
                | XmppStringprepException
                | NotConnectedException
                | NoResponseException e)
        {
            logger.error("Failed to change nickname for chat room: "
                    + getName());
            throw new OperationFailedException("Nickname change error", 0, e);
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
    @Override
    public void banParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException
    {
        try
        {
            Jid jid = ((ChatRoomMemberJabberImpl)chatRoomMember).getJabberID();
            multiUserChat.banUser(jid, reason);
        }
        catch (XMPPErrorException e)
        {
            // If a moderator or a user with an affiliation of "owner" or "admin"
            // was intended to be kicked.
            if (e.getStanzaError().getCondition() == not_allowed)
            {
                throw new OperationFailedException(
                    "Kicking an admin user or a chat room owner is a forbidden operation.",
                    OperationFailedException.FORBIDDEN);
            }
            else
            {
                logger.error("Failed to ban participant.", e);
                throw new OperationFailedException(
                    "An error occured while trying to kick the participant.",
                    OperationFailedException.GENERAL_ERROR);
            }
        }
        catch (InterruptedException
                | NoResponseException
                | NotConnectedException e)
        {
            logger.error("Failed to ban participant.", e);
            throw new OperationFailedException(
                "An error occured while trying to kick the participant.",
                OperationFailedException.GENERAL_ERROR);
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
    @Override
    public void kickParticipant(ChatRoomMember member, String reason)
        throws OperationFailedException
    {
        try
        {
            Resourcepart nick = ((ChatRoomMemberJabberImpl)member)
                    .getNameAsResourcepart();
            multiUserChat.kickParticipant(nick, reason);
        }
        catch (XMPPErrorException e)
        {
            logger.error("Failed to kick participant.", e);

            // If a moderator or a user with an affiliation of "owner" or "admin"
            // was intended to be kicked.
            if (e.getStanzaError().getCondition() == not_allowed) //not allowed
            {
                throw new OperationFailedException(
                    "Kicking an admin user or a chat room owner is a forbidden "
                            + "operation.",
                    OperationFailedException.FORBIDDEN);
            }
            // If a participant that intended to kick another participant does
            // not have kicking privileges.
            else if (e.getStanzaError().getCondition() == forbidden) //forbidden
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
        catch (NoResponseException
            | NotConnectedException
            | InterruptedException e)
        {
            logger.error("Failed to kick participant.", e);
            throw new OperationFailedException(
                "An error occured while trying to kick the participant.",
                OperationFailedException.GENERAL_ERROR);
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

        Iterator<ChatRoomMemberPresenceListener> listeners;
        synchronized (memberListeners)
        {
            listeners = new ArrayList<>(memberListeners).iterator();
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
            listeners = new ArrayList<>(memberListeners);
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
            listeners = new ArrayList<>(memberRoleListeners);
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
            listeners = new ArrayList<>(messageListeners);
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
     * <tt>cd</tt> on success or <tt>null</tt> on failure or not sent)
     */
    @Override
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
                        new String[]{nickname.toString()});
            }
            else
            {
                displayName = name;
            }
            cd.setDisplayName(displayName);
        }

        ConferenceDescriptionExtension ext
            = new ConferenceDescriptionExtension(
                cd.getUri(),
                cd.getUri(),
                cd.getPassword());

        if (lastPresenceSent != null)
        {

            if (setPacketExtension(lastPresenceSent, ext, ConferenceDescriptionExtension.NAMESPACE))
            {
                try
                {
                    sendLastPresence();
                }
                catch (NotConnectedException | InterruptedException e)
                {
                    logger.warn("Could not publish conference", e);
                    return null;
                }
            }
            else
            {
                return null;
            }
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
            = !cd.isAvailable()
                ? null
                : cd;
        publishedConferenceExt
            = publishedConference == null
                ? null
                : ext;

        fireConferencePublishedEvent(members.get(nickname), cd,
            ChatRoomConferencePublishedEvent.CONFERENCE_DESCRIPTION_SENT);
        return cd;
    }

    /**
     * Sets <tt>ext</tt> as the only <tt>ExtensionElement</tt> that belongs to
     * given <tt>namespace</tt> of the <tt>packet</tt>.
     *
     * @param packet the <tt>Packet<tt> to be modified.
     * @param extension the <tt>ConferenceDescriptionPacketExtension<tt> to set,
     * or <tt>null</tt> to not set one.
     * @param namespace the namespace of <tt>ExtensionElement</tt>.
     * @param matchElementName if {@code true} only extensions matching both
     * the element name and namespace will be matched and removed. Otherwise,
     * only the namespace will be matched.
     * @return whether packet was modified.
     */
    private static boolean setPacketExtension(
            Stanza packet,
            ExtensionElement extension,
            String namespace,
            boolean matchElementName)
    {
        boolean modified = false;

        if (org.apache.commons.lang3.StringUtils.isEmpty(namespace))
        {
            return modified;
        }

        //clear previous announcements
        ExtensionElement pe;
        if (matchElementName && extension != null)
        {
            String element = extension.getElementName();
            while (null != (pe = packet.getExtensionElement(element, namespace)))
            {
                if (packet.removeExtension(pe) != null)
                {
                    modified = true;
                }
            }
        }
        else
        {
            while (null != (pe = packet.getExtension(namespace)))
            {
                if (packet.removeExtension(pe) != null)
                {
                    modified = true;
                }
            }
        }

        if (extension != null)
        {
            packet.addExtension(extension);
            modified = true;
        }

        return modified;
    }

    /**
     * Sets <tt>ext</tt> as the only <tt>ExtensionElement</tt> that belongs to
     * given <tt>namespace</tt> of the <tt>packet</tt>.
     *
     * @param packet the <tt>Packet<tt> to be modified.
     * @param extension the <tt>ConferenceDescriptionPacketExtension<tt> to set,
     * or <tt>null</tt> to not set one.
     * @param namespace the namespace of <tt>ExtensionElement</tt>.
     * @return whether packet was modified.
     */
    private static boolean setPacketExtension(
        Stanza packet,
        ExtensionElement extension,
        String namespace)
    {
        return setPacketExtension(packet, extension, namespace, false);
    }

    /**
     * Publishes new status message in chat room presence.
     * @param newStatus the new status message to be published in the MUC.
     */
    public void publishPresenceStatus(String newStatus)
    {
        if (lastPresenceSent == null)
        {
            return;
        }

        lastPresenceSent.setStatus(newStatus);

        try
        {
            sendLastPresence();
        }
        catch (NotConnectedException | InterruptedException e)
        {
            logger.error("Could not publish presence", e);
        }
    }

    /**
     * Adds given <tt>ExtensionElement</tt> to the MUC presence and publishes it
     * immediately.
     * @param extension the <tt>ExtensionElement</tt> to be included in MUC
     *                  presence.
     */
    public void sendPresenceExtension(ExtensionElement extension)
    {
        if (lastPresenceSent == null)
        {
            return;
        }

        if (setPacketExtension(lastPresenceSent, extension, extension.getNamespace(), true))
        {
            try
            {
                sendLastPresence();
            }
            catch (NotConnectedException | InterruptedException e)
            {
                logger.error("Could not send presence", e);
            }
        }
    }

    /**
     * Removes given <tt>ExtensionElement</tt> from the MUC presence and
     * publishes it immediately.
     * @param extension the <tt>ExtensionElement</tt> to be removed from the MUC
     *                  presence.
     */
    public void removePresenceExtension(ExtensionElement extension)
    {
        if (lastPresenceSent == null)
        {
            return;
        }

        if(setPacketExtension(lastPresenceSent, null, extension.getNamespace()))
        {
            try
            {
                sendLastPresence();
            }
            catch (NotConnectedException | InterruptedException e)
            {
                logger.error("Could not remove presence", e);
            }
        }
    }

    /**
     * Returns the ids of the users that has the member role in the room.
     * When the room is member only, this are the users allowed to join.
     * @return the ids of the users that has the member role in the room.
     */
    public List<String> getMembersWhiteList()
    {
        List<String> res = new ArrayList<>();
        try
        {
            for(Affiliate a : multiUserChat.getMembers())
            {
                res.add(a.getJid().toString());
            }
        }
        catch(Exception e)
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
            for (Affiliate a : multiUserChat.getMembers())
            {
                if (!members.contains(a.getJid().toString()))
                {
                    multiUserChat.revokeMembership(a.getJid());
                }
            }

            for (String m : members)
            {
                multiUserChat.grantMembership(JidCreate.from(m));
            }
        }
        catch(Exception e)
        {
            logger.error("Cannot modify members list", e);
        }
    }

    /**
     * Prepares and sends the last seen presence.
     * Removes the initial <x> extension and sets new id.
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    private void sendLastPresence()
        throws NotConnectedException,
               InterruptedException
    {
        // The initial presence sent by smack contains an empty "x"
        // extension. If this extension is included in a subsequent stanza,
        // it indicates that the client lost its synchronization and causes
        // the MUC service to re-send the presence of each occupant in the
        // room.
        lastPresenceSent.removeExtension(MUCInitialPresence.ELEMENT, MUCInitialPresence.NAMESPACE);

        lastPresenceSent = lastPresenceSent
            .asBuilder(StandardStanzaIdSource.DEFAULT.getNewStanzaId())
            .build();

        provider.getConnection().sendStanza(lastPresenceSent);
    }

    /**
     * A listener that listens for packets of type Message and fires an event
     * to notifier interesting parties that a message was received.
     */
    private class SmackMessageListener
        implements MessageListener
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
         * @param msg to process.
         */
        @Override
        public void processMessage(org.jivesoftware.smack.packet.Message msg)
        {
            Date timeStamp;
            DelayInformation delay =
                    msg.getExtension("x", "jabber:x:delay");

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
                            provider
                                .getAccountID()
                                .getAccountUniqueID(),
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
                    provider
                        .getAccountID()
                        .getAccountUniqueID(),
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

            Jid msgFrom = msg.getFrom();
            ChatRoomMember member = null;

            // when the message comes from the room itself its a system message
            Jid roomName = multiUserChat.getRoom();
            if(msgFrom.equals(roomName))
            {
                messageReceivedEventType =
                    ChatRoomMessageReceivedEvent.SYSTEM_MESSAGE_RECEIVED;
                member = new ChatRoomMemberJabberImpl(
                    ChatRoomJabberImpl.this, Resourcepart.EMPTY, roomName);
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
                    ChatRoomJabberImpl.this,
                        msgFrom.getResourceOrThrow(), msgFrom);
            }

            if(logger.isDebugEnabled())
            {
                if (logger.isDebugEnabled())
                    logger.debug("Received from "
                             + msgFrom
                             + " the message "
                             + msg.toXML());
            }

            Message newMessage = createMessage(msgBody);

            // if we are sending this message, this either a delivery report
            // or if there is a delay extension this is a history coming from
            // the chat room
            if(multiUserChat.getNickname() != null
                && multiUserChat.getNickname()
                    .equals(msgFrom.getResourceOrThrow()))
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
                    logger.info("Message error received from " + msgFrom);

                StanzaError error = msg.getError();
                Condition errorCode = error.getCondition();
                int errorResultCode
                    = ChatRoomMessageDeliveryFailedEvent.UNKNOWN_ERROR;
                String errorReason = error.getConditionText();

                if(errorCode == service_unavailable)
                {
                    org.jivesoftware.smackx.xevent.packet.MessageEvent msgEvent =
                        (org.jivesoftware.smackx.xevent.packet.MessageEvent)
                            msg.getExtension("x", "jabber:x:event");
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
        @Override
        public void subjectUpdated(String subject, EntityFullJid from)
        {
            // only fire event if subject has really changed, not for new one
            if(subject != null && !subject.equals(oldSubject))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Subject updated to " + subject);
                }

                ChatRoomPropertyChangeEvent evt = new ChatRoomPropertyChangeEvent(
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
         * Called when a room was destroyed. This means that the room you have
         * joined is no longer available.
         *
         * @param alternateMUC <tt>MultiUserChat</tt>.
         * @param reason why room was destroyed.
         */
        @Override
        public void roomDestroyed(MultiUserChat alternateMUC, String reason)
        {
            opSetMuc.fireLocalUserPresenceEvent(
                ChatRoomJabberImpl.this,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_ROOM_DESTROYED,
                reason,
                alternateMUC != null && alternateMUC.getRoom() != null
                    ? alternateMUC.getRoom().toString() : null);
        }

        /**
         * Called when a moderator kicked your user from the room. This
         * means that you are no longer participating in the room.
         *
         * @param actor the moderator that kicked your user from the room
         * (e.g. user@host.org).
         * @param reason the reason provided by the actor to kick you from
         * the room.
         */
        @Override
        public void kicked(Jid actor, String reason)
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
        @Override
        public void voiceGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when a moderator revokes voice from your user. This means that
        * you were a participant in the room able to speak and now you are a
        * visitor that can't send messages to the room occupants.
        */
        @Override
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
        @Override
        public void banned(Jid actor, String reason)
        {
            opSetMuc.fireLocalUserPresenceEvent(ChatRoomJabberImpl.this,
                LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_DROPPED, reason);
            leave();
        }

        /**
        * Called when an administrator grants your user membership to the room.
        * This means that you will be able to join the members-only room.
        */
        @Override
        public void membershipGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when an administrator revokes your user membership to the room.
        * This means that you will not be able to join the members-only room.
        */
        @Override
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
        @Override
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
        @Override
        public void moderatorRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when an owner grants to your user ownership on the room. This
        * means that you will be able to change defining room features as well
        * as perform all administrative functions.
        */
        @Override
        public void ownershipGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.OWNER);
        }

        /**
        * Called when an owner revokes from your user ownership on the room.
        * This means that you will no longer be able to change defining room
        * features as well as perform all administrative functions.
        */
        @Override
        public void ownershipRevoked()
        {
            setLocalUserRole(ChatRoomMemberRole.MEMBER);
        }

        /**
        * Called when an owner grants administrator privileges to your user.
        * This means that you will be able to perform administrative functions
        * such as banning users and edit moderator list.
        */
        @Override
        public void adminGranted()
        {
            setLocalUserRole(ChatRoomMemberRole.ADMINISTRATOR);
        }

        /**
        * Called when an owner revokes administrator privileges from your user.
        * This means that you will no longer be able to perform administrative
        * functions such as banning users and edit moderator list.
        */
        @Override
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

        /**
         * The corresponding configuration form.
         */
        ChatRoomConfigurationFormJabberImpl configForm;
        try
        {
            smackConfigForm = multiUserChat.getConfigurationForm();

            configForm
                = new ChatRoomConfigurationFormJabberImpl(
                    multiUserChat, smackConfigForm);
        }
        catch (XMPPErrorException e)
        {
            if(e.getStanzaError().getCondition() == forbidden)
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
        catch (InterruptedException
                | NoResponseException
                | NotConnectedException e)
        {
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
        EntityBareJid roomName = multiUserChat.getRoom();
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
    public ChatRoomMemberJabberImpl findMemberForNickName(Resourcepart jabberID)
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
            multiUserChat.grantAdmin(JidCreate.from(jid));
        }
        catch (XMPPErrorException
                | InterruptedException
                | NoResponseException
                | NotConnectedException
                | XmppStringprepException ex)
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
            multiUserChat.grantMembership(JidCreate.from(jid));
        }
        catch (XMPPErrorException
                | InterruptedException
                | NoResponseException
                | NotConnectedException
                | XmppStringprepException ex)
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
            multiUserChat.grantModerator(Resourcepart.from(nickname));
        }
        catch (XMPPErrorException
                | InterruptedException
                | NoResponseException
                | NotConnectedException
                | XmppStringprepException ex)
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
            multiUserChat.grantOwnership(JidCreate.from(jid));
        }
        catch (XMPPErrorException
                | InterruptedException
                | NoResponseException
                | NotConnectedException
                | XmppStringprepException ex)
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
            multiUserChat.grantVoice(Resourcepart.from(nickname));
        }
        catch (XMPPErrorException
                | InterruptedException
                | NoResponseException
                | NotConnectedException
                | XmppStringprepException ex)
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
            multiUserChat.revokeAdmin(JidCreate.from(jid).asEntityJidOrThrow());
        }
        catch (XMPPErrorException
                | InterruptedException
                | NoResponseException
                | NotConnectedException
                | XmppStringprepException ex)
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
            multiUserChat.revokeMembership(JidCreate.from(jid));
        }
        catch (XMPPErrorException
                | InterruptedException
                | NoResponseException
                | NotConnectedException
                | XmppStringprepException ex)
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
            multiUserChat.revokeModerator(Resourcepart.from(nickname));
        }
        catch (XMPPErrorException
                | InterruptedException
                | NoResponseException
                | NotConnectedException
                | XmppStringprepException ex)
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
            multiUserChat.revokeOwnership(JidCreate.from(jid));
        }
        catch (XMPPErrorException
                | InterruptedException
                | NoResponseException
                | NotConnectedException
                | XmppStringprepException ex)
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
            multiUserChat.revokeVoice(Resourcepart.from(nickname));
        }
        catch (XMPPErrorException
                | InterruptedException
                | NoResponseException
                | NotConnectedException
                | XmppStringprepException ex)
        {
            logger.info("An error occurs revoking voice from a participant", ex);
        }
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
    private class ChatRoomPresenceListener
        implements StanzaListener
    {
        /**
         * Creates an instance of a listener of presence packets.
         */
        public ChatRoomPresenceListener()
        {
            super();
        }

        /**
         * Processes an incoming presence packet.
         * @param packet the incoming packet.
         */
        @Override
        public void processStanza(Stanza packet)
        {
            if (packet == null
                    || !(packet instanceof Presence)
                    || packet.getError() != null)
            {
                logger.warn("Unable to handle packet: " + packet);
                return;
            }

            Presence presence = (Presence) packet;
            if (MUCUserStatusCodeFilter.STATUS_110_PRESENCE_TO_SELF
                    .accept(presence))
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
                MUCAffiliation affiliation = mucUser.getItem().getAffiliation();
                MUCRole role = mucUser.getItem().getRole();

                // if status 201 is available means that
                // room is created and locked till we send
                // the configuration
                if (mucUser.getStatus().contains(MUCUser.Status.ROOM_CREATED_201))
                {
                    try
                    {
                        multiUserChat.sendConfigurationForm(
                            multiUserChat.getConfigurationForm().getFillableForm()
                        );
                    }
                    catch (XMPPErrorException
                            | InterruptedException
                            | NoResponseException
                            | NotConnectedException e)
                    {
                        logger.error("Failed to send config form.", e);
                    }

                    opSetMuc.addSmackInvitationRejectionListener(multiUserChat, ChatRoomJabberImpl.this);

                    if(affiliation == MUCAffiliation.owner)
                    {
                        setLocalUserRole(ChatRoomMemberRole.OWNER, true);
                    }
                    else
                    {
                        setLocalUserRole(ChatRoomMemberRole.MODERATOR, true);
                    }
                }
                else if (mucUser.getStatus().contains(MUCUser.Status.KICKED_307))
                {
                    // if this is a kick skip processing, we will be notified by multiUserChat
                    return;
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
                        && affiliation == MUCAffiliation.none
                        && role == MUCRole.none)
                    {
                        Destroy destroy = mucUser.getDestroy();
                        if(destroy == null)
                        {
                            // the room is unavailable to us, there is no
                            // message we will just leave
                            leave();
                        }
                        else
                        {
                            // do nothing as the room destroyed event will be fired in UserListener.roomDestroyed
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
            Jid from = presence.getFrom();
            Resourcepart participantName = null;
            if (from != null)
            {
                participantName = from.getResourceOrNull();
            }

            ChatRoomMemberJabberImpl member = participantName == null
                ? null
                : members.get(participantName);

            ConferenceDescriptionExtension cdExt
                = presence.getExtension(ConferenceDescriptionExtension.class);
            if (presence.isAvailable() && cdExt != null)
            {
                ConferenceDescription cd
                    = new ConferenceDescription(
                        cdExt.getUri(),
                        cdExt.getCallId(),
                        cdExt.getPassword());
                cd.setAvailable(cdExt.isAvailable());
                cd.setDisplayName(getName());
                for (TransportExtension t
                    : cdExt.getChildExtensionsOfType(TransportExtension.class))
                {
                    cd.addTransport(t.getNamespace());
                }

                if (!processConferenceDescription(cd, participantName == null
                    ? null
                    : participantName.toString()))
                {
                    return;
                }

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

            // if member wasn't just created, we should potentially modify some
            // elements
            if(member == null)
            {
                return;
            }

            updateMemberLastPresence(member, presence);


            // tell listeners the member was updated (and new information
            // about it is available)
            fireMemberPresenceEvent(member,
                ChatRoomMemberPresenceChangeEvent.MEMBER_UPDATED,
                null);

        }
    }

    /**
     * Listens for rejection message and delivers system message when received.
     */
    private class InvitationRejectionListeners
        implements StanzaListener
    {
        /**
         * Process incoming packet, checking for muc extension.
         * @param packet the incoming packet.
         */
        @Override
        public void processStanza(Stanza packet)
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
                    ChatRoomJabberImpl.this,
                        Resourcepart.EMPTY,
                        multiUserChat.getRoom());

                EntityBareJid from = mucUser.getDecline().getFrom();

                String contactDisplayName = from.toString();
                OperationSetPersistentPresenceJabberImpl presenceOpSet
                    = (OperationSetPersistentPresenceJabberImpl) provider
                        .getOperationSet(OperationSetPersistentPresence.class);
                if(presenceOpSet != null)
                {
                    Contact c = presenceOpSet.findContactByID(from.asBareJid());
                    if(c != null)
                    {
                        contactDisplayName =
                                c.getDisplayName() + " (" + from + ")";
                    }
                }

                String msgBody =
                    JabberActivator.getResources().getI18NString(
                        "service.gui.INVITATION_REJECTED",
                        new String[]
                        {
                            contactDisplayName,
                            mucUser.getDecline().getReason()
                        });

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
     * We use this to make sure that our outgoing <tt>Presence</tt> packets contain the correct
     * <tt>ConferenceAnnouncementPacketExtension</tt> and custom extensions.
     */
    private void presenceIntercept(PresenceBuilder presenceBuilder)
    {
        if (publishedConferenceExt != null)
        {
            presenceBuilder.overrideExtension(publishedConferenceExt);
        }
        else
        {
            presenceBuilder.removeExtension(
                ConferenceDescriptionExtension.ELEMENT,
                ConferenceDescriptionExtension.NAMESPACE);
        }

        for(ExtensionElement ext : presencePacketExtensions)
        {
            presenceBuilder.overrideExtension(ext);
        }
    }

    /**
     * Stores the last sent presence.
     */
    private class LastPresenceListener
        implements StanzaListener
    {
        @Override
        public void processStanza(Stanza packet)
            throws NotConnectedException,
                   InterruptedException,
                   NotLoggedInException
        {
            lastPresenceSent = (Presence)packet;
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
        ContactJabberImpl sourceContact;
        try
        {
            sourceContact = (ContactJabberImpl)presenceOpSet.findContactByID(
                    JidCreate.fullFrom(
                            multiUserChat.getRoom(),
                            Resourcepart.from(nickname)).toString());
        }
        catch (XmppStringprepException e)
        {
            logger.error("Invalid nickname: " + nickname, e);
            return;
        }

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
        Resourcepart nickname;
        try
        {
            nickname = JidCreate.from(contact.getAddress()).getResourceOrThrow();
        }
        catch (XmppStringprepException e)
        {
            logger.error("Invalid contact address: " + contact.getAddress());
            return;
        }

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
