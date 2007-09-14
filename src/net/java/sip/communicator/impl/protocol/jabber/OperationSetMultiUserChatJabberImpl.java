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

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.*;

/**
 * A jabber implementation of the multi user chat operation set.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class OperationSetMultiUserChatJabberImpl
    implements  OperationSetMultiUserChat
{
    private static final Logger logger
        = Logger.getLogger(OperationSetMultiUserChatJabberImpl.class);

    /**
     * The currently valid Jabber protocol provider service implementation.
     */
    private ProtocolProviderServiceJabberImpl jabberProvider = null;

    /**
     * A list of listeners subscribed for invitations multi user chat events.
     */
    private Vector invitationListeners = new Vector();

    /**
     * A list of listeners subscribed for events indicating rejection of a
     * multi user chat invitation sent by us.
     */
    private Vector invitationRejectionListeners = new Vector();

    /**
     * Listeners that will be notified of changes in our status in the
     * room such as us being kicked, banned, or granted admin permissions.
     */
    private Vector presenceListeners = new Vector();

    /**
     * A list of the rooms that are currently open by this account. Note that
     * we have not necessarily joined these rooms, we might have simply been
     * searching through them.
     */
    private Hashtable chatRoomCache = new Hashtable();

    /**
     * The registration listener that would get notified when the underlying
     * Jabber provider gets registered.
     */
    private RegistrationStateListener providerRegListener
        = new RegistrationStateListener();

    /**
     * Instantiates the user operation set with a currently valid instance of
     * the Jabber protocol provider.
     * @param jabberProvider a currently valid instance of
     * ProtocolProviderServiceJabberImpl.
     */
    OperationSetMultiUserChatJabberImpl(
                        ProtocolProviderServiceJabberImpl jabberProvider)
    {
        this.jabberProvider = jabberProvider;
        
        jabberProvider.addRegistrationStateChangeListener(providerRegListener);
    }

    /**
     * Adds a listener to invitation notifications.
     *
     * @param listener an invitation listener.
     */
    public void addInvitationListener(ChatRoomInvitationListener listener)
    {
        synchronized(invitationListeners)
        {
            if (!invitationListeners.contains(listener))
                invitationListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation events.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationListener(ChatRoomInvitationListener listener)
    {
        synchronized(invitationListeners)
        {
            invitationListeners.remove(listener);
        }
    }
    
    /**
     * Adds a listener that will be notified of changes in our status in a chat
     * room such as us being kicked, banned or dropped.
     *
     * @param listener the <tt>LocalUserChatRoomPresenceListener</tt>.
     */
    public void addPresenceListener(LocalUserChatRoomPresenceListener listener)
    {
        synchronized(presenceListeners)
        {
            if (!presenceListeners.contains(listener))
                presenceListeners.add(listener);
        }
    }

    /**
     * Removes a listener that was being notified of changes in our status in
     * a room such as us being kicked, banned or dropped.
     *
     * @param listener the <tt>LocalUserChatRoomPresenceListener</tt>.
     */
    public void removePresenceListener(
        LocalUserChatRoomPresenceListener listener)
    {
        synchronized(presenceListeners)
        {
            presenceListeners.remove(listener);
        }
    }

    /**
     * Subscribes <tt>listener</tt> so that it would receive events indicating
     * rejection of a multi user chat invitation that we've sent earlier.
     *
     * @param listener the listener that we'll subscribe for invitation
     * rejection events.
     */
    public void addInvitationRejectionListener(
                                ChatRoomInvitationRejectionListener listener)
    {
        synchronized(invitationRejectionListeners)
        {
            if (!invitationRejectionListeners.contains(listener))
                invitationRejectionListeners.add(listener);
        }
    }

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation rejection events.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationRejectionListener(
                                ChatRoomInvitationRejectionListener listener)
    {
        synchronized(invitationRejectionListeners)
        {
            invitationRejectionListeners.remove(listener);
        }
    }

    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to.
     *
     * @param roomName the name of the <tt>ChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     *   created.
     *
     * @throws OperationFailedException if the room couldn't be created for
     * some reason (e.g. room already exists; user already joined to an
     * existent room or user has no permissions to create a chat room).
     * @throws OperationNotSupportedException if chat room creation is not
     * supported by this server
     *
     * @return ChatRoom the chat room that we've just created.
     */
    public ChatRoom createChatRoom(String roomName, Hashtable roomProperties)
        throws OperationFailedException, OperationNotSupportedException
    {
        //first make sure we are connected and the server supports multichat
        assertSupportedAndConnected();

        //make sure we have the complete room name.
        roomName = getCanonicalRoomName(roomName);

        //check if the room hasn't been created already
        ChatRoom room = findRoom(roomName);

        if(room != null)
            throw new OperationFailedException(
                            "There is already a room with"
                            , OperationFailedException.IDENTIFICATION_CONFLICT);

        MultiUserChat muc = new MultiUserChat(getXmppConnection(), roomName);

        try
        {
            muc.create(getXmppConnection().getUser());
        }
        catch (XMPPException ex)
        {
            logger.error("Failed to create chat room.", ex);
            throw new OperationFailedException("Failed to create chat room"
                                               , ex.getXMPPError().getCode()
                                               , ex.getCause());
        }
        return createLocalChatRoomInstance(muc);
    }

    /**
     * Creates a <tt>ChatRoom</tt> from the specified smack
     * <tt>MultiUserChat</tt>.
     *
     * @param muc the smack MultiUserChat instance that we're going to wrap our
     * chat room around.
     *
     * @return ChatRoom the chat room that we've just created.
     */
    private ChatRoom createLocalChatRoomInstance(MultiUserChat muc)
    {
        synchronized(chatRoomCache)
        {            
            ChatRoomJabberImpl chatRoom
                = new ChatRoomJabberImpl(muc, jabberProvider);
            cacheChatRoom(chatRoom);

            // Add the contained in this class SmackInvitationRejectionListener
            // which will dispatch all rejection events to the
            // ChatRoomInvitationRejectionListener.
            muc.addInvitationRejectionListener(
                new SmackInvitationRejectionListener(chatRoom));
            
            return chatRoom;
        }
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
    public ChatRoom findRoom(String roomName)
        throws OperationFailedException, OperationNotSupportedException
    {
        //make sure we are connected and multichat is supported.
        assertSupportedAndConnected();

        //first check whether we have already initialized the room.
        ChatRoom room = (ChatRoom)chatRoomCache.get(roomName);

        //if yes - we return it
        if(room != null)
        {
            return room;
        }
        //if not, we create it.
        else
        {
            return createLocalChatRoomInstance(
                new MultiUserChat(getXmppConnection(), roomName));
        }
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
        synchronized(chatRoomCache)
        {
            List joinedRooms
                = new LinkedList(this.chatRoomCache.values());

            Iterator joinedRoomsIter = joinedRooms.iterator();

            while (joinedRoomsIter.hasNext())
            {
                if ( !( (ChatRoom) joinedRoomsIter.next()).isJoined())
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
    public List getCurrentlyJoinedChatRooms(Contact contact)
        throws OperationFailedException, OperationNotSupportedException
    {
        assertSupportedAndConnected();

        Iterator joinedRoomsIter
            = MultiUserChat.getJoinedRooms( getXmppConnection()
                                            , contact.getAddress());

        List joinedRoomsForContact = new LinkedList();

        while ( joinedRoomsIter.hasNext() )
        {
            MultiUserChat muc = (MultiUserChat)joinedRoomsIter.next();
            joinedRoomsForContact.add(muc.getRoom());
        }

        return joinedRoomsForContact;
    }

    /**
     * Returns the <tt>List</tt> of <tt>String</tt>s indicating chat rooms
     * currently available on the server that this protocol provider is
     * connected to.
     *
     * @return a <tt>java.util.List</tt> of the name <tt>String</tt>s for chat
     * rooms that are currently available on the server that this protocol
     * provider is connected to.
     *
     * @throws OperationFailedException if we faile retrieving this list from
     * the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi user chat
     */
    public List getExistingChatRooms()
        throws  OperationFailedException,
                OperationNotSupportedException
    {
        assertSupportedAndConnected();
        
        List list = new LinkedList();
        
        //first retrieve all conference service names available on this server
        Iterator serviceNames = null;
        try
        {
            serviceNames = MultiUserChat
                .getServiceNames(getXmppConnection()).iterator();
        }
        catch (XMPPException ex)
        {
            throw new OperationFailedException(
                "Failed to retrieve Jabber conference service names"
                , OperationFailedException.GENERAL_ERROR
                , ex);
        }

        //now retrieve all chat rooms currently available for every service name
        while(serviceNames.hasNext())
        {
            String serviceName = (String)serviceNames.next();
            List roomsOnThisService = new LinkedList();

            try
            {
                roomsOnThisService
                    .addAll(MultiUserChat.getHostedRooms(getXmppConnection()
                                                         , serviceName));
            }
            catch (XMPPException ex)
            {
                logger.error("Failed to retrieve rooms for serviceName="
                             + serviceName);
                //continue bravely with other service names
                continue;
            }

            //now go through all rooms available on this service
            Iterator serviceRoomsIter = roomsOnThisService.iterator();

            while(serviceRoomsIter.hasNext())
            {
                HostedRoom hostedRoom = (HostedRoom)serviceRoomsIter.next();

                //add the room name to the list of names we are returning
                list.add(hostedRoom.getJid());
            }
        }

        /** @todo maybe we should add a check here and fail if retrieving chat
         * rooms failed for all service names*/

        return list;
    }

    /**
     * Returns true if <tt>contact</tt> supports multi user chat sessions.
     *
     * @param contact reference to the contact whose support for chat rooms
     *   we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports
     *   chatrooms.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetMultiUserChat
     *   method
     */
    public boolean isMultiChatSupportedByContact(Contact contact)
    {
        if(contact.getProtocolProvider()
            .getOperationSet(OperationSetMultiUserChat.class) != null)
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
        MultiUserChat.decline(jabberProvider.getConnection(),
            invitation.getTargetChatRoom().getName(),
            invitation.getInviter(),
            rejectReason);
    }

    /**
     * Almost all <tt>MultiUserChat</tt> methods require an xmpp connection
     * param so I added this method only for the sake of utility.
     *
     * @return the XMPPConnection currently in use by the jabber provider or
     * null if jabber provider has yet to be initialized.
     */
    private XMPPConnection getXmppConnection()
    {
        return (jabberProvider == null)
            ? null
            :jabberProvider.getConnection();
    }

    /**
     * Makes sure that we are properly connected and that the server supports
     * multi user chats.
     *
     * @throws OperationFailedException if the provider is not registered or
     * the xmpp connection not connected.
     * @throws OperationNotSupportedException if the service is not supported
     * by the server.
     */
    private void assertSupportedAndConnected()
        throws OperationFailedException, OperationNotSupportedException
    {
        //throw an exception if the provider is not registered or the xmpp
        //connection not connected.
        if( !jabberProvider.isRegistered()
            || !getXmppConnection().isConnected())
        {
            throw new OperationFailedException(
                "Provider not connected to jabber server"
                , OperationFailedException.NETWORK_FAILURE);
        }

        //throw an exception if the service is not supported by the server.
        if(MultiUserChat.isServiceEnabled(
            getXmppConnection()
            , jabberProvider.getAccountID().getUserID()))
        {
            throw new OperationNotSupportedException(
                "Chat rooms not supported on server "
                + jabberProvider.getAccountID().getService()
                + " for user "
                + jabberProvider.getAccountID().getUserID());
        }

    }

    /**
     * In case <tt>roomName</tt> does not represent a complete room id, the
     * method returns a canonincal chat room name in the following form:
     * roomName@muc-servicename.jabserver.com. In case <tt>roomName</tt> is
     * already a canonical room name, the method simply returns it without
     * changing it.
     *
     * @param roomName the name of the room that we'd like to "canonize".
     *
     * @return the canonincal name of the room (which might be equal to
     * roomName in case it was already in a canonical format).
     *
     * @throws OperationFailedException if we fail retrieving the conference
     * service name
     */
    private String getCanonicalRoomName(String roomName)
        throws OperationFailedException
    {

        if (roomName.indexOf('@') > 0)
            return roomName;

        Iterator serviceNamesIter = null;
        try
        {
            serviceNamesIter
                = MultiUserChat.getServiceNames(getXmppConnection()).iterator();
        }
        catch (XMPPException ex)
        {
            logger.error("Failed to retrieve conference service name for user: "
                + jabberProvider.getAccountID().getUserID()
                + " on server: "
                + jabberProvider.getAccountID().getService()
                , ex);
            throw new OperationFailedException(
                "Failed to retrieve conference service name for user: "
                + jabberProvider.getAccountID().getUserID()
                + " on server: "
                + jabberProvider.getAccountID().getService()
                , OperationFailedException.GENERAL_ERROR
                , ex);

        }

        while( serviceNamesIter.hasNext() )
        {
            return roomName
                + "@"
                + (String)serviceNamesIter.next()
                + jabberProvider.getAccountID().getService();
        }

        //hmmmm strange.. no service name returned. we should probably throw an
        //exception
        throw new OperationFailedException(
            "Failed to retrieve MultiUserChat service names."
            , OperationFailedException.GENERAL_ERROR);
    }

    /**
     * Adds <tt>chatRoom</tt> to the cache of chat rooms that this operation
     * set is handling.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to cache.
     */
    private void cacheChatRoom(ChatRoom chatRoom)
    {
        this.chatRoomCache.put(chatRoom.getName(), chatRoom);
    }

    /**
     * Returns a reference to the chat room named <tt>chatRoomName</tt> or
     * null if the room hasn't been cached yet.
     *
     * @param chatRoomName the name of the room we're looking for.
     *
     * @return the <tt>ChatRoomJabberImpl</tt> instance that has been cached
     * for <tt>chatRoomName</tt> or null if no such room has been cached so far.
     */
    public ChatRoomJabberImpl getChatRoom(String chatRoomName)
    {
        return (ChatRoomJabberImpl)this.chatRoomCache.get(chatRoomName);
    }

    /**
     * Returns the list of currently joined chat rooms.
     */
    public List getCurrentlyJoinedChatRooms(ChatRoomMember chatRoomMember)
        throws OperationFailedException, OperationNotSupportedException
    {
        return null;
    }
    
    /**
     * Delivers a <tt>LocalUserChatRoomPresenceChangeEvent</tt> to all
     * registered <tt>LocalUserChatRoomPresenceListener</tt>s.
     * 
     * @param chatRoom the <tt>ChatRoom</tt> which has been joined, left, etc.
     * @param eventType the type of this event; one of LOCAL_USER_JOINED,
     * LOCAL_USER_LEFT, etc.
     * @param reason the reason
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
     * Delivers a <tt>ChatRoomInvitationReceivedEvent</tt> to all
     * registered <tt>ChatRoomInvitationListener</tt>s.
     * 
     * @param targetChatRoom the room that invitation refers to
     * @param inviter the inviter that sent the invitation
     * @param reason the reason why the inviter sent the invitation
     * @param password the password to use when joining the room 
     */
    public void fireInvitationEvent(
        ChatRoom targetChatRoom,
        String inviter,
        String reason,
        byte[] password)
    {
        ChatRoomInvitationJabberImpl invitation
            = new ChatRoomInvitationJabberImpl( targetChatRoom,
                                                inviter,
                                                reason,
                                                password);
        
        ChatRoomInvitationReceivedEvent evt
            = new ChatRoomInvitationReceivedEvent(this, invitation,
                new Date(System.currentTimeMillis()));
        
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
     * Delivers a <tt>ChatRoomInvitationRejectedEvent</tt> to all
     * registered <tt>ChatRoomInvitationRejectionListener</tt>s.
     * 
     * @param sourceChatRoom the room that invitation refers to
     * @param invitee the name of the invitee that rejected the invitation
     * @param reason the reason of the rejection
     */
    public void fireInvitationRejectedEvent(ChatRoom sourceChatRoom,
                                            String invitee,
                                            String reason)
    {
        ChatRoomInvitationRejectedEvent evt
            = new ChatRoomInvitationRejectedEvent(
                this, sourceChatRoom, invitee, reason,
                new Date(System.currentTimeMillis()));
        
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
     * A listener that is fired anytime an invitation to join a MUC room is
     * received.
     */
    private class SmackInvitationListener
        implements InvitationListener
    {
        /**
         * Called when the an invitation to join a MUC room is received.<p>
         * 
         * If the room is password-protected, the invitee will receive a
         * password to use to join the room. If the room is members-only, the
         * the invitee may be added to the member list.
         * 
         * @param conn the XMPPConnection that received the invitation.
         * @param room the room that invitation refers to.
         * @param inviter the inviter that sent the invitation.
         * (e.g. crone1@shakespeare.lit).
         * @param reason the reason why the inviter sent the invitation.
         * @param password the password to use when joining the room.
         * @param message the message used by the inviter to send the invitation.
         */
        public void invitationReceived(XMPPConnection conn,
            String room, String inviter, String reason,
            String password, Message message)
        {
            ChatRoomJabberImpl chatRoom;
            try
            {
                chatRoom = (ChatRoomJabberImpl) findRoom(room);
                
                fireInvitationEvent(
                    chatRoom, inviter, reason, password.getBytes());
            }
            catch (OperationFailedException e)
            {
                logger.error("Failed to find room with name: " + room, e);
            }
            catch (OperationNotSupportedException e)
            {
                logger.error("Failed to find room with name: " + room, e);
            }
        }
    }
    
    /**
     * A listener that is fired anytime an invitee declines or rejects an
     * invitation.
     */
    private class SmackInvitationRejectionListener
        implements InvitationRejectionListener
    {
        private ChatRoom chatRoom;
        
        /**
         * Creates an instance of <tt>SmackInvitationRejectionListener</tt> and
         * passes to it the chat room for which it will listen for rejection
         * events.
         * 
         * @param chatRoom
         */
        public SmackInvitationRejectionListener(ChatRoom chatRoom)
        {
            this.chatRoom = chatRoom;
        }
        
        /**
         * Called when the invitee declines the invitation.
         * 
         * @param invitee the invitee that declined the invitation.
         * (e.g. hecate@shakespeare.lit).
         * @param reason the reason why the invitee declined the invitation.
         */
        public void invitationDeclined(String invitee, String reason)
        {   
            fireInvitationRejectedEvent(chatRoom, invitee, reason);
        }
    }
    
    /**
     * Our listener that will tell us when we're registered to jabber and the
     * smack MultiUserChat is ready to accept us as a listener.
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    { 
        /**
         * The method is called by a ProtocolProvider implementation whenver
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (evt.getNewState() == RegistrationState.REGISTERED)
            {
                logger.debug("adding an Invitation listener to the smack muc");
                
                MultiUserChat.addInvitationListener(
                    jabberProvider.getConnection(),
                    new SmackInvitationListener());
            }
        }
    }
}