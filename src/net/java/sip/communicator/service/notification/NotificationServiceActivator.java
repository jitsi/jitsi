/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

import org.jitsi.service.configuration.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>NotificationActivator</tt> is the activator of the notification
 * bundle.
 * 
 * @author Yana Stamcheva
 */
public class NotificationServiceActivator
    implements BundleActivator
{
    private final Logger logger
        = Logger.getLogger(NotificationServiceActivator.class);

    protected static BundleContext bundleContext;
    private static ConfigurationService configService;
    private ServiceRegistration notificationService;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        try
        {
            logger.logEntry();
            logger.info("Notification Service...[  STARTED ]");

            notificationService = bundleContext.registerService(
                NotificationService.class.getName(),
                new NotificationServiceImpl(),
                null);

            logger.info("Notification Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }
    }

    public void stop(BundleContext bc) throws Exception
    {
        notificationService.unregister();
        logger.info("Notification Service ...[STOPPED]");
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }
}
