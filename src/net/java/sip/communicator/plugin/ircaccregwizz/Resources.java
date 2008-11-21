/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ircaccregwizz;

import java.io.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The Messages class manages the access to the internationalization
 * properties files.
 *
 * @author Lionel Ferreira & Michael Tarantino
 */
public class Resources
{

    private static Logger log = Logger.getLogger(Resources.class);

    private static ResourceManagementService resourcesService;
    
    /**
     * A constant pointing to the IRC protocol logo image.
     */
    public static ImageID IRC_LOGO = new ImageID("protocolIconIrc");

    /**
     * A constant pointing to the IRC protocol wizard page image.
     */
    public static ImageID PAGE_IMAGE = new ImageID("pageImageIrc");

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
        InputStream in = 
            getResources().getImageInputStream(imageID.getId());
        
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
    
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = IrcAccRegWizzActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;
            
            resourcesService = 
                (ResourceManagementService)IrcAccRegWizzActivator.bundleContext
                    .getService(serviceReference);
        }

        return resourcesService;
    }
}
