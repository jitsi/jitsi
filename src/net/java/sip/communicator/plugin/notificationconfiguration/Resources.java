/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

import java.util.*;

import net.java.sip.communicator.service.resources.*;

/**
 * The Messages class manages the access to the internationalization
 * properties files.
 * @author Yana Stamcheva
 */
public class Resources 
{
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
    public static byte[] getImageInBytes(String imageID)
    {
        return getResources().getImageInBytes(imageID);
    }

    private static ResourceManagementService getResources()
    {
        /*
         * TODO If the method is called more than once, the trend seems to be
         * caching the value.
         */
        return ResourceManagementServiceUtils
            .getService(NotificationConfigurationActivator.bundleContext);
    }
}
