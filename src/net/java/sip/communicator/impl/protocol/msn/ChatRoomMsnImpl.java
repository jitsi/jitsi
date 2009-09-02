/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.msn;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.sf.jml.*;

/**
 * Represents a chat room, where multiple chat users could communicate in a
 * many-to-many fashion.
 * 
 * @author Rupert Burchardi
 */

public class ChatRoomMsnImpl
    implements ChatRoom
{
    private static final Logger logger =
        Logger.getLogger(ChatRoomMsnImpl.class);

    /**
     * The parent protocol service provider.
     */
    private ProtocolProviderServiceMsnImpl parentProvider = null;

    /**
     * List of the members of the chat room.
     */
    private Hashtable<String, ChatRoomMemberMsnImpl> members
        = new Hashtable<String, ChatRoomMemberMsnImpl>();

    /**
     * List of unresolved member names.
     */
    private ArrayList<String> pendingInvitations = new ArrayList<String>();

    /**
     * List of the users that are banned. Note: Not possible inside the MSN
     * protocol, the list is always empty.
     */
    private Hashtable<String, ChatRoomMemberMsnImpl> banList 
        = new Hashtable<String, ChatRoomMemberMsnImpl>();

    /**
     * The chat rooms name.
     */
    private String chatRoomName = null;

    /**
     * The nick name of the user inside this chat room. Note: display name of
     * the user.
     */
    private String nickname;

    /**
     * The subject of the chat room. Note: Not possible inside the MSN protocol.
     */
    private String chatSubject = null;

    /**
     * The corresponding switchboard for the chat room. Each chat room has its
     * own switchboard and if it is closed the user cannot reconnect to it, see
     * MSN documentation for further infos.
     */
    private MsnSwitchboard switchboard = null;

    /**
     * The OperationSet for MSN multi user chats.
     */
    private OperationSetMultiUserChatMsnImpl opSetMuc = null;

    /**
     * Listeners that will be notified of changes in member status in the room
     * such as member joined, left or being kicked or dropped.
     */
    private Vector<ChatRoomMemberPresenceListener> memberListeners 
        = new Vector<ChatRoomMemberPresenceListener>();

    /**
     * Listeners that will be notified of changes in member role in the room
     * such as member being granted admin permissions, or revoked admin
     * permissions.
     */
    private Vector<ChatRoomMemberRoleListener> memberRoleListeners 
        = new Vector<ChatRoomMemberRoleListener>();

    /**
     * Listeners that will be notified of changes in local user role in the room
     * such as member being granted administrator permissions, or revoked
     * administrator permissions.
     */
    private Vector<ChatRoomLocalUserRoleListener> localUserRoleListeners 
        = new Vector<ChatRoomLocalUserRoleListener>();

    /**
     * Listeners that will be notified every time a new message is received on
     * this chat room.
     */
    private Vector<ChatRoomMessageListener> messageListeners 
        = new Vector<ChatRoomMessageListener>();

    /**
     * Listeners that will be notified every time a chat room property has been
     * changed.
     */
    private Vector<ChatRoomPropertyChangeListener> propertyChangeListeners 
        = new Vector<ChatRoomPropertyChangeListener>();

    /**
     * Listeners that will be notified every time a chat room member property
     * has been changed.
     */
    private Vector<ChatRoomMemberPropertyChangeListener> 
        memberPropChangeListeners 
            = new Vector<ChatRoomMemberPropertyChangeListener>();

    /**
     * A Message buffer, will keep all messages until the msn chatroom is ready.
     */
    public Vector<EventObject> messageBuffer = new Vector<EventObject>();

    /**
     * Creates an instance of <tt>ChatRoomMsnImpl</tt>, by specifying the name
     * of the chat room and the protocol provider.
     * 
     * @param chatRoomName Name of the chat room.
     * @param provider Protocol provider.
     */
    public ChatRoomMsnImpl( String chatRoomName,
                            ProtocolProviderServiceMsnImpl provider)
    {
        this.chatRoomName = chatRoomName;
        this.parentProvider = provider;
        this.opSetMuc =
            (OperationSetMultiUserChatMsnImpl) this.parentProvider
                .getOperationSet(OperationSetMultiUserChat.class);
    }

    /**
     * Creates an instance of <tt>ChatRoomMsnImpl</tt>, by specifying the name
     * of the chat room, the protocol provider and the corresponding
     * switchboard.
     * 
     * @param chatRoomName Name of the chat room.
     * @param provider Protocol provider.
     * @param switchboard The corresponding switchboard.
     */
    public ChatRoomMsnImpl( String chatRoomName,
                            ProtocolProviderServiceMsnImpl provider,
                            MsnSwitchboard switchboard)
    {
        this.chatRoomName = chatRoomName;
        this.parentProvider = provider;
        this.opSetMuc =
            (OperationSetMultiUserChatMsnImpl) this.parentProvider
                .getOperationSet(OperationSetMultiUserChat.class);
        this.switchboard = switchboard;
    }

    /**
     * Adds a listener that will be notified of changes in our role in the room
     * such as us being granded operator.
     * 
     * @param listener a local user role listener.
     */
    public void addLocalUserRoleListener(ChatRoomLocalUserRoleListener listener)
    {
        synchronized (localUserRoleListeners)
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
        synchronized (localUserRoleListeners)
        {
            if (localUserRoleListeners.contains(listener))
                localUserRoleListeners.remove(listener);
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
        synchronized (memberListeners)
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
        synchronized (memberListeners)
        {
            memberListeners.remove(listener);
        }
    }

    /**
     * Registers <tt>listener</tt> so that it would receive events every time a
     * new message is received on this chat room.
     * 
     * @param listener a <tt>MessageListener</tt> that would be notified every
     *            time a new message is received on this chat room.
     */
    public void addMessageListener(ChatRoomMessageListener listener)
    {
        synchronized (messageListeners)
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
        synchronized (messageListeners)
        {
            if (messageListeners.contains(listener))
                messageListeners.remove(messageListeners.indexOf(listener));
        }
    }

    /**
     * Adds <tt>listener</tt> to the list of listeners registered to receive
     * events upon modification of chat room properties such as its subject for
     * example.
     * 
     * @param listener the <tt>ChatRoomChangeListener</tt> that is to be
     *            registered for <tt>ChatRoomChangeEvent</tt>-s.
     */
    public void addMemberPropertyChangeListener(
        ChatRoomMemberPropertyChangeListener listener)
    {
        synchronized (memberPropChangeListeners)
        {
            if (!memberPropChangeListeners.contains(listener))
                memberPropChangeListeners.add(listener);
        }

    }

    /**
     * Removes <tt>listener</tt> from the list of listeneres current registered
     * for chat room modification events.
     * 
     * @param listener the <tt>ChatRoomChangeListener</tt> to remove.
     */
    public void removeMemberPropertyChangeListener(
        ChatRoomMemberPropertyChangeListener listener)
    {
        synchronized (memberPropChangeListeners)
        {
            memberPropChangeListeners.remove(listener);
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
        synchronized (memberRoleListeners)
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
        synchronized (memberRoleListeners)
        {
            if (memberRoleListeners.contains(listener))
                memberRoleListeners.remove(listener);
        }
    }

    /**
     * Adds the given <tt>listener</tt> to the list of listeners registered to
     * receive events upon modification of chat room member properties such as
     * its nickname being changed for example.
     * 
     * @param listener the <tt>ChatRoomMemberPropertyChangeListener</tt> that is
     *            to be registered for
     *            <tt>ChatRoomMemberPropertyChangeEvent</tt>s.
     */
    public void addPropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        synchronized (propertyChangeListeners)
        {
            if (!propertyChangeListeners.contains(listener))
                propertyChangeListeners.add(listener);
        }
    }

    /**
     * Removes the given <tt>listener</tt> from the list of listeners currently
     * registered for chat room member property change events.
     * 
     * @param listener the <tt>ChatRoomMemberPropertyChangeListener</tt> to
     *            remove.
     */
    public void removePropertyChangeListener(
        ChatRoomPropertyChangeListener listener)
    {
        synchronized (propertyChangeListeners)
        {
            propertyChangeListeners.remove(listener);
        }
    }

    /**
     * Bans a user from the room. The MSN protocol does not support blocking a
     * chat room for a specific user, so this method will always throw an
     * OperationFailedException.
     * 
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to be banned.
     * @param reason the reason why the user was banned.
     * @throws OperationFailedException Always throws such an Exception, because
     *             user banning based on chatrooms is not possible.
     */
    public void banParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException
    {
        throw new OperationFailedException(
            "This operation is not possible to perform inside the msn protocol.",
            OperationFailedException.GENERAL_ERROR);
    }

    /**
     * Kicks a participant from the room. The MSN protocol does not support
     * this.
     * 
     * @param chatRoomMember the <tt>ChatRoomMember</tt> to kick from the room
     * @param reason the reason why the participant is being kicked from the
     *            room
     * @throws OperationFailedException Always throws an
     *             OperationFailedException, because you cannot kick users from
     *             a switchboard in the msn protocol.
     */

    public void kickParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException
    {
        throw new OperationFailedException(
            "This operation is not possible to perform inside the msn protocol.",
            OperationFailedException.GENERAL_ERROR);
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
    public Message createMessage(byte[] content, String contentType,
        String contentEncoding, String subject)
    {
        return new MessageMsnImpl(new String(content), contentType,
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
        Message msg =
            new MessageMsnImpl(messageText,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
                OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);

        return msg;
    }

    /**
     * Returns the list of banned users.
     */
    public Iterator<ChatRoomMember> getBanList() throws OperationFailedException
    {
        return new LinkedList<ChatRoomMember>(banList.values()).iterator();
    }

    /**
     * Returns the <tt>ChatRoomConfigurationForm</tt> containing all
     * configuration properties for this chat room. MSN does not support any
     * chat room configuration, so an OperationFailedException is always thrown.
     * 
     * @return the <tt>ChatRoomConfigurationForm</tt> containing all
     *         configuration properties for this chat room
     * @throws OperationFailedException Always thrown if called, because the MSN
     *             protocol does not support any chat room configuration
     */
    public ChatRoomConfigurationForm getConfigurationForm()
        throws OperationFailedException
    {
        throw new OperationFailedException(
            "The configuration form is not yet implemented for msn.",
            OperationFailedException.GENERAL_ERROR);
    }

    /**
     * Returns the identifier of this <tt>ChatRoom</tt>.
     * 
     * @return a <tt>String</tt> containing the identifier of this
     *         <tt>ChatRoom</tt>.
     */
    public String getIdentifier()
    {
        return chatRoomName;
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
        return new LinkedList<ChatRoomMember>(members.values());
    }

    /**
     * Returns the number of participants that are currently in this chat room.
     * 
     * @return the number of <tt>Contact</tt>s, currently participating in this
     *         room.
     */
    public int getMembersCount()
    {
        return members.size();
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
     * Returns the protocol provider service that created us.
     * 
     * @return the protocol provider service that created us.
     */
    public ProtocolProviderService getParentProvider()
    {
        return parentProvider;
    }

    /**
     * Returns the last known room subject/theme or <tt>null</tt> if the user
     * hasn't joined the room or the room does not have a subject yet.
     * <p>
     * To be notified every time the room's subject change you should add a
     * <tt>ChatRoomPropertyChangelistener</tt> to this room.
     * <p>
     * 
     * 
     * To change the room's subject use {@link #setSubject(String)}. Note: Not
     * possible inside the msn protocol!
     * 
     * @return the room subject or <tt>null</tt> if the user hasn't joined the
     *         room or the room does not have a subject yet.
     */
    public String getSubject()
    {
        return chatSubject;
    }

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     * 
     * @return the nickname currently being used by the local user in the
     *         context of the local chat room.
     */
    public String getUserNickname()
    {
        if (nickname == null && isJoined())
            nickname =
                parentProvider.getMessenger().getOwner().getDisplayName();

        return nickname;
    }

    /**
     * Invites another user to this room. If we're not joined nothing will
     * happen.
     * 
     * @param userAddress the address of the user (email address) to invite to
     *            the room.(one may also invite users not on their contact
     *            list).
     * @param reason You cannot specify a Reason inside the msn protocol
     */
    public void invite(String userAddress, String reason)
    {
        // msn requires lower case email addresses
        userAddress = userAddress.toLowerCase();

        if (switchboard == null)
        {
            pendingInvitations.add(userAddress);
        }
        else
        {
            switchboard.inviteContact(Email.parseStr(userAddress));
        }
    }

    public boolean isJoined()
    {
        if (this.switchboard == null)
            return false;

        // unfortunately we can't check if the switchboard session is still
        // active
        // so we have to compare it to the active switchboards from the provider
        for (MsnSwitchboard board : parentProvider.getMessenger()
            .getActiveSwitchboards())
        {
            if (switchboard.equals(board))
                return true;
        }

        return false;
    }

    public boolean isSystem()
    {
        return false;
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     * 
     * @param message The <tt>Message</tt> to send.
     * @throws OperationFailedException if the underlying stack is not
     *             registered or initialized or if the chat room is not joined.
     */

    public void sendMessage(Message message) throws OperationFailedException
    {
        if (!isJoined())
        {
            throw new OperationFailedException("This chat room is not active.",
                OperationFailedException.CHAT_ROOM_NOT_JOINED);
        }

        switchboard.sendText(message.getContent());

        ChatRoomMessageDeliveredEvent msgDeliveredEvt
            = new ChatRoomMessageDeliveredEvent(
                this,
                System.currentTimeMillis(),
                message,
                ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED);

        fireMessageEvent(msgDeliveredEvt);
    }

    /**
     * Sets the subject of this chat room. If the user does not have the right
     * to change the room subject, or the protocol does not support this, or the
     * operation fails for some other reason, the method throws an
     * <tt>OperationFailedException</tt> with the corresponding code. Note: Not
     * supported inside the MSN protocol.
     * 
     * @param subject the new subject that we'd like this room to have
     * @throws OperationFailedException thrown if the user is not joined to the
     *             channel or if he/she doesn't have enough privileges to change
     *             the topic or if the topic is null.
     */
    public void setSubject(String subject) throws OperationFailedException
    {
        throw new OperationFailedException("You cannot change the subject!",
            OperationFailedException.GENERAL_ERROR);
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
        joinAs(parentProvider.getMessenger().getOwner().getDisplayName());
    }

    /**
     * Joins this chat room so that the user would start receiving events and
     * messages for it. Note: Secured chat rooms are not supported inside the
     * msn protocol,
     * 
     * @see #join()
     * 
     * @param password the password to use when authenticating on the chat room.
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void join(byte[] password) throws OperationFailedException
    {
        joinAs(parentProvider.getMessenger().getOwner().getDisplayName());
    }

    /**
     * Joins this chat room with the specified nickname so that the user would
     * start receiving events and messages for it.
     * 
     * @param nickName the nickname to use.
     * @param password Not support inside the msn protocol
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void joinAs(String nickName, byte[] password)
        throws OperationFailedException
    {
        joinAs(parentProvider.getMessenger().getOwner().getDisplayName());
    }

    /**
     * Joins this chat room with the specified nickname so that the user would
     * start receiving events and messages for it.
     * 
     * @param nickName the nickname to use.
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void joinAs(String nickName) throws OperationFailedException
    {
        // We don't specify a reason.
        opSetMuc.fireLocalUserPresenceEvent(this,
            LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED, null);

        // We buffered the messages before the user has joined the chat, now the
        // user has joined so we fire them again
        for (EventObject evt : messageBuffer)
        {
            fireMessageEvent(evt);
        }
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
        if (switchboard != null)
        {
            switchboard.close();
            switchboard = null;
        }

        Iterator<ChatRoomMemberMsnImpl> membersIter 
            = members.values().iterator();

        while (membersIter.hasNext())
        {
            ChatRoomMember member = (ChatRoomMember) membersIter.next();

            fireMemberPresenceEvent(member,
                ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT,
                "Local user has left the chat room.");
        }

        // Delete the list of members
        members.clear();
    }

    /**
     * Sets the nickName for this chat room.
     * 
     * @param nickname the nick name to set
     * @throws OperationFailedException If called, an OpFailedException is
     *             called, because MSN does not support nickname inside chat
     *             rooms.
     * 
     */
    public void setUserNickname(String nickname)
        throws OperationFailedException
    {
        throw new OperationFailedException("You cannot set the user nickname!",
            OperationFailedException.GENERAL_ERROR);
    }

    /**
     * Fills the member list with all members inside the switchboard (chat
     * room).
     * 
     * @param switchboard The corresponding switchboard
     */
    public void updateMemberList(MsnSwitchboard switchboard)
    {
        MsnContact[] contacts = switchboard.getAllContacts();

        for (MsnContact msnContact : contacts)
        {
            if (!members.containsKey(msnContact.getId()))
            {   // if the member is not inside the members list, create a member
                // instance,
                // add it to the list and fire a member presence event
                ChatRoomMemberMsnImpl member =
                    new ChatRoomMemberMsnImpl(this,
                        msnContact.getDisplayName(),
                        msnContact.getEmail().getEmailAddress(),
                        ChatRoomMemberRole.MEMBER);

                members.put(msnContact.getId(), member);

                fireMemberPresenceEvent(member,
                    ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
            }
        }

        for (String contactAddress: pendingInvitations)
        {
            this.invite(contactAddress, "");
        }
    }

    public ChatRoomMemberMsnImpl getChatRoomMember(String id)
    {
        return members.get(id);
    }

    /**
     * Notifies all interested listeners that a
     * <tt>ChatRoomMessageDeliveredEvent</tt>,
     * <tt>ChatRoomMessageReceivedEvent</tt> or a
     * <tt>ChatRoomMessageDeliveryFailedEvent</tt> has been fired.
     * 
     * @param evt The specific event
     */
    public void fireMessageEvent(EventObject evt)
    {
        Iterator<ChatRoomMessageListener> listeners = null;
        synchronized (messageListeners)
        {
            listeners = new ArrayList<ChatRoomMessageListener>(
                                messageListeners).iterator();
        }

        if (!listeners.hasNext())
        {
            messageBuffer.add(evt);
        }

        while (listeners.hasNext())
        {
            ChatRoomMessageListener listener = listeners.next();

            if (evt instanceof ChatRoomMessageDeliveredEvent)
            {
                listener.messageDelivered((ChatRoomMessageDeliveredEvent) evt);
            }
            else if (evt instanceof ChatRoomMessageReceivedEvent)
            {
                listener.messageReceived((ChatRoomMessageReceivedEvent) evt);
            }
            else if (evt instanceof ChatRoomMessageDeliveryFailedEvent)
            {
                listener
                    .messageDeliveryFailed((ChatRoomMessageDeliveryFailedEvent) evt);
            }
        }
    }

    /**
     * Sets the corresponding switchboard.
     * 
     * @param switchboard Corresponding switchboard.
     */
    public void setSwitchboard(MsnSwitchboard switchboard)
    {
        this.switchboard = switchboard;
    }

    /**
     * Adds a chat room member to the members list.
     * 
     * @param member The member to add.
     */
    public void addChatRoomMember(String id, ChatRoomMemberMsnImpl member)
    {
        members.put(id, member);

        fireMemberPresenceEvent(member,
            ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
    }

    /**
     * Removes a chat room member from the and fires a member presence change
     * event, so that the user gets the leaving information.
     * 
     * @param id The member ID to remove.
     */
    public void removeChatRoomMember(String id)
    {
        ChatRoomMember member = members.get(id);

        members.remove(id);

        fireMemberPresenceEvent(member,
            ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT, null);
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
    private void fireMemberPresenceEvent(   ChatRoomMember member,
                                            String eventID,
                                            String eventReason)
    {
        ChatRoomMemberPresenceChangeEvent evt =
            new ChatRoomMemberPresenceChangeEvent(this, member, eventID,
                eventReason);

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

    public void setChatRoomName(String name)
    {
        this.chatRoomName = name;
    }

    /**
     * Determines whether this chat room should be stored in the configuration
     * file or not. If the chat room is persistent it still will be shown after
     * a restart in the chat room list. A non-persistent chat room will be only
     * in the chat room list until the the program is running.
     * 
     * @return true if this chat room is persistent, false otherwise
     */
    public boolean isPersistent()
    {
        return false;
    }

    /**
     * Finds the member of this chat room corresponding to the given nick name.
     * 
     * @param userAddress the nick name to search for.
     * @return the member of this chat room corresponding to the given nick name.
     */
    public ChatRoomMemberMsnImpl findMemberForAddress(String userAddress)
    {
        Iterator<ChatRoomMemberMsnImpl> membersIter
            = members.values().iterator();

        while (membersIter.hasNext())
        {
            ChatRoomMemberMsnImpl member = membersIter.next();

            if (member.getContactAddress().equals(userAddress))
            {
                return member;
            }
        }

        return null;
    }
}
