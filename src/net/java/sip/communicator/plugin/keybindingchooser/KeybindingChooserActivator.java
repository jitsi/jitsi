/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.keybindingchooser;

import net.java.sip.communicator.service.gui.*;
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

        if (logger.isDebugEnabled())
            logger.debug("Service Impl: " + getClass().getName() + " [  STARTED ]");

        context
            .registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin.keybindingchooser.KeybindingsConfigPanel",
                    getClass().getClassLoader(),
                    "plugin.keybinding.PLUGIN_ICON",
                    "plugin.keybindings.PLUGIN_NAME",
                    900),
                null);
    }

    /**
     * Stops this bundles.
     */
    public void stop(BundleContext arg0) throws Exception
    {
    }

    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }
}
