/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.i18n;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
/**
 * The Messages class manages the access to the internationalization
 * properties files.
 * @author Yana Stamcheva
 */
public class Messages {
    private static final String BUNDLE_NAME 
        = "net.java.sip.communicator.impl.gui.main.i18n.messages";

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle(BUNDLE_NAME);

    /**
     * Returns an internationalized string corresponding to the given key.
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);

        } catch (MissingResourceException e) {

            return '!' + key + '!';
        }
    }

    /**
     * Returns an internationalized string corresponding to the given key,
     * by replacing all occurences of '?' with the given string param.
     * @param key The key of the string.
     * @param param The param, which will replace '?'.
     * @return An internationalized string corresponding to the given key,
     * by replacing all occurences of '?' with the given string param.
     */
    public static String getString(String key, String param) {
        try {
            String sourceString = RESOURCE_BUNDLE.getString(key);

            return sourceString.replaceAll("\\?", param);

        } catch (MissingResourceException e) {

            return '!' + key + '!';
        }
    }
}
