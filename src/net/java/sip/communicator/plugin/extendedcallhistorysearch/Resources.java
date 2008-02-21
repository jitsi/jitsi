/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.extendedcallhistorysearch;

import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files, image resources and color resources used in this plugin.
 * 
 * @author Yana Stamcheva
 */
public class Resources {

    private static Logger log = Logger.getLogger(Resources.class);

    /**
     * The name of the resource, where internationalization strings for this
     * plugin are stored.
     */
    private static final String STRING_RESOURCE_NAME
        = "resources.languages.plugin.extendedcallhistorysearch.resources";

    /**
     * The name of the resource, where paths to images used in this bundle are
     * stored.
     */
    private static final String IMAGE_RESOURCE_NAME
        = "net.java.sip.communicator.plugin.extendedcallhistorysearch.resources";

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

    private static final String COLOR_BUNDLE_NAME
        = "resources.colors.colorResources";

    private static final ResourceBundle COLOR_RESOURCE_BUNDLE
        = ResourceBundle.getBundle(COLOR_BUNDLE_NAME);

    /**
     * Returns an internationalized string corresponding to the given key.
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        String resourceString;

        try
        {
            resourceString = STRING_RESOURCE_BUNDLE.getString(key);

            int mnemonicIndex = resourceString.indexOf('&');

            if(mnemonicIndex > -1)
            {
                String firstPart = resourceString.substring(0, mnemonicIndex);
                String secondPart = resourceString.substring(mnemonicIndex + 1);
                
                resourceString = firstPart.concat(secondPart);
            }
        }        
        catch (MissingResourceException e)
        {
            resourceString = '!' + key + '!';
        }

        return resourceString;
    }
    
    /**
     * Loads an image from a given image identifier.
     * 
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static ImageIcon getImage(String imageID)
    {
        BufferedImage image = null;

        String path = IMAGE_RESOURCE_BUNDLE.getString(imageID);

        try
        {
            image = ImageIO.read(Resources.class.getClassLoader()
                    .getResourceAsStream(path));

        }
        catch (IOException e)
        {
            log.error("Failed to load image:" + path, e);
        }

        return new ImageIcon(image);
    }
    
    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static char getMnemonic(String key)
    {
        String resourceString;
        try
        {
            resourceString = STRING_RESOURCE_BUNDLE.getString(key);

            int mnemonicIndex = resourceString.indexOf('&');

            if(mnemonicIndex > -1)
            {
                return resourceString.charAt(mnemonicIndex + 1);
            }
        }
        catch (MissingResourceException e)
        {
            return '!';
        }
        return '!';
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
}
