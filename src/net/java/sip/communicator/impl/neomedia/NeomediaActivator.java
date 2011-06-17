/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.util.*;

import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.packetlogging.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import net.java.sip.communicator.impl.neomedia.notify.*;

import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the neomedia bundle.
 *
 * @author Martin Andre
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class NeomediaActivator
    implements BundleActivator
{

    /**
     * The <tt>Logger</tt> used by the <tt>NeomediaActivator</tt> class and its
     * instances for logging output.
     */
    private final Logger logger = Logger.getLogger(NeomediaActivator.class);

    /**
     * The context in which the one and only <tt>NeomediaActivator</tt> instance
     * has started executing.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>ConfigurationService</tt> registered in {@link #bundleContext}
     * and used by the <tt>NeomediaActivator</tt> instance to read and write
     * configuration properties.
     */
    private static ConfigurationService configurationService;

    /**
     * The <tt>FileAccessService</tt> registered in {@link #bundleContext} and
     * used by the <tt>NeomediaActivator</tt> instance to safely access files.
     */
    private static FileAccessService fileAccessService;

    /**
     * The one and only <tt>MediaServiceImpl</tt> instance registered in
     * {@link #bundleContext} by the <tt>NeomediaActivator</tt> instance.
     */
    private static MediaServiceImpl mediaServiceImpl;

    /**
     * The <tt>NetworkAddressManagerService</tt> registered in
     * {@link #bundleContext} and used by the <tt>NeomediaActivator</tt>
     * instance for network address resolution.
     */
    private static NetworkAddressManagerService networkAddressManagerService;

    /**
     * The <tt>ResourceManagementService</tt> registered in
     * {@link #bundleContext} and representing the resources such as
     * internationalized and localized text and images used by the neomedia
     * bundle.
     */
    private static ResourceManagementService resources;

    /**
     * The OSGi <tt>ServiceRegistration</tt> of {@link #mediaServiceImpl} in
     * {@link #bundleContext}.
     */
    private ServiceRegistration mediaServiceRegistration;

    /**
     * The OSGi <tt>PacketLoggingService</tt> of {@link #mediaServiceImpl} in
     * {@link #bundleContext} and used for debugging.
     */
    private static PacketLoggingService packetLoggingService  = null;

    /**
     * A reference to the <tt>UIService</tt> currently in use.
     */
    private static UIService uiService = null;

    /**
     * Starts the execution of the neomedia bundle in the specified context.
     *
     * @param bundleContext the context in which the neomedia bundle is to start
     * executing
     * @throws Exception if an error occurs while starting the execution of the
     * neomedia bundle in the specified context
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Started.");

        NeomediaActivator.bundleContext = bundleContext;

        // MediaService
        mediaServiceImpl = new MediaServiceImpl();
        mediaServiceImpl.start();

        mediaServiceRegistration
            = bundleContext
                .registerService(
                    MediaService.class.getName(),
                    mediaServiceImpl,
                    null);
        if (logger.isDebugEnabled())
            logger.debug("Media Service ... [REGISTERED]");

        Dictionary<String, String> mediaProps = new Hashtable<String, String>();
        mediaProps.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.GENERAL_TYPE);

        // Audio
        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                        "net.java.sip.communicator.impl.neomedia"
                            + ".AudioConfigurationPanel",
                        getClass().getClassLoader(),
                        "plugin.mediaconfig.AUDIO_ICON",
                        "impl.neomedia.configform.AUDIO",
                        3),
                mediaProps);

        // Video
        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                        "net.java.sip.communicator.impl.neomedia"
                            + ".VideoConfigurationPanel",
                        getClass().getClassLoader(),
                        "plugin.mediaconfig.VIDEO_ICON",
                        "impl.neomedia.configform.VIDEO",
                        4),
                mediaProps);
        // H.264
        Dictionary<String, String> h264Props
            = new Hashtable<String, String>();
        h264Props.put(
                ConfigurationForm.FORM_TYPE,
                ConfigurationForm.ADVANCED_TYPE);
        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                        "net.java.sip.communicator.impl.neomedia"
                            + ".codec.video.h264.ConfigurationPanel",
                        getClass().getClassLoader(),
                        "plugin.mediaconfig.VIDEO_ICON",
                        "impl.neomedia.configform.H264",
                        -1,
                        true),
                h264Props);

        // ZRTP
        Dictionary<String, String> securityProps
            = new Hashtable<String, String>();
        securityProps.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.SECURITY_TYPE);
        bundleContext.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                "net.java.sip.communicator.impl.neomedia.ZrtpConfigurePanel",
                getClass().getClassLoader(),
                "impl.media.security.zrtp.CONF_ICON",
                "impl.media.security.zrtp.TITLE",
                0),
            securityProps);

        GatherEntropy entropy
            = new GatherEntropy(mediaServiceImpl.getDeviceConfiguration());

        entropy.setEntropy();

        //we use the nist-sdp stack to make parse sdp and we need to set the
        //following property to make sure that it would accept java generated
        //IPv6 addresses that contain address scope zones.
        System.setProperty("gov.nist.core.STRIP_ADDR_SCOPES", "true");

        // AudioNotify Service
        AudioNotifierServiceImpl audioNotifier = new AudioNotifierServiceImpl(
            mediaServiceImpl.getDeviceConfiguration());

        audioNotifier.setMute(
                !getConfigurationService()
                    .getBoolean(
                        "net.java.sip.communicator.impl.sound.isSoundEnabled",
                        true));

        getBundleContext()
            .registerService(
                AudioNotifierService.class.getName(),
                audioNotifier,
                null);

        if (logger.isInfoEnabled())
            logger.info("Audio Notifier Service ...[REGISTERED]");

        // Call Recording
        Dictionary<String, String> callRecordingProps
            = new Hashtable<String, String>();
        callRecordingProps.put(
                ConfigurationForm.FORM_TYPE,
                ConfigurationForm.ADVANCED_TYPE);
        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                        CallRecordingConfigForm.class.getName(),
                        getClass().getClassLoader(),
                        null,
                        "plugin.callrecordingconfig.CALL_RECORDING_CONFIG", 
                        1100,
                        true), 
                callRecordingProps);
    }

    /**
     * Stops the execution of the neomedia bundle in the specified context.
     *
     * @param bundleContext the context in which the neomedia bundle is to stop
     * executing
     * @throws Exception if an error occurs while stopping the execution of the
     * neomedia bundle in the specified context
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        mediaServiceImpl.stop();
        mediaServiceRegistration.unregister();
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     *
     * @return a reference to the BundleContext instance that we were started
     * with.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            ServiceReference confReference
                = bundleContext
                    .getServiceReference(ConfigurationService.class.getName());

            configurationService
                = (ConfigurationService)
                    bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to a FileAccessService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * FileAccessService .
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null && bundleContext != null)
        {
            ServiceReference faReference
                = bundleContext
                    .getServiceReference(FileAccessService.class.getName());

            fileAccessService
                = (FileAccessService) bundleContext.getService(faReference);
        }
        return fileAccessService;
    }

    /**
     * Gets the <tt>MediaService</tt> implementation instance registered by the
     * neomedia bundle.
     *
     * @return the <tt>MediaService</tt> implementation instance registered by
     * the neomedia bundle
     */
    public static MediaServiceImpl getMediaServiceImpl()
    {
        return mediaServiceImpl;
    }

    /**
     * Returns a reference to a NetworkAddressManagerService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * NetworkAddressManagerService .
     */
    public static NetworkAddressManagerService getNetworkAddressManagerService()
    {
        if (networkAddressManagerService == null)
        {
            ServiceReference namReference
                = bundleContext
                    .getServiceReference(
                        NetworkAddressManagerService.class.getName());

            networkAddressManagerService
                = (NetworkAddressManagerService)
                    bundleContext.getService(namReference);
        }
        return networkAddressManagerService;
    }

    /**
     * Gets the <tt>ResourceManagementService</tt> instance which represents the
     * resources such as internationalized and localized text and images used by
     * the neomedia bundle.
     *
     * @return the <tt>ResourceManagementService</tt> instance which represents
     * the resources such as internationalized and localized text and images
     * used by the neomedia bundle
     */
    public static ResourceManagementService getResources()
    {
        if (resources == null)
            resources
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resources;
    }

    /**
     * Returns a reference to the <tt>PacketLoggingService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a reference to a <tt>PacketLoggingService</tt> implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     */
    public static PacketLoggingService getPacketLogging()
    {
        if (packetLoggingService == null)
        {
            ServiceReference plReference
                = bundleContext.getServiceReference(
                        PacketLoggingService.class.getName());

            packetLoggingService
                = (PacketLoggingService)bundleContext.getService(plReference);
        }
        return packetLoggingService;
    }

    /**
     * Returns a reference to an UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to an UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static UIService getUIService()
    {
        if(uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }
}