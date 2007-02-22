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
import org.jivesoftware.smackx.muc.MultiUserChat;

/**
 * A jabber implementation of the multi user chat operation set.
 *
 * @author Emil Ivov
 */
public class OperationSetMultiUserChatJabberImpl
    implements OperationSetMultiUserChat
{

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
     * Instantiates the user operation set with a currently valid instance of
     * the jabber protocol provider.
     * @param jImpl a currently valid instance of
     * ProtocolProviderServiceJabberImpl.
     */
    OperationSetMultiUserChatJabberImpl(
                        ProtocolProviderServiceJabberImpl jabberProvider)
    {
        this.jabberProvider = jabberProvider;
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
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to.
     *
     * @param roomName the name of the <tt>ChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     *   created.
     * @throws OperationFailedException if the room couldn't be created for
     *   some reason (e.g. room already exists; user already joined to an
     *   existant room or user has no permissions to create a chat room).
     * @return ChatRoom the chat room that we've just created.
     */
    public ChatRoom createChatRoom(String roomName, Hashtable roomProperties)
        throws OperationFailedException
    {
//        return MultiUserChat.c;
        return null;
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
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation events.
     *
     * @param listener the invitation listener to remove.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetMultiUserChat
     *   method
     */
    public void removeInvitationListener(InvitationListener listener)
    {
    }

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation rejection events.
     *
     * @param listener the invitation listener to remove.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetMultiUserChat
     *   method
     */
    public void removeInvitationRejectionListener(InvitationRejectionListener
                                                  listener)
    {
    }

    public static void main(String[] args)
    {
    }
}
