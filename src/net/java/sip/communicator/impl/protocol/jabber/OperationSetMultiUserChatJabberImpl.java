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
     * A list of the rooms that are currently open by this account. (We have
     * not necessarily joined these rooms).
     */
    private Vector currentlyOpenChatRooms = new Vector();

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

//        throw new RuntimeException("implement invitation listeners");
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
        if(MultiUserChat.isServiceEnabled(
            getXmppConnection()
            , jabberProvider.getAccountID().getUserID()))
        {
            throw new OperationNotSupportedException(
                "Impossible to create chat rooms on server "
                + jabberProvider.getAccountID().getService()
                + " for user "
                + jabberProvider.getAccountID().getUserID());
        }

        //retrieve room info in order to determine whether the room actually
        //exists
//        RoomInfo roomInfo = null;
//        try
//        {
//            roomInfo
//                = MultiUserChat.getRoomInfo(getXmppConnection(), roomName);
//            logger.error("RoomInfo=" + roomInfo.toString());
//        }
//        catch (XMPPException ex)
//        {
//            logger.error("Failed to retrieve room info.", ex);
//            if(ex.getXMPPError().getCode() == 404)
//                logger.warn("niama iaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
//            throw new OperationFailedException("Failed to retrieve room info."
//                      , OperationFailedException.GENERAL_ERROR
//                      , ex);
//        }
//
        MultiUserChat muc
            = new MultiUserChat(getXmppConnection(), roomName);

        try
        {
            logger.error("MUCI=" + muc.toString());
            logger.debug("muc.getRoom()=" + muc.getRoom());
            logger.debug("muc.getConfigurationForm();=" +
                         muc.getConfigurationForm());
            logger.debug("muc.getSubject();=" + muc.getSubject());
            logger.debug("muc.getOwners();=" + muc.getOwners());
//            muc.is
        }
        catch (XMPPException ex2)
        {
            ex2.printStackTrace(System.out);
            throw new OperationFailedException("", 0, ex2);
        }


        try
        {
            try
            {
                muc.create("kiki");
            }
            catch (XMPPException ex1)
            {
            }
            muc.join("kiki");
            muc.sendMessage("created a room");
        }
        catch (XMPPException ex)
        {
            logger.error("Failed to create or join a chat room", ex);
            throw new OperationFailedException(
                "Failed to create or join a chat room"
                , OperationFailedException.FORBIDDEN);
        }

        ChatRoomJabberImpl chatRoom = new ChatRoomJabberImpl(muc);
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
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetMultiUserChat
     *   method
     */
    public ChatRoom findRoom(String roomName)
    {
        return null;
    }

    /**
     * Returns a list of the chat rooms that we have joined and are currently
     * active in.
     *
     * @return a <tt>List</tt> of the rooms where the user has joined using
     *   a given connection.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetMultiUserChat
     *   method
     */
    public List getCurrentlyJoinedChatRooms()
    {
        return null;
    }

    /**
     * Returns a list of the chat rooms that <tt>contact</tt> has joined and
     * is currently active in.
     *
     * @param contact the contact whose current ChatRooms we will be
     *   querying.
     * @return a list of the chat rooms that <tt>contact</tt> has joined and
     *   is currently active in.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetMultiUserChat
     *   method
     */
    public List getCurrentlyJoinedChatRooms(Contact contact)
    {
        return null;
    }

    /**
     * Returns the <tt>List</tt> of <tt>ChatRoom</tt>s currently available on
     * the server that this protocol provider is connected to.
     *
     * @return a <tt>java.util.List</tt> of <tt>ChatRoom</tt>s that are
     *   currently available on the server that this protocol provider is
     *   connected to.
     * @throws OperationFailedException if we faile retrieving this list
     *   from the server.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetMultiUserChat
     *   method
     */
    public List getExistingChatRooms()
        throws OperationFailedException
    {
        return null;
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
}
