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
 * <tt>CallPeer</tt> delivered in the form of
 * <tt>CallPeerConferenceEvent</tt>s.
 *
 * @author Lubomir Marinov
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
