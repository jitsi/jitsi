/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;

/**
 * Represents a member and its details in a Jabber telephony conference managed
 * by a <tt>CallPeer</tt> in its role as a conference focus.
 *
 * @author Sebastien Vincent
 */
public class ConferenceMemberJabberImpl
    extends AbstractConferenceMember
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
     * Constructor.
     *
     * @param callPeer the <tt>CallPeer</tt>
     * @param address address of the conference member
     */
    public ConferenceMemberJabberImpl(CallPeerJabberImpl callPeer,
            String address)
    {
        super(callPeer, address);
    }

    /**
     * Overrides {@link AbstractCallPeer#getDisplayName()} in order to return
     * the Jabber address of this <tt>ConferenceMember</tt> if the display name
     * is empty.
     *
     * @return if the <tt>displayName</tt> property of this instance is an empty
     * <tt>String</tt> value, returns the <tt>address</tt> property of this
     * instance; otherwise, returns the value of the <tt>displayName</tt>
     * property of this instance
     */
    @Override
    public String getDisplayName()
    {
        String displayName = super.getDisplayName();

        if ((displayName == null) || (displayName.length() < 1))
        {
            String address = getAddress();

            if ((address != null) && (address.length() > 0))
                return address;
        }
        return displayName;
    }

    /**
     * Sets the <tt>state</tt> property of this <tt>ConferenceMember</tt> by
     * translating it from its conference-info XML endpoint status.
     *
     * @param endpointStatus the conference-info XML endpoint status of this
     * <tt>ConferenceMember</tt> indicated by its
     * <tt>conferenceFocusCallPeer</tt>
     */
    void setEndpointStatus(String endpointStatus)
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
}
