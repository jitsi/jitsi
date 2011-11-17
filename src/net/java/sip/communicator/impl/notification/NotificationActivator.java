/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.resources.*;
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

    protected static BundleContext bundleContext;
    
    private static ConfigurationService configService;
    
    private static AudioNotifierService audioNotifierService;
    
    private static SystrayService systrayService;

    private static NotificationService notificationService;

    private static ResourceManagementService resourcesService;

    private static UIService uiService = null;

    private static MediaService mediaService;
    
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        try {
            // Create the notification service implementation
            notificationService = new NotificationServiceImpl();

            if (logger.isInfoEnabled())
                logger.info("Notification Service...[  STARTED ]");

            bundleContext.registerService(NotificationService.class.getName(),
                notificationService, null);

            new NotificationManager().init();

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

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        return notificationService;
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
        {
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
    }

    /**
     * Returns the current implementation of the <tt>UIService</tt>.
     * @return the current implementation of the <tt>UIService</tt>
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        }

        return uiService;
    }

    /**
     * Returns an instance of the <tt>MediaService</tt> obtained from the
     * bundle context.
     * @return an instance of the <tt>MediaService</tt> obtained from the
     * bundle context
     */
    public static MediaService getMediaService()
    {
        if (mediaService == null)
        {
            mediaService
                = ServiceUtils.getService(bundleContext, MediaService.class);
        }
        return mediaService;
    }
}
