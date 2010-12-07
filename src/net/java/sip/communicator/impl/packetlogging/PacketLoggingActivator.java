/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.packetlogging;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.packetlogging.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import org.osgi.framework.*;

import java.util.*;

import static net.java.sip.communicator.service.protocol.ProtocolProviderFactory.*;

/**
 * Creates and registers Packet Logging service into OSGi.
 * Also handles saving and retrieving configuration options for
 * the service and is used from the configuration form.
 * 
 * @author Damian Minkov
 */
public class PacketLoggingActivator
    implements BundleActivator
{
    /**
     * Our logging.
     */
    private static Logger logger =
        Logger.getLogger(PacketLoggingActivator.class);

    /**
     * The OSGI bundle context. 
     */
    private static BundleContext        bundleContext         = null;

    /**
     * Our packet logging service instance.
     */
    private static PacketLoggingServiceImpl packetLoggingService = null;

    /**
     * The configuration service.
     */
    private static ConfigurationService configurationService = null;

    /**
     * The service giving access to files.
     */
    private static FileAccessService fileAccessService;

    /**
     * The resource service.
     */
    private static ResourceManagementService resourceService;

    /**
     * Configuration property for packet logging enabled/disabled.
     */
    private final static String PACKET_LOGGING_ENABLED_PROPERTY_NAME
        = "net.java.sip.communicator.packetlogging.PACKET_LOGGING_ENABLED";

    /**
     * Configuration property for packet logging for
     * sip protocol enabled/disabled.
     */
    private final static String PACKET_LOGGING_SIP_ENABLED_PROPERTY_NAME
        = "net.java.sip.communicator.packetlogging.PACKET_LOGGING_SIP_ENABLED";

    /**
     * Configuration property for packet logging for
     * jabber protocol enabled/disabled.
     */
    private final static String PACKET_LOGGING_JABBER_ENABLED_PROPERTY_NAME
        = "net.java.sip.communicator.packetlogging.PACKET_LOGGING_JABBER_ENABLED";

    /**
     * Configuration property for packet logging for
     * RTP enabled/disabled.
     */
    private final static String PACKET_LOGGING_RTP_ENABLED_PROPERTY_NAME
        = "net.java.sip.communicator.packetlogging.PACKET_LOGGING_RTP_ENABLED";

    /**
     * Configuration property for packet logging for
     * ICE4J enabled/disabled.
     */
    private final static String PACKET_LOGGING_ICE4J_ENABLED_PROPERTY_NAME
        = "net.java.sip.communicator.packetlogging.PACKET_LOGGING_ICE4J_ENABLED";

    /**
     * Configuration property for packet logging file count.
     */
    final static String PACKET_LOGGING_FILE_COUNT_PROPERTY_NAME
        = "net.java.sip.communicator.packetlogging.PACKET_LOGGING_FILE_COUNT";

    /**
     * Configuration property for packet logging file size.
     */
    final static String PACKET_LOGGING_FILE_SIZE_PROPERTY_NAME
        = "net.java.sip.communicator.packetlogging.PACKET_LOGGING_FILE_SIZE";

    /**
     * Is Packet Logging Service enabled.
     */
    private static boolean globalLoggingEnabled = false;

    /**
     * Is Packet Logging Service enabled for sip protocol.
     */
    private static boolean sipLoggingEnabled = false;

    /**
     * Is Packet Logging Service enabled for jabber protocol.
     */
    private static boolean jabberLoggingEnabled = false;

    /**
     * Is Packet Logging Service enabled for rtp. 
     */
    private static boolean rtpLoggingEnabled = false;

    /**
     * Is Packet Logging Service enabled for ice4j.
     */
    private static boolean ice4jLoggingEnabled = false;

    /**
     * Creates a PacketLoggingServiceImpl, starts it, and registers it as a
     * PacketLoggingService.
     *
     * @param bundleContext  OSGI bundle context
     * @throws Exception if starting the PacketLoggingServiceImpl.
     */
    public void start(BundleContext bundleContext)
            throws
            Exception
    {
        PacketLoggingActivator.bundleContext = bundleContext;

        packetLoggingService = new PacketLoggingServiceImpl();

        getPacketLoggingService().start();

        loadConfig();

        bundleContext.registerService(PacketLoggingService.class.getName(),
                getPacketLoggingService(), null);

        if (logger.isInfoEnabled())
            logger.info("Packet Logging Service ...[REGISTERED]");

        // Config Form
        Dictionary<String, String> packetLoggingProps
            = new Hashtable<String, String>();
        packetLoggingProps.put(
                ConfigurationForm.FORM_TYPE,
                ConfigurationForm.ADVANCED_TYPE);
        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                        PacketLoggingConfigForm.class.getName(),
                        getClass().getClassLoader(),
                        null,
                        "impl.packetlogging.PACKET_LOGGING_CONFIG",
                        1200,
                        true),
                packetLoggingProps);
    }

    /**
     * Stops the Packet Logging bundle
     *
     * @param bundleContext  the OSGI bundle context
     */
    public void stop(BundleContext bundleContext)
            throws
            Exception
    {
        if(getPacketLoggingService() != null)
            getPacketLoggingService().stop();
        if (logger.isInfoEnabled())
            logger.info("Packet Logging Service ...[STOPPED]");
    }

    /**
     * Reads the configurations and loads them.
     */
    private void loadConfig()
    {
        globalLoggingEnabled = getConfigurationService().getBoolean(
                PACKET_LOGGING_ENABLED_PROPERTY_NAME, false);

        sipLoggingEnabled = getConfigurationService().getBoolean(
                PACKET_LOGGING_SIP_ENABLED_PROPERTY_NAME, false);

        jabberLoggingEnabled = getConfigurationService().getBoolean(
                PACKET_LOGGING_JABBER_ENABLED_PROPERTY_NAME, false);

        rtpLoggingEnabled = getConfigurationService().getBoolean(
                PACKET_LOGGING_RTP_ENABLED_PROPERTY_NAME, false);

        ice4jLoggingEnabled = getConfigurationService().getBoolean(
                PACKET_LOGGING_ICE4J_ENABLED_PROPERTY_NAME, false);
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
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService) bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> obtained from the
     * bundle context.
     *
     * @return the <tt>ResourceManagementService</tt> obtained from the
     * bundle context
     */
    public static ResourceManagementService getResourceService()
    {
        if (resourceService == null)
        {
            ServiceReference resourceReference
                = bundleContext.getServiceReference(
                    ResourceManagementService.class.getName());

            resourceService =
                (ResourceManagementService) bundleContext
                    .getService(resourceReference);
        }

        return resourceService;
    }

    /**
     * Checks whether packet logging is enabled in the configuration.
     * @return <tt>true</tt> if packet logging is enabled.
     */
    public static boolean isGlobalLoggingEnabled()
    {
        return globalLoggingEnabled;
    }

    /**
     * Checks whether packet logging is enabled in the configuration
     * for sip protocol.
     * @return <tt>true</tt> if packet logging is enabled for sip protocol.
     */
    public static boolean isSipLoggingEnabled()
    {
        return sipLoggingEnabled;
    }

    /**
     * Checks whether packet logging is enabled in the configuration
     * for jabber protocol.
     * @return <tt>true</tt> if packet logging is enabled for jabber protocol.
     */
    public static boolean isJabberLoggingEnabled()
    {
        return jabberLoggingEnabled;
    }

    /**
     * Checks whether packet logging is enabled in the configuration
     * for RTP.
     * @return <tt>true</tt> if packet logging is enabled for RTP.
     */
    public static boolean isRTPLoggingEnabled()
    {
        return rtpLoggingEnabled;
    }

    /**
     * Checks whether packet logging is enabled in the configuration
     * for Ice4J.
     * @return <tt>true</tt> if packet logging is enabled for RTP.
     */
    public static boolean isIce4JLoggingEnabled()
    {
        return ice4jLoggingEnabled;
    }

    /**
     * Change whether packet logging is enabled and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    static void setGlobalLoggingEnabled(boolean enabled)
    {
        if(enabled)
        {
            getConfigurationService().setProperty(
                PACKET_LOGGING_ENABLED_PROPERTY_NAME, Boolean.TRUE);
        }
        else
        {
            getConfigurationService().removeProperty(
                PACKET_LOGGING_ENABLED_PROPERTY_NAME);

            // as we are globbally off, set it and to services
            sipLoggingEnabled = false;
            jabberLoggingEnabled = false;
            rtpLoggingEnabled = false;
            ice4jLoggingEnabled = false;
        }

        globalLoggingEnabled = enabled;
    }

    /**
     * Change whether packet logging for sip protocol is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public static void setSipLoggingEnabled(boolean enabled)
    {
        if(enabled)
        {
            getConfigurationService().setProperty(
                PACKET_LOGGING_SIP_ENABLED_PROPERTY_NAME, Boolean.TRUE);
        }
        else
        {
            getConfigurationService().removeProperty(
                PACKET_LOGGING_SIP_ENABLED_PROPERTY_NAME);
        }

        sipLoggingEnabled = enabled;
    }

    /**
     * Change whether packet logging for jabber protocol is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public static void setJabberLoggingEnabled(boolean enabled)
    {
        if(enabled)
        {
            getConfigurationService().setProperty(
                PACKET_LOGGING_JABBER_ENABLED_PROPERTY_NAME, Boolean.TRUE);
        }
        else
        {
            getConfigurationService().removeProperty(
                PACKET_LOGGING_JABBER_ENABLED_PROPERTY_NAME);
        }

        jabberLoggingEnabled = enabled;
    }

    /**
     * Change whether packet logging for RTP is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public static void setRTPLoggingEnabled(boolean enabled)
    {
        if(enabled)
        {
            getConfigurationService().setProperty(
                PACKET_LOGGING_RTP_ENABLED_PROPERTY_NAME, Boolean.TRUE);
        }
        else
        {
            getConfigurationService().removeProperty(
                PACKET_LOGGING_RTP_ENABLED_PROPERTY_NAME);
        }

        rtpLoggingEnabled = true;
    }

    /**
     * Change whether packet logging for Ice4J is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public static void setIce4JLoggingEnabled(boolean enabled)
    {
        if(enabled)
        {
            getConfigurationService().setProperty(
                PACKET_LOGGING_ICE4J_ENABLED_PROPERTY_NAME, Boolean.TRUE);
        }
        else
        {
            getConfigurationService().removeProperty(
                PACKET_LOGGING_ICE4J_ENABLED_PROPERTY_NAME);
        }

        ice4jLoggingEnabled = true;
    }

    /**
     * Our packet logging service instance.
     */
    public static PacketLoggingServiceImpl getPacketLoggingService()
    {
        return packetLoggingService;
    }
}
