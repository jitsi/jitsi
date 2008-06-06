/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.simpleaccreg;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 * 
 * @author Yana Stamcheva
 */
public class Resources
{

    private static Logger log = Logger.getLogger(Resources.class);

    /**
     * The name of the resource, where internationalization strings for this
     * plugin are stored.
     */
    private static final String STRING_RESOURCE_NAME
        = "resources.languages.plugin.simpleaccreg.resources";

    /**
     * The string resource bundle.
     */
    private static final ResourceBundle STRING_RESOURCE_BUNDLE
        = ResourceBundle.getBundle(STRING_RESOURCE_NAME);

    /**
     * Name of the bundle where we will search for color resources.
     */
    private static final String COLOR_BUNDLE_NAME
        = "resources.colors.colorResources";

    /**
     * Bundle which handle access to localized resources.
     */
    private static final ResourceBundle COLOR_RESOURCE_BUNDLE = ResourceBundle
            .getBundle( COLOR_BUNDLE_NAME,
                        Locale.getDefault(),
                        Resources.class.getClassLoader());

    /**
     * Name of the bundle where we will search for color resources.
     */
    private static final String LOGIN_BUNDLE_NAME
        = "resources.login";

    /**
     * Bundle which handle access to localized resources.
     */
    private static final ResourceBundle LOGIN_PROPERTIES_BUNDLE = ResourceBundle
            .getBundle(LOGIN_BUNDLE_NAME);
    
    /**
     * Name of the bundle where we will search for application resources.
     */
    private static final String APPLICATION_RESUORCE_LOCATION
        = "resources.application";

    /**
     * Bundle which handle access to application resources.
     */
    private static final ResourceBundle applicationBundle 
        = ResourceBundle.getBundle(APPLICATION_RESUORCE_LOCATION);
    
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
            return STRING_RESOURCE_BUNDLE.getString(key);
        }
        catch (MissingResourceException exc)
        {
            return '!' + key + '!';
        }
    }

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
            return Integer.parseInt(COLOR_RESOURCE_BUNDLE.getString(key), 16);
        }
        catch (MissingResourceException e)
        {
            log.error("Missing color resource.", e);

            return 0xFFFFFF;
        }
    }

    /**
     * Returns the application property corresponding to the given key.
     *
     * @param key The key of the string.
     *
     * @return the application property corresponding to the given key
     */
    public static String getLoginProperty(String key)
    {
        try
        {
            return LOGIN_PROPERTIES_BUNDLE.getString(key);
        }
        catch (MissingResourceException e)
        {
            log.error("Missing property.", e);

            return "";
        }
    }
    
    /**
     * Returns the application property corresponding to the given key.
     *
     * @param key The key of the string.
     *
     * @return the application property corresponding to the given key
     */
    public static String getApplicationProperty(String key)
    {
        try
        {
            return applicationBundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            log.error("Missing property.", e);

            return "";
        }
    }
}
