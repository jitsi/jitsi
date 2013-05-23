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
 * Implements an <tt>EventObject</tt> related to a <tt>PluginComponent</tt>.
 *
 * @author Yana Stamcheva
 */
public class PluginComponentEvent
    extends EventObject
{
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
     * The ID of this event which is one of the <tt>PLUGIN_COMPONENT_XXX</tt>
     * constants defined by the <tt>PluginComponentEvent</tt> class.
     */
    private final int eventID;

    /**
     * Initializes a new <tt>PluginComponentEvent</tt> instance which is to
     * notify about a specific <tt>PluginComponent</tt> and which is to be of a
     * nature indicated by a specific ID.
     *
     * @param pluginComponent the <tt>PluginComponent</tt> about which the new
     * instance is to notify
     * @param eventID one of the <tt>PLUGIN_COMPONENT_XXX</tt> constants defined
     * by the <tt>PluginComponentEvent</tt> class which indicates the very
     * nature of the event that the new instance is to represent
     */
    public PluginComponentEvent(
            PluginComponent pluginComponent,
            int eventID)
    {
        super(pluginComponent);

        this.eventID = eventID;
    }

    /**
     * Returns the ID of this event which is one of
     * {@link #PLUGIN_COMPONENT_ADDED} and {@link #PLUGIN_COMPONENT_REMOVED}.
     *
     * @return the ID of this event which is one of the
     * <tt>PLUGIN_COMPONENT_XXX</tt> constants defined by the
     * <tt>PluginComponentEvent</tt> class
     */
    public int getEventID()
    {
        return eventID;
    }

    /**
     * Returns the <tt>PluginComponent</tt> associated with this event.
     *
     * @return the <tt>PluginComponent</tt> associated with this event
     */
    public PluginComponent getPluginComponent()
    {
        return (PluginComponent) getSource();
    }
}
