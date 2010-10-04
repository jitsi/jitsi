/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.event;

import java.util.*;

public class ContainerEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * ID of the event.
     */
    private int eventID = -1;

    /**
     * Indicates that the ContainerEvent instance was triggered by
     * adding a new container to the list of supported containers.
     */
    public static final int CONTAINER_ADDED = 1;

    /**
     * Indicates that the ContainerEvent instance was triggered by the
     * removal of an existing container from the list of supported containers.
     */
    public static final int CONTAINER_REMOVED = 2;

    /**
     * Creates a new ContainerEvent according to the specified parameters.
     * @param source The containerID of the container that is added to supported
     * containers.
     * @param eventID one of the CONTAINER_XXX static fields indicating the
     * nature of the event.
     */
    public ContainerEvent(Object source, int eventID)
    {
        super(source);
        this.eventID = eventID;
    }

    /**
     * Returns an event id specifying whether the type of this event
     * (CONTAINER_ADDED or CONTAINER_REMOVED)
     * @return one of the CONTAINER_XXX int fields of this class.
     */
    public int getEventID()
    {
        return eventID;
    }
}
