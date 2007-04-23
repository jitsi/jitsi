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
import org.jivesoftware.smack.*;
import net.java.sip.communicator.util.*;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.muc.HostedRoom;

/**
 * A jabber implementation of the multi user chat operation set.
 *
 * @author Emil Ivov
 */
public class OperationSetMultiUserChatJabberImpl
    implements OperationSetMultiUserChat
{
    private static final Logger logger
        = Logger.getLogger(OperationSetMultiUserChatJabberImpl.class);

    /**
     * The currently valid jabber protocol provider service implementation.
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
     * A list of the rooms that are currently open by this account. Note that
     * we have not necessarily joined these rooms, we might have simply been
     * searching through them.
     */
    private Hashtable chatRoomCache = new Hashtable();

    /**
     * Instantiates the user operation set with a currently valid instance of
     * the jabber protocol provider.
     * @param jabberProvider a currently valid instance of
     * ProtocolProviderServiceJabberImpl.
     */
    OperationSetMultiUserChatJabberImpl(
                        ProtocolProviderServiceJabberImpl jabberProvider)
    {
        this.jabberProvider = jabberProvider;

        /** @todo implement invitation listeners */
//        MultiUserChat.addInvitationListener(jabberProvider.getConnection()
//                                            , this);
    }

    /**
     * Adds a listener to invitation notifications.
     *
     * @param listener an invitation listener.
     */
    public void addInvitationListener(InvitationListener listener)
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
    public void removeInvitationListener(InvitationListener listener)
    {
        synchronized(invitationListeners)
        {
            invitationListeners.remove(listener);
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
                                        InvitationRejectionListener listener)
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
    public void removeInvitationRejectionListener(InvitationRejectionListener
                                                  listener)
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
     *   some reason (e.g. room already exists; user already joined to an
     *   existant room or user has no permissions to create a chat room).
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
        //if not, then check whether it's on the server and if yes - create it
        //locally.
        else
        {
            try
            {
                MultiUserChat.getRoomInfo( getXmppConnection(), roomName );

                //if we get here then we didn't get an error while retrieving
                //room info and we can assume it exists
                return createLocalChatRoomInstance(
                    new MultiUserChat(getXmppConnection(),roomName));
            }
            catch (XMPPException ex)
            {
                //if this is a 404 then it simply means the room doesn't exist
                //on the server, so we return null and don't rethrow the
                //exception
                if(ex.getXMPPError() != null
                   && ex.getXMPPError().getCode() == 404)
                {
                    return null;
                }
                logger.error(
                    "Failed to retrieve room info for "
                    + roomName
                    +". Error was: "
                    + ex.getMessage()
                    , ex);

                //otherwise we rethrow the exception cause it means a real error
                throw new OperationFailedException(
                    "Failed to retrieve room info for "
                    + roomName
                    +". Error was: "
                    + ex.getMessage()
                    , OperationFailedException.GENERAL_ERROR);
            }
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
                = new LinkedList(this.chatRoomCache.entrySet());

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
     * @return a list of <tt>String</tt> indicating tha names of  the chat rooms
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
        throws OperationFailedException, OperationNotSupportedException
    {
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
        return false;
    }

    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param invitation the connection to use for sending the rejection.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetMultiUserChat
     *   method
     */
    public void rejectInvitation(ChatRoomInvitation invitation)
    {
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
}
