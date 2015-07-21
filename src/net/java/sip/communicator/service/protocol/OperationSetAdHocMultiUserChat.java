/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
     * returns the ad-hoc room the local user will have joined it.
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
     * including to the specified <tt>contacts</tt> for the given <tt>reason
     * </tt>. When the method returns the ad-hoc room the local user will have
     * joined it.
     * <p>
     *
     * @param adHocRoomName
     *            the name of the <tt>AdHocChatRoom</tt> to create.
     * @param contacts
     *            the contacts (ID) who are added to the room when it's created;
     *            <tt>null</tt> for no contacts
     * @param reason the reason for this invitation
     * @throws OperationFailedException
     *             if the ad-hoc room couldn't be created for some reason.
     * @throws OperationNotSupportedException
     *             if chat room creation is not supported by this server
     *
     * @return the newly created <tt>AdHocChatRoom</tt> named <tt>roomName</tt>.
     */
    public AdHocChatRoom createAdHocChatRoom(String adHocRoomName,
                                   List<String> contacts, String reason)
        throws OperationFailedException, OperationNotSupportedException;

    /**
     * Returns a list of all currently joined <tt>AdHocChatRoom</tt>-s.
     *
     * @return a list of all currently joined <tt>AdHocChatRoom</tt>-s
     */
    public List<AdHocChatRoom> getAdHocChatRooms();

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
     * Removes <tt>listener</tt> from the list of invitation listeners
     * registered to receive invitation events.
     *
     * @param listener the invitation listener to remove.
     */
     public void removeInvitationListener(
         AdHocChatRoomInvitationListener listener);

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
     * Removes the given listener from the list of invitation listeners
     * registered to receive events every time an invitation has been rejected.
     *
     * @param listener the invitation listener to remove.
     */
    public void removeInvitationRejectionListener(
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
