/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.util.*;

/**
 * Provides the default implementation of the <tt>ConferenceMember</tt>
 * interface.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class AbstractConferenceMember
    extends PropertyChangeNotifier
    implements ConferenceMember
{
    /**
     * The <tt>CallPeer</tt> which is the conference focus of this
     * <tt>ConferenceMember</tt>.
     */
    private final CallPeer conferenceFocusCallPeer;

    /**
     * The user-friendly display name of this <tt>ConferenceMember</tt> in
     * the conference.
     */
    private String displayName;

    /**
     * The protocol address of this <tt>ConferenceMember</tt>.
     */
    private String address;

    /**
     * The state of the device and signaling session of this
     * <tt>ConferenceMember</tt> in the conference.
     */
    private ConferenceMemberState state = ConferenceMemberState.UNKNOWN;

    /**
     * Creates an instance of <tt>AbstractConferenceMember</tt> by specifying
     * the corresponding <tt>conferenceFocusCallPeer</tt>, to which this member
     * is connected.
     * @param conferenceFocusCallPeer the <tt>CallPeer</tt> to which this member
     * is connected
     * @param address the protocol address of this <tt>ConferenceMember</tt>
     */
    public AbstractConferenceMember(CallPeer conferenceFocusCallPeer,
                                    String address)
    {
        this.conferenceFocusCallPeer = conferenceFocusCallPeer;

        if (address == null)
            throw new NullPointerException("address");
        this.address = address;
    }

    /**
     * Returns the <tt>CallPeer</tt>, to which this member is connected.
     * Implements <tt>ConferenceMember#getConferenceFocusCallPeer()</tt>.
     * @return the <tt>CallPeer</tt>, to which this member is connected
     */
    public CallPeer getConferenceFocusCallPeer()
    {
        return conferenceFocusCallPeer;
    }

    /**
     * Returns the display name of this conference member. Implements
     * <tt>ConferenceMember#getDisplayName()</tt>.
     * @return the display name of this conference member
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the state of this conference member. Implements
     * <tt>ConferenceMember#getState()</tt>.
     * @return the state of this conference member
     */
    public ConferenceMemberState getState()
    {
        return state;
    }

    /**
     * Returns the protocol address of this <tt>ConferenceMember</tt>.
     *
     * @return the protocol address of this <tt>ConferenceMember</tt>
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Sets the user-friendly display name of this <tt>ConferenceMember</tt>
     * in the conference and fires a new <tt>PropertyChangeEvent</tt> for
     * the property <tt>#DISPLAY_NAME_PROPERTY_NAME</tt>.
     *
     * @param displayName
     *            the user-friendly display name of this
     *            <tt>ConferenceMember</tt> in the conference
     */
    public void setDisplayName(String displayName)
    {
        if (((this.displayName == null) && (displayName != null))
                || ((this.displayName != null)
                        && !this.displayName.equals(displayName)))
        {
            String oldValue = this.displayName;

            this.displayName = displayName;

            firePropertyChange(
                DISPLAY_NAME_PROPERTY_NAME,
                oldValue,
                this.displayName);
        }
    }

    /**
     * Sets the state of the device and signaling session of this
     * <tt>ConferenceMember</tt> in the conference and fires a new
     * <tt>PropertyChangeEvent</tt> for the property
     * <tt>#STATE_PROPERTY_NAME</tt>.
     *
     * @param state
     *            the state of the device and signaling session of this
     *            <tt>ConferenceMember</tt> in the conference
     */
    public void setState(ConferenceMemberState state)
    {
        if (this.state != state)
        {
            ConferenceMemberState oldValue = this.state;

            this.state = state;

            firePropertyChange(STATE_PROPERTY_NAME, oldValue, this.state);
        }
    }
}
