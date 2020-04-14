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

import java.beans.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * CallChangeEvent-s are triggered whenever a change occurs in a Call.
 * Dispatched events may be of one of the following types.
 * <p>
 * CALL_STATE_CHANGE - indicates a change in the state of a Call.
 * <p>
 * @author Emil Ivov
 */
public class CallChangeEvent
    extends PropertyChangeEvent
{
    /**
     * The type of <tt>CallChangeEvent</tt> which indicates that the state of
     * the associated <tt>Call</tt> has changed.
     */
    public static final String CALL_STATE_CHANGE = "CallState";

    /**
     * The type of <tt>CallChangeEvent</tt> which indicates that there was some
     * kind of change in the participants in the associated <tt>Call</tt> (e.g.
     * a <tt>CallPeer</tt> participating in the <tt>Call</tt> has enabled
     * or disabled video)
     */
    public static final String CALL_PARTICIPANTS_CHANGE
            = "CallParticipantsChanged";

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>CallPeerChangeEvent</tt>, if any, which is the cause for this
     * <tt>CallChangeEvent</tt> to be fired. For example, when the last
     * <tt>CallPeer</tt> in a <tt>Call</tt> is disconnected, the <tt>Call</tt>
     * will end.
     */
    private final CallPeerChangeEvent cause;

    /**
     * Creates a CallChangeEvent with the specified source, type, oldValue and
     * newValue.
     * @param source the peer that produced the event.
     * @param type the type of the event (the name of the property that has
     * changed).
     * @param oldValue the value of the changed property before the event
     * occurred
     * @param newValue current value of the changed property.
     */
    public CallChangeEvent(Call source, String type,
                                      Object oldValue, Object newValue)
    {
        this(source, type, oldValue, newValue, null);
    }

    /**
     * Creates a CallChangeEvent with the specified source, type, oldValue and
     * newValue.
     * @param source the peer that produced the event.
     * @param type the type of the event (the name of the property that has
     * changed).
     * @param oldValue the value of the changed property before the event
     * occurred
     * @param newValue current value of the changed property.
     * @param cause the event that causes this event, if any(null otherwise).
     */
    public CallChangeEvent(Call source, String type,
                            Object oldValue, Object newValue,
                            CallPeerChangeEvent cause)
    {
        super(source, type, oldValue, newValue);

        this.cause = cause;
    }

    /**
     * The event which was the cause for current event, like last peer
     * removed from call will hangup current call, if any, otherwise is null.
     *
     * @return <tt>CallPeerChangeEvent</tt> that represents the cause
     */
    public CallPeerChangeEvent getCause()
    {
        return cause;
    }

    /**
     * Returns the type of this event.
     * @return a string containing the name of the property whose change this
     * event is reflecting.
     */
    public String getEventType()
    {
        return getPropertyName();
    }

    /**
     * The Call on which the event has occurred.
     *
     * @return   The Call on which the event has occurred.
     */
    public Call getSourceCall()
    {
        return (Call) getSource();
    }

    /**
     * Returns a String representation of this CallChangeEvent.
     *
     * @return  A a String representation of this CallChangeEvent.
     */
    @Override
    public String toString()
    {
        return
            "CallChangeEvent: type="
                + getEventType()
                + " oldV="
                + getOldValue()
                + " newV="
                + getNewValue();
    }
}

