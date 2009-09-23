/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Represents a member and its details in a telephony conference managed by a
 * <code>CallPeer</code> in its role as a conference focus.
 *
 * @author Lubomir Marinov
 */
public interface ConferenceMember
{

    /**
     * The name of the property of <code>ConferenceMember</code> which specifies
     * the user-friendly display name of the respective
     * <code>ConferenceMember</code> in the conference.
     */
    public static final String DISPLAY_NAME_PROPERTY_NAME = "DisplayName";

    /**
     * The name of the property of <code>ConferenceMember</code> which specifies
     * the state of the device and signaling session of the respective
     * <code>ConferenceMember</code> in the conference.
     */
    public static final String STATE_PROPERTY_NAME = "State";

    /**
     * Adds a specific <code>PropertyChangeListener</code> to the list of
     * listeners interested in and notified about changes in the values of the
     * properties of this <code>ConferenceMember</code> such as
     * <code>#DISPLAY_NAME_PROPERTY_NAME</code> and
     * <code>#STATE_PROPERTY_NAME</code>.
     *
     * @param listener
     *            a <code>PropertyChangeListener</code> to be notified about
     *            changes in the values of the properties of this
     *            <code>ConferenceMember</code>. If the specified listener is
     *            already in the list of interested listeners (i.e. it has been
     *            previously added), it is not added again.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Gets the user-friendly display name of this <code>ConferenceMember</code>
     * in the conference.
     *
     * @return the user-friendly display name of this
     *         <code>ConferenceMember</code> in the conference
     */
    public String getDisplayName();

    /**
     * Gets the <code>CallPeer</code> which is the conference focus of
     * this <code>ConferenceMember</code>.
     *
     * @return the <code>CallPeer</code> which is the conference focus of
     *         this <code>ConferenceMember</code>
     */
    public CallPeer getConferenceFocusCallPeer();

    /**
     * Gets the state of the device and signaling session of this
     * <code>ConferenceMember</code> in the conference in the form of a
     * <code>ConferenceMemberState</code> value.
     *
     * @return a <code>ConferenceMemberState</code> value which represents the
     *         state of the device and signaling session of this
     *         <code>ConferenceMember</code> in the conference
     */
    public ConferenceMemberState getState();

    /**
     * Removes a specific <code>PropertyChangeListener</code> from the list of
     * listeners interested in and notified about changes in the values of the
     * properties of this <code>ConferenceMember</code> such as
     * <code>#DISPLAY_NAME_PROPERTY_NAME</code> and
     * <code>#STATE_PROPERTY_NAME</code>.
     *
     * @param listener
     *            a <code>PropertyChangeListener</code> to no longer be notified
     *            about changes in the values of the properties of this
     *            <code>ConferenceMember</code>
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Adds a specific <tt>CallPeerSoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in sound level related
     * information.
     * 
     * @param listener the <tt>CallPeerSoundLevelListener</tt> to add
     */
    public void addCallPeerSoundLevelListener(
        CallPeerSoundLevelListener listener);

    /**
     * Removes a specific <tt>CallPeerSoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in sound level related
     * information.
     * 
     * @param listener the <tt>CallPeerSoundLevelListener</tt> to remove
     */
    public void removeCallPeerSoundLevelListener(
        CallPeerSoundLevelListener listener);
}
