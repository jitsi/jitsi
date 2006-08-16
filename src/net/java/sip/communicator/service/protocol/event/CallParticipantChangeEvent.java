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
 * CALL_PARTICIPANT_ADDRESS_CHANGE - means that participant's address has changed.
 * <p>
 * CALL_PARTICIPANT_IMAGE_CHANGE - participant update photo.
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
    public static final String CALL_PARTICIPANT_STATUS_CHANGE =
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
     * change of the participant's photo/picture.
     */
    public static final String CALL_PARTICIPANT_IMAGE_CHANGE =
                                                   "CallParticipantImageChange";

    /**
     * Creates a CallParticipantChangeEvent with the specified source, type,
     * oldValue and newValue.
     * @param source the participant that produced the event.
     * @param type the type of the event (i.e. address change, state change etc.).
     * @param oldValue the value of the changed property before the event occurred
     * @param newValue current value of the changed property.
     */
    public CallParticipantChangeEvent(CallParticipant source, String type,
                                      Object oldValue, Object newValue)
    {
        super(source, type, oldValue, newValue);
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
}

