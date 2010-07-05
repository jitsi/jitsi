/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>BundleActivator</tt> of the AudioConfiguration plugin.
 * @author Alexandre Maillard
 */
public class NotificationConfigurationActivator implements BundleActivator
{
    private final Logger logger
        = Logger.getLogger(NotificationConfigurationActivator.class);

    public static BundleContext bundleContext;

    private static AudioNotifierService audioService;

    /**
     * Starts this bundle and adds the <tt>AudioConfigurationConfigForm</tt> 
     * contained in it to the configuration window obtained from the 
     * <tt>UIService</tt>.
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        bundleContext
            .registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin.notificationconfiguration.NotificationConfigurationPanel",
                    getClass().getClassLoader(),
                    "plugin.notificationconfig.PLUGIN_ICON",
                    "service.gui.EVENTS",
                    30),
                null);

        if (logger.isTraceEnabled())
            logger.trace("Notification Configuration: [ STARTED ]");
    }

    /**
     * Stops this bundles.
     */
    public void stop(BundleContext arg0) throws Exception
    {   
    }
    
    /**
     * Returns the <tt>AudioService</tt> obtained from the bundle
     * context.
     * @return the <tt>AudioService</tt> obtained from the bundle
     * context
     */
    public static AudioNotifierService getAudioNotifierService()
    {
        if(audioService == null)
        {
            ServiceReference audioReference 
                    = bundleContext.getServiceReference(
                    AudioNotifierService.class.getName());

            audioService = (AudioNotifierService) bundleContext.getService(
                    audioReference);
        }
        return audioService;
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle
     * context.
     * <p>
     * <b>Note</b>: No caching of the returned value is made available. Clients
     * interested in bringing down the penalties imposed by acquiring the value
     * in question should provide it by themselves.
     * </p>
     * 
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        ServiceReference notificationReference =
            bundleContext.getServiceReference(NotificationService.class
                .getName());

        return (NotificationService) bundleContext
            .getService(notificationReference);
    }
}
