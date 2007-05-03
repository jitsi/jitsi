/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.event;

import java.util.*;

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>PluginComponentEvent</tt> 
 * @author Yana Stamcheva
 */
public class PluginComponentEvent
    extends EventObject
{

    private int eventID = -1;

    /**
     * Indicates that the PluginComponentEvent instance was triggered by
     * adding a plugin component.
     */
    public static final int PLUGIN_COMPONENT_ADDED = 1;

    /**
     * Indicates that the MetaContactEvent instance was triggered by the
     * removal of an existing MetaContact.
     */
    public static final int PLUGIN_COMPONENT_REMOVED = 2;

    /**
     * The identifier of the container to which or from which the plugin
     * component is added or removed.
     */
    private ContainerID containerID;
    
    /**
     * Creates a new PluginComponentEvent according to the specified
     * parameters.
     * @param source The pluginComponent that is added to the container.
     * @param containerID The containerID pointing to the container where the
     * component is added.
     * @param eventID one of the PLUGIN_COMPONENT_XXX static fields indicating
     * the nature of the event.
     */
    public PluginComponentEvent(Object source, ContainerID containerID,
            int eventID)
    {
        super(source);
        this.eventID = eventID;
        this.containerID = containerID;
    }
    
    /**
     * Returns an event id specifying whether the type of this event 
     * (PLUGIN_COMPONENT_ADDED or PLUGIN_COMPONENT_REMOVED)
     * @return one of the PLUGIN_COMPONENT_XXX int fields of this class.
     */
    public int getEventID(){
        return eventID;
    }

    /**
     * Returns the identifier of the container, where the plugin component, which
     * is the source of this event is added or removed.
     * 
     * @return the identifier of the plugin container
     */
    public ContainerID getContainerID()
    {
        return containerID;
    }
}
