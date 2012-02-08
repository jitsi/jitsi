/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * An event class representing that an <tt>Call</tt> or <tt>CallPeer</tt> is
 * added/removed to/from a <tt>CallGroup</tt>.
 *
 * @author Sebastien Vincent
 */
public class CallGroupEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * CALLGROUP_CALL_ADDED event name.
     */
    public static final int CALLGROUP_CALL_ADDED = 0;

    /**
     * CALLGROUP_CALL_REMOVED event name.
     */
    public static final int CALLGROUP_CALL_REMOVED = 1;

    /**
     * The id indicating the type of this event.
     */
    private final int eventID;

    /**
     * Constructor.
     *
     * @param call call source
     * @param eventID event ID
     */
    public CallGroupEvent(Call call, int eventID)
    {
        super(call);
        this.eventID = eventID;
    }

    /**
     * Returns one of the CALLGROUP_XXX member ints indicating
     * the type of this event.
     * @return one of the CALLGROUP_XXX member ints indicating
     * the type of this event.
     */
    public int getEventID()
    {
        return this.eventID;
    }

    /**
     * Returns the source call.
     *
     * @return The source call
     */
    public Call getSourceCall()
    {
        return (Call)getSource();
    }
}
