/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.event;

import java.util.*;

/**
 * Listens for all events caused by a change in the supported containers list.
 *
 * @author Yana Stamcheva
 */
public interface ContainerListener
    extends EventListener {

    /**
     * Indicates that a container was added to the list of supported containers.
     * @param event the ContainerEvent containing the corresponding container.
     */
    public void containerAdded(ContainerEvent event);

    /**
     * Indicates that a container was removed from the list of supported
     * containers.
     * @param event the ContainerEvent containing the corresponding container.
     */
    public void containerRemoved(ContainerEvent event);
}
