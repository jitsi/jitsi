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

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The <tt>NotificationActivator</tt> is the activator of the notification
 * bundle.
 *
 * @author Yana Stamcheva
 */
public class NotificationServiceActivator
    extends DependentActivator
{
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(NotificationServiceActivator.class);

    private ServiceRegistration<NotificationService> notificationService;

    public NotificationServiceActivator()
    {
        super(ConfigurationService.class);
    }

    @Override
    public void startWithServices(BundleContext context)
    {
        logger.info("Notification Service...[  STARTED ]");

        ConfigurationService configService
            = getService(ConfigurationService.class);
        notificationService = context.registerService(
            NotificationService.class,
            new NotificationServiceImpl(configService),
            null);

        logger.info("Notification Service ...[REGISTERED]");
    }

    public void stop(BundleContext bc)
    {
        notificationService.unregister();
        logger.info("Notification Service ...[STOPPED]");
    }
}
