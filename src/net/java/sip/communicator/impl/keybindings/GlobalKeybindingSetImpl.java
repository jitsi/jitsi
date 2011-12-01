/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
