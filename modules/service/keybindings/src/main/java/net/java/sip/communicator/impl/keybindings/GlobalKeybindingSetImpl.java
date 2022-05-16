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
package net.java.sip.communicator.impl.keybindings;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.service.keybindings.*;

/**
 * Global keybinding set.
 *
 * @author Sebastien Vincent
 */
public class GlobalKeybindingSetImpl
    implements GlobalKeybindingSet
{
    /**
     * List of bindings (name and list of different keystroke that would
     * trigger the action).
     */
    private Map<String, List<AWTKeyStroke>> bindings = new
        LinkedHashMap<String, List<AWTKeyStroke>>();

    /**
     * Provides current keybinding mappings.
     * @return mapping of keystrokes to the string representation of the actions
     * they perform
     */
    public Map<String, List<AWTKeyStroke>> getBindings()
    {
        return new LinkedHashMap<String, List<AWTKeyStroke>>(this.bindings);
    }

    /**
     * Resets the bindings and notifies the observer's listeners if they've
     * changed.
     * @param bindings new keybindings to be held
     */
    public void setBindings(Map<String, List<AWTKeyStroke>> bindings)
    {
        if(!this.bindings.equals(bindings))
        {
            this.bindings.clear();
            this.bindings.putAll(bindings);
        }
    }
}
