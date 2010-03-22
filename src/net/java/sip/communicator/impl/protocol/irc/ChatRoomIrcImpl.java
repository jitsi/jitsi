/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.beans.PropertyChangeEvent;
import java.util.*;

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
 */
public class ChatRoomIrcImpl
    implements ChatRoom
{
    /**
     * The object used for logging.
     */
    private static final Logger logger
        = Logger.getLogger(ChatRoomIrcImpl.class);

    /**
     * The name of the chat room.
     */
    private String chatRoomName = null;

    /**
     * The subject of the chat room.
     */
    private String chatSubject = null;

    /**
     * list of members of this chatRoom
     */
    private final Hashtable<String, ChatRoomMember> chatRoomMembers
        = new Hashtable<String, ChatRoomMember>();

    /**
     * The parent protocol service provider
     */
    private final ProtocolProviderServiceIrcImpl parentProvider;

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
     * Indicates if this chat room is a private one (i.e. created with the 
     * query command ).
     */
    private final boolean isPrivate;

    /**
     * Indicates if this chat room is a system one (i.e. corresponding to the
     * server channel).
     */
    private boolean isSystem = false;

    /**
     * The nick name of the local user for this chat room.
     */
    private String userNickName;

    /**
     * Creates an instance of <tt>ChatRoomIrcImpl</tt>, by specifying the room
     * name and the protocol provider.
     *  
     * @param chatRoomName the name of the chat room
     * @param parentProvider the protocol provider
     */
    public ChatRoomIrcImpl( String chatRoomName,
                            ProtocolProviderServiceIrcImpl parentProvider)
    {
        this(chatRoomName, parentProvider, false, false);
    }

    /**
     * Creates an instance of <tt>ChatRoomIrcImpl</tt>, by specifying the room
     * name, the protocol provider and the isPrivate property. Private chat
     * rooms are one-to-one chat rooms.
     *  
     * @param chatRoomName the name of the chat room
     * @param parentProvider the protocol provider
     * @param isPrivate indicates if this chat room is a private one
     * @param isSystem indicates if this chat room is a system room
     */
    public ChatRoomIrcImpl( String chatRoomName,
                            ProtocolProviderServiceIrcImpl parentProvider,
                            boolean isPrivate,
                            boolean isSystem)
    {
        this.parentProvider = parentProvider;
        this.chatRoomName = chatRoomName;
        this.isPrivate = isPrivate;
        this.isSystem = isSystem;
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
     * Joins this chat room with the nickname of the local user so that the user
     * would start receiving events and messages for it.
     * 
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void join() throws OperationFailedException
    {
        if (!parentProvider.getIrcStack().isConnected())
            throw new OperationFailedException(
                "We are currently not connected to the server.",
                OperationFailedException.NETWORK_FAILURE);

        if (parentProvider.getIrcStack().isJoined(this))
            throw new OperationFailedException(
                "Channel is already joined.",
                OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);

        parentProvider.getIrcStack().join(this);
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
    public void join(byte[] password) throws OperationFailedException
    {
        parentProvider.getIrcStack().join(this, password);
    }

    /**
     * Joins this chat room with the specified nickname so that the user would
     * start receiving events and messages for it. If the chat room already
     * contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     * 
     * @param nickname the nickname to use.
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void joinAs(String nickname) throws OperationFailedException
    {
        this.setUserNickname(nickname);
        this.join();
    }

    /**
     * Joins this chat room with the specified nickname and password so that the
     * user would start receiving events and messages for it. If the chatroom
     * already contains a user with this nickname, the method would throw an
     * OperationFailedException with code IDENTIFICATION_CONFLICT.
     * 
     * @param nickname the nickname to use.
     * @param password a password necessary to authenticate when joining the
     *            room.
     * @throws OperationFailedException with the corresponding code if an error
     *             occurs while joining the room.
     */
    public void joinAs(String nickname, byte[] password)
        throws OperationFailedException
    {
        this.setUserNickname(nickname);
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
        return parentProvider.getIrcStack().isJoined(this);
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
        this.parentProvider.getIrcStack().leave(this);
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
    public void banParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException
    {
        this.parentProvider.getIrcStack().banParticipant(this.getName(),
            chatRoomMember.getContactAddress(), reason);
    }

    /**
     * Kicks the given <tt>ChatRoomMember</tt>.
     * 
     * @param chatRoomMember the chat room member to kick
     * @param reason the reason of the kick 
     * @throws OperationFailedException if we are not joined or we don't have
     * enough privileges to kick a participant.
     */
    public void kickParticipant(ChatRoomMember chatRoomMember, String reason)
        throws OperationFailedException
    {
        this.parentProvider.getIrcStack().kickParticipant(this.getName(),
            chatRoomMember.getContactAddress(), reason);
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
        ChatRoomPropertyChangeListener listener)
    {
        synchronized (propertyChangeListeners)
        {
            if (!propertyChangeListeners.contains(listener))
                propertyChangeListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of listeners current
     * registered for chat room modification events.
     * 
     * @param listener the <tt>ChatRoomChangeListener</tt> to remove.
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
     * Adds a listener that will be notified of changes of a member role in the
     * room such as being granted operator.
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
     * chat room such as us being granted operator.
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
    public void setSubject(String subject)
        throws OperationFailedException
    {
        parentProvider.getIrcStack().setSubject(getName(), subject);
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
        if(userNickName == null && isJoined())
            userNickName =  parentProvider.getIrcStack().getNick();

        return userNickName;
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
    public void setUserNickname(String nickName)
        throws OperationFailedException
    {
        parentProvider.getIrcStack().setUserNickname(nickName);
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
     * Registers <tt>listener</tt> so that it would receive events every time
     * a new message is received on this chat room.
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
     * Adds a <tt>ChatRoomMember</tt> to the list of members of this chat room.
     * 
     * @param memberID the identifier of the member
     * @param member the <tt>ChatRoomMember</tt> to add.
     */
    protected void addChatRoomMember(String memberID, ChatRoomMember member)
    {
        chatRoomMembers.put(memberID, member);
    }

    /**
     * Removes a <tt>ChatRoomMember</tt> from the list of members of this chat
     * room.
     * 
     * @param memberID the name of the <tt>ChatRoomMember</tt> to remove.
     */
    protected void removeChatRoomMember(String memberID)
    {
        chatRoomMembers.remove(memberID);
    }

    /**
     * Returns the <tt>ChatRoomMember</tt> corresponding to the given member id.
     * If no member is found for the given id, returns NULL.
     * 
     * @param memberID the identifier of the member
     * @return the <tt>ChatRoomMember</tt> corresponding to the given member id.
     */
    protected ChatRoomMember getChatRoomMember(String memberID)
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
    public void invite(String userAddress, String reason)
    {
        parentProvider.getIrcStack()
            .sendInvite(userAddress, chatRoomName);
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
    public Message createMessage(   byte[] content,
                                    String contentType,
                                    String contentEncoding,
                                    String subject)
    {
        Message msg = new MessageIrcImpl(  new String(content),
                                            contentType,
                                            contentEncoding,
                                            subject);

        return msg;
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
    public void sendMessage(Message message) throws OperationFailedException
    {
        assertConnected();

        String[] splitMessages = message.getContent().split("\n");

        String messagePortion = null;
        for (int i = 0; i < splitMessages.length; i ++)
        {
            messagePortion = splitMessages[i];

            // As we only send one message per line, we ignore empty lines in
            // the incoming multi line message.
            if(messagePortion.equals("\n")
                || messagePortion.matches("[\\ ]*"))
                continue;

            if (((MessageIrcImpl) message).isCommand())
            {
                parentProvider.getIrcStack()
                    .sendCommand(this, messagePortion);
            }
            else
            {
                parentProvider.getIrcStack()
                    .sendMessage(chatRoomName, messagePortion);
            }

            this.fireMessageDeliveredEvent(
                new MessageIrcImpl( messagePortion,
                                    message.getContentType(),
                                    message.getEncoding(),
                                    message.getSubject()));
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
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (parentProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                +"service before being able to communicate.");
        if (!parentProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                +"being able to communicate.");
    }
    
    /**
     * Notifies all interested listeners that a
     * <tt>ChatRoomMessageDeliveredEvent</tt> has been fired.
     * 
     * @param message the delivered message 
     */
    private void fireMessageDeliveredEvent(Message message)
    {
        int eventType
            = ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED;

        MessageIrcImpl msg = (MessageIrcImpl) message;

        if (msg.isAction())
        {
            eventType = ChatRoomMessageDeliveredEvent.ACTION_MESSAGE_DELIVERED;

            if (msg.getContent().indexOf(' ') != -1)
                msg.setContent(
                    msg.getContent()
                        .substring(message.getContent().indexOf(' ')));
        }

        ChatRoomMessageDeliveredEvent msgDeliveredEvt
            = new ChatRoomMessageDeliveredEvent(this,
                                                System.currentTimeMillis(),
                                                msg,
                                                eventType);

        Iterable<ChatRoomMessageListener> listeners;
        synchronized (messageListeners)
        {
            listeners
                = new ArrayList<ChatRoomMessageListener>(messageListeners);
        }
    
        for (ChatRoomMessageListener listener : listeners)
            listener.messageDelivered(msgDeliveredEvt);
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
    public void fireMessageReceivedEvent(   Message message,
                                            ChatRoomMember fromMember,
                                            long date,
                                            int eventType)
    {
        ChatRoomMessageReceivedEvent event
            = new ChatRoomMessageReceivedEvent( this,
                                                fromMember,
                                                date,
                                                message,
                                                eventType);

        Iterable<ChatRoomMessageListener> listeners;
        synchronized (messageListeners)
        {
            listeners
                = new ArrayList<ChatRoomMessageListener>(messageListeners);
        }

        for (ChatRoomMessageListener listener : listeners)
            listener.messageReceived(event);
    }
    
    /**
     * Delivers the specified event to all registered property change listeners.
     * 
     * @param evt the <tt>PropertyChangeEvent</tt> that we'd like delivered to
     * all registered property change listeners.
     */
    public void firePropertyChangeEvent(PropertyChangeEvent evt)
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
    public void fireMemberPresenceEvent(ChatRoomMember member,
                                        ChatRoomMember actorMember,
                                        String eventID,
                                        String eventReason)
    {
        ChatRoomMemberPresenceChangeEvent evt;
        if(actorMember != null)
            evt = new ChatRoomMemberPresenceChangeEvent(
                this, member, actorMember, eventID, eventReason);
        else
            evt = new ChatRoomMemberPresenceChangeEvent(
                this, member, eventID, eventReason);

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
     * changed his role in this <tt>ChatRoom</tt>.
     *
     * @param member the <tt>ChatRoomMember</tt> that this event is about 
     * @param newRole the new role of the given member
     */
    public void fireMemberRoleEvent(   ChatRoomMember member,
                                        ChatRoomMemberRole newRole)
    {
        member.setRole(newRole);
        ChatRoomMemberRole previousRole = member.getRole();

        ChatRoomMemberRoleChangeEvent evt
            = new ChatRoomMemberRoleChangeEvent(this,
                                                member,
                                                previousRole,
                                                newRole);
        
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
     * Indicates if this chat room is a private one or not. Private chat rooms
     * are created with the query command.
     * 
     * @return <code>true</code> if this chat room is private and
     * <code>false</code> otherwise.
     */
    public boolean isPrivate()
    {
        return isPrivate;
    }

    /**
     * Indicates whether or not this chat room is corresponding to a server
     * channel.
     * 
     * @return <code>true</code> to indicate that this chat room is
     * corresponding to a server channel, <code>false</code> - otherwise.
     */
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
    protected void setSystem(boolean isSystem)
    {
        this.isSystem = isSystem;
    }

    /**
     * Sets the nickName for this chat room.
     * 
     * @param nickName the nick name to set
     */
    protected void setNickName(String nickName)
    {
        this.userNickName = nickName;
    }

    /**
     * Sets the subject obtained from the server once we're connected.
     *
     * @param subject the subject to set
     */
    protected void setSubjectFromServer(String subject)
    {
        this.chatSubject = subject;
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
        /*
         * Private ChatRooms are not persistent because they correspond to
         * conversations created by sending private messages and such
         * conversations are not traditionally persisted by other IRC clients.
         */
        return !isPrivate();
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
}
