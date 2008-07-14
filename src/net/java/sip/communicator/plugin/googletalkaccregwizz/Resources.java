/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.googletalkaccregwizz;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.resources.*;

import org.osgi.framework.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 *
 * @author Lubomir Marinov
 */
public class Resources
{
    private static Logger log = Logger.getLogger(Resources.class);

    private static ResourceManagementService resourcesService;
    
    /**
     * A constant pointing to the Google Talk protocol logo image.
     */
    public static ImageID PROTOCOL_ICON = new ImageID("protocolIcon");

    /**
     * A constant pointing to the Aim protocol wizard page image.
     */
    public static ImageID PAGE_IMAGE = new ImageID("pageImage");

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
     * Represents the Image Identifier.
     */
    public static class ImageID
    {
        private String id;

        private ImageID(String id)
        {
            this.id = id;
        }

        public String getId()
        {
            return id;
        }
    }
    
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = GoogleTalkAccRegWizzActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;
            
            resourcesService = 
                (ResourceManagementService)GoogleTalkAccRegWizzActivator.bundleContext
                    .getService(serviceReference);
        }

        return resourcesService;
    }
}
