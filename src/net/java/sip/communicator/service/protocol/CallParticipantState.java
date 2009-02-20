/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * The CallParticipantState class reflects the current state of a call
 * participant. In other words when you start calling your grand mother she will
 * be in a INITIATING_CALL state, when her phone rings her state will change to
 * ALERTING_REMOTE_SIDE, and when she replies she will enter a CONNCECTED state.
 *
 * <p>Though not mandatory CallParticipantState would generally have one of the
 * following life cycles
 *
 * <p> In the case with your grand mother that we just described we have:
 * <br>INITIATING_CALL -> CONNECTING -> ALERTING_REMOTE_USER -> CONNECTED -> DISCONNECTED
 *
 * <p> If your granny was already on the phone we have:
 * <br>INITIATING_CALL -> CONNECTING -> BUSY -> DISCONNECTED
 *
 * <p>Whenever someone tries to reach you:
 * <br>INCOMING_CALL -> CONNECTED -> DISCONNECTED
 *
 * <p>A FAILED state is prone to appear at any place in the above diagram and is
 * generally followed by a disconnected state.
 *
 * <p>Information on call participant is shown in the phone user interface until
 * they enter the DISCONNECTED state. At that point call participant information
 * is automatically removed from the user interface and the call is considered
 * terminated.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class CallParticipantState
{
    /**
     * This constant value indicates a String representation of the UNKNOWN
     * call state.
     * <br>This constant has the String value "Unknown".
     */
    public static final String _UNKNOWN      = "Unknown";

    /**
     * This constant value indicates that the state of the call participant is
     * is UNKNOWN - which means that there is no information on the state for
     * the time being (this constant should be used as a default value for
     * newly created call participant that don't yet have an attributed call
     * state.
     */
    public static final CallParticipantState UNKNOWN =
                                        new CallParticipantState(_UNKNOWN);

    /**
     * This constant value indicates a String representation of the
     * INITIATING_CALL call state.
     * <br>This constant has the String value "Initiating Call".
     */
    public static final String _INITIATING_CALL      = "Initiating Call";

    /**
     * This constant value indicates that the state of the call participant is
     * is INITIATING_CALL - which means that we're currently trying to open a
     * socket and send our request. In the case of SIP for example we will leave
     * this state the moment we receive a "100 Trying" request from a proxy or
     * the remote side.
     */
    public static final CallParticipantState INITIATING_CALL =
                                        new CallParticipantState(_INITIATING_CALL);

    /**
     * This constant value indicates a String representation of the CONNECTING
     * call state.
     * <br>This constant has the String value "Connecting".
     */
    public static final String _CONNECTING      = "Connecting";

    /**
     * This constant value indicates that the state of the call participant is
     * is CONNECTING - which means that a network connection to that participant
     * is currently being established.
     */
    public static final CallParticipantState CONNECTING =
                                        new CallParticipantState(_CONNECTING);
    
    /**
     * This constant value indicates a String representation of the CONNECTING
     * call state but in cases where early media is being exchanged.
     * <br>This constant has the String value "Connecting".
     */
    public static final String _CONNECTING_WITH_EARLY_MEDIA = "Connecting*";

    /**
     * This constant value indicates that the state of the call participant is
     * is CONNECTING - which means that a network connection to that participant
     * is currently being established.
     */
    public static final CallParticipantState CONNECTING_WITH_EARLY_MEDIA =
                       new CallParticipantState( _CONNECTING_WITH_EARLY_MEDIA );

    /**
     * This constant value indicates a String representation of the
     * ALERTING_REMOTE_SIDE call state.
     * <br>This constant has the String value "Alerting Remote User".
     */
    public static final String _ALERTING_REMOTE_SIDE
                                             = "Alerting Remote User (Ringing)";

    /**
     * This constant value indicates that the state of the call participant is
     * is ALERTING_REMOTE_SIDE - which means that a network connection to that participant
     * has been established and participant's phone is currently alerting the
     * remote user of the current call.
     */
    public static final CallParticipantState ALERTING_REMOTE_SIDE =
                                new CallParticipantState(_ALERTING_REMOTE_SIDE);

    /**
     * This constant value indicates a String representation of the
     * INCOMING_CALL call state.
     * <br>This constant has the String value "Incoming Call".
     */
    public static final String _INCOMING_CALL         = "Incoming Call";

    /**
     * This constant value indicates that the state of the call participant is
     * is INCOMING_CALL - which means that the participant is willing to start
     * a call with us. At that point local side should be playing a sound or a
     * graphical alert (the phone is ringing).
     */
    public static final CallParticipantState INCOMING_CALL
                                         = new CallParticipantState(_INCOMING_CALL);

    /**
     * This constant value indicates a String representation of the CONNECTED
     * call state.
     * <br>This constant has the String value "Connected".
     */
    public static final String _CONNECTED       = "Connected";

    /**
     * This constant value indicates that the state of the call participant is
     * is CONNECTED - which means that there is an ongoing call with that
     * participant.
     */
    public static final CallParticipantState CONNECTED
                                       = new CallParticipantState(_CONNECTED);

    /**
     * This constant value indicates a String representation of the DISCONNECTED
     * call state.
     * <br>This constant has the String value "Disconnected".
     */
    public static final String _DISCONNECTED    = "Disconnected";

    /**
     * This constant value indicates that the state of the call participant is
     * is DISCONNECTET - which means that this participant is not participating :)
     * in the call any more.
     */
    public static final CallParticipantState DISCONNECTED    =
                                      new CallParticipantState(_DISCONNECTED);

    /**
     * This constant value indicates a String representation of the BUSY
     * call state.
     * <br>This constant has the String value "Busy".
     */
    public static final String _BUSY            = "Busy";

    /**
     * This constant value indicates that the state of the call participant is
     * is BUSY - which means that an attempt to establish a call with that
     * participant has been made and that it has been turned down by them (e.g.
     * because they were already in a call).
     */
    public static final CallParticipantState BUSY
                                            = new CallParticipantState(_BUSY);

    /**
     * This constant value indicates a String representation of the FAILED
     * call state.
     * <br>This constant has the String value "Failed".
     */
    public static final String _FAILED          = "Failed";
    /**
     * This constant value indicates that the state of the call participant is
     * is ON_HOLD - which means that an attempt to establish a call with that
     * participant has failed for an unexpected reason.
     */
    public static final CallParticipantState FAILED
                                          = new CallParticipantState(_FAILED);

    /**
     * The constant value being a String representation of the ON_HOLD_LOCALLY
     * call participant state.
     * <p>
     * This constant has the String value "Locally On Hold".
     * </p>
     */
    public static final String _ON_HOLD_LOCALLY = "Locally On Hold";
    /**
     * The constant value indicating that the state of a call participant is
     * locally put on hold.
     */
    public static final CallParticipantState ON_HOLD_LOCALLY
                                = new CallParticipantState(_ON_HOLD_LOCALLY);

    /**
     * The constant value being a String representation of the ON_HOLD_MUTUALLY
     * call participant state.
     * <p>
     * This constant has the String value "Mutually On Hold".
     * </p>
     */
    public static final String _ON_HOLD_MUTUALLY = "Mutually On Hold";
    /**
     * The constant value indicating that the state of a call participant is
     * mutually - locally and remotely - put on hold.
     */
    public static final CallParticipantState ON_HOLD_MUTUALLY
                                = new CallParticipantState(_ON_HOLD_MUTUALLY);

    /**
     * The constant value being a String representation of the ON_HOLD_REMOTELY
     * call participant state.
     * <p>
     * This constant has the String value "Remotely On Hold".
     * </p>
     */
    public static final String _ON_HOLD_REMOTELY = "Remotely On Hold";

    /**
     * The constant value indicating that the state of a call participant is
     * remotely put on hold.
     */
    public static final CallParticipantState ON_HOLD_REMOTELY
                                = new CallParticipantState(_ON_HOLD_REMOTELY);

    /**
     * Determines whether a specific <tt>CallParticipantState</tt> value
     * signal a call hold regardless of the issuer (which may be local and/or
     * remote).
     * 
     * @param state
     *            the <tt>CallParticipantState</tt> value to be checked
     *            whether it signals a call hold
     * @return <tt>true</tt> if the specified <tt>state</tt> signals a call
     *         hold; <tt>false</tt>, otherwise
     */
    public static final boolean isOnHold(CallParticipantState state)
    {
        return CallParticipantState.ON_HOLD_LOCALLY.equals(state)
                || CallParticipantState.ON_HOLD_MUTUALLY.equals(state)
                || CallParticipantState.ON_HOLD_REMOTELY.equals(state);
    }

    /**
     * A string representationf this Participant Call State. Could be
     * _CONNECTED, _FAILED, _CALLING and etc.
     */
    private String callStateStr;

    /**
     * Create a participant call state object with a value corresponding to the
     * specified string.
     * @param callParticipantState a string representation of the state.
     */
    private CallParticipantState(String callParticipantState)
    {
        this.callStateStr = callParticipantState;
    }

    /**
     * Returns a String representation of tha CallParticipantSte.
     *
     * @return A string value (one of the _BUSY, _CALLING, _CONNECTED,
     * _CONNECTING, _DISCONNECTED, _FAILED, _RINGING constants) representing
     * this call participant state).
     */
    public String getStateString()
    {
        return callStateStr;
    }

    /**
     * Returns a string representation of this call state. Strings returned
     * by this method have the following format:
     * CallParticipantState:<STATE_STRING>
     * and are meant to be used for logging/debugging purposes.
     * @return a string representation of this object.
     */
    public String toString()
    {
        return getClass().getName()+":"+getStateString();
    }
}
