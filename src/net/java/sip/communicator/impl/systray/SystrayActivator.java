/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.systray;

import net.java.sip.communicator.impl.systray.jdic.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>Systray</tt> in the UI Service.
 *
 * @author Nicolas Chamouard
 */
public class SystrayActivator
    implements BundleActivator
{
    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    public static UIService uiService;

    private static ConfigurationService configService;

    private static Logger logger = Logger.getLogger(
            SystrayActivator.class.getName());

    /**
     * Called when this bundle is started.
     *
     * @param bc The execution context of the bundle being started.
     * @throws Exception If
     */
    public void start(BundleContext bc) throws Exception
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
    public void stop(BundleContext bc) throws Exception {
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

            uiService = (UIService) bundleContext
                .getService(serviceRef);
        }

        return uiService;
    }
}
