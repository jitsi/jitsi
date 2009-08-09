/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * Represents a listener of changes in the conference-related information of
 * <code>CallPeer</code> delivered in the form of
 * <code>CallPeerConferenceEvent</code>s.
 *
 * @author Lubomir Marinov
 */
public interface CallPeerConferenceListener
    extends EventListener
{

    /**
     * Notifies this listener about a change in the characteristic of being a
     * conference focus of a specific <code>CallPeer</code>.
     *
     * @param conferenceEvent
     *            a <code>CallPeerConferenceEvent</code> with ID
     *            <code>CallPeerConferenceEvent#CONFERENCE_FOCUS_CHANGED</code>
     *            and no associated <code>ConferenceMember</code>
     */
    public void conferenceFocusChanged(
        CallPeerConferenceEvent conferenceEvent);

    /**
     * Notifies this listener about the addition of a specific
     * <code>ConferenceMember</code> to the list of
     * <code>ConferenceMember</code>s of a specific <code>CallPeer</code>
     * acting as a conference focus.
     *
     * @param conferenceEvent
     *            a <code>CallPeerConferenceEvent</code> with ID
     *            <code>CallPeerConferenceEvent#CONFERENCE_MEMBER_ADDED</code>
     *            and <code>conferenceMember</code> property specifying the
     *            <code>ConferenceMember</code> which was added
     */
    public void conferenceMemberAdded(
        CallPeerConferenceEvent conferenceEvent);

    /**
     * Notifies this listener about the removal of a specific
     * <code>ConferenceMember</code> from the list of
     * <code>ConferenceMember</code>s of a specific <code>CallPeer</code>
     * acting as a conference focus.
     *
     * @param conferenceEvent
     *            a <code>CallPeerConferenceEvent</code> with ID
     *            <code>CallPeerConferenceEvent#CONFERENCE_MEMBER_REMOVED</code>
     *            and <code>conferenceMember</code> property specifying the
     *            <code>ConferenceMember</code> which was removed
     */
    public void conferenceMemberRemoved(
        CallPeerConferenceEvent conferenceEvent);
}
