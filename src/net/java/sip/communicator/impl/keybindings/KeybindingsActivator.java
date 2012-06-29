/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.keybindings;

import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Enabling and disabling osgi functionality for keybindings.
 *
 * @author Damian Johnson
 */
public class KeybindingsActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>KeybindingsActivator</tt> class and its instances for logging
     * output.
     */
    private static final Logger logger =
        Logger.getLogger(KeybindingsActivator.class);

    /**
     * The <tt>KeybindingsService</tt> reference.
     */
    private KeybindingsServiceImpl keybindingsService = null;

    /**
     * Reference to the configuration service
     */
    private static ConfigurationService configService = null;

    /**
     * The resource service.
     */
    private static ResourceManagementService resourceService = null;

    /**
     * OSGi bundle context.
     */
    private static BundleContext bundleContext = null;

    /**
     * Called when this bundle is started.
     *
     * @param context The execution context of the bundle being started.
     */
    public void start(BundleContext context)
    {
        if (this.keybindingsService == null)
        {
            bundleContext = context;

            if (logger.isDebugEnabled())
                logger.debug("Service Impl: " + getClass().getName()
                + " [  STARTED ]");
            this.keybindingsService = new KeybindingsServiceImpl();
            this.keybindingsService.start(context);
            context.registerService(KeybindingsService.class.getName(),
                this.keybindingsService, null);
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
    {
        if (this.keybindingsService != null)
        {
            this.keybindingsService.stop();
            this.keybindingsService = null;
        }
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigService()
    {
        if(configService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                        ConfigurationService.class.getName());
            configService
                = (ConfigurationService) bundleContext.getService(
                        confReference);
        }
        return configService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> obtained from the
     * bundle context.
     *
     * @return the <tt>ResourceManagementService</tt> obtained from the
     * bundle context
     */
    public static ResourceManagementService getResourceService()
    {
        if (resourceService == null)
        {
            ServiceReference resourceReference
                = bundleContext.getServiceReference(
                    ResourceManagementService.class.getName());

            resourceService =
                (ResourceManagementService) bundleContext
                    .getService(resourceReference);
        }

        return resourceService;
    }
}
