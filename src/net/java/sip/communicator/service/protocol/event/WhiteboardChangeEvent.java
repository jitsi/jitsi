/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>WhiteboardChangeEvent</tt>s are triggerred whenever a change occurs in a
 * Whiteboard. Dispatched events may be of one of the following types.
 * <p>
 * WHITEBOARD_STATE_CHANGE - indicates a change in the state of a Whiteboard.
 * <p>
 * @author Julien Waechter
 * @author Emil Ivov
 */
public class WhiteboardChangeEvent
    extends java.beans.PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * An event type indicating that the corresponding event is caused by a
     * change of the Whiteboard state.
     */
    public static final String WHITEBOARD_STATE_CHANGE = "WhiteboardState";

    /**
     * Creates a WhiteboardChangeEvent with the specified source, type, oldValue
     * and newValue.
     *
     * @param source the participant that produced the event.
     * @param type the type of the event (the name of the property that has
     * changed).
     * @param oldValue the value of the changed property before the event
     * occurred
     * @param newValue current value of the changed property.
     */
    public WhiteboardChangeEvent(WhiteboardSession source, String type,
                                      Object oldValue, Object newValue)
    {
        super(source, type, oldValue, newValue);
    }

    /**
     * Returns the type of this event.
     *
     * @return a string containing the name of the property whose change this
     * event is reflecting.
     */
    public String getEventType()
    {
        return getPropertyName();
    }

    /**
     * Returns a String representation of this WhiteboardChangeEvent.
     *
     * @return  A a String representation of this WhiteboardChangeEvent.
     */
    public String toString()
    {
        return "WhiteboardChangeEvent: type="+getEventType()
            + " oldV="+getOldValue()
            + " newV="+getNewValue();
    }

    /**
     * The Whiteboard on which the event has occurred.
     *
     * @return A reference to the <tt>Whiteboard</tt> on which the event has
     * occurred.
     */
    public WhiteboardSession getSourceWhiteboard()
    {
        return (WhiteboardSession)getSource();
    }

}
