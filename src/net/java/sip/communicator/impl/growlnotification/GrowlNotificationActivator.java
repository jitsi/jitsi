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
        /* Check Java version: do not start if Java 6 */
        /* Actually, this plugin uses the Growl Java bindings which 
         * in turn uses the Cocoa Java bridge. Java 6 on Mac OS X is 
         * 64-bit only (as of 01/2008), and the Cocoa-Java bridge 
         * will certainly never get any 64-bit support (it has been
         * deprecated). 
         */
        String version = System.getProperty("java.version");
        char minor = version.charAt(2);
        if(minor > '5') {
            logger.info("Growl Notification Plugin cannot be started " +
                        "on JDK version " + version);
        } else {
            /* Create and start the Growl Notification service. */
            growlNotificationService = new GrowlNotificationServiceImpl();
            growlNotificationService.start(bundleContext);

            logger.info("Growl Notification Plugin ...[Started]");
        }
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        logger.info("Growl Notification Service ...[Stopped]");
    }
}
