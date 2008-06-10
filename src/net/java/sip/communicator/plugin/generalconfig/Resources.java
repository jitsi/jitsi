/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.generalconfig;

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
        = "resources.languages.plugin.generalconfig.resources";

    /**
     * The name of the resource, where paths to images used in this bundle are
     * stored.
     */
    private static final String IMAGE_RESOURCE_NAME
        = "net.java.sip.communicator.plugin.generalconfig.resources";
    
    /**
     * The name of the resource, where application properties are
     * stored.
     */
    private static final String APPLICATION_RESUORCE_LOCATION
        = "resources.application";

    /**
     * The application resource bundle.
     */
    private static final ResourceBundle APPLICATION_RESOURCE_BUNDLE 
        = ResourceBundle.getBundle(APPLICATION_RESUORCE_LOCATION);

    /**
     * The string resource bundle.
     */
    private static final ResourceBundle STRING_RESOURCE_BUNDLE
        = ResourceBundle.getBundle(STRING_RESOURCE_NAME);

    /**
     * The image resource bundle.
     */
    private static final ResourceBundle IMAGE_RESOURCE_BUNDLE
        = ResourceBundle.getBundle(IMAGE_RESOURCE_NAME);

    /**
     * Returns an internationalized string corresponding to the given key.
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        try
        {
            return STRING_RESOURCE_BUNDLE.getString(key);
        }
        catch (MissingResourceException e)
        {
            return '!' + key + '!';
        }
    }
    
    /**
     * Returns an application property string corresponding to the given key.
     * @param key The key of the string.
     * @return A string corresponding to the given key.
     */
    public static String getApplicationString(String key)
    {
        try
        {
            return APPLICATION_RESOURCE_BUNDLE.getString(key);
        }
        catch (MissingResourceException e)
        {
            return '!' + key + '!';
        }
    }

    /**
     * Loads an image from a given image identifier.
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImage(String imageId)
    {
        byte[] image = new byte[100000];

        String path = IMAGE_RESOURCE_BUNDLE.getString(imageId);
        try
        {
            Resources.class.getClassLoader()
                    .getResourceAsStream(path).read(image);
        }
        catch (IOException e)
        {
            log.error("Failed to load image:" + path, e);
        }

        return image;
    }
}
