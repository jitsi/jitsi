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
 * The BrandingResources class manages the access to the
 * brandingResources.properties file.
 *
 * @author Yana Stamcheva
 */
public class BrandingResources
{
    private static final String RESOUCRE_LOCATION
        = "net.java.sip.communicator.plugin.branding.brandingResources";

    private static final ResourceBundle resourceBundle 
        = ResourceBundle.getBundle(RESOUCRE_LOCATION);

    private static final String APPLICATION_RESUORCE_LOCATION
        = "resources.application";

    private static final ResourceBundle applicationBundle 
        = ResourceBundle.getBundle(APPLICATION_RESUORCE_LOCATION);

    /**
     * Returns the resource string corresponding to the given key.
     *
     * @param key The key of the string.
     * @return the resource string corresponding to the given key
     */
    public static String getResourceString(String key)
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
     * Returns the application property string corresponding to the given key.
     *
     * @param key The key of the string.
     * @return the application property string corresponding to the given key
     */
    public static String getApplicationString(String key)
    {
        try
        {
            return applicationBundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            return '!' + key + '!';
        }
    }
}