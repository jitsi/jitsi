/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smack.*;

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
    private Vector participantStatusListeners = new Vector();

    /**
     * Listeners that will be notified every time
     * a new message is received on this chat room.
     */
    private Vector messageListeners = new Vector();

    public ChatRoomJabberImpl(MultiUserChat multiUserChat)
    {
        this.multiUserChat = multiUserChat;
    }

    /**
     * Adds <tt>listener</tt> to the list of listeners registered to receive
     * events upon modification of chat room properties such as its subject
     * for example.
     *
     * @param listener the <tt>ChatRoomChangeListener</tt> that is to be
     * registered for <tt>ChatRoomChangeEvent</tt>-s.
     */
    public void addChatRoomPropertyChangeListener(
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
    public void removeChatRoomPropertyChangeListener(
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
    public void addLocalUserStatusListener(
                            ChatRoomLocalUserStatusListener listener)
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
    public void removeLocalUserStatusListener(ChatRoomLocalUserStatusListener
                                              listener)
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
    public void addParticipantStatusListener(
                    ChatRoomParticipantStatusListener listener)
    {
        synchronized(participantStatusListeners)
        {
            if (!participantStatusListeners.contains(listener))
                participantStatusListeners.add(listener);
        }
    }

    /**
     * Removes a listener that was being notified of changes in the status of
     * other chat room participants such as users being kicked, banned, or
     * granted admin permissions.
     *
     * @param listener a participant status listener.
     */
    public void removeParticipantStatusListener(
        ChatRoomParticipantStatusListener listener)
    {
        synchronized(participantStatusListeners)
        {
            participantStatusListeners.remove(listener);
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
        return null;
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
        return null;
    }

    /**
     * Returns a <tt>List</tt> of <tt>Contact</tt>s corresponding to all
     * members currently participating in this room.
     *
     * @return a <tt>List</tt> of <tt>Contact</tt> corresponding to all room
     *   members.
     */
    public List getMembers()
    {
        return null;
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
        return 0;
    }

    /**
     * Returns the name of this <tt>ChatRoom</tt>.
     *
     * @return a <tt>String</tt> containing the name of this
     *   <tt>ChatRoom</tt>.
     */
    public String getName()
    {
        return "";
    }

    /**
     * Returns the local user's nickname in the context of this chat room or
     * <tt>null</tt> if not currently joined.
     *
     * @return the nickname currently being used by the local user in the
     *   context of the local chat room.
     */
    public String getNickname()
    {
        return "";
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
        return "";
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
    }

    /**
     * Returns true if the local user is currently in the multi user chat
     * (after calling one of the {@link #join(String)} methods).
     *
     * @return true if currently we're currently in this chat room and false
     *   otherwise.
     */
    public boolean isJoined()
    {
        return false;
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
    }

    /**
     * Leave this chat room.
     *
     */
    public void leave()
    {
    }

    /**
     * Sends the <tt>message</tt> to the destination indicated by the
     * <tt>to</tt> contact.
     *
     * @param message the <tt>Message</tt> to send.
     * @throws IllegalStateException if the underlying stack is not
     *   registered or initialized or if the chat room is not joined.
     */
    public void sendMessage(Message message)
        throws IllegalStateException
    {
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
}
