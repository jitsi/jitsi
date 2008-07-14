/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.guicustomization;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

public class GuiCustomizationActivator implements BundleActivator
{
    private Logger logger = Logger.getLogger(GuiCustomizationActivator.class);

    static BundleContext bundleContext;

    private static ResourceManagementService resourceService;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;
        
//        CustomizationWindow customizationWindow
//            = new CustomizationWindow();
//
//        customizationWindow.pack();
//        customizationWindow.setSize(600, 500);
//        customizationWindow.setVisible(true);
    }

    public void stop(BundleContext bc) throws Exception
    {
    }

    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourceService = (ResourceManagementService) bundleContext
                .getService(serviceReference);
        }

        return resourceService;
    }
}