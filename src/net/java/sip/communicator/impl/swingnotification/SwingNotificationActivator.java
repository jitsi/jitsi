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

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activator for the swing notification service.
 * @author Symphorien Wanko
 */
public class SwingNotificationActivator implements BundleActivator
{
    /**
     * The bundle context in which we started
     */
    public static BundleContext bundleContext;

    /**
     * A reference to the configuration service.
     */
    private static ConfigurationService configService;

    /**
     * Logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(SwingNotificationActivator.class);

    /**
     * A reference to the resource management service.
     */
    private static ResourceManagementService resourcesService;

    /**
     * Start the swing notification service
     * @param bc
     * @throws java.lang.Exception
     */
    public void start(BundleContext bc) throws Exception
    {
        if (logger.isInfoEnabled())
            logger.info("Swing Notification ...[  STARTING ]");

        bundleContext = bc;

        getConfigurationService();

        bc.registerService(
                PopupMessageHandler.class.getName()
                , new PopupMessageHandlerSwingImpl()
                , null);
        bc.registerService(
                PopupMessageHandler.class.getName()
                , new NonePopupMessageHandlerImpl()
                , null);

        if (logger.isInfoEnabled())
            logger.info("Swing Notification ...[REGISTERED]");
    }

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
        if(configService == null) {
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> obtained from the bundle
     * context.
     * @return the <tt>ResourceManagementService</tt> obtained from the bundle
     * context
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService =
                ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;

    }
}
