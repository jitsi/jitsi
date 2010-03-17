/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.globalproxyconfig;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>Resources</tt> class manages the access to the internationalization
 * properties files and the image resources used in this plugin.
 * 
 * @author Atul Aggarwal
 * @author Damian Minkov
 */
public class Resources
{
    /**
     * Our logger.
     */
    private static Logger log = Logger.getLogger(Resources.class);

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourcesService;

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
     * Returns an application property string corresponding to the given key.
     * @param key The key of the string.
     * @return A string corresponding to the given key.
     */
    public static String getApplicationString(String key)
    {
        return getResources().getSettingsString(key);
    }

    /**
     * Returns the resource service.
     * @return
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = GlobalProxyPluginActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;
            
            resourcesService = 
                (ResourceManagementService)GlobalProxyPluginActivator.bundleContext
                    .getService(serviceReference);
        }

        return resourcesService;
    }
}
