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

/**
 * Listens for all events caused by adding or removing of a plugin component.
 *
 * @author Yana Stamcheva
 */
public interface PluginComponentListener
    extends EventListener
{
    /**
     * Indicates that a plugin component has been successfully added
     * to the container.
     *
     * @param event the PluginComponentEvent containing the corresponding
     * plugin component
     */
    public void pluginComponentAdded(PluginComponentEvent event);

    /**
     * Indicates that a plugin component has been successfully removed
     * from the container.
     *
     * @param event the PluginComponentEvent containing the corresponding
     * plugin component
     */
    public void pluginComponentRemoved(PluginComponentEvent event);
}
