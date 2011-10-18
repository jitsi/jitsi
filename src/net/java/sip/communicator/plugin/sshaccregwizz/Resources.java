/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * Resources.java
 *
 * Created on 22 May, 2007, 8:53 AM
 *
 * SSH Suport in SIP Communicator - GSoC' 07 Project
 *
 */
package net.java.sip.communicator.plugin.sshaccregwizz;

import net.java.sip.communicator.service.resources.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 * 
 * @author Shobhit Jindal
 */
public class Resources
{
    private static ResourceManagementService resourcesService;

    public static ImageID SSH_LOGO
        = new ImageID("service.protocol.ssh.SSH_16x16");

    public static ImageID PAGE_IMAGE
        = new ImageID("service.protocol.ssh.SSH_64x64");

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
                    .getService(SSHAccRegWizzActivator.bundleContext);
        return resourcesService;
    }
}
