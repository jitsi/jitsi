/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * Allows creating, configuring, joining and administrating of individual
 * text-based conference rooms.
 *
 * @author Emil Ivov
 */
public interface OperationSetMultiUserChat
{
    /**
     * Returns the <tt>List</tt> of <tt>ChatRoom</tt>s currently available on
     * the server that this protocol provider is connected to.
     *
     * @return a <tt>java.util.List</tt> of <tt>ChatRoom</tt>s that are
     * currently available on the server that this protocol provider is
     * connected to.
     *
     * @throws OperationFailedException if we faile retrieving this list from
     * the server.
     */
    public List getExistingChatRooms()
        throws OperationFailedException;

    /**
     * Returns a list of the chat rooms that we have joined and are currently
     * active in.
     *
     * @return a <tt>List</tt> of the rooms where the user has joined using a
     * given connection.
     */
    public List getCurrentlyJoinedChatRooms();

    /**
     * Returns a list of the chat rooms that <tt>contact</tt> has joined and is
     * currently active in.
     *
     * @param contact the contact whose current ChatRooms we will be querying.
     * @return a list of the chat rooms that <tt>contact</tt> has joined and is
     * currently active in.
     */
    public List getCurrentlyJoinedChatRooms(Contact contact);

    /**
     * Creates a room with the named <tt>roomName</tt> and according to the
     * specified <tt>roomProperties</tt> on the server that this protocol
     * provider is currently connected to. When the method returns the room the
     * local user will not have joined it and thus will not receive messages on
     * it until the <tt>ChatRoom.join()</tt> method is called.
     * <p>
     * @param roomName the name of the <tt>ChatRoom</tt> to create.
     * @param roomProperties properties specifying how the room should be
     * created.
     * @throws OperationFailedException if the room couldn't be created for some
     * reason (e.g. room already exists; user already joined to an existant
     * room or user has no permissions to create a chat room).
     */
    public ChatRoom createChatRoom(String roomName, Hashtable roomProperties)
        throws OperationFailedException;

    /**
     * Returns a reference to a chatRoom named <tt>roomName</tt> or null if
     * no such room exists.
     * <p>
     * @param roomName the name of the <tt>ChatRoom</tt> that we're looking for.
     * @return the <tt>ChatRoom</tt> named <tt>roomName</tt> or null if no such
     * room exists on the server that this provider is currently connected to.
     */
    public ChatRoom findRoom(String roomName);

    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param conn the connection to use for sending the rejection.
     * @param room the room that sent the original invitation.
     * @param inviter the inviter of the declined invitation.
     * @param reason the reason why the invitee is declining the invitation.
     */
    public void rejectInvitation(ChatRoomInvitation invitation);

    /**
     * Adds a listener to invitation notifications. The listener will be fired
     * anytime an invitation is received.
     *
     * @param listener an invitation listener.
     */
    public void addInvitationListener(InvitationListener listener);

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation events.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationListener(InvitationListener listener);

    /**
     * Adds a listener to invitation notifications. The listener will be fired
     * anytime an invitation is received.
     *
     * @param listener an invitation listener.
     */
    public void addInvitationRejectionListener(
                                        InvitationRejectionListener listener);

    /**
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation rejection events.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationRejectionListener(
                                        InvitationRejectionListener listener);

    /**
     * Returns true if <tt>contact</tt> supports multi user chat sessions.
     *
     * @param contact reference to the contact whose support for chat rooms
     * we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports chatrooms.
     */
    public boolean isMultiChatSupportedByContact(Contact contact);
}
