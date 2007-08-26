/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.mailbox;

import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.util.*;

/**
 * The Messages class manages the access to the internationalization properties
 * files.
 *
 * @author Ryan Ricard
 */
public class Resources
{
    private static Logger logger = Logger.getLogger(Resources.class);

    private static final String RESOUCRE_LOCATION
        = "net.java.sip.communicator.plugin.mailbox.resources";

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
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static ImageIcon getImage(String imageID) 
    {
        BufferedImage image = null;

        String path = Resources.getString(imageID);
        try
        {
            image = ImageIO.read(Resources.class.getClassLoader()
                    .getResourceAsStream(path));
        }
        catch (IOException exc)
        {
            logger.error("Failed to load image:" + path, exc);
        }

        return new ImageIcon(image);
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImageInBytes(String imageID)
    {
        byte[] image = new byte[100000];

        String path = Resources.getString(imageID);
        
        logger.debug("Loading imageID=" + imageID + " from path=" + path);

        try
        {
            Resources.class.getClassLoader()
                    .getResourceAsStream(path).read(image);
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + path, e);
        }

        return image;
    }
}