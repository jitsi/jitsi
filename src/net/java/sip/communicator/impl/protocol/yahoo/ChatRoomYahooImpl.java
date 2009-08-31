/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.yahoo;

import java.io.IOException;
import java.util.*;

import ymsg.network.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a yahoo chat room, where multiple chat users could communicate in
 * a many-to-many fashion.
 * 
 * @author Rupert Burchardi
 */
public class ChatRoomYahooImpl implements ChatRoom
{

   private static final Logger logger = Logger
           .getLogger(ChatRoomYahooImpl.class);

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
    * such as member being granted admin permissions, or revoked admin
    * permissions.
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
    * The protocol provider that created us
    */
   private ProtocolProviderServiceYahooImpl provider = null;

   /**
    * The operation set that created us.
    */
   private OperationSetMultiUserChatYahooImpl opSetMuc = null;

   /**
    * The list of members of this chat room.
    */
   private Hashtable<String, ChatRoomMember> members 
       = new Hashtable<String, ChatRoomMember>();

   /**
    * The list of members of this chat room.
    */
   private Hashtable<String, ChatRoomMemberYahooImpl> banList 
       = new Hashtable<String, ChatRoomMemberYahooImpl>();

   /**
    * The nickname of this chat room local user participant.
    */
   private String nickname;

   /**
    * The subject of this chat room. Keeps track of the subject changes.
    */
   private String oldSubject;

   /**
    * The yahoo conference model of this chat room, its the representation
    * of a chat room in the lib for this protocol.
    */
   private YahooConference yahooConference = null;

   /**
    * Creates an instance of a chat room that has been.
    * 
    * @param multiUserChat
    *            MultiUserChat
    * @param provider
    *            a reference to the currently valid jabber protocol provider.
    */
   public ChatRoomYahooImpl(YahooConference multiUserChat,
           ProtocolProviderServiceYahooImpl provider)
   {
       
       this.yahooConference = multiUserChat;
       this.provider = provider;
       this.opSetMuc = (OperationSetMultiUserChatYahooImpl) provider
               .getOperationSet(OperationSetMultiUserChat.class);
       
   }

   /**
    * Adds <tt>listener</tt> to the list of listeners registered to receive
    * events upon modification of chat room properties such as its subject for
    * example.
    * 
    * @param listener
    *            the <tt>ChatRoomChangeListener</tt> that is to be registered
    *            for <tt>ChatRoomChangeEvent</tt>-s.
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
    * Removes <tt>listener</tt> from the list of listeneres current registered
    * for chat room modification events.
    * 
    * @param listener The <tt>ChatRoomChangeListener</tt> to remove.
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
    * @param listener The <tt>ChatRoomMemberPropertyChangeListener</tt> that is
    *        to be registered for <tt>ChatRoomMemberPropertyChangeEvent</tt>s.
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
    * Removes the given <tt>listener</tt> from the list of listeners currently
    * registered for chat room member property change events.
    * 
    * @param listener The <tt>ChatRoomMemberPropertyChangeListener</tt> to remove.
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
    * Registers <tt>listener</tt> so that it would receive events every time a
    * new message is received on this chat room.
    * 
    * @param listener A <tt>MessageListener</tt> that would be notified every 
    *                 time a new message is received on this chat room.
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
    * @param listener The <tt>MessageListener</tt> to remove from this room
    */
   public void removeMessageListener(ChatRoomMessageListener listener)
   {
       synchronized (messageListeners)
       {
           messageListeners.remove(listener);
       }

   }

   /**
    * Adds a listener that will be notified of changes in our status in the
    * room such as us being kicked, banned, or granted admin permissions.
    * 
    * @param listener A participant status listener.
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
    * @param listener A participant status listener.
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
    * Create a Message instance for sending arbitrary MIME-encoding content.
    * 
    * @param content
    *            content value
    * @param contentType
    *            the MIME-type for <tt>content</tt>
    * @param contentEncoding
    *            encoding used for <tt>content</tt>
    * @param subject
    *            a <tt>String</tt> subject or <tt>null</tt> for now subject.
    * @return the newly created message.
    */
   public Message createMessage(byte[] content, String contentType,
           String contentEncoding, String subject)
   {
       return new MessageYahooImpl(new String(content), contentType,
               contentEncoding, subject);
   }

   /**
    * Create a Message instance for sending a simple text messages with default
    * (text/plain) content type and encoding.
    * 
    * @param messageText
    *            the string content of the message.
    * @return Message the newly created message
    */
   public Message createMessage(String messageText)
   {
       Message msg = new MessageYahooImpl(messageText,
               OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
               OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);

       return msg;
   }

   /**
    * Returns a <tt>List</tt> of <tt>Member</tt>s corresponding to all members
    * currently participating in this room.
    * 
    * @return a <tt>List</tt> of <tt>Member</tt> corresponding to all room
    *         members.
    */
   public List<ChatRoomMember> getMembers()
   {
       return new LinkedList<ChatRoomMember>(members.values());
   }

   /**
    * Updates the member list of the chat room.
    * 
    */
   
   public void updateMemberList()
   {
       Vector<YahooUser> memberList = yahooConference.getMembers();
       Iterator<YahooUser> it = memberList.iterator();

       while (it.hasNext())
       {
           YahooUser user = it.next();
           ChatRoomMemberYahooImpl member = new ChatRoomMemberYahooImpl(this,
                   user.getId(), user.getId(), ChatRoomMemberRole.MEMBER);
           
           if(!members.containsKey(member.getName()))
           {
               members.put(member.getName(), member);
           }
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
    * Bans a user from the room. The Yahoo protocol does not support blocking a
    * chat room for a specific user, so this method will always throw an
    * OperationFailedException.
    * 
    * @param chatRoomMember the <tt>ChatRoomMember</tt> to be banned.
    * @param reason the reason why the user was banned.
    * @throws OperationFailedException Always throws such an Exception, because
    * user banning based on chatrooms is not possible.
    */
   public void banParticipant(ChatRoomMember chatRoomMember, String reason)
           throws OperationFailedException
   {
       throw new OperationFailedException(
               "This operation is not possible to perform in the yahoo network.",
               OperationFailedException.GENERAL_ERROR);
   }
   

   /**
    * Returns the list of banned users, since it is not possible to 
    */
   
   public Iterator<ChatRoomMember> getBanList() throws OperationFailedException
   {
       return new LinkedList<ChatRoomMember>(banList.values()).iterator();
   }
   
   /**
    * Returns the <tt>ChatRoomConfigurationForm</tt> containing all
    * configuration properties for this chat room. Yahoo does not support any
    * chat room configuration, so an OperationFailedException is always thrown.
    * 
    * @return the <tt>ChatRoomConfigurationForm</tt> containing all
    * configuration properties for this chat room
    * @throws OperationFailedException Always thrown if called, because the
    * Yahoo protocol does not support any chat room configuration
    */
   
   public ChatRoomConfigurationForm getConfigurationForm()
           throws OperationFailedException
   {
       throw new OperationFailedException(
           "The configuration form is not implemented for the yahoo protocol.",
           OperationFailedException.GENERAL_ERROR);
   }

   /**
    * Returns the identifier of this <tt>ChatRoom</tt>.
    * 
    * @return a <tt>String</tt> containing the identifier of this
    * <tt>ChatRoom</tt>.
    */
   
   public String getIdentifier()
   {
       return yahooConference.getName();
   }

   /**
    * Returns the number of participants that are currently in this chat room.
    * 
    * @return the number of <tt>Contact</tt>s, currently participating in
    * this room.
    */
   public int getMembersCount()
   {
       return yahooConference.getMembers().size();
   }

   /**
    * Returns the name of this <tt>ChatRoom</tt>.
    * 
    * @return a <tt>String</tt> containing the name of this <tt>ChatRoom</tt>.
    */
   public String getName()
   {
       return yahooConference.getName();
   }

   /**
    * Returns the protocol provider service that created us.
    * 
    * @return the protocol provider service that created us.
    */
   public ProtocolProviderService getParentProvider()
   {
       return provider;
   }

   /**
    * Returns the last known room subject/theme or <tt>null</tt> if the user
    * hasn't joined the room or the room does not have a subject yet. <p> To be
    * notified every time the room's subject change you should add a
    * <tt>ChatRoomPropertyChangelistener</tt> to this room. <p>
    * 
    * 
    * To change the room's subject use {@link #setSubject(String)}. Note: Not
    * possible in the yahoo protocol!
    * 
    * @return the room subject or <tt>null</tt> if the user hasn't joined the
    * room or the room does not have a subject yet.
    */
   
   public String getSubject()
   {
       return oldSubject;
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
       if(nickname == null && isJoined())
           nickname = provider.getYahooSession().getLoginIdentity().getId();
       
       return nickname;
   }
   
   /**
    * Invites another user to this room. If we're not joined nothing will
    * happen.
    * 
    * @param userAddress The identifier of the contact (email address or yahoo id)
    * @param reason The invite reason, which is send to the invitee.
    */
   
   public void invite(String userAddress, String reason)
   {
       try
       {
           provider.getYahooSession().extendConference(yahooConference,
                   userAddress, reason);
       } catch (IOException ioe)
       {
           logger.debug("Failed to invite the user: " + userAddress
                   + " Error: " + ioe);
       }

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
       if(yahooConference == null || yahooConference.isClosed())
           return false;
               
       return true;
   }

    /**
     * Indicates whether or not this chat room is corresponding to a server
     * channel. Note: Returns always <code>false</code>.
     * 
     * @return Always <code>false</code> since system chat room can't be joined
     * with current yahoo library.
     */
   
   public boolean isSystem()
   {
       return false;
   }
   
   /**
    * Joins this chat room with the nickname of the local user so that the user
    * would start receiving events and messages for it.
    * 
    * @throws OperationFailedException with the corresponding code if an error
    * occurs while joining the room.
    */
   public void join() throws OperationFailedException
   {
       joinAs(provider.getAccountID().getUserID());
   }

   
   /**
    * Joins this chat room so that the user would start receiving events and
    * messages for it. Note: Secured chat rooms are not supported inside the
    * yahoo protocol.
    * @see join()
    * 
    * @param password the password to use when authenticating on the chat room.
    * @throws OperationFailedException with the corresponding code if an error
    * occurs while joining the room.
    */
   
   public void join(byte[] password) throws OperationFailedException
   {
       joinAs(provider.getAccountID().getUserID());
   }
   
   /**
    * Joins this chat room so that the user would start receiving events and
    * messages for it. Note: Not needed for the yahoo protocol.
    * @see join()
    * @param nickname the nickname to use.
    * @param password the password to use when authenticating on the chat room.
    * @throws OperationFailedException with the corresponding code if an error
    * occurs while joining the room.
    */
   
   public void joinAs(String nickname, byte[] password)
           throws OperationFailedException
   {
       // not needed
   }

   /**
    * Joins this chat room with the specified nickname so that the user would
    * start receiving events and messages for it.
    * 
    * @param nickname the nickname to use.
    * @throws OperationFailedException with the corresponding code if an error
    * occurs while joining the room.
    */
   
   public void joinAs(String nickname) throws OperationFailedException
   {
       this.nickname = nickname;

       try
       {
           provider.getYahooSession().acceptConferenceInvite(yahooConference);

           ChatRoomMemberYahooImpl member = new ChatRoomMemberYahooImpl(this,
                   nickname, provider.getYahooSession().getLoginIdentity()
                           .getId(), ChatRoomMemberRole.MEMBER);

           if(!members.containsKey(nickname))
           {
               members.put(nickname, member);
           }
           updateMemberList();

           // We don't specify a reason.
           opSetMuc.fireLocalUserPresenceEvent(this,
                   LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED,
                   null);

       } catch (Exception e)
       {
           logger.debug("Couldn't join the chat room: "
                   + yahooConference.getName() + " || " + e);
       }

   }
   
   /**
    * Kicks a participant from the room. 
    * 
    * @param member the <tt>ChatRoomMember</tt> to kick from the room
    * @param reason the reason why the participant is being kicked from the
    * room
    * @throws OperationFailedException 
    */

   public void kickParticipant(ChatRoomMember chatRoomMember, String reason)
           throws OperationFailedException
   {
   
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
       try
       {
           provider.getYahooSession().leaveConference(yahooConference);
           
           Iterator< Map.Entry<String, ChatRoomMember>> membersSet 
               = members.entrySet().iterator();

           while (membersSet.hasNext())
           {
               Map.Entry<String, ChatRoomMember> memberEntry 
                   = membersSet.next();

               ChatRoomMember member = memberEntry.getValue();

               fireMemberPresenceEvent(member,
                       ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT,
                       "Local user has left the chat room.");
           }
       }
       catch (IOException ioe)
       {
           logger.debug("Failed to leave the chat room: "
                   + yahooConference.getName() + " Error: " + ioe);
       }

       members.clear();
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
    * Sends the <tt>message</tt> to the destination indicated by the
    * <tt>to</tt> contact.
    * 
    * @param message The <tt>Message</tt> to send.
    * @throws OperationFailedException if the underlying stack is not
    * registered or initialized or if the chat room is not joined.
    */
   
   public void sendMessage(Message message) throws OperationFailedException
   {
       assertConnected();

       try
       {
           provider.getYahooSession().sendConferenceMessage(yahooConference,
                   message.getContent());

           ChatRoomMessageDeliveredEvent msgDeliveredEvt
               = new ChatRoomMessageDeliveredEvent(
                   this,
                   System.currentTimeMillis(),
                   message,
                   ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED);

           fireMessageEvent(msgDeliveredEvt);
       } catch (Exception e)
       {
           logger.debug("Failed to send a conference message.");
       }
   }

   /**
    * Sets the subject of this chat room. If the user does not have the right
    * to change the room subject, or the protocol does not support this, or the
    * operation fails for some other reason, the method throws an
    * <tt>OperationFailedException</tt> with the corresponding code. Note:
    * Not supported inside the yahoo protocol.
    * 
    * @param subject the new subject that we'd like this room to have
    * @throws OperationFailedException Always thrown because you cannot set the
    *           subject in the yahoo protocol.
    */
   public void setSubject(String subject) throws OperationFailedException
   {
       throw new OperationFailedException(
               "You cannot set the chat room subject in the yahoo network.",
               OperationFailedException.NETWORK_FAILURE);
   }

   /**
    * Sets the nickName for this chat room.
    * 
    * @param nickName the nick name to set
    * @throws OperationFailedException If called, an OpFailedException is
    * called, because yahoo does not support nickname inside chat rooms.
    * 
    */
   public void setUserNickname(String nickname)
           throws OperationFailedException
   {
       throw new OperationFailedException(
           "You cannot change or nick name in a chat room in the ICQ network.",
           OperationFailedException.NETWORK_FAILURE);
   }

   /**
    * Returns a chat room member from the member list by a given 
    * user address or null.
    * @param userAddress The user identifier, in this case: the email
    * or nickname.
    * @return The ChatRoomMember with the specified user address or null. 
    */
   public ChatRoomMemberYahooImpl getChatRoomMember(String userAddress)
   {
       Iterator<ChatRoomMember> it = members.values().iterator();

       while (it.hasNext())
       {
           ChatRoomMemberYahooImpl member = (ChatRoomMemberYahooImpl) it.next();

           if (member.getContactAddress().equals(userAddress))
           {
               return member;
           }
       }

       return null;
   }

   /**
    * Notifies all interested listeners that a
    * <tt>ChatRoomMessageDeliveredEvent</tt>,
    * <tt>ChatRoomMessageReceivedEvent</tt> or a
    * <tt>ChatRoomMessageDeliveryFailedEvent</tt> has been fired.
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

       while (listeners.hasNext())
       {
           ChatRoomMessageListener listener = listeners.next();

           if (evt instanceof ChatRoomMessageDeliveredEvent)
           {
               listener.messageDelivered((ChatRoomMessageDeliveredEvent) evt);
           } else if (evt instanceof ChatRoomMessageReceivedEvent)
           {
               listener.messageReceived((ChatRoomMessageReceivedEvent) evt);
           } else if (evt instanceof ChatRoomMessageDeliveryFailedEvent)
           {
               listener.messageDeliveryFailed(
                   (ChatRoomMessageDeliveryFailedEvent) evt);
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
   public void fireMemberPresenceEvent(ChatRoomMember member, String eventID,
           String eventReason)
   {
       ChatRoomMemberPresenceChangeEvent evt
           = new ChatRoomMemberPresenceChangeEvent(this,
                                                   member,
                                                   eventID,
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

   /**
    * Removes the specified chat room member from the member list
    * of this chat room.
    * @param member The member, who should be removed from the chat room
    * member list.
    */
   public void removeChatRoomMember(ChatRoomMemberYahooImpl member)
   {
       if(member == null)
           return;
       
       members.remove(member.getName());

       fireMemberPresenceEvent(member,
               ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT, null);
   }

   /**
    * Adds a member to the chat room member list.
    * @param member The member, who should be added to the chat room
    * member list.
    */
   public void addChatRoomMember(ChatRoomMemberYahooImpl member)
   {
       if (member == null)
           return;
       
       if (!members.containsKey(member.getName()))
       {
           members.put(member.getName(), member);

           fireMemberPresenceEvent(member,
                   ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
       }
   }
   /**
    * Returns the yahoo conference model of this chat room.
    * @return The yahoo conference.
    */
   public YahooConference getYahooConference()
   {
       return yahooConference;
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
     * Determines whether this chat room should be stored in the configuration
     * file or not. If the chat room is persistent it still will be shown after a
     * restart in the chat room list. A non-persistent chat room will be only in
     * the chat room list until the the program is running.
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
     * @param nickName the nick name to search for.
     * @return the member of this chat room corresponding to the given nick name.
     */
    public ChatRoomMemberYahooImpl findMemberForNickName(String nickName)
    {
        return (ChatRoomMemberYahooImpl) members.get(nickName);
    }
}
