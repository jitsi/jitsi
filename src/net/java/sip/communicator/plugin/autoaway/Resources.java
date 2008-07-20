/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.autoaway;

import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.*;
import net.java.sip.communicator.service.resources.*;

import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * The Messages class manages the access to the internationalization properties
 * files.
 * 
 * @author Thomas Hofer;
 */
public class Resources
{
    private static Logger logger = Logger.getLogger(Resources.class);
    
    private static ResourceManagementService resourcesService;

    /**
     * Returns an internationalized string corresponding to the given key.
     * 
     * @param key The key of the string.
     * 
     * @return An internationalized string corresponding to the given key.
     */
    public static String getString(String key)
    {
        return getResources().getI18NString(key);
    }
    
    /**
     * Returns a reference to the resrouce management service.
     * 
     * @return a reference to the resource management service.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = AutoAwayActivator.bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;
            
            resourcesService = 
                (ResourceManagementService)AutoAwayActivator.bundleContext
                    .getService(serviceReference);
        }

        return resourcesService;
    }
}
