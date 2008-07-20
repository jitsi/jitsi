/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.notificationconfiguration;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The Messages class manages the access to the internationalization
 * properties files.
 * @author Yana Stamcheva
 */
public class Resources {

    private static Logger log = Logger.getLogger(Resources.class);

    /**
     * Returns an internationalized string corresponding to the given key.
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        try
        {
            return getResources().getI18NString(key);

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
    public static ImageIcon getImage(String imageID)
    {
        URL imageURL = 
            getResources().getImageURL(imageID);

        if(imageURL == null)
            return null;

        return new ImageIcon(imageURL);
    }
    
    /**
     * Loads an image from a given image identifier.
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImageInBytes(String imageID)
    {
        InputStream in = 
            getResources().getImageInputStream(imageID);

        if(in == null)
            return null;

        byte[] image = null;

        try
        {
            image = new byte[in.available()];
            in.read(image);
        }
        catch (IOException e)
        {
            log.error("Failed to load image:" + imageID, e);
        }

        return image;
    }

    private static ResourceManagementService getResources()
    {
        ServiceReference serviceReference = NotificationConfigurationActivator
            .bundleContext.getServiceReference(
                ResourceManagementService.class.getName());

        if(serviceReference == null)
            return null;

        return (ResourceManagementService) NotificationConfigurationActivator
            .bundleContext.getService(serviceReference);
    }
}
