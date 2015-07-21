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
package net.java.sip.communicator.plugin.loggingutils;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.packetlogging.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Creates and registers logging config form.
 * @author Damian Minkov
 */
public class LoggingUtilsActivator
    implements BundleActivator
{
    /**
     * The OSGI bundle context.
     */
    static BundleContext        bundleContext         = null;

    /**
     * The resource service.
     */
    private static ResourceManagementService resourceService;

    /**
     * The configuration service.
     */
    private static ConfigurationService configurationService = null;

    /**
     * The packet logging service.
     */
    private static PacketLoggingService packetLoggingService = null;

    /**
     * The service giving access to files.
     */
    private static FileAccessService fileAccessService;

    /**
     * Notification service.
     */
    private static NotificationService notificationService;

    /**
     * Indicates if the logging configuration form should be disabled, i.e.
     * not visible to the user.
     */
    protected static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.loggingconfig.DISABLED";

    /**
     * Creates and register logging configuration.
     *
     * @param bundleContext  OSGI bundle context
     * @throws Exception if error creating configuration.
     */
    public void start(BundleContext bundleContext)
            throws
            Exception
    {
        LoggingUtilsActivator.bundleContext = bundleContext;

        // If the logging configuration form is disabled don't continue.
        if (!getConfigurationService().getBoolean(DISABLED_PROP, false))
        {
            // Config Form
            Dictionary<String, String> packetLoggingProps
                = new Hashtable<String, String>();
            packetLoggingProps.put(
                    ConfigurationForm.FORM_TYPE,
                    ConfigurationForm.ADVANCED_TYPE);
            bundleContext.registerService(
                    ConfigurationForm.class.getName(),
                    new LazyConfigurationForm(
                            "net.java.sip.communicator.plugin.loggingutils" +
                                ".LoggingConfigForm",
                            getClass().getClassLoader(),
                            null,
                            "plugin.loggingutils.PACKET_LOGGING_CONFIG",
                            1200,
                            true),
                    packetLoggingProps);
        }
    }

    /**
     * Stops the Logging utils bundle
     *
     * @param bundleContext  the OSGI bundle context
     */
    public void stop(BundleContext bundleContext)
            throws
            Exception
    {
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
     * Returns a reference to a PacketLoggingService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the PacketLoggingService.
     */
    public static PacketLoggingService getPacketLoggingService()
    {
        if (packetLoggingService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    PacketLoggingService.class.getName());
            packetLoggingService
                = (PacketLoggingService) bundleContext.getService(confReference);
        }
        return packetLoggingService;
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
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        if (notificationService == null)
        {
            notificationService
                = ServiceUtils.getService(
                        bundleContext,
                        NotificationService.class);
        }
        return notificationService;
    }
}
