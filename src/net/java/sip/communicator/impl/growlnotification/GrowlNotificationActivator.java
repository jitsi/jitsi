/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.growlnotification;

import org.osgi.framework.*;
import net.java.sip.communicator.util.*;

/**
 * Activates the GrowlNotificationService
 *
 * @author Romain Kuntz
 */
public class GrowlNotificationActivator
    implements BundleActivator
{
    private static Logger logger =
        Logger.getLogger(GrowlNotificationActivator.class);

    private GrowlNotificationServiceImpl growlNotificationService = null;

    /**
     * Initialize and start Growl Notifications Service
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        /* Create and start the Growl Notification service. */
        growlNotificationService = new GrowlNotificationServiceImpl();
        growlNotificationService.start(bundleContext);

        logger.info("Growl Notification Plugin ...[Started]");
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        logger.info("Growl Notification Service ...[Stopped]");
    }
}
