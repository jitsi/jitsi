/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.swing;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.resources.*;

import org.osgi.framework.*;

public class SwingCommonActivator
    implements BundleActivator
{
    private static BundleContext bundleContext;

    private static ConfigurationService configurationService;

    private static KeybindingsService keybindingsService;

    private static ResourceManagementService resources;

    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            ServiceReference serviceReference =
                bundleContext.getServiceReference(ConfigurationService.class
                    .getName());

            if (serviceReference != null)
                configurationService =
                    (ConfigurationService) bundleContext
                        .getService(serviceReference);
        }
        return configurationService;
    }

    public static KeybindingsService getKeybindingsService()
    {
        if (keybindingsService == null)
        {
            ServiceReference serviceReference =
                bundleContext.getServiceReference(KeybindingsService.class
                    .getName());

            if (serviceReference != null)
                keybindingsService =
                    (KeybindingsService) bundleContext
                        .getService(serviceReference);
        }
        return keybindingsService;
    }

    public static ResourceManagementService getResources()
    {
        if (resources == null)
            resources =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resources;
    }

    public void start(BundleContext bundleContext)
    {
        SwingCommonActivator.bundleContext = bundleContext;
    }

    public void stop(BundleContext bundleContext)
    {
        if (SwingCommonActivator.bundleContext == bundleContext)
            SwingCommonActivator.bundleContext = null;
    }
}
