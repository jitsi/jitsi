/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.net.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

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
     * The constant indicating that a <code>CallParticipant</code> has not yet
     * transitioned into a state marking the beginning of a participation in a
     * <code>Call</code> or that such a transition may have happened but the
     * time of its occurrence is unknown.
     */
    public static final long CALL_DURATION_START_TIME_UNKNOWN = 0;

    /**
     * The mute property name.
     */
    public static final String MUTE_PROPERTY_NAME = "Mute";

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
     * CallParticipantState may vary among CONNECTING, RINGING, CALLING, BUSY,
     * CONNECTED, and others, and it reflects the state of the connection between
     * us and that participant.
     * @return a CallParticipantState instance representing the participant's
     * state.
     */
    public CallParticipantState getState();

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
     * Allows the user interface to register a listener interested in security
     * status changes.
     * 
     * @param listener a listener instance to register with this participant
     */
    public void addCallParticipantSecurityListener(
        CallParticipantSecurityListener listener);

    /**
     * Unregisters the specified listener.
     * 
     * @param listener the listener to unregister
     */
    public void removeCallParticipantSecurityListener(
        CallParticipantSecurityListener listener);

    /**
     * Allows the user interface to register a listener interested in property
     * changes.
     * @param listener a property change listener instance to register with this
     * participant.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Unregisters the specified property change listener.
     * 
     * @param listener the property change listener to unregister.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Gets the time at which this <code>CallParticipant</code> transitioned
     * into a state (likely {@link CallParticipantState#CONNECTED}) marking the
     * start of the duration of the participation in a <code>Call</code>.
     * 
     * @return the time at which this <code>CallParticipant</code> transitioned
     *         into a state marking the start of the duration of the
     *         participation in a <code>Call</code> or
     *         {@link #CALL_DURATION_START_TIME_UNKNOWN} if such a transition
     *         has not been performed
     */
    long getCallDurationStartTime();

    /**
     * Returns a string representation of the participant in the form of
     * <br>
     * Display Name &lt;address&gt;;status=CallParticipantStatus
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

    /**
     * Returns the protocol provider that this participant belongs to.
     * @return a reference to the ProtocolProviderService that this participant
     * belongs to.
     */
    public ProtocolProviderService getProtocolProvider();

    /**
     * Returns the contact corresponding to this participant or null if no
     * particular contact has been associated.
     * <p>
     * @return the <tt>Contact</tt> corresponding to this participant or null
     * if no particular contact has been associated.
     */
    public Contact getContact();
    
    /**
     * Returns a URL pointing to a location with call control information or 
     * null if such an URL is not available for the current call participant.
     * 
     * @return a URL link to a location with call information or a call control
     * web interface related to this participant or <tt>null</tt> if no such URL
     * is available.
     */
    public URL getCallInfoURL();

    /**
     * Determines whether the audio stream (if any) being sent to this
     * participant is mute.
     * 
     * @return <tt>true</tt> if an audio stream is being sent to this
     *         participant and it is currently mute; <tt>false</tt>, otherwise
     */
    public boolean isMute();
}
