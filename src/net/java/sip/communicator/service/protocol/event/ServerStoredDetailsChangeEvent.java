package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * Instances of this class represent a change in the server stored details
 * change that triggered them.
 *
 * @author Damian Minkov
 */
public class ServerStoredDetailsChangeEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that the ServerStoredDetailsChangeEvent instance was triggered
     * by adding a new detail.
     */
    public static final int DETAIL_ADDED = 1;

    /**
     * Indicates that the ServerStoredDetailsChangeEvent instance was triggered
     * by the removal of an existing detail.
     */
    public static final int DETAIL_REMOVED = 2;

    /**
     * Indicates that the ServerStoredDetailsChangeEvent instance was triggered
     * by the fact a detail was replaced with new value.
     */
    public static final int DETAIL_REPLACED  = 3;

    /**
     * The event type id.
     */
    private final int eventID;

    /**
     * New value for property.  May be null if not known.
     * @serial
     */
    private Object newValue;

    /**
     * Previous value for property.  May be null if not known.
     * @serial
     */
    private Object oldValue;

    /**
     * Constructs a ServerStoredDetailsChangeEvent.
     *
     * @param source The object on which the Event initially occurred.
     * @param eventID the event ID
     * @param oldValue old value
     * @param newValue new value
     * @throws IllegalArgumentException if source is null.
     */
    public ServerStoredDetailsChangeEvent(
            ProtocolProviderService source,
            int eventID,
            Object oldValue,
            Object newValue)
    {
        super(source);

        this.eventID = eventID;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Returns the provider that has generated this event
     * @return the provider that generated the event.
     */
    public ProtocolProviderService getProvider()
    {
        return (ProtocolProviderService)getSource();
    }

    /**
     * Gets the new value for the event, expressed as an Object.
     *
     * @return  The new value for the event, expressed as an Object.
     */
    public Object getNewValue()
    {
        return newValue;
    }

    /**
     * Gets the old value for the event, expressed as an Object.
     *
     * @return  The old value for the event, expressed as an Object.
     */
    public Object getOldValue()
    {
        return oldValue;
    }

    /**
     * Returns the event type id.
     *
     * @return the event ID
     */
    public int getEventID()
    {
        return eventID;
    }
}
