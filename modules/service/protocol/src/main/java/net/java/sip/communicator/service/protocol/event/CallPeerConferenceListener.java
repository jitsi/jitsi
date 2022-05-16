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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Represents a listener of changes in the conference-related information of
 * <tt>CallPeer</tt> delivered in the form of
 * <tt>CallPeerConferenceEvent</tt>s.
 *
 * @author Lyubomir Marinov
 */
public interface CallPeerConferenceListener
    extends EventListener
{

    /**
     * Notifies this listener about a change in the characteristic of being a
     * conference focus of a specific <tt>CallPeer</tt>.
     *
     * @param conferenceEvent
     *            a <tt>CallPeerConferenceEvent</tt> with ID
     *            <tt>CallPeerConferenceEvent#CONFERENCE_FOCUS_CHANGED</tt>
     *            and no associated <tt>ConferenceMember</tt>
     */
    public void conferenceFocusChanged(
        CallPeerConferenceEvent conferenceEvent);

    /**
     * Notifies this listener about the addition of a specific
     * <tt>ConferenceMember</tt> to the list of
     * <tt>ConferenceMember</tt>s of a specific <tt>CallPeer</tt>
     * acting as a conference focus.
     *
     * @param conferenceEvent
     *            a <tt>CallPeerConferenceEvent</tt> with ID
     *            <tt>CallPeerConferenceEvent#CONFERENCE_MEMBER_ADDED</tt>
     *            and <tt>conferenceMember</tt> property specifying the
     *            <tt>ConferenceMember</tt> which was added
     */
    public void conferenceMemberAdded(
        CallPeerConferenceEvent conferenceEvent);
    
    /**
     * Notifies this listener about an error packet received from specific 
     * <tt>CallPeer</tt>.
     *
     * @param conferenceEvent a <tt>CallPeerConferenceEvent</tt> with ID
     *        <tt>CallPeerConferenceEvent#CONFERENCE_MEMBER_ERROR_RECEIVED</tt> 
     *        and the error message associated with the packet.
     */
    public void conferenceMemberErrorReceived(
        CallPeerConferenceEvent conferenceEvent);

    /**
     * Notifies this listener about the removal of a specific
     * <tt>ConferenceMember</tt> from the list of
     * <tt>ConferenceMember</tt>s of a specific <tt>CallPeer</tt>
     * acting as a conference focus.
     *
     * @param conferenceEvent
     *            a <tt>CallPeerConferenceEvent</tt> with ID
     *            <tt>CallPeerConferenceEvent#CONFERENCE_MEMBER_REMOVED</tt>
     *            and <tt>conferenceMember</tt> property specifying the
     *            <tt>ConferenceMember</tt> which was removed
     */
    public void conferenceMemberRemoved(CallPeerConferenceEvent conferenceEvent);
}
