/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.notificationconfiguration;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

/**
 * The <tt>BundleActivator</tt> of the AudioConfiguration plugin.
 * @author Alexandre Maillard
 */
public class NotificationConfigurationActivator implements BundleActivator
{
    public static BundleContext bundleContext;
    
    private static ConfigurationService configService;
    
    private static AudioNotifierService audioService;
    
    private static NotificationService notificationService;
   
    /**
     * Starts this bundle and adds the <tt>AudioConfigurationConfigForm</tt> 
     * contained in it to the configuration window obtained from the 
     * <tt>UIService</tt>.
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;
        
        NotificationConfigurationConfigForm notificationconfiguration
            = new NotificationConfigurationConfigForm();

        bundleContext.registerService(  ConfigurationForm.class.getName(),
                                        notificationconfiguration,
                                        null);
    }

    /**
     * Stops this bundles.
     */
    public void stop(BundleContext arg0) throws Exception
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
        if(configService == null)
        {
            ServiceReference configReference 
                    = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext.getService(
                    configReference);
        }

        return configService;
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
     * @return the <tt>NotificationService</tt> obtained from the bundle
     * context
     */
    public static NotificationService getNotificationService()
    {
        if(notificationService == null)
        {
            ServiceReference notificationReference 
                    = bundleContext.getServiceReference(
                    NotificationService.class.getName());

            notificationService 
                    = (NotificationService) bundleContext.getService(
                    notificationReference);
        }
        return notificationService;
    }
}
