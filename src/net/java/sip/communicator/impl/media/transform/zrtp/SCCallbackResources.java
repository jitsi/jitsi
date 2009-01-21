/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.transform.zrtp;

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
//    private static Hashtable loadedImages = new Hashtable();
    
//    public static final String WARNING_NO_RS_MATCH
//        = getString("impl.media.security.WARNING_NO_RS_MATCH");
//    public static final String WARNING_NO_EXPECTED_RS_MATCH
//        = getString("impl.media.security.WARNING_NO_EXPECTED_RS_MATCH");
//    
//    public static String TOGGLE_OFF_SECURITY
//        = getString("impl.media.security.TOGGLE_OFF_SECURITY");
//    
//    public static String TOGGLE_ON_SECURITY
//        = getString("impl.media.security.TOGGLE_ON_SECURITY");
//    
//    public static String SAS_SECURED_MESSAGE
//        = getString("impl.media.security.SECURED_MESSAGE");
//    
//    public static String SAS_SECURED_TOOLTIP
//        = getString("impl.media.security.SECURED_TOOLTIP");
//    
//    public static String SAS_NOT_SECURED_MESSAGE
//        = getString("impl.media.security.NOT_SECURED_MESSAGE");
//    
//    public static String SAS_NOT_SECURED_TOOLTIP
//        = getString("impl.media.security.NOT_SECURED_TOOLTIP");
//    
//    public static String DEFAULT_SAS_TOOLTIP
//        = getString("impl.media.security.DEFAULT_TOOLTIP");
//    
//    public static String DEFAULT_SAS_MESSAGE
//        = getString("impl.media.security.DEFAULT_MESSAGE");
//    
//    public static String ENGINE_FAIL_SAS_TOOLTIP
//        = getString("impl.media.security.ENGINE_FAIL_TOOLTIP");
//    
//    public static String ENGINE_FAIL_SAS_MESSAGE
//        = getString("impl.media.security.ENGINE_FAIL_MESSAGE");
//    
//    public static String SAS_SECURING_FAIL_TOOLTIP
//        = getString("impl.media.security.SECURING_FAIL_TOOLTIP");
//    
//    public static String SAS_UNSECURED_AT_REQUEST_TOOLTIP
//        = getString("impl.media.security.UNSECURED_AT_REQUEST");
//    
//    public static String PEER_UNSUPORTED_SECURITY
//        = getString("impl.media.security.PEER_UNSUPPORTED_SECURITY");
//    
//    public static String SAS_PEER_UNSUPORTED_TOOLTIP
//        = getString("impl.media.security.PEER_UNSUPPORTED_TOOLTIP");
//    
//    public static String PEER_TOGGLED_SECURITY_OFF_MESSAGE
//        = getString("impl.media.security.PEER_TOGGLED_OFF_SECURITY_MESSAGE");
//    
//    public static String PEER_TOGGLED_SECURITY_OFF_CAPTION
//        = getString("impl.media.security.PEER_TOGGLED_OFF_SECURITY_SECTION");
//    
//    public static String SAS_UNSECURED_AT_PEER_REQUEST_TOOLTIP
//        = getString("impl.media.security.UNSECURED_AT_PEER_REQUEST_TOOLTIP");
//    
//    public static String ZRTP_ENGINE_INIT_FAILURE
//        = getString("impl.media.security.ENGINE_INIT_FAILURE");
//    
//    public static String GOCLEAR_REQUEST_AC_FLAG_FAILURE
//        = getString("impl.media.security.ALLOW_CLEAR_REQUEST_FAILURE");
    
    /**
     * The icon on the "Secure" button in the <tt>QuickMenu</tt>.
     */
//    public static final ImageID SECURE_ON_ICON
//       = new ImageID("SECURE_BUTTON_ON");

    /**
     * The icon on the "Secure" button in the <tt>QuickMenu</tt>.
     */
//    public static final ImageID SECURE_OFF_ICON
//        = new ImageID("SECURE_BUTTON_OFF");

//    /**
//     * The resource management service used to get the needed
//     * resources: button images, text predefined messages, etc
//     */
//    private static ResourceManagementService resourcesService;
//    
//    /**
//     * Returns an internationalized string corresponding to the given key.
//     * 
//     * @param key The key of the string.
//     * @return An internationalized string corresponding to the given key.
//     */
//    public static String getString(String key)
//    {
//        return getResources().getI18NString(key);
//    }
//    
//    /**
//     * Provides the resource management service
//     * 
//     * @return the resource management service
//     */
//    public static ResourceManagementService getResources()
//    {
//        if (resourcesService == null)
//        {
//            ServiceReference serviceReference = MediaActivator.getBundleContext()
//                .getServiceReference(ResourceManagementService.class.getName());
//
//            if(serviceReference == null)
//                return null;
//            
//            resourcesService = 
//                (ResourceManagementService)MediaActivator.getBundleContext()
//                    .getService(serviceReference);
//        }
//
//        return resourcesService;
//    }   
//    
    /**
     * Method for getting the images used by the ZRTP GUI plugin
     *  
     * @param imageID the component's image ID
     * @return the buffered image
     */
//    public static BufferedImage getImage(ImageID imageID)
//    {
//        BufferedImage image = null;
//
//        if (loadedImages.containsKey(imageID))
//        {
//            image = (BufferedImage) loadedImages.get(imageID);
//        }
//        else
//        {
//            URL path = getResources().getImageURL(imageID.getId());
//
//            if (path == null)
//            {
//                return null;
//            }
//
//            try
//            {
//                image = ImageIO.read(path);
//
//                loadedImages.put(imageID, image);
//            }
//            catch (Exception exc)
//            {
//                exc.printStackTrace();
//            }
//        }
//
//        return image;
//    }
}
