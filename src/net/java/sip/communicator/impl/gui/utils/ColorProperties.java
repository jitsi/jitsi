/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * Accesses the color resources saved in the colorResources.properties file.
 * 
 * @author Yana Stamcheva
 */
public class ColorProperties
{
    /**
     * Logger for this class.
     */
    private static Logger log = Logger.getLogger(ColorProperties.class);

    /**
     * Name of the bundle where we will search for color resources.
     */
    private static final String BUNDLE_NAME
        = "resources.colors.colorResources";

    /**
     * Bundle which handle access to localized resources.
     */
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
            .getBundle( BUNDLE_NAME,
                        Locale.getDefault(),
                        ColorProperties.class.getClassLoader());

    /**
     * Returns an int RGB color corresponding to the given key.
     *
     * @param key The key of the string.
     *
     * @return An internationalized string corresponding to the given key.
     */
    public static int getColor(String key)
    {
        try
        {
            return Integer.parseInt(RESOURCE_BUNDLE.getString(key), 16);
        }
        catch (MissingResourceException e)
        {
            log.error("Missing color resource.", e);

            return 0xFFFFFF;
        }
    }
}
