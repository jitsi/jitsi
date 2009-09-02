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
    extends AbstractOperationSetMultiUserChat
    implements SubscriptionListener
{
   private static final Logger logger = Logger
           .getLogger(OperationSetMultiUserChatYahooImpl.class);

   /**
    * A list of the rooms that are currently open by this account.
    */
   private Hashtable<String, ChatRoomYahooImpl> chatRoomCache 
       = new Hashtable<String, ChatRoomYahooImpl>();

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
   public ChatRoom createChatRoom(String roomName, 
                                  Map<String, Object> roomProperties)
           throws OperationFailedException,
                  OperationNotSupportedException
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
       ChatRoomYahooImpl room = chatRoomCache.get(yahooConference.getName());

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
   private ChatRoomYahooImpl 
       createLocalChatRoomInstance(YahooConference yahooConference)
   {
       synchronized (chatRoomCache)
       {
           ChatRoomYahooImpl newChatRoom 
               = new ChatRoomYahooImpl(yahooConference, yahooProvider);

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
   public List<ChatRoom> getCurrentlyJoinedChatRooms()
   {
       synchronized (chatRoomCache)
       {
           List<ChatRoom> joinedRooms 
               = new LinkedList<ChatRoom>(this.chatRoomCache.values());

           Iterator<ChatRoom> joinedRoomsIter = joinedRooms.iterator();

           while (joinedRoomsIter.hasNext())
           {
               if (!joinedRoomsIter.next().isJoined())
                   joinedRoomsIter.remove();
           }

           return joinedRooms;
       }
   }

   /**
    * Returns a list of the names of all chat rooms that <tt>contact</tt> is
    * currently a member of.
    * 
    * @param chatRoomMember
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
   public List<String> getCurrentlyJoinedChatRooms(ChatRoomMember chatRoomMember)
           throws OperationFailedException, OperationNotSupportedException
   {
       synchronized (chatRoomCache)
       {
           List<String> joinedRooms 
               = new LinkedList<String>();

           Iterator<ChatRoom> joinedRoomsIter = new LinkedList<ChatRoom>(
                           this.chatRoomCache.values()).iterator();

           while (joinedRoomsIter.hasNext())
           {
               ChatRoom room = joinedRoomsIter.next();
               if (room.isJoined())
                   joinedRooms.add(room.getName());
           }

           return joinedRooms;
       }
   }

   public List<String> getExistingChatRooms() throws OperationFailedException,
           OperationNotSupportedException
   {
       LinkedList<String> list = new LinkedList<String>();

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
       ChatRoomInvitationYahooImpl invitation
           = new ChatRoomInvitationYahooImpl(
                   targetChatRoom,
                   inviter,
                   reason,
                   password);

       fireInvitationReceived(invitation);
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
                       OperationSetBasicInstantMessaging.HTML_MIME_TYPE,
                       OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING, 
                       null);

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
                           System.currentTimeMillis(),
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
