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

import net.java.sip.communicator.impl.osdependent.jdic.*;
import net.java.sip.communicator.impl.osdependent.macosx.*;
import net.java.sip.communicator.service.desktop.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * Registers the <tt>Systray</tt> in the UI Service.
 *
 * @author Nicolas Chamouard
 * @author Lyubomir Marinov
 */
public class OsDependentActivator
    implements BundleActivator
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
    private static final Logger logger
        = Logger.getLogger(OsDependentActivator.class);

    private static ResourceManagementService resourcesService;

    public static UIService uiService;

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
        if (resourcesService == null)
        {
            resourcesService
                = ResourceManagementServiceUtils.getService(bundleContext);
        }
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
     * @throws Exception
     */
    @Override
    public void start(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        try
        {
            // Adds a MacOSX specific dock icon listener in order to show main
            // contact list window on dock icon click.
            if (OSUtils.IS_MAC)
                MacOSXDockIcon.addDockIconListener();

            // Create the notification service implementation
            SystrayService systrayService = new SystrayServiceJdicImpl();

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

            if (logger.isInfoEnabled())
                logger.info("Desktop Service ...[REGISTERED]");

            logger.logEntry();
        }
        finally
        {
            logger.logExit();
        }
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bc The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is still
     * marked as stopped, and the Framework will remove the bundle's listeners,
     * unregister all services registered by the bundle, and release all
     * services used by the bundle.
     */
    @Override
    public void stop(BundleContext bc)
            throws Exception
    {
    }
}
