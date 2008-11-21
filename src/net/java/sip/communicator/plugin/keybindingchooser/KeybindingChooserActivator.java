/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.keybindingchooser;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Enabling and disabling osgi functionality for the keybinding chooser.
 * 
 * @author Damian Johnson
 */
public class KeybindingChooserActivator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(KeybindingChooserActivator.class);

    private static BundleContext bundleContext;

    public static ResourceManagementService resourcesService;

    /**
     * Called when this bundle is started.
     * 
     * @param context The execution context of the bundle being started.
     */
    public void start(BundleContext context)
    {
        bundleContext = context;

        logger.debug("Service Impl: " + getClass().getName() + " [  STARTED ]");

        ServiceReference keybindingRef =
            context.getServiceReference(KeybindingsService.class.getName());

        KeybindingsService keybingingsService =
            (KeybindingsService) context.getService(keybindingRef);

        KeybindingsConfigForm keybindingsManager =
            new KeybindingsConfigForm(keybingingsService);

        context.registerService(ConfigurationForm.class.getName(),
            keybindingsManager, null);
    }

    /**
     * Stops this bundles.
     */
    public void stop(BundleContext arg0) throws Exception
    {
    }

    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }
}
