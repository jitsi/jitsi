/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.facebookaccregwizz;

import net.java.sip.communicator.service.resources.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 * 
 * @author Dai Zhiwei
 */
public class Resources
{
    private static ResourceManagementService resourcesService;

    /**
     * A constant pointing to the Facebook protocol logo icon.
     */
    public static final ImageID PROTOCOL_ICON
        = new ImageID("service.protocol.facebook.FACEBOOK_16x16");

    /**
     * A constant pointing to the Facebook protocol wizard page image.
     */
    public static final ImageID PAGE_IMAGE
        = new ImageID("service.protocol.facebook.FACEBOOK_48x48");

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
                    .getService(FacebookAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
