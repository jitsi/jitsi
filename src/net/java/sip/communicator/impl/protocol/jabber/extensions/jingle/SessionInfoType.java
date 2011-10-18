/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

/**
 * Contains an enumeration of all possible <tt>session-info</tt> element.
 *
 * @author Emil Ivov
 */
public enum SessionInfoType
{
    /**
     * The <tt>active</tt> payload indicates that the principal or device is
     * again actively participating in the session after having been on
     * mute or having put the other party on hold. The <tt>active</tt> element
     * applies to all aspects of the session, and thus does not possess a
     * 'name' attribute.
     */
    active,

    /**
     * The <tt>hold</tt> payload indicates that the principal is temporarily not
     * listening for media from the other party
     */
    hold,

    /**
     * The <tt>mute</tt> payload indicates that the principal is temporarily not
     * sending media to the other party but continuing to accept media from
     * the other party.
     */
    mute,

    /**
     * The <tt>ringing</tt> payload indicates that the device is ringing but the
     * principal has not yet interacted with it to answer (this maps to the SIP
     * 180 response code).
     */
    ringing,

    /**
     * Ends a <tt>hold</tt> state.
     */
    unhold,

    /**
     * Ends a <tt>mute</tt> state.
     */
    unmute
}
