/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.event;

import java.util.*;

import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>PluginComponentEvent</tt> 
 * @author Yana Stamcheva
 */
public class PluginComponentEvent
    extends EventObject
{
    private final int eventID;

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
     * Creates a new PluginComponentEvent according to the specified
     * parameters.
     * @param pluginComponent The pluginComponent that is added to the container.
     * @param eventID one of the PLUGIN_COMPONENT_XXX static fields indicating
     * the nature of the event.
     */
    public PluginComponentEvent(PluginComponent pluginComponent,
                                int eventID)
    {
        super(pluginComponent);

        this.eventID = eventID;
    }

    /**
     * Returns the <tt>PluginComponent</tt> associated with this event.
     * @return the <tt>PluginComponent</tt> associated with this event
     */
    public PluginComponent getPluginComponent()
    {
        return (PluginComponent) getSource();
    }

    /**
     * Returns an event id specifying whether the type of this event 
     * (PLUGIN_COMPONENT_ADDED or PLUGIN_COMPONENT_REMOVED)
     * 
     * @return one of the PLUGIN_COMPONENT_XXX int fields of this class.
     */
    public int getEventID()
    {
        return eventID;
    }
}
