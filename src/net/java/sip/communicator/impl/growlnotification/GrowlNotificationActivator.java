/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.growlnotification;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.systray.*;

import org.osgi.framework.*;

import net.java.sip.communicator.util.*;

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
     * @throws Exception
     */
    public void start(BundleContext bc) throws Exception
    {
        logger.info("Growl Notification ...[Starting]");
        bundleContext  = bc;
        
        getConfigurationService();

        handler = new GrowlNotificationServiceImpl();
        
        if (handler.isGrowlInstalled())
        {
            handler.start(bc);
            bc.registerService(PopupMessageHandler.class.getName(), handler, null);
        } else 
        {
            logger.info("Growl Notification ...[Aborted]");
            return;
        }
        
        logger.info("Growl Notification ...[Started]");
    }

    public void stop(BundleContext bContext) throws Exception
    {
        handler.stop(bContext);
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
