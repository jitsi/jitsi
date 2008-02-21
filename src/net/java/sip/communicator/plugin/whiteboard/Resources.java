/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.whiteboard;

import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;

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
        = "resources.languages.plugin.whiteboard.resources";

    /**
     * The name of the resource, where paths to images used in this bundle are
     * stored.
     */
    private static final String IMAGE_RESOURCE_NAME
        = "net.java.sip.communicator.plugin.whiteboard.resources";

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
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        try
        {
            String resourceString = STRING_RESOURCE_BUNDLE.getString(key);

            int mnemonicIndex = resourceString.indexOf('&');

            if(mnemonicIndex > -1)
            {
                String firstPart = resourceString.substring(0, mnemonicIndex);
                String secondPart = resourceString.substring(mnemonicIndex + 1);

                resourceString = firstPart.concat(secondPart);
            }

            return resourceString;
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
     * by replacing all occurences of '?' with the given string param.
     */
    public static String getString(String key, String[] params)
    {
        try
        {
            String resourceString = STRING_RESOURCE_BUNDLE.getString(key);

            resourceString = MessageFormat.format(
                resourceString, (Object[]) params);

            int mnemonicIndex = resourceString.indexOf('&');

            if(mnemonicIndex > -1)
            {
                String firstPart = resourceString.substring(0, mnemonicIndex);
                String secondPart = resourceString.substring(mnemonicIndex + 1);

                resourceString = firstPart.concat(secondPart);
            }

            return resourceString;
        }
        catch (MissingResourceException e)
        {
            return '!' + key + '!';
        }
    }

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static char getMnemonic(String key)
    {
        try
        {
            String resourceString = STRING_RESOURCE_BUNDLE.getString(key);

            int mnemonicIndex = resourceString.indexOf('&');

            if(mnemonicIndex > -1)
                return resourceString.charAt(mnemonicIndex + 1);
        }
        catch (MissingResourceException e)
        {
            return 0;
        }

        return 0;
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
            image =
                ImageIO.read(Resources.class.getClassLoader()
                    .getResourceAsStream(path));
        }
        catch (IOException e)
        {
            log.error("Failed to load image:" + path, e);
        }

        return new ImageIcon(image);
    }
}