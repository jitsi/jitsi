/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.googletalkaccregwizz;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 *
 * @author Lubomir Marinov
 */
public class Resources
{
    private static Logger log = Logger.getLogger(Resources.class);

    /**
     * The name of the resource, where internationalization strings for this
     * plugin are stored.
     */
    private static final String STRING_RESOURCE_NAME =
        "resources.languages.plugin.googletalkaccregwizz.resources";

    /**
     * The name of the resource, where paths to images used in this bundle are
     * stored.
     */
    private static final String RESOURCE_NAME =
        "net.java.sip.communicator.plugin.googletalkaccregwizz.resources";

    /**
     * The string resource bundle.
     */
    private static final ResourceBundle STRING_RESOURCE_BUNDLE =
        ResourceBundle.getBundle(STRING_RESOURCE_NAME);

    /**
     * The image resource bundle.
     */
    private static final ResourceBundle RESOURCE_BUNDLE =
        ResourceBundle.getBundle(RESOURCE_NAME);

    /**
     * A constant pointing to the Google Talk protocol logo image.
     */
    public static ImageID PROTOCOL_ICON = new ImageID("protocolIcon");

    /**
     * A constant pointing to the Aim protocol wizard page image.
     */
    public static ImageID PAGE_IMAGE = new ImageID("pageImage");

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
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

            if (mnemonicIndex > -1)
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

            if (mnemonicIndex > -1)
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
     * Loads an image from a given image identifier.
     * 
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImage(ImageID imageID)
    {
        byte[] image = new byte[100000];

        String path = RESOURCE_BUNDLE.getString(imageID.getId());

        try
        {
            Resources.class.getClassLoader().getResourceAsStream(path).read(
                image);
        }
        catch (IOException e)
        {
            log.error("Failed to load image:" + path, e);
        }

        return image;
    }

    /**
     * Returns the resource for the given key. This could be any resource stored
     * in the resources.properties file of this bundle.
     * 
     * @param key the key of the resource to search for
     * @return the resource for the given key
     */
    public static String getProperty(String key)
    {
        try
        {
            return RESOURCE_BUNDLE.getString(key);
        }
        catch (MissingResourceException e)
        {
            return '!' + key + '!';
        }
    }

    /**
     * Represents the Image Identifier.
     */
    public static class ImageID
    {
        private String id;

        private ImageID(String id)
        {
            this.id = id;
        }

        public String getId()
        {
            return id;
        }
    }
}
