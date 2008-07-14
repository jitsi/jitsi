/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.zeroconfaccregwizz;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The Resources class manages the access to the internationalization
 * properties files and the images properties file.
 * 
 * @author Christian Vincenot
 * @author Maxime Catelin
 */
public class Resources
{

    private static Logger log = Logger.getLogger(Resources.class);

    private static ResourceManagementService resourcesService;
    
    public static ImageID ZEROCONF_LOGO = new ImageID("protocolIconZeroconf");
    
    public static ImageID PAGE_IMAGE = new ImageID("pageImageZeroconf");
    
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

    /**
     * Represents the Image Identifier.
     */
    public static class ImageID
    {
        private String id;

        private ImageID(String id)
        {
            this.id = id;
        }

        /**
         * Returns the user ID of this account
         * @return user ID
         */
        public String getId()
        {
            return id;
        }
    }
    
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = ZeroconfAccRegWizzActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;
            
            resourcesService = 
                (ResourceManagementService)ZeroconfAccRegWizzActivator.bundleContext
                    .getService(serviceReference);
        }

        return resourcesService;
    }
}
