/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Represents the state of the device and signaling session of
 * <code>ConferenceMember</code> in the conference it is participating in.
 * 
 * @author Lubomir Marinov
 */
public enum ConferenceMemberState
{

    /**
     * A Public Switched Telephone Network (PSTN) ALERTING or SIP 180 Ringing
     * was returned for the outbound call; endpoint is being alerted.
     */
    ALTERTING,

    /**
     * The endpoint is a participant in the conference. Depending on the media
     * policies, he/she can send and receive media to and from other
     * participants.
     */
    CONNECTED,

    /**
     * Endpoint is dialing into the conference, not yet in the roster (probably
     * being authenticated).
     */
    DIALING_IN,

    /**
     * Focus has dialed out to connect the endpoint to the conference, but the
     * endpoint is not yet in the roster (probably being authenticated).
     */
    DIALING_OUT,

    /**
     * The endpoint is not a participant in the conference, and no active dialog
     * exists between the endpoint and the focus.
     */
    DISCONNECTED,

    /**
     * Focus is in the process of disconnecting the endpoint (e.g., in SIP a
     * DISCONNECT or BYE was sent to the endpoint).
     */
    DISCONNECTING,

    /**
     * Active signaling dialog exists between an endpoint and a focus and the
     * endpoint can "listen" to the conference, but the endpoint's media is not
     * being mixed into the conference.
     */
    MUTED_VIA_FOCUS,

    /**
     * Active signaling dialog exists between an endpoint and a focus, but
     * endpoint is "on-hold" for this conference, i.e., he/she is neither
     * "hearing" the conference mix nor is his/her media being mixed in the
     * conference.
     */
    ON_HOLD,

    /**
     * Endpoint is not yet in the session, but it is anticipated that he/she
     * will join in the near future.
     */
    PENDING,

    /**
     * The state of the device and signaling session of the associated
     * <code>ConferenceMember</code> in the conference is unknown.
     */
    UNKNOWN
}
