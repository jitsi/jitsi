/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.text.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * The Messages class manages the access to the internationalization properties
 * files.
 *
 * @author Yana Stamcheva
 */
public class Resources
{
    private static Logger log = Logger.getLogger(Resources.class);

    private static final String RESOURCE_LOCATION
        = "resources.languages.plugin.branding.resources";

    private static final ResourceBundle resourceBundle 
        = ResourceBundle.getBundle(RESOURCE_LOCATION);

    private static final String COLOR_BUNDLE_NAME
        = "resources.colors.colorResources";

    private static final ResourceBundle COLOR_RESOURCE_BUNDLE
        = ResourceBundle.getBundle(COLOR_BUNDLE_NAME);

    /**
     * Returns an internationalized string corresponding to the given key.
     *
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        try
        {
            return resourceBundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            return '!' + key + '!';
        }
    }
    
    /**
     * Returns an internationalized string corresponding to the given key,
     * by replacing all occurences of '?' with the given string param.
     * @param key The key of the string.
     * @param params the params, that should replace {1}, {2}, etc. in the
     * string given by the key parameter 
     * @return An internationalized string corresponding to the given key,
     * by replacing all occurences of {#number} with the given string param.
     */
    public static String getString(String key, String[] params)
    {
        String resourceString;

        try
        {
            resourceString = resourceBundle.getString(key);

            resourceString = MessageFormat.format(
                resourceString, (Object[]) params);

        }
        catch (MissingResourceException e)
        {
            log.error("Missing string resource.", e);

            resourceString = '!' + key + '!';
        }

        return resourceString;
    }

    /**
     * Returns an int RGB color corresponding to the given key.
     *
     * @param key The key of the string.
     *
     * @return an int RGB color corresponding to the given key.
     */
    public static String getColor(String key)
    {
        try
        {
            return COLOR_RESOURCE_BUNDLE.getString(key);
        }
        catch (MissingResourceException e)
        {
            log.error("Missing color resource.", e);

            return "FFFFFF";
        }
    }
}
