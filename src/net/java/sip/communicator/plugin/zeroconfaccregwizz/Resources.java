/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.zeroconfaccregwizz;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * The Resources class manages the access to the internationalization
 * properties files and the images properties file.
 * 
 * @author Christian Vincenot
 * @author Maxime Catelin
 */
public class Resources
{

    private static Logger log = Logger.getLogger(Resources.class);

    /**
     * The name of the resource, where internationalization strings for this
     * plugin are stored.
     */
    private static final String STRING_RESOURCE_NAME
        = "resources.languages.plugin.zeroconfaccregwizz.resources";

    /**
     * The name of the resource, where paths to images used in this bundle are
     * stored.
     */
    private static final String IMAGE_RESOURCE_NAME
        = "net.java.sip.communicator.plugin.zeroconfaccregwizz.resources";

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

    public static ImageID ZEROCONF_LOGO = new ImageID("protocolIcon");
    
    public static ImageID PAGE_IMAGE = new ImageID("pageImage");
    
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
        catch (MissingResourceException exc)
        {
            return '!' + key + '!';
        }
    }

    /**
     * Loads an image from a given image identifier.
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImage(ImageID imageID)
    {
        byte[] image = new byte[100000];

        String path = IMAGE_RESOURCE_BUNDLE.getString(imageID.getId());
        try
        {
            Resources.class.getClassLoader()
                .getResourceAsStream(path).read(image);

        }
        catch (IOException exc)
        {
            log.error("Failed to load image:" + path, exc);
        }

        return image;
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

        /**
         * Returns the user ID of this account
         * @return user ID
         */
        public String getId()
        {
            return id;
        }
    }
}
