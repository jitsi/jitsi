/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.keybindingchooser.globalchooser;

import net.java.sip.communicator.plugin.keybindingchooser.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 *
 * @author Yana Stamcheva
 */
public class Resources
{
    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        return KeybindingChooserActivator.getResources()
            .getI18NString(key);
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The key of the string.
     * @param params additionnal parameters
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key, String[] params)
    {
        return KeybindingChooserActivator.getResources()
            .getI18NString(key, params);
    }
}
