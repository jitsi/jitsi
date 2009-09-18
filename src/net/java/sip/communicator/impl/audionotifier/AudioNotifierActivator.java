/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.audionotifier;

import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The AudioNotifier activator class.
 * 
 * @author Yana Stamcheva
 */
public class AudioNotifierActivator implements BundleActivator
{
    private AudioNotifierServiceImpl audioNotifier;
    
    private ConfigurationService configService;
    
    private static ResourceManagementService resourcesService;
    
    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;
    
    private static final Logger logger
        = Logger.getLogger(AudioNotifierActivator.class);
    
    /**
     * Called when this bundle is started.
     *
     * @param bContext The execution context of the bundle being started.
     */
    public void start(BundleContext bContext) throws Exception
    {
        try {
            AudioNotifierActivator.bundleContext = bContext;
            
            //Create the audio notifier service
            audioNotifier = new AudioNotifierServiceImpl();

            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());
        
            configService = (ConfigurationService) bundleContext
                .getService(configReference);

            audioNotifier.setMute(
                !configService
                    .getBoolean(
                        "net.java.sip.communicator.impl.sound.isSoundEnabled",
                        true));

            logger.logEntry();
            
            logger.info("Audio Notifier Service...[  STARTED ]");

            bundleContext
                .registerService(
                    AudioNotifierService.class.getName(),
                    audioNotifier,
                    null);

            logger.info("Audio Notifier Service ...[REGISTERED]");
            
        } finally {
            logger.logExit();
        }
    }
    
    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bContext The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext bContext) throws Exception
    {
        //TODO: Stop all currently playing sounds here
        try {
            configService.setProperty(
                "net.java.sip.communicator.impl.sound.isSoundEnabled",
                Boolean.toString(!audioNotifier.isMute()));

        }
        catch (PropertyVetoException e1) {
            logger.error("The proposed property change "
                    + "represents an unacceptable value");
        }
        
        logger.info("AudioNotifier Service ...[STOPPED]");
    }
    
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }
}
