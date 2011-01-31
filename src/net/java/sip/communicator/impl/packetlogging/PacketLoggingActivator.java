/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.packetlogging;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.packetlogging.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import org.osgi.framework.*;

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
     * The name of the log dir.
     */
    final static String LOGGING_DIR_NAME = "log";

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

        packetLoggingService.start();

        bundleContext.registerService(PacketLoggingService.class.getName(),
                packetLoggingService, null);

        if (logger.isInfoEnabled())
            logger.info("Packet Logging Service ...[REGISTERED]");
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
        if(packetLoggingService != null)
            packetLoggingService.stop();

        if (logger.isInfoEnabled())
            logger.info("Packet Logging Service ...[STOPPED]");
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
}
