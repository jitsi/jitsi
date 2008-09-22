/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.zrtp;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.impl.media.*;

import org.osgi.framework.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 *  
 * @author Emanuel Onica 
 *
 */
public class SCCallbackResources
{
	/**
	 * Contains the secure button icons
	 */
	private static Hashtable loadedImages = new Hashtable();
	
    public static String TOGGLE_OFF_SECURITY
		= getString("toggleOffSecurity");
    
    public static String TOGGLE_ON_SECURITY
		= getString("toggleOnSecurity");
    
    public static String SAS_SECURED_MESSAGE
    	= getString("sasSecuredMessage");
    
    public static String SAS_SECURED_TOOLTIP
    	= getString("sasSecuredTooltip");
    
    public static String SAS_NOT_SECURED_MESSAGE
    	= getString("sasNotSecuredMessage");
    
    public static String SAS_NOT_SECURED_TOOLTIP
    	= getString("sasNotSecuredTooltip");
    
    public static String DEFAULT_SAS_TOOLTIP
		= getString("defaultSASTooltip");

    public static String DEFAULT_SAS_MESSAGE
		= getString("defaultSASMessage");
    
    public static String ENGINE_FAIL_SAS_TOOLTIP
		= getString("sasEngineFailTooltip");
    
    public static String ENGINE_FAIL_SAS_MESSAGE
		= getString("sasEngineFailMessage");
    
    public static String SAS_SECURING_FAIL_TOOLTIP
    	= getString("sasSecuringFailTooltip");
    
    public static String SAS_UNSECURED_AT_REQUEST_TOOLTIP
    	= getString("sasUnsecuredAtRequestTooltip");
    
    public static String PEER_UNSUPORTED_SECURITY
    	= getString("peerUnsuportedSecurity");
    
    public static String SAS_PEER_UNSUPORTED_TOOLTIP
    	= getString("sasPeerUnsuportedTooltip");
    
    public static String PEER_TOGGLED_SECURITY_OFF_MESSAGE
    	= getString("peerToggledOffSecurityMessage");
    
    public static String PEER_TOGGLED_SECURITY_OFF_CAPTION
    	= getString("peerToggledOffSecurityCaption");
    
    public static String SAS_UNSECURED_AT_PEER_REQUEST_TOOLTIP
    	= getString("sasUnsecuredAtPeerRequestTooltip");
    
    public static String ZRTP_ENGINE_INIT_FAILURE
		= getString("engineInitFailure");

    public static String GOCLEAR_REQUEST_AC_FLAG_FAILURE
		= getString("allowClearRequestFailure");
    
    /**
     * The icon on the "Secure" button in the <tt>QuickMenu</tt>.
     */
    public static final ImageID SECURE_ON_ICON
    = new ImageID("SECURE_BUTTON_ON");

    /**
     * The icon on the "Secure" button in the <tt>QuickMenu</tt>.
     */
    public static final ImageID SECURE_OFF_ICON
    = new ImageID("SECURE_BUTTON_OFF");
      
    /**
     * The resource management service used to get the needed
     * resources: button images, text predefined messages, etc
     */
    private static ResourceManagementService resourcesService;
    
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
     * Provides the resource management service
     * 
     * @return the resource management service
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = MediaActivator.getBundleContext()
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;
            
            resourcesService = 
                (ResourceManagementService)MediaActivator.getBundleContext()
                    .getService(serviceReference);
        }

        return resourcesService;
    }   
    
    /**
     * Method for getting the images used by the ZRTP GUI plugin
     *  
     * @param imageID the component's image ID
     * @return the buffered image
     */
    public static BufferedImage getImage(ImageID imageID)
    {
        BufferedImage image = null;

        if (loadedImages.containsKey(imageID))
        {
            image = (BufferedImage) loadedImages.get(imageID);
        }
        else
        {
            URL path = getResources().getImageURL(imageID.getId());

            if (path == null)
            {
                return null;
            }

            try
            {
                image = ImageIO.read(path);

                loadedImages.put(imageID, image);
            }
            catch (Exception exc)
            {
                exc.printStackTrace();
            }
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

        public String getId() 
        {
            return id;
        }
    }
}
