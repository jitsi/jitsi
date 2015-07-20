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
     * @param pluginComponentProvider the <tt>PluginComponentFactory</tt> about
     * which the new instance is to notify
     * @param eventID one of the <tt>PLUGIN_COMPONENT_XXX</tt> constants defined
     * by the <tt>PluginComponentEvent</tt> class which indicates the very
     * nature of the event that the new instance is to represent
     */
    public PluginComponentEvent(
            PluginComponentFactory pluginComponentProvider,
            int eventID)
    {
        super(pluginComponentProvider);

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
     * Returns the <tt>PluginComponentFactory</tt> associated with this event.
     *
     * @return the <tt>PluginComponentFactory</tt> associated with this event
     */
    public PluginComponentFactory getPluginComponentFactory()
    {
        return (PluginComponentFactory) getSource();
    }
}
