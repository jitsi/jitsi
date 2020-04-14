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
package net.java.sip.communicator.service.gui.event;

import java.util.*;

/**
 * The <tt>ContainerEvent</tt> indicates that a change in a <tt>container</tt>
 * such a <tt>Comonent</tt> added or removed.
 */
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
