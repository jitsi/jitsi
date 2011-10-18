/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.keybindings;

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
}