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
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 * 
 * @author Ryan Ricard
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
        return MailboxActivator.getResources().getI18NString(key);
    }

    /**
     * Loads an image from a given image identifier.
     *
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImageInBytes(String imageID)
    {
        logger.debug("Loading imageID=" + imageID);

        try
        {
            InputStream in = 
                MailboxActivator.getResources().getImageInputStream(imageID);
            byte[] image = new byte[in.available()];
            in.read(image);
            
            return image;
        }
        catch (IOException e)
        {
            logger.error("Failed to load image:" + imageID, e);
        }

        return null;
    }
}