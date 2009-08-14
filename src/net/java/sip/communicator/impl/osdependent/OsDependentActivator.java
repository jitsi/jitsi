/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.osdependent;

import net.java.sip.communicator.impl.osdependent.jdic.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.desktop.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>Systray</tt> in the UI Service.
 *
 * @author Nicolas Chamouard
 * @author Lubomir Marinov
 */
public class OsDependentActivator
    implements BundleActivator
{
    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    public static UIService uiService;

    private static ConfigurationService configService;

    private static ResourceManagementService resourcesService;

    private static final Logger logger =
        Logger.getLogger(OsDependentActivator.class);

    /**
     * Called when this bundle is started.
     *
     * @param bc The execution context of the bundle being started.
     * @throws Exception If
     */
    public void start(BundleContext bc)
            throws Exception
    {
        bundleContext = bc;

        try {
            // Create the notification service implementation
            SystrayService systrayService = new SystrayServiceJdicImpl();

            logger.info("Systray Service...[  STARTED ]");

            bundleContext.registerService(
                    SystrayService.class.getName(),
                    systrayService,
                    null);

            logger.info("Systray Service ...[REGISTERED]");

            // Create the desktop service implementation
            DesktopService desktopService = new DesktopServiceImpl();

            logger.info("Desktop Service...[  STARTED ]");

            bundleContext.registerService(
                    DesktopService.class.getName(),
                    desktopService,
                    null);

            logger.info("Desktop Service ...[REGISTERED]");

            logger.logEntry();
        }
        finally {
            logger.logExit();
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bc The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext bc)
            throws Exception
    {
    }


    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null) {
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }

    /**
     * Gets a reference to a <code>ShutdownService</code> implementation
     * currently registered in the bundle context of the active
     * <code>OsDependentActivator</code> instance.
     * <p>
     * The returned reference to <code>ShutdownService</code> is not being
     * cached.
     * </p>
     * 
     * @return reference to a <code>ShutdownService</code> implementation
     *         currently registered in the bundle context of the active
     *         <code>OsDependentActivator</code> instance
     */
    public static ShutdownService getShutdownService()
    {
        return
            (ShutdownService)
                bundleContext.getService(
                    bundleContext.getServiceReference(
                        ShutdownService.class.getName()));
    }
    
    /**
     * Returns the <tt>UIService</tt> obtained from the bundle
     * context.
     * @return the <tt>UIService</tt> obtained from the bundle
     * context
     */
    public static UIService getUIService()
    {
        if(uiService == null)
        {
            ServiceReference serviceRef = bundleContext
                .getServiceReference(UIService.class.getName());

            if (serviceRef != null)
                uiService = (UIService) bundleContext.getService(serviceRef);
        }

        return uiService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }
}
