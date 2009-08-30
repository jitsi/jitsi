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
import net.kano.joscar.*;
import net.kano.joustsim.oscar.oscar.service.chatrooms.*;

/**
 * A ICQ implementation of the multi user chat operation set.
 *
 * @author Rupert Burchardi
 */
public class OperationSetMultiUserChatIcqImpl
    extends AbstractOperationSetMultiUserChat
    implements SubscriptionListener
{
   private static final Logger logger = Logger
           .getLogger(OperationSetMultiUserChatIcqImpl.class);

   /**
    * The currently valid ICQ protocol provider service implementation.
    */
   private ProtocolProviderServiceIcqImpl icqProvider = null;

   /**
    * A list of the rooms that are currently open by this account. Note that
    * we have not necessarily joined these rooms, we might have simply been
    * searching through them.
    */
   private Hashtable chatRoomCache = new Hashtable();

   /**
   * The registration listener that would get notified when the underlying
   * ICQ provider gets registered.
   */
   private RegistrationStateListener providerRegListener
       = new RegistrationStateListener();

   /**
   * A reference to the persistent presence operation set that we use
   * to match incoming messages to <tt>Contact</tt>s and vice versa.
   */
   protected OperationSetPersistentPresenceIcqImpl opSetPersPresence = null;

   /**
    * Hash table that contains all invitations, this is needed if the user wants
    * to reject an invitation.
    */
   private Hashtable invitations = new Hashtable();
   
   /**
    * Default Invitation message.
    */
   private final String DEFAULT_INVITATION = "Please join my chat room!";

   /**
   * Instantiates the user operation set with a currently valid instance of
   * the Icq protocol provider.
   * @param icqProvider a currently valid instance of
   * ProtocolProviderServiceIcqImpl.
   */
   OperationSetMultiUserChatIcqImpl(ProtocolProviderServiceIcqImpl icqProvider)
   {
       this.icqProvider = icqProvider;
       icqProvider.addRegistrationStateChangeListener(providerRegListener);

       OperationSetPersistentPresence presenceOpSet
           = (OperationSetPersistentPresence) icqProvider
               .getOperationSet(OperationSetPersistentPresence.class);

       presenceOpSet.addSubscriptionListener(this);
   }

   /**
   * Returns a reference to a chatRoom by an given chat invitation. This method
   * is called, when the user received a chat invitation. The chat room will be
   * created and the chatInvitation will be saved in the created chat room. This
   * ensures to get the chat room session for each chat room.
   * 
   * @param chatInvitation The Chat invitation the user received
   * @return A chat room based on the chat invitation
   * @throws OperationFailedException if an error occurs while trying to
   * discover the room on the server.
   * @throws OperationNotSupportedException if the server does not support
   * multi user chat
   */
   public ChatRoom findRoom(ChatInvitation chatInvitation)
           throws OperationFailedException, OperationNotSupportedException
   {
       ChatRoom chatRoom = (ChatRoom) chatRoomCache.get(chatInvitation
               .getRoomName());

       if (chatRoom == null)
       {
           chatRoom = createLocalChatRoomInstance(chatInvitation);
       }

       return chatRoom;
   }

   /**
    * Creates a <tt>ChatRoom</tt> from the specified smack
    * <tt>MultiUserChat</tt>.
    *
    * @param chatInvitation The chat invitation we received from the
    * chatRoomManager
    *
    * @return ChatRoom the chat room that we've just created.
    */

   private ChatRoom createLocalChatRoomInstance(ChatInvitation chatInvitation)
   {
       synchronized (chatRoomCache)
       {
           ChatRoom newChatRoom = new ChatRoomIcqImpl(chatInvitation,
                   icqProvider);

           chatRoomCache.put(chatInvitation.getRoomName(), newChatRoom);
           return newChatRoom;
       }
   }

   /**
    * Creates a room with the named <tt>roomName</tt> and according to the
    * specified <tt>roomProperties</tt> on the server that this protocol
    * provider is currently connected to.
    *
    * @param roomName the name of the <tt>ChatRoom</tt> to create.
    * @param roomProperties properties specifying how the room should be
    *   created. Contains list of invitees and the invitation message.
    *
    * @throws OperationFailedException if the room couldn't be created for
    * some reason (e.g. room already exists; user already joined to an
    * existent room or user has no permissions to create a chat room).
    * @throws OperationNotSupportedException if chat room creation is not
    * supported by this server
    *
    * @return ChatRoom the chat room that we've just created.
    */
   public ChatRoom createChatRoom( String roomName,
                                   Map<String, Object> roomProperties)
           throws  OperationFailedException,
                   OperationNotSupportedException
   {
       ChatRoom chatRoom = null;

       ChatRoomManager chatRoomManager = icqProvider.getAimConnection()
               .getChatRoomManager();

       ChatRoomSession chatRoomSession = chatRoomManager.joinRoom(roomName);

       if(chatRoomSession != null)
       {
           chatRoom = new ChatRoomIcqImpl(  roomName,
                                            chatRoomSession,
                                            icqProvider);
       }

       return chatRoom;
   }

   /**
    * Returns a reference to a chatRoom named <tt>roomName</tt> or null if
    * no such room exists.
    *
    * @param roomName the name of the <tt>ChatRoom</tt> that we're looking
    *   for.
    * @return the <tt>ChatRoom</tt> named <tt>roomName</tt> or null if no
    *   such room exists on the server that this provider is currently
    *   connected to.
    * @throws OperationFailedException if an error occurs while trying to
    * discover the room on the server.
    * @throws OperationNotSupportedException if the server does not support
    * multi user chat
    */

   public ChatRoom findRoom(String roomName) throws OperationFailedException,
           OperationNotSupportedException
   {
       ChatRoom room = (ChatRoom) chatRoomCache.get(roomName);

       return room;
   }

   /**
    * Returns a list of the chat rooms that we have joined and are currently
    * active in.
    *
    * @return a <tt>List</tt> of the rooms where the user has joined using
    *   a given connection.
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
    * currently a  member of.
    *
    * @param contact the contact whose current ChatRooms we will be
    *   querying.
    * @return a list of <tt>String</tt> indicating the names of  the chat rooms
    * that <tt>contact</tt> has joined and is currently active in.
    *
    * @throws OperationFailedException if an error occurs while trying to
    * discover the room on the server.
    * @throws OperationNotSupportedException if the server does not support
    * multi user chat
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

   /**
    * Returns the <tt>List</tt> of <tt>String</tt>s indicating chat rooms
    * currently available on the server that this protocol provider is
    * connected to. Note: There are no more system chat rooms in the ICQ
    * network at this moment.
    *
    * @return a <tt>java.util.List</tt> of the name <tt>String</tt>s for chat
    * rooms that are currently available on the server that this protocol
    * provider is connected to.
    *
    * @throws OperationFailedException if we failed retrieving this list from
    * the server.
    * @throws OperationNotSupportedException if the server does not support
    * multi user chat
    */
   public List getExistingChatRooms() throws OperationFailedException,
           OperationNotSupportedException
   {
       return new LinkedList();
   }

   /**
    * Returns true if <tt>contact</tt> supports multi user chat sessions.
    *
    * @param contact reference to the contact whose support for chat rooms
    *   we are currently querying.
    * @return a boolean indicating whether <tt>contact</tt> supports
    *   chat rooms.
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
   * @param invitation the connection to use for sending the rejection.
   * @param rejectReason the reason to reject the given invitation
   */
   public void rejectInvitation(ChatRoomInvitation invitation,
           String rejectReason)
   {
       ChatInvitation inv = (ChatInvitation) invitations.get(invitation
               .getTargetChatRoom());

       if (inv != null)
       {   //send the rejection
           inv.reject();
       }
       //remove the invitation
       invitations.remove(invitation.getTargetChatRoom());
   }

   /**
   * Delivers a <tt>ChatRoomInvitationReceivedEvent</tt> to all
   * registered <tt>ChatRoomInvitationListener</tt>s.
   * 
   * @param targetChatRoom the room that invitation refers to
   * @param inviter the inviter that sent the invitation
   * @param reason the reason why the inviter sent the invitation
   * @param password the password to use when joining the room 
   */
   public void fireInvitationEvent(ChatRoom targetChatRoom, String inviter,
           String reason, byte[] password)
   {
       ChatRoomInvitationIcqImpl invitation
           = new ChatRoomInvitationIcqImpl(
                   targetChatRoom,
                   inviter,
                   reason,
                   password);

       fireInvitationReceived(invitation);
   }

   /**
    * Our listener that will tell us when we're registered to icq and joust
    * sim is ready to accept us as a listener.
    */
   private class RegistrationStateListener implements
           RegistrationStateChangeListener
   {
       /**
        * The method is called by a ProtocolProvider implementation whenever
        * a change in the registration state of the corresponding provider had
        * occurred.
        * @param evt ProviderStatusChangeEvent the event describing the status
        * change.
        */
       public void registrationStateChanged(RegistrationStateChangeEvent evt)
       {
           logger.debug("The ICQ provider changed state from: "
                   + evt.getOldState() + " to: " + evt.getNewState());
           if (evt.getNewState() == RegistrationState.REGISTERED)
           {
               String customMessageEncoding = null;
               if ((customMessageEncoding = System
                       .getProperty("icq.custom.message.charset")) != null)
                   OscarTools.setDefaultCharset(customMessageEncoding);

               opSetPersPresence =
                    (OperationSetPersistentPresenceIcqImpl) icqProvider
                        .getOperationSet(OperationSetPersistentPresence.class);

               //add ChatRoomMangagerListener
               icqProvider.getAimConnection().getChatRoomManager()
                       .addListener(new ChatRoomManagerListenerImpl());
           }
       }
   }

   /**
    * Our listener for chat room invitations. 
    *
    */
   private class ChatRoomManagerListenerImpl implements
           ChatRoomManagerListener
   {
       public void handleInvitation(ChatRoomManager chatRoomManager,
               ChatInvitation chatInvitation)
       {
           logger
                   .debug("Invitation received: "
                           + chatInvitation.getRoomName());
           try
           {
               ChatRoom chatRoom = findRoom(chatInvitation);
               // save chatInvitation, for a possible rejection
               invitations.put(chatRoom, chatInvitation);

               fireInvitationEvent(chatRoom, chatInvitation.getScreenname()
                       .toString(), chatInvitation.getMessage(), null);

           }
           catch (OperationNotSupportedException onse)
           {
               logger.debug("Failed to handle ChatInvitation: " + onse);
           }
           catch (OperationFailedException ofe)
           {
               logger.debug("Failed to handle ChatInvitation: " + ofe);
           }
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
        Enumeration<ChatRoomIcqImpl> chatRooms = chatRoomCache.elements();

        while (chatRooms.hasMoreElements())
        {
            ChatRoomIcqImpl chatRoom = chatRooms.nextElement();

            ChatRoomMemberIcqImpl member
                = chatRoom.findMemberForNickName(contact.getAddress());

            if (member != null)
            {
                member.setContact(contact);
                member.setAvatar(contact.getImage());
            }
        }
    }
}
