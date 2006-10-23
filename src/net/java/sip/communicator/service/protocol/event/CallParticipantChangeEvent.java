/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * CallParticipantChangeEvent-s are triggerred whenever a change occurs in a
 * CallParticipant. Dispatched events may be of one of the following types.
 * <p>
 * CALL_PARTICIPANT_STATUS_CHANGE - indicates a change in the status of the
 * participant.
 * <p>
 * CALL_PARTICIPANT_DISPLAY_NAME_CHANGE - means that participant's displayName
 * has changed
 * <p>
 * CALL_PARTICIPANT_ADDRESS_CHANGE - means that participant's address has
 * changed.
 * <p>
 * CALL_PARTICIPANT_ADDRESS_CHANGE - means that the transport address of the
 * participant (the one that we use to communicate with her) has changed.
 * <p>
 * CALL_PARTICIPANT_IMAGE_CHANGE - participant updated photo.
 * <p>
 *
 * @author Emil Ivov
 */
public class CallParticipantChangeEvent
    extends java.beans.PropertyChangeEvent
{
    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the CallParticipant's status.
     */
    public static final String CALL_PARTICIPANT_STATE_CHANGE =
                                                "CallParticipantStatusChange";

    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the participant's display name.
     */
    public static final String CALL_PARTICIPANT_DISPLAY_NAME_CHANGE =
                                             "CallParticipantDisplayNameChange";

    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the participant's address.
     */
    public static final String CALL_PARTICIPANT_ADDRESS_CHANGE =
                                                "CallParticipantAddressChange";

    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the participant's address.
     */
    public static final String CALL_PARTICIPANT_TRANSPORT_ADDRESS_CHANGE =
        "CallParticipantAddressChange";

    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the participant's photo/picture.
     */
    public static final String CALL_PARTICIPANT_IMAGE_CHANGE =
                                                   "CallParticipantImageChange";

    /**
     * A reason string further explaining the event (may be null). The string
     * would be mostly used for events issued upon a CallParticipantState
     * transition that has led to a FAILED state.
     */
    private String reason = null;

    /**
     * Creates a CallParticipantChangeEvent with the specified source, type,
     * oldValue and newValue.
     * @param source the participant that produced the event.
     * @param type the type of the event (i.e. address change, state change etc.).
     * @param oldValue the value of the changed property before the event occurred
     * @param newValue current value of the changed property.
     */
    public CallParticipantChangeEvent(CallParticipant source,
                                      String type,
                                      Object oldValue,
                                      Object newValue)
    {
        this(source, type, oldValue, newValue, null);
    }

    /**
     * Creates a CallParticipantChangeEvent with the specified source, type,
     * oldValue and newValue.
     * @param source the participant that produced the event.
     * @param type the type of the event (i.e. address change, state change etc.).
     * @param oldValue the value of the changed property before the event occurred
     * @param newValue current value of the changed property.
     * @param reason a string containing a human readable explanation for the
     * reason that triggerred this event (may be null).
     */
    public CallParticipantChangeEvent(CallParticipant source,
                                      String type,
                                      Object oldValue,
                                      Object newValue,
                                      String reason)
    {
        super(source, type, oldValue, newValue);
        this.reason = reason;
    }

    /**
     * Returns the type of this event.
     * @return a string containing one of the following values:
     *  CALL_PARTICIPANT_STATUS_CHANGE, CALL_PARTICIPANT_DISPLAY_NAME_CHANGE,
     *  CALL_PARTICIPANT_ADDRESS_CHANGE, CALL_PARTICIPANT_IMAGE_CHANGE
     */
    public String getEventType()
    {
        return getPropertyName();
    }

    /**
     * Returns a String representation of this CallParticipantChangeEvent.
     *
     * @return  A a String representation of this CallParticipantChangeEvent.
     */
    public String toString()
    {

        return "CallParticipantChangeEvent: type="+getEventType()
            + " oldV="+getOldValue()
            + " newV="+getNewValue()
            + " for participant=" + getSourceCallParticipant();
    }

    /**
     * Returns the <tt>CallParticipant</tt> that this event is about.
     *
     * @return a reference to the <tt>CallParticipant</tt> that is the source
     * of this event.
     */
    public CallParticipant getSourceCallParticipant()
    {
        return (CallParticipant)getSource();
    }

    /**
     * Returns a reason string further explaining the event (may be null). The
     * string would be mostly used for events issued upon a CallParticipantState
     * transition that has led to a FAILED state.
     *
     * @return a reason string further explaining the event or null if no reason
     * was set.
     */
    public String getReasonString()
    {
        return reason;
    }


}

