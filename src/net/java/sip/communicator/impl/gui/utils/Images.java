/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.utils;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Manages the access to the properties file containing all image paths.
 * @author Yana Stamcheva
 */
public class Images {

    private static final String BUNDLE_NAME 
        = "net.java.sip.communicator.impl.gui.utils.images";

    private static final ResourceBundle RESOURCE_BUNDLE 
        = ResourceBundle.getBundle(BUNDLE_NAME);

    private Images() {
    }

    public static String getString(String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);

        } catch (MissingResourceException e) {

            return '!' + key + '!';
        }
    }
}
