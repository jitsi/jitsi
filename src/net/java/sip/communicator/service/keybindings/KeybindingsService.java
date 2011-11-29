/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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