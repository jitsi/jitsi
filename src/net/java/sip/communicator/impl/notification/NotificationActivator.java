/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>NotificationActivator</tt> is the activator of the notification
 * bundle.
 * 
 * @author Yana Stamcheva
 */
public class NotificationActivator
    implements BundleActivator
{
    private final Logger logger = Logger.getLogger(NotificationActivator.class);

    private static BundleContext bundleContext;
    
    private static ConfigurationService configService;
    
    private static AudioNotifierService audioNotifierService;
    
    private static SystrayService systrayService;
    
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        try {
            // Create the notification service implementation
            NotificationService notificationService =
                new NotificationServiceImpl();

            if (logger.isInfoEnabled())
                logger.info("Notification Service...[  STARTED ]");

            bundleContext.registerService(NotificationService.class.getName(),
                notificationService, null);

            if (logger.isInfoEnabled())
                logger.info("Notification Service ...[REGISTERED]");
            
            logger.logEntry();
        }
        finally {
            logger.logExit();
        }
    }

    public void stop(BundleContext bc) throws Exception
    {
        if (logger.isInfoEnabled())
            logger.info("UI Service ...[STOPPED]");
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
    
    /**
     * Returns the <tt>AudioNotifierService</tt> obtained from the bundle
     * context.
     * @return the <tt>AudioNotifierService</tt> obtained from the bundle
     * context
     */
    public static AudioNotifierService getAudioNotifier()
    {
        if (audioNotifierService == null)
        {
            ServiceReference serviceReference
                = bundleContext
                    .getServiceReference(AudioNotifierService.class.getName());

            if (serviceReference != null)
                audioNotifierService
                    = (AudioNotifierService)
                        bundleContext.getService(serviceReference);
        }

        return audioNotifierService;
    }
    
    /**
     * Returns the <tt>SystrayService</tt> obtained from the bundle context.
     * 
     * @return the <tt>SystrayService</tt> obtained from the bundle context
     */
    public static SystrayService getSystray()
    {
        if (systrayService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(SystrayService.class.getName());

            systrayService = (SystrayService) bundleContext
                .getService(serviceReference);
        }

        return systrayService;
    }
}
