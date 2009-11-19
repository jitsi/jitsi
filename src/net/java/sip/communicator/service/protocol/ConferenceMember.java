/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.util.*;

/**
 * Represents a member and its details in a telephony conference managed by a
 * <tt>CallPeer</tt> in its role as a conference focus.
 *
 * @author Lubomir Marinov
 */
public interface ConferenceMember
{
    /**
     * The name of the property of <tt>ConferenceMember</tt> which specifies
     * the user-friendly display name of the respective
     * <tt>ConferenceMember</tt> in the conference.
     */
    public static final String DISPLAY_NAME_PROPERTY_NAME = "DisplayName";

    /**
     * The name of the property of <tt>ConferenceMember</tt> which specifies
     * the state of the device and signaling session of the respective
     * <tt>ConferenceMember</tt> in the conference.
     */
    public static final String STATE_PROPERTY_NAME = "State";

    /**
     * Adds a specific <tt>PropertyChangeListener</tt> to the list of
     * listeners interested in and notified about changes in the values of the
     * properties of this <tt>ConferenceMember</tt> such as
     * <tt>#DISPLAY_NAME_PROPERTY_NAME</tt> and
     * <tt>#STATE_PROPERTY_NAME</tt>.
     *
     * @param listener
     *            a <tt>PropertyChangeListener</tt> to be notified about
     *            changes in the values of the properties of this
     *            <tt>ConferenceMember</tt>. If the specified listener is
     *            already in the list of interested listeners (i.e. it has been
     *            previously added), it is not added again.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Gets the SIP address of this <tt>ConferenceMember</tt> as specified by
     * the conference-info XML received from its
     * <tt>conferenceFocusCallPeer</tt>.
     *
     * @return the SIP address of this <tt>ConferenceMember</tt> as specified by
     * the conference-info XML received from its
     * <tt>conferenceFocusCallPeer</tt>
     */
    public String getAddress();

    /**
     * Gets the user-friendly display name of this <tt>ConferenceMember</tt>
     * in the conference.
     *
     * @return the user-friendly display name of this
     *         <tt>ConferenceMember</tt> in the conference
     */
    public String getDisplayName();

    /**
     * Gets the <tt>CallPeer</tt> which is the conference focus of
     * this <tt>ConferenceMember</tt>.
     *
     * @return the <tt>CallPeer</tt> which is the conference focus of
     *         this <tt>ConferenceMember</tt>
     */
    public CallPeer getConferenceFocusCallPeer();

    /**
     * Gets the state of the device and signaling session of this
     * <tt>ConferenceMember</tt> in the conference in the form of a
     * <tt>ConferenceMemberState</tt> value.
     *
     * @return a <tt>ConferenceMemberState</tt> value which represents the
     *         state of the device and signaling session of this
     *         <tt>ConferenceMember</tt> in the conference
     */
    public ConferenceMemberState getState();

    /**
     * Removes a specific <tt>PropertyChangeListener</tt> from the list of
     * listeners interested in and notified about changes in the values of the
     * properties of this <tt>ConferenceMember</tt> such as
     * <tt>#DISPLAY_NAME_PROPERTY_NAME</tt> and
     * <tt>#STATE_PROPERTY_NAME</tt>.
     *
     * @param listener
     *            a <tt>PropertyChangeListener</tt> to no longer be notified
     *            about changes in the values of the properties of this
     *            <tt>ConferenceMember</tt>
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);
}
