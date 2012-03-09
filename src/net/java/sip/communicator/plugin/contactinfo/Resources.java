/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.contactinfo;

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 *
 * @author Yana Stamcheva
 */
public class Resources {

    private static Logger log = Logger.getLogger(Resources.class);

    private static ResourceManagementService resourcesService;

    /**
     * Returns an internationalized string corresponding to the given key.
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        return getResources().getI18NString(key);
    }

    /**
     * Loads an image from a given image identifier.
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static Image getImage(String imageID)
    {
        BufferedImage image = null;

        InputStream in =
            getResources().getImageInputStream(imageID);

        if(in == null)
            return null;

        try
        {
            image = ImageIO.read(in);
        }
        catch (IOException e)
        {
            log.error("Failed to load image:" + imageID, e);
        }

        return image;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils
                    .getService(ContactInfoActivator.bundleContext);
        return resourcesService;
    }
}
