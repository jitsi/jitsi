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
package net.java.sip.communicator.service.keybindings;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * The <tt>KeybindingService</tt> handles the distribution of configurable and
 * persistent keybinding sets.
 * @author Damian Johnson
 */
public interface KeybindingsService
{
    /**
     * Provides the bindings associated with a given category. This may be null
     * if the default bindings failed to be loaded.
     * @param category segment of the UI for which bindings should be retrieved
     * @return mappings of keystrokes to the string representation of their
     *         actions
     */
    KeybindingSet getBindings(KeybindingSet.Category category);

    /**
     * Provides the bindings associated with the global category.
     *
     * @return global keybinding set
     */
    GlobalKeybindingSet getGlobalBindings();

    /**
     * Returns list of global shortcuts from the configuration file.
     *
     * @return list of global shortcuts.
     */
    public Map<String, List<AWTKeyStroke>> getGlobalShortcutFromConfiguration();

    /**
     * Save the configuration file.
     */
    public void saveGlobalShortcutFromConfiguration();
}
