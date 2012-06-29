/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.zeroconfaccregwizz;

import net.java.sip.communicator.service.resources.*;

import org.jitsi.service.resources.*;

/**
 * The Resources class manages the access to the internationalization
 * properties files and the images properties file.
 *
 * @author Christian Vincenot
 * @author Maxime Catelin
 */
public class Resources
{
    private static ResourceManagementService resourcesService;

    public static ImageID ZEROCONF_LOGO
        = new ImageID("service.protocol.zeroconf.ZEROCONF_16x16");

    public static ImageID PAGE_IMAGE
        = new ImageID("service.protocol.zeroconf.ZEROCONF_64x64");

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
    public static byte[] getImage(ImageID imageID)
    {
        return getResources().getImageInBytes(imageID.getId());
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
                    .getService(ZeroconfAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
