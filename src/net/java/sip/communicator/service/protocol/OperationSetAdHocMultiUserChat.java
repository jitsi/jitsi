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
 * Allows creating, configuring, joining and administering of individual
 * text-based ad-hoc conference rooms.
 *
 * @author Valentin Martinet
 */
public interface OperationSetAdHocMultiUserChat
    extends OperationSet
{
    /**
     * Creates an ad-hoc room with the named <tt>adHocRoomName</tt> and 
     * according to the specified <tt>adHocRoomProperties</tt>. When the method
     * returns the ad-hoc room the local user will not have joined it and thus 
     * will not receive messages on it until the <tt>AdHocChatRoom.join()</tt> 
     * method is called.
     * <p>
     * 
     * @param adHocRoomName
     *            the name of the <tt>AdHocChatRoom</tt> to create.
     * @param adHocRoomProperties
     *            properties specifying how the ad-hoc room should be created;
     *            <tt>null</tt> for no properties just like an empty
     *            <code>Map</code>
     * @throws OperationFailedException
     *             if the ad-hoc room couldn't be created for some reason.
     * @throws OperationNotSupportedException
     *             if chat room creation is not supported by this server
     * 
     * @return the newly created <tt>AdHocChatRoom</tt> named <tt>roomName</tt>.
     */
    public AdHocChatRoom createAdHocChatRoom(String adHocRoomName, 
                                   Map<String, Object> adHocRoomProperties)
        throws OperationFailedException, OperationNotSupportedException;

    /**
     * Creates an ad-hoc room with the named <tt>adHocRoomName</tt> and in 
     * including to the specified <tt>contacts</tt>. When the method
     * returns the ad-hoc room the local user will not have joined it and thus 
     * will not receive messages on it until the <tt>AdHocChatRoom.join()</tt> 
     * method is called.
     * <p>
     * 
     * @param adHocRoomName
     *            the name of the <tt>AdHocChatRoom</tt> to create.
     * @param contacts
     *            the contacts who are added to the room when it's created;
     *            <tt>null</tt> for no contacts
     * @throws OperationFailedException
     *             if the ad-hoc room couldn't be created for some reason.
     * @throws OperationNotSupportedException
     *             if chat room creation is not supported by this server
     * 
     * @return the newly created <tt>AdHocChatRoom</tt> named <tt>roomName</tt>.
     */
    public AdHocChatRoom createAdHocChatRoom(String adHocRoomName, 
                                   List<Contact> contacts)
        throws OperationFailedException, OperationNotSupportedException;

    /**
     * Returns a reference to an  AdHocChatRoom named <tt>adHocRoomName</tt> or 
     * null if no ad-hoc room with the given name exist on the server.
     * <p>
     * @param adHocRroomName the name of the <tt>AdHocChatRoom</tt> that we're 
     * looking for.
     * @return the <tt>AdHocChatRoom</tt> named <tt>adHocRoomName</tt> if it
     * exists, null otherwise.
     *
     * @throws OperationFailedException if an error occurs while trying to
     * discover the ad-hoc room on the server.
     * @throws OperationNotSupportedException if the server does not support
     * multi-user chat
     */
    public AdHocChatRoom findRoom(String adHocRoomName)
        throws OperationFailedException, OperationNotSupportedException;

    /**
     * Returns true if <tt>contact</tt> supports multi-user chat sessions.
     *
     * @param contact reference to the contact whose support for ad-hoc chat
     * rooms we are currently querying.
     * @return a boolean indicating whether <tt>contact</tt> supports ad-hoc
     * chat rooms.
     */
    public boolean isMultiChatSupportedByContact(Contact contact);

    /**
     * Adds a listener that will be notified of changes in our participation in
     * an ad-hoc chat room such as us being joined, left.
     *
     * @param listener a local user participation listener.
     */
    public void addPresenceListener(
        LocalUserAdHocChatRoomPresenceListener listener);

    /**
     * Removes a listener that was being notified of changes in our
     * participation in an ad-hoc room such as us being joined, left.
     * 
     * @param listener a local user participation listener.
     */
    public void removePresenceListener(
        LocalUserAdHocChatRoomPresenceListener listener);

    /**
     * Adds the given <tt>listener</tt> to the list of
     * <tt>AdHocChatRoomInvitationListener</tt>-s that would be notified when
     * an add-hoc chat room invitation has been received.
     *
     * @param listener the <tt>AdHocChatRoomInvitationListener</tt> to add
     */
    public void addInvitationListener(AdHocChatRoomInvitationListener listener);

    /**
     * Adds the given <tt>listener</tt> to the list of
     * <tt>AdHocChatRoomInvitationRejectionListener</tt>-s that would be
     * notified when an add-hoc chat room invitation has been rejected.
     *
     * @param listener the <tt>AdHocChatRoomInvitationListener</tt> to add
     */
    public void addInvitationRejectionListener(
            AdHocChatRoomInvitationRejectionListener listener);

    /**
     * Informs the sender of an invitation that we decline their invitation.
     *
     * @param invitation the invitation we are rejecting.
     * @param rejectReason the reason to reject the invitation (optional)
     */
    public void rejectInvitation(   AdHocChatRoomInvitation invitation,
                                    String rejectReason);
}
