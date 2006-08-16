/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;

/**
 * The CallParticipant is an interface that represents participants in a call.
 * Users of the PhoneUIService need to implement this interface (or one of its
 * default implementations such DefaultCallParticipant) in order to be able to
 * register call participant in the user interface.
 *
 * <p>For SIP calls for example, it would be necessary to create a
 * SipCallParticipant class that would provide sip specific implementations of
 * various methods (getAddress() for example would return the participant's sip
 * URI).
 *
 * @author Emil Ivov
 */
public interface CallParticipant
{

    /**
     * Returns a unique identifier representing this participant. Identifiers
     * returned by this method should remain unique across calls. In other
     * words, if it returned the value of "A" for a given participant it should
     * not return that same value for any other participant and return a
     * different value even if the same person (address) is participating in
     * another call. Values need not remain unique after restarting the program.
     *
     * @return an identifier representing this call participant.
     */
    public String getParticipantID();

    /**
     * Returns a reference to the call that this participant belongs to.
     * @return a reference to the call containing this participant.
     */
    public Call getCall();

    /**
     * Returns a human readable name representing this participant.
     * @return a String containing a name for that participant.
     */
    public String getDisplayName();

    /**
     * Returns a String locator for that participant. A locator might be a SIP
     * URI, an IP address or a telephone number.
     * @return the participant's address or phone number.
     */
    public String getAddress();

    /**
     * Returns an object representing the current state of that participant.
     * CallParticipantState may vary among CONNECTING, RINGING, CALLING, BISY,
     * CONNECTED, and others, and it reflects the state of the connection between
     * us and that participant.
     * @return a CallParticipantState instance representin the participant's
     * state.
     */
    public CallParticipantState getState();

    /**
     * Determines whether or not this is the participant that originated the
     * call (as opposed to the one that was called).
     *
     * @return true if this is the participant that calls us and falls if
     * otherwise.
     */
    public boolean isCaller();

    /**
     * Allows the user interface to register a listener interested in changes
     * @param listener a listener instance to register with this participant.
     */
    public void addCallParticipantListener(CallParticipantListener listener);

    /**
     * Unregisters the specified listener.
     * @param listener the listener to unregister.
     */
    public void removeCallParticipantListener(CallParticipantListener listener);

    /**
     * Returns the date (time) when this call participant acquired its current
     * status. This method is to be used by the phone ui interface in order
     * to show the duration of a call.
     * @return a java.util.Date object containing the date when this call
     * participant entered its current state.
     */
    public Date getCurrentStateStartDate();

    /**
     * Returns a string representation of the participant in the form of
     * <br>
     * Display Name <address>;status=CallParticipantStatus
     * @return a string representation of the participant and its state.
     */
    public String toString();

    /**
     * The method returns an image representation of the call participant (e.g.
     * a photo). Generally, the image representation is acquired from the
     * underlying telephony protocol and is transferred over the network during
     * call negotiation.
     * @return byte[] a byte array containing the image or null if no image is
     * available.
     */
    public byte[] getImage();
}
