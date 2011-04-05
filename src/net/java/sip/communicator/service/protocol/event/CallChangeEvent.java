/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

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
    extends java.beans.PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the Call state.
     */
    public static final String CALL_STATE_CHANGE = "CallState";

    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the Call he became focus for a conference call or was one
     * and is no longer such.
     */
    public static final String CALL_FOCUS_CHANGE = "CallFocusState";

    /**
     * The event which was the cause for current event, like last peer
     * removed from call will hangup current call, if any, otherwise is null.
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
     * Returns the type of this event.
     * @return a string containing the name of the property whose change this
     * event is reflecting.
     */
    public String getEventType()
    {
        return getPropertyName();
    }

    /**
     * Returns a String representation of this CallChangeEvent.
     *
     * @return  A a String representation of this CallChangeEvent.
     */
    public String toString()
    {

        return "CallChangeEvent: type="+getEventType()
            + " oldV="+getOldValue()
            + " newV="+getNewValue();
    }

    /**
     * The Call on which the event has occurred.
     *
     * @return   The Call on which the event has occurred.
     */
    public Call getSourceCall()
    {
        return (Call)getSource();
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
}

