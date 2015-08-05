/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.packetlogging;

import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.packetlogging.*;
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
    private static Logger logger
        = Logger.getLogger(PacketLoggingActivator.class);

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
            throws Exception
    {
        /*
         * PacketLoggingServiceImpl requires a FileAccessService implementation.
         * Ideally, we'd be listening to the bundleContext and will be making
         * the PacketLoggingService implementation available in accord with the
         * availability of a FileAccessService implementation. Unfortunately,
         * the real world is far from ideal.
         */
        fileAccessService
            = ServiceUtils.getService(bundleContext, FileAccessService.class);
        if (fileAccessService != null)
        {
            PacketLoggingActivator.bundleContext = bundleContext;

            packetLoggingService = new PacketLoggingServiceImpl();
            packetLoggingService.start();

            bundleContext.registerService(
                    PacketLoggingService.class.getName(),
                    packetLoggingService,
                    null);

            if (logger.isInfoEnabled())
                logger.info("Packet Logging Service ...[REGISTERED]");
        }
    }

    /**
     * Stops the Packet Logging bundle
     *
     * @param bundleContext  the OSGI bundle context
     */
    public void stop(BundleContext bundleContext)
            throws Exception
    {
        if(packetLoggingService != null)
            packetLoggingService.stop();

        configurationService = null;
        fileAccessService = null;
        packetLoggingService = null;

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
        return fileAccessService;
    }
}
