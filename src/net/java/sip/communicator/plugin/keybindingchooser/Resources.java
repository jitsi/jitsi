/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.keybindingchooser;

import java.io.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 * 
 * @author Yana Stamcheva
 */
public class Resources
{
    private static Logger logger = Logger.getLogger(Resources.class);

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        return KeybindingChooserActivator.getResources().getI18NString(key);
    }

    /**
     * Loads an image from a given image identifier.
     * 
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImageInBytes(String imageID)
    {
        InputStream in =
            KeybindingChooserActivator.getResources().getImageInputStream(
                imageID);

        if (in == null)
            return null;

        byte[] image = null;

        try
        {
            image = new byte[in.available()];
            in.read(image);
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + imageID, e);
        }

        return image;
    }
}
