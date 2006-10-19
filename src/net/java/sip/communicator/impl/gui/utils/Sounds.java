/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.util.*;

/**
 * Manages the access to the properties file containing all sounds paths.
 * 
 * @author Yana Stamcheva
 */
public class Sounds {

    private static final String BUNDLE_NAME 
        = "net.java.sip.communicator.impl.gui.utils.sounds";

    private static final ResourceBundle RESOURCE_BUNDLE 
        = ResourceBundle.getBundle(BUNDLE_NAME);

    private Sounds() {
    }

    /**
     * Returns a sound path corresponding to the given sound key.
     * @param key The key of the sound.
     * @return A sound path corresponding to the given sound key.
     */
    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);

        } catch (MissingResourceException e) {

            return '!' + key + '!';
        }
    }
}
