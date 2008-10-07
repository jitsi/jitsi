/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.profiler4j;

import java.util.*;
import org.osgi.framework.*;
import net.java.sip.communicator.service.resources.*;

/**
 * The Messages class manages the access to the internationalization properties
 * files.
 * 
 * @author Vladimir Skarupelov;
 */
public class Resources
{
    private static ResourceManagementService resourcesService = null;

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
    
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = ProfilerActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if (serviceReference == null) {
                return null;
            }
            
            resourcesService = 
            	(ResourceManagementService) ProfilerActivator.bundleContext
                    .getService(serviceReference);
        }

        return resourcesService;
    }
}
