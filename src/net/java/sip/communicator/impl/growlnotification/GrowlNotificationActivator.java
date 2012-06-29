/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.growlnotification;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activates the GrowlNotificationService
 *
 * @author Romain Kuntz
 * @author Egidijus Jankauskas
 */
public class GrowlNotificationActivator
    implements BundleActivator
{
    /**
     * The bundle context in which we started
     */
    public static BundleContext bundleContext;

    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>GrowlNotificationActivator</tt> class and its instances for logging
     * output.
     */
    private static final Logger logger =
        Logger.getLogger(GrowlNotificationActivator.class);

    /**
     * A reference to the configuration service.
     */
    private static ConfigurationService configService;

    /**
     * A reference to the resource management service.
     */
    private static ResourceManagementService resourcesService;

    /**
     * A reference to the Growl notification service
     */
    private static GrowlNotificationServiceImpl handler;

    /**
     * Initialize and start Growl Notifications Service
     *
     * @param bc BundleContext
     * @throws Exception if initializing and starting this service fails
     */
    public void start(BundleContext bc) throws Exception
    {
        if (logger.isInfoEnabled())
            logger.info("Growl Notification ...[Starting]");
        bundleContext  = bc;

        getConfigurationService();

        handler = new GrowlNotificationServiceImpl();

        if (handler.isGrowlInstalled() && handler.isGrowlRunning())
        {
            handler.start(bc);
            bc.registerService(PopupMessageHandler.class.getName(), handler, null);
        } else
        {
            if (logger.isInfoEnabled())
                logger.info("Growl Notification ...[Aborted]");
            return;
        }

        if (logger.isInfoEnabled())
            logger.info("Growl Notification ...[Started]");
    }

    /**
     * Stops this bundle.
     *
     * @param bContext the <tt>BundleContext</tt>
     * @throws Exception if the stop operation goes wrong
     */
    public void stop(BundleContext bContext) throws Exception
    {
        handler.stop(bContext);
        if (logger.isInfoEnabled())
            logger.info("Growl Notification Service ...[Stopped]");
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
     * Returns the <tt>ResourceManagementService</tt> obtained from the bundle
     * context.
     * @return the <tt>ResourceManagementService</tt> obtained from the bundle
     * context
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }
}
