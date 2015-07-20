/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * WhiteboardParticipantChangeEvent-s are triggerred wheneve a change occurs in
 * a WhiteboardParticipant. Dispatched events may be of one of the following
 * types.
 * <p>
 * WHITEBOARD_PARTICIPANT_STATUS_CHANGE - indicates a change in the status of
 * the participant.
 * <p>
 * WHITEBOARD_PARTICIPANT_DISPLAY_NAME_CHANGE - means that participant's
 * display name has changed
 * <p>
 * WHITEBOARD_PARTICIPANT_IMAGE_CHANGE - participant updated photo.
 * <p>
 *
 * @author Julien Waechter
 * @author Emil Ivov
 */
public class WhiteboardParticipantChangeEvent
        extends java.beans.PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the WhiteboardParticipant's status.
     */
    public static final String WHITEBOARD_PARTICIPANT_STATE_CHANGE =
            "WhiteboardParticipantStatusChange";

    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the participant's display name.
     */
    public static final String WHITEBOARD_PARTICIPANT_DISPLAY_NAME_CHANGE =
            "WhiteboardParticipantDisplayNameChange";

    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the participant's photo/picture.
     */
    public static final String WHITEBOARD_PARTICIPANT_IMAGE_CHANGE =
            "WhiteboardParticipantImageChange";

    /**
     * A reason string further explaining the event (may be null). The string
     * would be mostly used for events issued upon a WhiteboardParticipantState
     * transition that has led to a FAILED state.
     */
    private String reason = null;

    /**
     * Creates a WhiteboardParticipantChangeEvent with the specified source,
     * type, oldValue and newValue.
     *
     * @param source the participant that produced the event.
     * @param type the type of the event
     * (i.e. address change, state change etc.).
     * @param oldValue the value of the changed property
     * before the event occurred
     * @param newValue current value of the changed property.
     */
    public WhiteboardParticipantChangeEvent(WhiteboardParticipant source,
                                            String type,
                                            Object oldValue,
                                            Object newValue)
    {
        this(source, type, oldValue, newValue, null);
    }

    /**
     * Creates a WhiteboardParticipantChangeEvent with the specified source,
     * type, oldValue and newValue.
     *
     * @param source the participant that produced the event.
     * @param type the type of the event (i.e. address change, state change
     * etc.).
     * @param oldValue the value of the changed property before the event
     * occurred
     * @param newValue current value of the changed property.
     * @param reason a string containing a human readable explanation for the
     * reason that triggerred this event (may be null).
     */
    public WhiteboardParticipantChangeEvent(WhiteboardParticipant source,
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
     *
     * @return a string containing one of the following values:
     * WHITEBOARD_PARTICIPANT_STATUS_CHANGE,
     * WHITEBOARD_PARTICIPANT_DISPLAY_NAME_CHANGE,
     * WHITEBOARD_PARTICIPANT_ADDRESS_CHANGE,
     * WHITEBOARD_PARTICIPANT_IMAGE_CHANGE
     */
    public String getEventType()
    {
        return getPropertyName();
    }

    /**
     * Returns a String representation of this WhiteboardParticipantChangeEvent.
     *
     * @return  A a String representation of
     * this WhiteboardParticipantChangeEvent.
     */
    @Override
    public String toString()
    {
        return "WhiteboardParticipantChangeEvent: type="+getEventType()
        + " oldV="+getOldValue()
        + " newV="+getNewValue()
        + " for participant=" + getSourceWhiteboardParticipant();
    }

    /**
     * Returns the <tt>WhiteboardParticipant</tt> that this event is about.
     *
     * @return a reference to the <tt>WhiteboardParticipant</tt> that
     * is the source of this event.
     */
    public WhiteboardParticipant getSourceWhiteboardParticipant()
    {
        return (WhiteboardParticipant)getSource();
    }

    /**
     * Returns a reason string further explaining the event (may be null).
     * The string would be mostly used for events issued upon a
     * WhiteboardParticipantState transition that has led to a FAILED state.
     *
     * @return a reason string further explaining the event or null
     * if no reason was set.
     */
    public String getReasonString()
    {
        return reason;
    }
}
