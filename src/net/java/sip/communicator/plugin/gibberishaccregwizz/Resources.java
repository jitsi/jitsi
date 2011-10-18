/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.gibberishaccregwizz;

import net.java.sip.communicator.service.resources.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 * 
 * @author Emil Ivov
 */
public class Resources
{
    private static ResourceManagementService resourcesService;

    /**
     * A constant pointing to the Gibberish protocol logo icon.
     */
    public static ImageID GIBBERISH_LOGO
        = new ImageID("service.protocol.gibberish.GIBBERISH_16x16");

    /**
     * A constant pointing to the Gibberish protocol wizard page image.
     */
    public static ImageID PAGE_IMAGE
        = new ImageID("service.protocol.gibberish.GIBBERISH_64x64");

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        return getResources().getI18NString(key);
    }

    /**
     * Loads an image from a given image identifier.
     * 
     * @param imageID The identifier of the image.
     * @return The image for the given identifier.
     */
    public static byte[] getImage(ImageID imageID)
    {
        return getResources().getImageInBytes(imageID.getId());
    }
    
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils
                    .getService(GibberishAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
