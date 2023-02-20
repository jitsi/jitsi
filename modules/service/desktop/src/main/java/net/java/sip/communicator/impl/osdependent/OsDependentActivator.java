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
package net.java.sip.communicator.impl.osdependent;

import java.awt.Desktop;
import java.awt.Desktop.*;
import java.awt.desktop.*;
import net.java.sip.communicator.impl.osdependent.jdic.*;
import net.java.sip.communicator.service.desktop.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Registers the <tt>Systray</tt> in the UI Service.
 *
 * @author Nicolas Chamouard
 * @author Lyubomir Marinov
 */
public class OsDependentActivator
    extends DependentActivator
{
    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    private static ConfigurationService configService;

    private static GlobalStatusService globalStatusService;

    /**
     * The <tt>Logger</tt> used by the <tt>OsDependentActivator</tt> class and
     * its instances for logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OsDependentActivator.class);

    private static ResourceManagementService resourcesService;

    public static UIService uiService;

    public OsDependentActivator()
    {
        super(
            ConfigurationService.class,
            GlobalStatusService.class,
            ResourceManagementService.class
        );
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
            configService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configService;
    }

    /**
     * Returns the <tt>GlobalStatusService</tt> obtained from the bundle
     * context.
     * @return the <tt>GlobalStatusService</tt> obtained from the bundle
     * context
     */
    public static GlobalStatusService getGlobalStatusService()
    {
        if (globalStatusService == null)
        {
            globalStatusService
                = ServiceUtils.getService(
                        bundleContext,
                        GlobalStatusService.class);
        }
        return globalStatusService;
    }


    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        return resourcesService;
    }

    /**
     * Gets a reference to a <tt>ShutdownService</tt> implementation currently
     * registered in the <tt>BundleContext</tt> of the active
     * <tt>OsDependentActivator</tt> instance.
     * <p>
     * The returned reference to <tt>ShutdownService</tt> is not cached.
     * </p>
     *
     * @return reference to a <tt>ShutdownService</tt> implementation currently
     * registered in the <tt>BundleContext</tt> of the active
     * <tt>OsDependentActivator</tt> instance
     */
    public static ShutdownService getShutdownService()
    {
        return ServiceUtils.getService(bundleContext, ShutdownService.class);
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if(uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }

    /**
     * Called when this bundle is started.
     *
     * @param bc The execution context of the bundle being started.
     */
    @Override
    public void startWithServices(BundleContext bc)
    {
        bundleContext = bc;
        resourcesService = getService(ResourceManagementService.class);
        globalStatusService = getService(GlobalStatusService.class);

        // Adds a listener to show the window on dock/app icon click
        if (java.awt.Desktop.isDesktopSupported())
        {
            var desktop = Desktop.getDesktop();
            if (desktop != null && Desktop.getDesktop().isSupported(Action.APP_EVENT_REOPENED))
            {
                desktop.addAppEventListener(
                    (AppReopenedListener) appReOpenedEvent ->
                    {
                        UIService uiService = getUIService();
                        if (uiService != null && !uiService.isVisible())
                        {
                            uiService.setVisible(true);
                        }
                    });
            }
        }

        // Create the notification service implementation
        SystrayService systrayService = new SystrayServiceJdicImpl(bc,
            getService(ConfigurationService.class));

        if (logger.isInfoEnabled())
            logger.info("Systray Service...[  STARTED ]");

        bundleContext.registerService(
                SystrayService.class.getName(),
                systrayService,
                null);

        if (logger.isInfoEnabled())
            logger.info("Systray Service ...[REGISTERED]");

        // Create the desktop service implementation
        DesktopService desktopService = new DesktopServiceImpl();

        if (logger.isInfoEnabled())
            logger.info("Desktop Service...[  STARTED ]");

        bundleContext.registerService(
                DesktopService.class.getName(),
                desktopService,
                null);

        logger.info("Desktop Service ...[REGISTERED]");
    }
}
