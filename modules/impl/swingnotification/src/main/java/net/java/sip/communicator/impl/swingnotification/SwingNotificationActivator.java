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
package net.java.sip.communicator.impl.swingnotification;

import net.java.sip.communicator.service.systray.*;

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activator for the swing notification service.
 * @author Symphorien Wanko
 */
public class SwingNotificationActivator
    extends DependentActivator
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SwingNotificationActivator.class);

    public SwingNotificationActivator()
    {
        super(
            ConfigurationService.class,
            ResourceManagementService.class
        );
    }

    /**
     * Start the swing notification service
     */
    public void startWithServices(BundleContext bc)
    {
        if (logger.isInfoEnabled())
            logger.info("Swing Notification ...[  STARTING ]");

        ResourceManagementService r
            = getService(ResourceManagementService.class);
        bc.registerService(
                PopupMessageHandler.class.getName()
                , new PopupMessageHandlerSwingImpl(r)
                , null);
        bc.registerService(
                PopupMessageHandler.class.getName()
                , new NonePopupMessageHandlerImpl(r)
                , null);

        if (logger.isInfoEnabled())
            logger.info("Swing Notification ...[REGISTERED]");
    }
}
