/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import org.jitsi.util.event.*;

/**
 * Provides the default implementation of the <tt>ConferenceMember</tt>
 * interface.
 *
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 * @author Emil Ivov
 */
public class AbstractConferenceMember
    extends PropertyChangeNotifier
    implements ConferenceMember
{
    /**
     * A Public Switched Telephone Network (PSTN) ALERTING or SIP 180 Ringing
     * was returned for the outbound call; endpoint is being alerted.
     */
    public static final String ALERTING = "alerting";

    /**
     * The endpoint is a participant in the conference. Depending on the media
     * policies, he/she can send and receive media to and from other
     * participants.
     */
    public static final String CONNECTED = "connected";

    /**
     * Endpoint is dialing into the conference, not yet in the roster (probably
     * being authenticated).
     */
    public static final String DIALING_IN = "dialing-in";

    /**
     * Focus has dialed out to connect the endpoint to the conference, but the
     * endpoint is not yet in the roster (probably being authenticated).
     */
    public static final String DIALING_OUT = "dialing-out";

    /**
     * The endpoint is not a participant in the conference, and no active dialog
     * exists between the endpoint and the focus.
     */
    public static final String DISCONNECTED = "disconnected";

    /**
     * Active signaling dialog exists between an endpoint and a focus, but
     * endpoint is "on-hold" for this conference, i.e., he/she is neither
     * "hearing" the conference mix nor is his/her media being mixed in the
     * conference.
     */
    public static final String ON_HOLD = "on-hold";

    /**
     * Endpoint is not yet in the session, but it is anticipated that he/she
     * will join in the near future.
     */
    public static final String PENDING = "pending";

    /**
     * The protocol address of this <tt>ConferenceMember</tt>.
     */
    private final String address;

    /**
     * The audio SSRC value if transmitted by the focus of the conference.
     */
    private long audioSsrc = -1;

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
     * The state of the device and signaling session of this
     * <tt>ConferenceMember</tt> in the conference.
     */
    private ConferenceMemberState state = ConferenceMemberState.UNKNOWN;

    /**
     * The video SSRC value if transmitted by the focus of the conference.
     */
    private long videoSsrc = -1;

    /**
     * Creates an instance of <tt>AbstractConferenceMember</tt> by specifying
     * the corresponding <tt>conferenceFocusCallPeer</tt>, to which this member
     * is connected.
     *
     * @param conferenceFocusCallPeer the <tt>CallPeer</tt> to which this member
     * is connected
     * @param address the protocol address of this <tt>ConferenceMember</tt>
     * @throws NullPointerException if <tt>conferenceFocusCallPeer</tt> or
     * <tt>address</tt> is <tt>null</tt>
     */
    public AbstractConferenceMember(
            CallPeer conferenceFocusCallPeer,
            String address)
    {
        if (conferenceFocusCallPeer == null)
            throw new NullPointerException("conferenceFocusCallPeer");
        if (address == null)
            throw new NullPointerException("address");

        this.conferenceFocusCallPeer = conferenceFocusCallPeer;
        this.address = address;
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
     * Returns the SSRC value associated with this participant;
     *
     * @return the audio ssrc id
     */
    public long getAudioSsrc()
    {
        return audioSsrc;
    }

    /**
     * {@inheritDoc}
     *
     * Implements {@link ConferenceMember#getConferenceFocusCallPeer()}.
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
        String displayName = this.displayName;

        if ((displayName == null) || (displayName.length() < 1))
        {
            String address = getAddress();

            if ((address != null) && (address.length() > 0))
                return address;
        }
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
     * Returns the SSRC value associated with this participant;
     *
     * @return the video ssrc id
     */
    public long getVideoSsrc()
    {
        return videoSsrc;
    }

    /**
     * Sets the audio SSRC identifier of this member.
     *
     * @param ssrc the audio SSRC ID to set for this member.
     */
    public void setAudioSsrc(long ssrc)
    {
        this.audioSsrc = ssrc;
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
                    oldValue, this.displayName);
        }
    }

    /**
     * Sets the <tt>state</tt> property of this <tt>ConferenceMember</tt> by
     * translating it from its conference-info XML endpoint status.
     *
     * @param endpointStatus the conference-info XML endpoint status of this
     * <tt>ConferenceMember</tt> indicated by its
     * <tt>conferenceFocusCallPeer</tt>
     */
    public void setEndpointStatus(String endpointStatus)
    {
        ConferenceMemberState state;

        if (ALERTING.equalsIgnoreCase(endpointStatus))
            state = ConferenceMemberState.ALERTING;
        else if (CONNECTED.equalsIgnoreCase(endpointStatus))
            state = ConferenceMemberState.CONNECTED;
        else if (DIALING_IN.equalsIgnoreCase(endpointStatus))
            state = ConferenceMemberState.DIALING_IN;
        else if (DIALING_OUT.equalsIgnoreCase(endpointStatus))
            state = ConferenceMemberState.DIALING_OUT;
        else if (DISCONNECTED.equalsIgnoreCase(endpointStatus))
            state = ConferenceMemberState.DISCONNECTED;
        else if (ON_HOLD.equalsIgnoreCase(endpointStatus))
            state = ConferenceMemberState.ON_HOLD;
        else if (PENDING.equalsIgnoreCase(endpointStatus))
            state = ConferenceMemberState.PENDING;
        else
            state = ConferenceMemberState.UNKNOWN;

        setState(state);
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

    /**
     * Sets the video SSRC identifier of this member.
     *
     * @param ssrc the video SSRC ID to set for this member.
     */
    public void setVideoSsrc(long ssrc)
    {
        if (this.videoSsrc != ssrc)
        {
            long oldValue = this.videoSsrc;

            this.videoSsrc = ssrc;

            firePropertyChange(
                    VIDEO_SSRC_PROPERTY_NAME,
                    oldValue, this.videoSsrc);
        }
    }
}
