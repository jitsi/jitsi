package net.java.sip.communicator.impl.protocol.yahoo;

import java.io.IOException;
import java.util.*;

import ymsg.network.*;
import ymsg.network.event.*;
import ymsg.support.MessageDecoder;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * A yahoo implementation of the multi user chat operation set.
 * 
 * @author Rupert Burchardi
 */
public class OperationSetMultiUserChatYahooImpl
    implements  OperationSetMultiUserChat,
                SubscriptionListener
{
   private static final Logger logger = Logger
           .getLogger(OperationSetMultiUserChatYahooImpl.class);
   /**
    * A list of listeners subscribed for invitations multi user chat events.
    */
   private Vector invitationListeners = new Vector();

   /**
    * A list of listeners subscribed for events indicating rejection of a multi
    * user chat invitation sent by us.
    */
   private Vector invitationRejectionListeners = new Vector();

   /**
    * Listeners that will be notified of changes in our status in the room such
    * as us being kicked, banned, or granted admin permissions.
    */
   private Vector presenceListeners = new Vector();

   /**
    * A list of the rooms that are currently open by this account.
    */
   private Hashtable chatRoomCache = new Hashtable();

   /**
    * The currently valid Yahoo protocol provider service implementation.
    */
   private ProtocolProviderServiceYahooImpl yahooProvider = null;

   /**
    * The operation set for the basic instant messaging, provides some 
    * message format functions.
    */
   private OperationSetBasicInstantMessagingYahooImpl opSetBasic = null;

   /**
    * Message decoder allows to convert Yahoo formated messages, which can
    * contains some specials characters, to HTML or to plain text.
    */
   private MessageDecoder messageDecoder = new MessageDecoder();

   /**
    * Default Invitation message.
    */
   private final String DEFAULT_INVITATION = "Please join my chat room!";

   /**
    * Instantiates the user operation set with a currently valid instance of
    * the Yahoo protocol provider.
    * 
    * @param yahooProvider
    *            a currently valid instance of
    *            ProtocolProviderServiceYahooImpl.
    */
   OperationSetMultiUserChatYahooImpl(
           ProtocolProviderServiceYahooImpl yahooProvider)
   {
       this.yahooProvider = yahooProvider;

       yahooProvider.addRegistrationStateChangeListener(
           new RegistrationStateListener());

       opSetBasic = (OperationSetBasicInstantMessagingYahooImpl) yahooProvider
               .getOperationSet(OperationSetBasicInstantMessaging.class);

       OperationSetPersistentPresence presenceOpSet
           = (OperationSetPersistentPresence) yahooProvider
               .getOperationSet(OperationSetPersistentPresence.class);

       presenceOpSet.addSubscriptionListener(this);
   }

   /**
    * Adds a listener to invitation notifications.
    * 
    * @param listener
    *            An invitation listener.
    */
   public void addInvitationListener(ChatRoomInvitationListener listener)
   {
       synchronized (invitationListeners)
       {
           if (!invitationListeners.contains(listener))
               invitationListeners.add(listener);
       }
   }

   /**
    * Removes a listener that was being notified of changes in our status in a
    * room such as us being kicked, banned or dropped.
    * 
    * @param listener
    *            the <tt>LocalUserChatRoomPresenceListener</tt>.
    */
   public void removeInvitationListener(ChatRoomInvitationListener listener)
   {
       synchronized (invitationListeners)
       {
           invitationListeners.remove(listener);
       }
   }

   /**
    * Subscribes <tt>listener</tt> so that it would receive events indicating
    * rejection of a multi user chat invitation that we've sent earlier.
    * 
    * @param listener
    *            the listener that we'll subscribe for invitation rejection
    *            events.
    */

   public void addInvitationRejectionListener(
           ChatRoomInvitationRejectionListener listener)
   {
       synchronized (invitationRejectionListeners)
       {
           if (!invitationRejectionListeners.contains(listener))
               invitationRejectionListeners.add(listener);
       }
   }

   /**
    * Removes <tt>listener</tt> from the list of invitation listeners
    * registered to receive invitation rejection events.
    * 
    * @param listener
    *            the invitation listener to remove.
    */
   public void removeInvitationRejectionListener(
           ChatRoomInvitationRejectionListener listener)
   {
       synchronized (invitationRejectionListeners)
       {
           invitationRejectionListeners.remove(listener);
       }
   }

   /**
    * Adds a listener that will be notified of changes in our status in a chat
    * room such as us being kicked, banned or dropped.
    * 
    * @param listener
    *            the <tt>LocalUserChatRoomPresenceListener</tt>.
    */
   public void addPresenceListener(LocalUserChatRoomPresenceListener listener)
   {
       synchronized (presenceListeners)
       {
           if (!presenceListeners.contains(listener))
               presenceListeners.add(listener);
       }
   }

   /**
    * Removes a listener that was being notified of changes in our status in a
    * room such as us being kicked, banned or dropped.
    * 
    * @param listener
    *            the <tt>LocalUserChatRoomPresenceListener</tt>.
    */
   public void removePresenceListener(
           LocalUserChatRoomPresenceListener listener)
   {
       synchronized (presenceListeners)
       {
           presenceListeners.remove(listener);
       }
   }

   /**
    * Creates a room with the named <tt>roomName</tt> and according to the
    * specified <tt>roomProperties</tt> on the server that this protocol
    * provider is currently connected to. Note the roomProperties also contain
    * users that we like to invite to the chatRoom, this is required in the
    * yahoo protocol.
    * 
    * @param roomName
    *            the name of the <tt>ChatRoom</tt> to create.
    * @param roomProperties
    *            properties specifying how the room should be created.
    * 
    * @throws OperationFailedException
    *             if the room couldn't be created for some reason (e.g. room
    *             already exists; user already joined to an existent room or
    *             user has no permissions to create a chat room).
    * @throws OperationNotSupportedException
    *             if chat room creation is not supported by this server
    * 
    * @return ChatRoom the chat room that we've just created.
    */
   public ChatRoom createChatRoom(String roomName, Hashtable roomProperties)
           throws OperationFailedException, OperationNotSupportedException
   {
       ChatRoom chatRoom = null;
       try
       {
           YahooConference conference = yahooProvider.getYahooSession()
               .createConference(
                       new String[]{},  //users invited to this conference
                       "",              //invite message / topic
                       yahooProvider.getYahooSession().getLoginIdentity());

           chatRoom = findRoom(conference);

       }
       catch (Exception e)
       {
           logger.debug("Failed to create the chat Room" + e);
       }

       return chatRoom;
   }

   /**
    * Returns a reference to a chatRoom named <tt>roomName</tt> or null.
    * 
    * @param roomName
    *            the name of the <tt>ChatRoom</tt> that we're looking for.
    * @return the <tt>ChatRoom</tt> named <tt>roomName</tt> or null if no such
    *         room exists on the server that this provider is currently
    *         connected to.
    * @throws OperationFailedException
    *             if an error occurs while trying to discover the room on the
    *             server.
    * @throws OperationNotSupportedException
    *             if the server does not support multi user chat
    */
   public ChatRoom findRoom(String roomName) throws OperationFailedException,
           OperationNotSupportedException
   {
       ChatRoom room = (ChatRoom) chatRoomCache.get(roomName);

       return room;
   }

   /**
    * Returns a reference to a chatRoom based on the YahooConference, or
    * creates a chatRoom with it.
    * 
    * @param yahooConference
    *            The yahoo conference model for a chat room
    * @return the <tt>ChatRoom</tt> with the name that is in the yahoo
    *         conference specified.
    * @throws OperationFailedException
    *             if an error occurs while trying to discover the room on the
    *             server.
    * @throws OperationNotSupportedException
    *             if the server does not support multi user chat
    */
   public ChatRoom findRoom(YahooConference yahooConference)
           throws OperationFailedException, OperationNotSupportedException
   {
       ChatRoom room = (ChatRoom) chatRoomCache.get(yahooConference.getName());

       if (room == null)
       {
           room = createLocalChatRoomInstance(yahooConference);
           chatRoomCache.put(yahooConference.getName(), room);
       }

       return room;
   }

   /**
    * Creates a <tt>ChatRoom</tt> instance from the specified Yahoo conference.
    * 
    * @param yahooConference
    *            The chat room model from the yahoo lib.
    * 
    * @return ChatRoom the chat room that we've just created.
    */
   private ChatRoom createLocalChatRoomInstance(YahooConference yahooConference)
   {
       synchronized (chatRoomCache)
       {
           ChatRoom newChatRoom = new ChatRoomYahooImpl(yahooConference,
                   yahooProvider);

           return newChatRoom;
       }
   }

   /**
    * Returns a list of the chat rooms that we have joined and are currently
    * active in.
    * 
    * @return a <tt>List</tt> of the rooms where the user has joined using a
    *         given connection.
    */
   public List getCurrentlyJoinedChatRooms()
   {
       synchronized (chatRoomCache)
       {
           List joinedRooms = new LinkedList(this.chatRoomCache.values());

           Iterator joinedRoomsIter = joinedRooms.iterator();

           while (joinedRoomsIter.hasNext())
           {
               if (!((ChatRoom) joinedRoomsIter.next()).isJoined())
                   joinedRoomsIter.remove();
           }

           return joinedRooms;
       }
   }

   /**
    * Returns a list of the names of all chat rooms that <tt>contact</tt> is
    * currently a member of.
    * 
    * @param contact
    *            the contact whose current ChatRooms we will be querying.
    * @return a list of <tt>String</tt> indicating the names of the chat rooms
    *         that <tt>contact</tt> has joined and is currently active in.
    * 
    * @throws OperationFailedException
    *             if an error occurs while trying to discover the room on the
    *             server.
    * @throws OperationNotSupportedException
    *             if the server does not support multi user chat
    */
   public List getCurrentlyJoinedChatRooms(ChatRoomMember chatRoomMember)
           throws OperationFailedException, OperationNotSupportedException
   {
       synchronized (chatRoomCache)
       {
           List joinedRooms = new LinkedList(this.chatRoomCache.values());

           Iterator joinedRoomsIter = joinedRooms.iterator();

           while (joinedRoomsIter.hasNext())
           {
               if (!((ChatRoom) joinedRoomsIter.next()).isJoined())
                   joinedRoomsIter.remove();
           }

           return joinedRooms;
       }
   }

   public List getExistingChatRooms() throws OperationFailedException,
           OperationNotSupportedException
   {
       LinkedList list = new LinkedList();

       // disabled due to new security system for chat rooms.

       // try
       // {
       // YahooChatCategory root = YahooChatCategory.loadCategories();
       // getChatCategories(root, "");
       // }
       // catch (Exception e) {}
       // return _chatRoomList;

       return list;
   }

   /**
    * Returns true if <tt>contact</tt> supports multi user chat sessions.
    * 
    * @param contact
    *            reference to the contact whose support for chat rooms we are
    *            currently querying.
    * @return a boolean indicating whether <tt>contact</tt> supports chatrooms.
    * @todo Implement this
    *       net.java.sip.communicator.service.protocol.OperationSetMultiUserChat
    *       method
    */
   public boolean isMultiChatSupportedByContact(Contact contact)
   {
       if (contact.getProtocolProvider().getOperationSet(
               OperationSetMultiUserChat.class) != null)
           return true;

       return false;
   }

   /**
    * Informs the sender of an invitation that we decline their invitation.
    * 
    * @param invitation
    *            the connection to use for sending the rejection.
    * @param rejectReason
    *            the reason to reject the given invitation
    */
   public void rejectInvitation(ChatRoomInvitation invitation,
           String rejectReason)
   {
       ChatRoomYahooImpl chatRoom = (ChatRoomYahooImpl) invitation
               .getTargetChatRoom();

       try
       {
           yahooProvider.getYahooSession().declineConferenceInvite(
                   chatRoom.getYahooConference(), rejectReason);

       }
       catch (IOException e)
       {
           logger.debug("Failed to reject Invitation: " + e);
       }
   }

   /**
    * Delivers a <tt>ChatRoomInvitationReceivedEvent</tt> to all registered
    * <tt>ChatRoomInvitationListener</tt>s.
    * 
    * @param targetChatRoom
    *            the room that invitation refers to
    * @param inviter
    *            the inviter that sent the invitation
    * @param reason
    *            the reason why the inviter sent the invitation
    * @param password
    *            the password to use when joining the room
    */
   public void fireInvitationEvent(ChatRoom targetChatRoom, String inviter,
           String reason, byte[] password)
   {
       ChatRoomInvitationYahooImpl invitation = new ChatRoomInvitationYahooImpl(
               targetChatRoom, inviter, reason, password);

       ChatRoomInvitationReceivedEvent evt = new ChatRoomInvitationReceivedEvent(
               this, invitation, new Date(System.currentTimeMillis()));

       Iterator listeners = null;
       synchronized (invitationListeners)
       {
           listeners = new ArrayList(invitationListeners).iterator();
       }

       while (listeners.hasNext())
       {
           ChatRoomInvitationListener listener
               = (ChatRoomInvitationListener) listeners.next();

           listener.invitationReceived(evt);
       }
   }

   /**
    * Delivers a <tt>ChatRoomInvitationRejectedEvent</tt> to all registered
    * <tt>ChatRoomInvitationRejectionListener</tt>s.
    * 
    * @param sourceChatRoom
    *            the room that invitation refers to
    * @param invitee
    *            the name of the invitee that rejected the invitation
    * @param reason
    *            the reason of the rejection
    */
   public void fireInvitationRejectedEvent(ChatRoom sourceChatRoom,
           String invitee, String reason)
   {
       ChatRoomInvitationRejectedEvent evt = new ChatRoomInvitationRejectedEvent(
               this, sourceChatRoom, invitee, reason, new Date(System
                       .currentTimeMillis()));

       Iterator listeners = null;
       synchronized (invitationRejectionListeners)
       {
           listeners = new ArrayList(invitationRejectionListeners).iterator();
       }

       while (listeners.hasNext())
       {
           ChatRoomInvitationRejectionListener listener
               = (ChatRoomInvitationRejectionListener) listeners.next();

           listener.invitationRejected(evt);
       }
   }

   /**
    * Delivers a <tt>LocalUserChatRoomPresenceChangeEvent</tt> to all
    * registered <tt>LocalUserChatRoomPresenceListener</tt>s.
    * 
    * @param chatRoom
    *            the <tt>ChatRoom</tt> which has been joined, left, etc.
    * @param eventType
    *            the type of this event; one of LOCAL_USER_JOINED,
    *            LOCAL_USER_LEFT, etc.
    * @param reason
    *            the reason
    */
   public void fireLocalUserPresenceEvent(ChatRoom chatRoom, String eventType,
           String reason)
   {
       LocalUserChatRoomPresenceChangeEvent evt
           = new LocalUserChatRoomPresenceChangeEvent( this,
                                                       chatRoom,
                                                       eventType,
                                                       reason);

       Iterator listeners = null;
       synchronized (presenceListeners)
       {
           listeners = new ArrayList(presenceListeners).iterator();
       }

       while (listeners.hasNext())
       {
           LocalUserChatRoomPresenceListener listener
               = (LocalUserChatRoomPresenceListener) listeners.next();

           listener.localUserPresenceChanged(evt);
       }
   }

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
           String contentEncoding, String subject)
   {
       return new MessageYahooImpl(new String(content), contentType,
               contentEncoding, subject);
   }

   /**
    * Creates a message by a given message text.
    * 
    * @param messageText
    *            The message text.
    * @return the newly created message.
    */
   public Message createMessage(String messageText)
   {
       return new MessageYahooImpl(messageText,
               OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
               OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, null);
   }

   /**
    * Our listener that will tell us when we're registered to yahoo network.
    * 
    */
   private class RegistrationStateListener implements
           RegistrationStateChangeListener
   {
       /**
        * The method is called by a ProtocolProvider implementation whenever a
        * change in the registration state of the corresponding provider had
        * occurred.
        * 
        * @param evt
        *            ProviderStatusChangeEvent the event describing the status
        *            change.
        */
       public void registrationStateChanged(RegistrationStateChangeEvent evt)
       {
           if (evt.getNewState() == RegistrationState.REGISTERED)
           {
               yahooProvider.getYahooSession().addSessionListener(
                       new YahooMessageListener());
           }
       }
   }

   /**
    * Our group chat message listener, it extends the SessionAdapter from the
    * the yahoo library.
    * 
    */
   private class YahooMessageListener extends SessionAdapter
   {

       public void conferenceInviteDeclinedReceived(SessionConferenceEvent ev)
       {
           logger.debug("Group Chat invite declined received. "
                   + ev.toString());
           try
           {
               ChatRoom chatRoom = findRoom(ev.getRoom());

               fireInvitationRejectedEvent(chatRoom, ev.getFrom(), ev
                       .getMessage());
           }
           catch (Exception e)
           {
               logger.debug("Error: " + e);
           }
       }

       public void conferenceInviteReceived(SessionConferenceEvent ev)
       {
           logger.debug("Conference Invite Received: " + ev.toString());

           try
           {
               ChatRoom chatRoom = findRoom(ev.getRoom().getName());
               if (chatRoom != null)
               {
                   chatRoom.join();
               } else
               {
                   chatRoom = findRoom(ev.getRoom());
                   fireInvitationEvent(chatRoom, ev.getFrom(),
                           ev.getMessage(), null);
               }

           }
           catch (Exception e)
           {
               logger.debug("Error: " + e);
           }
       }

       public void conferenceLogoffReceived(SessionConferenceEvent ev)
       {
           logger.debug("Conference Logoff Received: " + ev.toString());

           try
           {
               ChatRoomYahooImpl chatRoom = (ChatRoomYahooImpl) findRoom(ev
                       .getRoom().getName());

               if (chatRoom != null)
               {
                   ChatRoomMemberYahooImpl member = chatRoom
                           .getChatRoomMember(ev.getFrom());
                   chatRoom.removeChatRoomMember(member);
               }
           }
           catch (Exception e)
           {
               logger
                       .debug("Failed to remove a user from the chat room. "
                               + e);
           }
       }

       public void conferenceLogonReceived(SessionConferenceEvent ev)
       {
           logger.debug("Conference Logon Received: " + ev.toString());

           try
           {
               ChatRoomYahooImpl chatRoom = (ChatRoomYahooImpl) findRoom(ev
                       .getRoom().getName());

               if (chatRoom != null)
               {
                   ChatRoomMemberYahooImpl newMember
                       = new ChatRoomMemberYahooImpl(
                           chatRoom,
                           ev.getFrom(),
                           ev.getFrom(),
                           ChatRoomMemberRole.MEMBER);

                   chatRoom.addChatRoomMember(newMember);
               }
           }
           catch (Exception e)
           {
               logger.debug("Failed to add a user to the chat room. " + e);
           }
       }

       public void conferenceMessageReceived(SessionConferenceEvent ev)
       {
           logger.debug("Conference Message Received: " + ev.toString());

           try
           {
               String formattedMessage = ev.getMessage();
               logger.debug("original message received : " + formattedMessage);

               // if the message is decorated by Yahoo, we try to "decode" it
               // first.
               if (formattedMessage.startsWith("\u001b"))
               {
                   formattedMessage = opSetBasic.processLinks(messageDecoder
                           .decodeToHTML(formattedMessage));
               } else
               {
                   formattedMessage = opSetBasic
                           .processLinks(formattedMessage);
               }

               // now, we try to fix a wrong usage of the size attribute in the
               // <font> HTML element
               // here, the zero 0 correspond to 10px
               formattedMessage = formattedMessage.replaceAll(
                       "(<font) (.*) size=\"0\">", "$1 $2 size=\"10\">");
               formattedMessage = formattedMessage.replaceAll(
                       "(<font) (.*) size=\"(\\d+)\">",
                       "$1 $2 style=\"font-size: $3px;\">");

               logger.debug("formatted Message : " + formattedMessage);
               // As no indications in the protocol is it html or not. No harm
               // to set all messages html - doesn't affect the appearance of
               // the gui
               
               Message newMessage = createMessage(formattedMessage.getBytes(),
                       opSetBasic.HTML_MIME_TYPE,
                       opSetBasic.DEFAULT_MIME_ENCODING, null);

               ChatRoomYahooImpl chatRoom = (ChatRoomYahooImpl) findRoom(ev
                       .getRoom().getName());

               if (chatRoom != null)
               {
                   ChatRoomMemberYahooImpl member = chatRoom
                           .getChatRoomMember(ev.getFrom());

                   ChatRoomMessageReceivedEvent msgReceivedEvent
                       = new ChatRoomMessageReceivedEvent(
                           chatRoom,
                           member,
                           new Date(),
                           newMessage,
                           ChatRoomMessageReceivedEvent
                               .CONVERSATION_MESSAGE_RECEIVED);

                   chatRoom.fireMessageEvent(msgReceivedEvent);
               }
           }
           catch (Exception e)
           {
               logger.debug("Error while receiving a multi user chat message: "
                               + e);
           }

       }

       public void connectionClosed(SessionEvent ev)
       {
           logger.debug("Connection Closed: " + ev.toString());
       }
   }
   
   /**
    * Updates corresponding chat room members when a contact has been modified
    * in our contact list.
    */
    public void contactModified(ContactPropertyChangeEvent evt)
    {
        Contact modifiedContact = evt.getSourceContact();

        this.updateChatRoomMembers(modifiedContact);
    }

    /**
     * Updates corresponding chat room members when a contact has been created
     * in our contact list.
     */
    public void subscriptionCreated(SubscriptionEvent evt)
    {
        Contact createdContact = evt.getSourceContact();

        this.updateChatRoomMembers(createdContact);
    }

    /**
     * Not interested in this event for our member update purposes.
     */
    public void subscriptionFailed(SubscriptionEvent evt)
    {}

    /**
     * Not interested in this event for our member update purposes.
     */
    public void subscriptionMoved(SubscriptionMovedEvent evt)
    {}

    /**
     * Updates corresponding chat room members when a contact has been removed
     * from our contact list.
     */
    public void subscriptionRemoved(SubscriptionEvent evt)
    {
        // Set to null the contact reference in all corresponding chat room
        // members.
        this.updateChatRoomMembers(null);
    }

    /**
     * Not interested in this event for our member update purposes.
     */
    public void subscriptionResolved(SubscriptionEvent evt)
    {}

    /**
     * Finds all chat room members, which name corresponds to the name of the
     * given contact and updates their contact references.
     * 
     * @param contact the contact we're looking correspondences for.
     */
    private void updateChatRoomMembers(Contact contact)
    {
        Enumeration<ChatRoomYahooImpl> chatRooms = chatRoomCache.elements();

        while (chatRooms.hasMoreElements())
        {
            ChatRoomYahooImpl chatRoom = chatRooms.nextElement();

            ChatRoomMemberYahooImpl member
                = chatRoom.findMemberForNickName(contact.getAddress());

            if (member != null)
            {
                member.setContact(contact);
                member.setAvatar(contact.getImage());
            }
        }
    }
}
