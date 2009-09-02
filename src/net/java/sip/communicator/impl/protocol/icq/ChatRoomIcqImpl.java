/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.kano.joustsim.Screenname;
import net.kano.joustsim.oscar.oscar.service.chatrooms.*;

/**
 * Represents a chat room, where multiple chat users could communicate in a
 * many-to-many fashion.
 * 
 * @author Rupert Burchardi
 */
public class ChatRoomIcqImpl implements ChatRoom
{
   private static final Logger logger = Logger
           .getLogger(ChatRoomIcqImpl.class);

   /**
    * Listeners that will be notified of changes in member status in the
    * room such as member joined, left or being kicked or dropped.
    */
   private final List<ChatRoomMemberPresenceListener> memberListeners
       = new Vector<ChatRoomMemberPresenceListener>();

   /**
    * Listeners that will be notified of changes in member role in the
    * room such as member being granted admin permissions, or revoked admin
    * permissions.
    */
   private final List<ChatRoomMemberRoleListener> memberRoleListeners 
       = new Vector<ChatRoomMemberRoleListener>();

   /**
    * Listeners that will be notified of changes in local user role in the
    * room such as member being granted admin permissions, or revoked admin
    * permissions.
    */
   private final List<ChatRoomLocalUserRoleListener> localUserRoleListeners 
       = new Vector<ChatRoomLocalUserRoleListener>();

   /**
    * Listeners that will be notified every time
    * a new message is received on this chat room.
    */
   private final List<ChatRoomMessageListener> messageListeners 
       = new Vector<ChatRoomMessageListener>();

   /**
    * Listeners that will be notified every time
    * a chat room property has been changed.
    */
   private Vector<ChatRoomPropertyChangeListener> propertyChangeListeners 
       = new Vector<ChatRoomPropertyChangeListener>();

   /**
    * Listeners that will be notified every time
    * a chat room member property has been changed.
    */
   private final List<ChatRoomMemberPropertyChangeListener> memberPropChangeListeners 
       = new Vector<ChatRoomMemberPropertyChangeListener>();

   /**
    * Chat room invitation from the icq provider, needed for joining a
    * chat room. 
    */
   private ChatInvitation chatInvitation = null;
   
   /**
    * Chat room session from the icq provider, we get this after joining a
    * chat room. 
    * Provides most of the function we need for multi user chatting. 
    */
   private ChatRoomSession chatRoomSession = null;

   /**
    * The list of members of this chat room.
    */
   private Hashtable<String, ChatRoomMember> members = new Hashtable<String, ChatRoomMember>();

   /**
    * The operation set that created us.
    */
   private OperationSetMultiUserChatIcqImpl opSetMuc = null;

   /**
    * The protocol provider that created us
    */
   private ProtocolProviderServiceIcqImpl provider = null;
   
   /**
    * List with invitations.
    */
   private Hashtable<String, String> inviteUserList = new Hashtable<String, String>();
   
   /**
    * Invitation message text.
    */
   private String inviteMessageText = "";
   
   /**
    * HTML mime type
    */
   private static final String HTML_MIME_TYPE = "text/html";

   private final String defaultHtmlStartTag = "<HTML>";

   private final String defaultHtmlEndTag = "</HTML>";
   
   /**
    * Chat room name.
    */

   private String chatRoomName = "";

   /**
    * The nick name of the user.
    */
   private String nickName = "";

   /**
    * Chat room subject. Note: ICQ does not support chat room subjects.
    */
   private String chatSubject = "";
   
   /**
    * Constructor for chat room instances, with a given chat room invitation.
    * If this constructor is used the user was invited to a chat room.
    * @param chatInvitation Chat room invitation that the user received from
    * the icq network
    * @param icqProvider The icq provider
    */
   public ChatRoomIcqImpl(ChatInvitation chatInvitation,
           ProtocolProviderServiceIcqImpl icqProvider)
   {

       chatRoomName = chatInvitation.getRoomName();
       this.chatInvitation = chatInvitation;
       this.provider = icqProvider;

       this.opSetMuc = (OperationSetMultiUserChatIcqImpl) provider
               .getOperationSet(OperationSetMultiUserChat.class);
   }

   /**
    * Constructor for chat room instances.
    * @param roomName The name of the chat room.
    * @param chatRoomSession Chat room session from the icq network
    * @param icqProvider  The icq provider
    */

   public ChatRoomIcqImpl(String roomName, ChatRoomSession chatRoomSession,
           ProtocolProviderServiceIcqImpl icqProvider)
   {
       this.chatRoomSession = chatRoomSession;
       chatRoomName = roomName;
       this.provider = icqProvider;

       this.opSetMuc = (OperationSetMultiUserChatIcqImpl) provider
               .getOperationSet(OperationSetMultiUserChat.class);

       this.chatRoomSession.addListener(new ChatRoomSessionListenerImpl(this));

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
       synchronized (memberPropChangeListeners)
       {
           if (!memberPropChangeListeners.contains(listener))
               memberPropChangeListeners.add(listener);
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
    * Registers <tt>listener</tt> so that it would receive events every time
    * a new message is received on this chat room.
    *
    * @param listener a <tt>MessageListener</tt> that would be notified
    *   every time a new message is received on this chat room.
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
       synchronized (propertyChangeListeners)
       {
           if (!propertyChangeListeners.contains(listener))
               propertyChangeListeners.add(listener);
       }
   }

   /**
    * Bans a user from the room. The ICQ protocol does not support blocking a
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
               "You cannot ban a participant in the icq protocol",
               OperationFailedException.NETWORK_FAILURE);
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
       return new MessageIcqImpl(new String(content), contentType,
               contentEncoding, subject);
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
       return
           new MessageIcqImpl(
                   messageText,
                   OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
                   OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING,
                   null);
   }

   /**
   * Returns the list of banned users.
   */
   public Iterator<ChatRoomMember> getBanList() throws OperationFailedException
   {
       throw new OperationFailedException(
               "This operation cannot be performed in the ICQ network.",
               OperationFailedException.NETWORK_FAILURE);
   }

   /**
    * Returns the <tt>ChatRoomConfigurationForm</tt> containing all
    * configuration properties for this chat room. ICQ does not support any
    * chat room configuration, so an OperationFailedException is always thrown.
    * 
    * @return the <tt>ChatRoomConfigurationForm</tt> containing all
    * configuration properties for this chat room
    * @throws OperationFailedException Always thrown if called, because the MSN
    * protocol does not support any chat room configuration
    */
   public ChatRoomConfigurationForm getConfigurationForm()
           throws OperationFailedException
   {
       throw new OperationFailedException(
               "The chat room configuration is not possible in the ICQ network.",
               OperationFailedException.NETWORK_FAILURE);
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
    * Returns a <tt>List</tt> of <tt>ChatRoomMembers</tt>s corresponding
    * to all members currently participating in this room.
    * 
    * @return a <tt>List</tt> of <tt>Contact</tt> corresponding to all room
    * members.
    */
   public List<ChatRoomMember> getMembers()
   {
       return new LinkedList<ChatRoomMember>(members.values());
   }

   /**
    * Returns the number of participants that are currently in this chat room.
    * 
    * @return the number of <tt>Contact</tt>s, currently participating in
    * this room.
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
    * possible inside the msn protocol!
    * 
    * @return the room subject or <tt>null</tt> if the user hasn't joined the
    * room or the room does not have a subject yet.
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
    * context of the local chat room.
    */
   public String getUserNickname()
   {
       if (nickName == null && isJoined())
           nickName = provider.getInfoRetreiver().getNickName(
                   provider.getAccountID().getUserID());

       return nickName;
   }

   /**
    * Invites another user to this room. If we're not joined nothing will
    * happen.
    * 
    * @param userAddress the address of the user (email address) to invite to
    * the room.(one may also invite users not on their contact list).
    * @param reason You cannot specify a Reason inside the msn protocol
    */
   public void invite(String userAddress, String reason)
   {
       assertConnected();

       if (chatRoomSession.getState().equals(ChatSessionState.INROOM))
           chatRoomSession.invite(new Screenname(userAddress), reason);
       else
           inviteUserList.put(userAddress, reason);
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
       if (chatRoomSession == null)
           return false;

       if (chatRoomSession.getState() == ChatSessionState.INROOM
               || chatRoomSession.getState() == ChatSessionState.CONNECTING
               || chatRoomSession.getState() == ChatSessionState.INITIALIZING)
           return true;

       return false;
   }

   /**
    * The ICQ multi user chat implementation doesn't support system rooms.
    * 
    * @return false to indicate that the ICQ protocol implementation doesn't
    * support system rooms.
    */
   public boolean isSystem()
   {
       return false;
   }

   /**
   * Joins this chat room with the nickname of the local user so that the
   * user would start receiving events and messages for it.
   *
   * @throws OperationFailedException with the corresponding code if an
   *   error occurs while joining the room.
   */
   public void join() throws OperationFailedException
   {
       joinAs(provider.getInfoRetreiver().getNickName(
               provider.getAccountID().getUserID()));
   }

   /**
   * Joins this chat room so that the user would start receiving events and
   * messages for it.
   *
   * @param password the password to use when authenticating on the
   *   chat room.
   * @throws OperationFailedException with the corresponding code if an
   *   error occurs while joining the room.
   */
   public void join(byte[] password) throws OperationFailedException
   {
       joinAs(provider.getInfoRetreiver().getNickName(
               provider.getAccountID().getUserID()));
   }

   /**
    * Joins this chat room with the specified nickname so that the user
    * would start receiving events and messages for it.
    *
    * @param nickname the nickname to use.
    * @throws OperationFailedException with the corresponding code if an
    *   error occurs while joining the room.
    */
   public void joinAs(String nickname, byte[] password)
           throws OperationFailedException
   {
       // TODO Auto-generated method stub
   }

   /**
    * Joins this chat room with the specified nickname so that
    * the user would start receiving events and messages for it.
    *
    * @param nickname the nickname to use.
    * @throws OperationFailedException with the corresponding code if an
    *   error occurs while joining the room.
    */
   public void joinAs(String nickname) throws OperationFailedException
   {
       if (chatRoomSession == null && chatInvitation == null)
       {   // the session is not set and we don't have a chatInvitatoin
           // so we try to join the chatRoom again 
           ChatRoomManager chatRoomManager = provider.getAimConnection()
                   .getChatRoomManager();
           chatRoomSession = chatRoomManager.joinRoom(this.getName());
           chatRoomSession.addListener(new ChatRoomSessionListenerImpl(this));
       }

       if (chatInvitation != null)
       {
           chatRoomSession = chatInvitation.accept();
           chatRoomSession.addListener(new ChatRoomSessionListenerImpl(this));
       }

       // We don't specify a reason.
       opSetMuc.fireLocalUserPresenceEvent(this,
               LocalUserChatRoomPresenceChangeEvent.LOCAL_USER_JOINED, null);
   }

   /**
    * Kicks a participant from the room. The ICQ protocol does not support
    * this.
    * 
    * @param chatRoomMember the <tt>ChatRoomMember</tt> to kick from the room
    * @param reason the reason why the participant is being kicked from the
    * room
    * @throws OperationFailedException Always throws an
    * OperationFailedException, because you cannot kick users from a
    * switchboard in the ICQ protocol.
    */
   public void kickParticipant(ChatRoomMember chatRoomMember, String reason)
           throws OperationFailedException
   {
       throw new OperationFailedException(
               "You cannot kick a participant from a chat room.",
               OperationFailedException.NETWORK_FAILURE);
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
       if (chatRoomSession != null)
       { // manually close the chat room session
           // and set the chat room session to null.
           chatRoomSession.close();
           chatRoomSession = null;
       }

       for (ChatRoomMember member : members.values())
           fireMemberPresenceEvent(
               member,
               ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT,
               "Local user has left the chat room.");

       // Delete the list of members
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
           localUserRoleListeners.remove(listener);
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
    * Removes the given <tt>listener</tt> from the list of listeners currently
    * registered for chat room member property change events.
    *
    * @param listener the <tt>ChatRoomMemberPropertyChangeListener</tt> to
    * remove.
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
    * Removes a listener that was being notified of changes of a member role in
    * this chat room such as us being granded operator.
    * 
    * @param listener a member role listener.
    */
   public void removeMemberRoleListener(ChatRoomMemberRoleListener listener)
   {
       synchronized (memberRoleListeners)
       {
           memberRoleListeners.remove(listener);
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
           messageListeners.remove(listener);
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
       synchronized (propertyChangeListeners)
       {
           propertyChangeListeners.remove(listener);
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
           chatRoomSession.sendMessage(message.getContent());

           //we don't need to fire a message delivered event, because 
           // we will receive this message again.
           ChatRoomMessageDeliveredEvent msgDeliveredEvt
               = new ChatRoomMessageDeliveredEvent(
                   this,
                   System.currentTimeMillis(),
                   message,
                   ChatRoomMessageDeliveredEvent.CONVERSATION_MESSAGE_DELIVERED);

           fireMessageEvent(msgDeliveredEvt);
       }
       catch (Exception e)
       {
           logger.debug("Failed to send a conference message.");
           throw new OperationFailedException(
                   "Failed to send a conference message.",
                   OperationFailedException.GENERAL_ERROR);
       }
   }

   /**
    * Sets the subject of this chat room. If the user does not have the right
    * to change the room subject, or the protocol does not support this, or the
    * operation fails for some other reason, the method throws an
    * <tt>OperationFailedException</tt> with the corresponding code. Note:
    * Not supported inside the ICQ protocol.
    * 
    * @param subject the new subject that we'd like this room to have
    * @throws OperationFailedException Always thrown because you cannot set the
    * subject in the icq protocol.
    */
   public void setSubject(String subject) throws OperationFailedException
   {
       throw new OperationFailedException(
               "You cannot set the chat room subject in the ICQ network.",
               OperationFailedException.NETWORK_FAILURE);
   }

   /**
    * Sets the nickName for this chat room.
    * 
    * @param nickname the nick name to set
    * @throws OperationFailedException If called, an OpFailedException is
    * called, because icq does not support nickname inside chat rooms.
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
    * Notifies all interested listeners that a
    * <tt>ChatRoomMessageDeliveredEvent</tt>,
    * <tt>ChatRoomMessageReceivedEvent</tt> or a
    * <tt>ChatRoomMessageDeliveryFailedEvent</tt> has been fired.
    * @param evt The specific event
    */
   public void fireMessageEvent(EventObject evt)
   {
       Iterable<ChatRoomMessageListener> listeners;
       synchronized (messageListeners)
       {
           listeners = new ArrayList<ChatRoomMessageListener>(messageListeners);
       }

       for (ChatRoomMessageListener listener : listeners)
       {
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
   private void fireMemberPresenceEvent(ChatRoomMember member, String eventID,
           String eventReason)
   {
       ChatRoomMemberPresenceChangeEvent evt
           = new ChatRoomMemberPresenceChangeEvent(this,
                                                   member,
                                                   eventID,
                                                   eventReason);

       logger.trace("Will dispatch the following ChatRoom event: " + evt);

       Iterable<ChatRoomMemberPresenceListener> listeners;
       synchronized (memberListeners)
       {
           listeners
               = new ArrayList<ChatRoomMemberPresenceListener>(memberListeners);
       }

       for (ChatRoomMemberPresenceListener listener : listeners)
           listener.memberPresenceChanged(evt);
   }

   /**
    * Our listener for all events for this chat room, e.g. incoming messages or 
    * users that leave or join the chat room.
    *
    */
   private class ChatRoomSessionListenerImpl
       implements ChatRoomSessionListener
   {
       /**
        * The chat room this listener is for.
        */
       private final ChatRoomIcqImpl chatRoom;

       /**
        * Constructor for this listener, needed to set the chatRoom.
        * @param room The containing chat room.
        */
       public ChatRoomSessionListenerImpl(ChatRoomIcqImpl room)
       {
           chatRoom = room;
       }

       /**
        * Handles incoming messages for the specified chat room.
        * @param chatRoomSession Specific chat room session
        * @param chatRoomUser The User who sends the message
        * @param chatMessage The message
        */

       public void handleIncomingMessage(ChatRoomSession chatRoomSession,
               ChatRoomUser chatRoomUser, ChatMessage chatMessage)
       {
           logger.debug("Incoming multi user chat message received: "
                   + chatMessage.getMessage());

           String msgBody = chatMessage.getMessage();
           

            String msgContent;
            if (msgBody.startsWith(defaultHtmlStartTag))
            {
                msgContent = msgBody.substring(
                    msgBody.indexOf(defaultHtmlStartTag)
                        + defaultHtmlStartTag.length(),
                    msgBody.indexOf(defaultHtmlEndTag));
            }
            else
                msgContent = msgBody;

           Message newMessage = createMessage(
                   msgContent.getBytes(),
                   HTML_MIME_TYPE,
                   OperationSetBasicInstantMessagingIcqImpl.DEFAULT_MIME_ENCODING,
                   null);

           String memberUID = chatRoomUser.getScreenname().getFormatted();
           String memberNickName = provider.getInfoRetreiver().getNickName(
                   memberUID);
           
           if(memberUID.equals(nickName))
               return;

           ChatRoomMemberIcqImpl member = new ChatRoomMemberIcqImpl(chatRoom,
                   memberNickName, memberUID, ChatRoomMemberRole.MEMBER);

           ChatRoomMessageReceivedEvent msgReceivedEvent
               = new ChatRoomMessageReceivedEvent(
                   chatRoom,
                   member,
                   System.currentTimeMillis(),
                   newMessage,
                   ChatRoomMessageReceivedEvent.CONVERSATION_MESSAGE_RECEIVED);

           fireMessageEvent(msgReceivedEvent);

       }

       public void handleStateChange(ChatRoomSession chatRoomSession,
               ChatSessionState oldChatSessionState,
               ChatSessionState newChatSessionState)
       {
           logger.debug("ChatRoomSessionState changed to: " + newChatSessionState);

           if (chatInvitation == null
                   && newChatSessionState.equals(ChatSessionState.INROOM))
           {
               try
               {
                   chatRoom.join();
               }
               catch (Exception e)
               {
                   logger.debug("Failed to join the chat room: " + e);
               }
           }

           if(inviteUserList != null
               && newChatSessionState.equals(ChatSessionState.INROOM) )
           {
               Iterator<Map.Entry<String, String>> invitesIter
                   = inviteUserList.entrySet().iterator();

               while (invitesIter.hasNext()) 
               {
                   Map.Entry<String, String> entry = invitesIter.next();

                   chatRoom.invite(entry.getKey(),
                                   entry.getValue());
               }
           }

           if (newChatSessionState.equals(ChatSessionState.CLOSED)
                   || newChatSessionState.equals(ChatSessionState.FAILED))
           {
               // the chatRoom is closed or we failed to join, so we remove all
               // chat room user from the chat room member list
               updateMemberList(chatRoomSession.getUsers(), true);
           }
       }

       public void handleUsersJoined(ChatRoomSession chatRoomSession,
               Set<ChatRoomUser> chatRoomUserSet)
       {
           // add the new members to the member list
           updateMemberList(chatRoomUserSet, false);
       }

       public void handleUsersLeft(ChatRoomSession chatRoomSession,
               Set<ChatRoomUser> chatRoomUserSet)
       {
           // remove the given members from the member list
           updateMemberList(chatRoomUserSet, true);
       }
   }

   /**
    * Updates the member list, if the given boolean is true given members will
    * be added, if it is false the given members will be removed.
    * 
    * @param chatRoomUserSet New members or members to remove
    * @param removeMember True if members should be removed, False if members
    * should be added.
    */

   private void updateMemberList(  Set<ChatRoomUser> chatRoomUserSet,
                                   boolean removeMember)
   {
       for (ChatRoomUser user : chatRoomUserSet)
       {
           String uid = user.getScreenname().getFormatted();

           //we want to add a member and he/she is not in our member list
           if (!removeMember
                   && !members.containsKey(uid)
                   && !uid.equals(provider.getAccountID().getUserID()))
           {
               String userNickName = provider.getInfoRetreiver().getNickName(
                       uid);

               ChatRoomMemberIcqImpl member = new ChatRoomMemberIcqImpl(this,
                       userNickName, uid, ChatRoomMemberRole.MEMBER);

               members.put(uid, member);

               fireMemberPresenceEvent(member,
                       ChatRoomMemberPresenceChangeEvent.MEMBER_JOINED, null);
           }
           //we want to remove a member and found him/her in our member list
           if (removeMember && members.containsKey(uid))
           {
               ChatRoomMemberIcqImpl member = (ChatRoomMemberIcqImpl) members
                       .get(uid);

               members.remove(uid);

               fireMemberPresenceEvent(member,
                       ChatRoomMemberPresenceChangeEvent.MEMBER_LEFT, null);
           }
       }
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
                           + "service before being able to communicate.");
       if (!provider.isRegistered())
           throw new IllegalStateException(
                   "The provider must be signed on the service before "
                           + "being able to communicate.");
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
    public ChatRoomMemberIcqImpl findMemberForNickName(String nickName)
    {
        return (ChatRoomMemberIcqImpl) members.get(nickName);
    }
}
