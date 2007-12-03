/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.text.*;
import java.util.*;

/**
 * The Messages class manages the access to the internationalization properties
 * files.
 *
 * @author Yana Stamcheva
 */
public class Resources
{
    private static final String RESOUCRE_LOCATION
        = "net.java.sip.communicator.plugin.branding.resources";

    private static final ResourceBundle resourceBundle 
        = ResourceBundle.getBundle(RESOUCRE_LOCATION);

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
            resourceString = '!' + key + '!';
        }

        return resourceString;
    }
}
