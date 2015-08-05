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
package net.java.sip.communicator.service.notification;

import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The <tt>NotificationActivator</tt> is the activator of the notification
 * bundle.
 *
 * @author Yana Stamcheva
 */
public class NotificationServiceActivator
    implements BundleActivator
{
    private final Logger logger
        = Logger.getLogger(NotificationServiceActivator.class);

    protected static BundleContext bundleContext;
    private static ConfigurationService configService;
    private ServiceRegistration notificationService;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        try
        {
            logger.logEntry();
            logger.info("Notification Service...[  STARTED ]");

            notificationService = bundleContext.registerService(
                NotificationService.class.getName(),
                new NotificationServiceImpl(),
                null);

            logger.info("Notification Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }
    }

    public void stop(BundleContext bc) throws Exception
    {
        notificationService.unregister();
        logger.info("Notification Service ...[STOPPED]");
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
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }
}
