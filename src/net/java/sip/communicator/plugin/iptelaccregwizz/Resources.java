/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.iptelaccregwizz;

import net.java.sip.communicator.service.resources.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 *
 * @author Yana Stamcheva
 */
public class Resources
{
    private static ResourceManagementService resourcesService;
    
    /**
     * A constant pointing to the IP Tel protocol logo image.
     */
    public static final ImageID PROTOCOL_ICON
        = new ImageID("service.protocol.iptel.IPTEL_16x16");

    /**
     * A constant pointing to the IP Tel protocol wizard page image.
     */
    public static final ImageID PAGE_IMAGE
        = new ImageID("service.protocol.iptel.IPTEL_64x64");

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
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * @return An internationalized string corresponding to the given key.
     */
    public static char getMnemonic(String key)
    {
        return getResources().getI18nMnemonic(key);
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

    /**
     * Returns the resource for the given key. This could be any resource stored
     * in the resources.properties file of this bundle.
     * 
     * @param key the key of the resource to search for
     * @return the resource for the given key
     */
    public static String getProperty(String key)
    {
        return getResources().getI18NString(key);
    }

    /**
     * Returns the service giving access to resources.
     * @return the service giving access to resources
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService = ResourceManagementServiceUtils
                .getService(IptelAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
