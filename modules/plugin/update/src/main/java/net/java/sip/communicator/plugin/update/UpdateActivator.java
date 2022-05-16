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
package net.java.sip.communicator.plugin.update;

import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the update plug-in.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class UpdateActivator
    extends DependentActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>UpdateActivator</tt> class and its
     * instances for logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UpdateActivator.class);

    /**
     * The name of the configuration property which indicates whether the
     * checking for updates on application startup is enabled.
     */
    private static final String UPDATE_ENABLED
        = "net.java.sip.communicator.plugin.updatechecker.ENABLED";

    /**
     * The name of the configuration property which indicates whether the
     * "checking for updates" menu entry is disabled.
     */
    private static final String CHECK_FOR_UPDATES_MENU_DISABLED_PROP
        = "net.java.sip.communicator.plugin.update.checkforupdatesmenu.DISABLED";

    /**
     * The name of the configuration property which indicates whether the client
     * should automatically check for updates each day or not.
     */
    private static final String CHECK_FOR_UPDATES_DAILY_ENABLED_PROP =
    "net.java.sip.communicator.plugin.update.checkforupdatesmenu.daily.ENABLED";

    /**
     * The name of the configuration property which indicates the hour that
     * the client should check for updates (if daily update checking is enabled)
     */
    private static final String CHECK_FOR_UPDATES_DAILY_TIME_PROP =
       "net.java.sip.communicator.plugin.update.checkforupdatesmenu.daily.HOUR";

    /**
     * Reference to the <tt>BrowserLauncherService</tt>.
     */
    private static BrowserLauncherService browserLauncher;

    /**
     * The <tt>BundleContext</tt> in which the one and only
     * <tt>UpdateActivator</tt> instance of the update plug-in has been started.
     */
    static BundleContext bundleContext;

    /**
     * Reference to the <tt>ConfigurationService</tt>.
     */
    private static ConfigurationService configuration;

    /**
     * Reference to the <tt>UIService</tt>.
     */
    private static UIService uiService;

    /**
     * The update service.
     */
    private static UpdateServiceImpl updateService;

    /**
     * A scheduler to check for updates once a day
     */
    private ScheduledExecutorService mUpdateExecutor = null;

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncher == null)
        {
            browserLauncher
                = ServiceUtils.getService(
                        bundleContext,
                        BrowserLauncherService.class);
        }
        return browserLauncher;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    static ConfigurationService getConfiguration()
    {
        if (configuration == null)
        {
            configuration
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configuration;
    }

    /**
     * Gets a reference to a <code>ShutdownService</code> implementation
     * currently registered in the bundle context of the active
     * <code>UpdateCheckActivator</code> instance.
     * <p>
     * The returned reference to <code>ShutdownService</code> is not being
     * cached.
     * </p>
     *
     * @return reference to a <code>ShutdownService</code> implementation
     *         currently registered in the bundle context of the active
     *         <code>UpdateCheckActivator</code> instance
     */
    static ShutdownService getShutdownService()
    {
        return ServiceUtils.getService(bundleContext, ShutdownService.class);
    }

    /**
     * Returns a reference to the UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    static UIService getUIService()
    {
        if(uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }

    public UpdateActivator()
    {
        super(
            UIService.class,
            BrowserLauncherService.class,
            ShutdownService.class,
            ResourceManagementService.class,
            ConfigurationService.class
        );
    }

    /**
     * The dependent service is available and the bundle will start.
     */
    @Override
    public void startWithServices(BundleContext context)
    {
        bundleContext = context;
        if (logger.isDebugEnabled())
            logger.debug("Update checker [STARTED]");

        ConfigurationService cfg = getConfiguration();

        if (OSUtils.IS_WINDOWS)
        {
            updateService = new UpdateServiceImpl();

            // Register the "Check for Updates" menu item if
            // the "Check for Updates" property isn't disabled.
            if(!cfg.getBoolean(CHECK_FOR_UPDATES_MENU_DISABLED_PROP, false))
            {
                // Register the "Check for Updates" menu item.

                Hashtable<String, String> toolsMenuFilter
                    = new Hashtable<String, String>();
                toolsMenuFilter.put(
                        Container.CONTAINER_ID,
                        Container.CONTAINER_HELP_MENU.getID());

                bundleContext.registerService(
                    PluginComponentFactory.class,
                    new PluginComponentFactory(Container.CONTAINER_HELP_MENU)
                    {
                        @Override
                        protected PluginComponent getPluginInstance()
                        {
                            return new CheckForUpdatesMenuItemComponent(
                                getContainer(),
                                this,
                                updateService);
                        }
                    },
                    toolsMenuFilter);
            }

            // Check for software update upon startup if enabled.
            if(cfg.getBoolean(UPDATE_ENABLED, true))
                updateService.checkForUpdates(false);
        }

        if (cfg.getBoolean(CHECK_FOR_UPDATES_DAILY_ENABLED_PROP, false))
        {
            logger.info("Scheduled update checking enabled");

            // Schedule a "check for updates" task that will run once a day
            int hoursToWait = calcHoursToWait();
            Runnable updateRunnable = new Runnable()
            {
                public void run()
                {
                    logger.debug("Performing scheduled update check");
                    updateService.checkForUpdates(false);
                }
            };

            mUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
            mUpdateExecutor.scheduleAtFixedRate(updateRunnable,
                                                hoursToWait,
                                                24*60*60,
                                                TimeUnit.SECONDS);
        }

        if (logger.isDebugEnabled())
            logger.debug("Update checker [REGISTERED]");
    }

    /**
     * Calculate the number of hour to wait until the first scheduled update
     * check.  This will only be called if daily checking for config updates
     * is enabled
     *
     * @return The number of hours to wait
     */
    private int calcHoursToWait()
    {
        // The hours to wait is the number of hours until midnight tonight (24
        // minus the current hour) plus the hour that the config says updates
        // should be
        return 24 - Calendar.getInstance().get(Calendar.HOUR_OF_DAY) +
                     configuration.getInt(CHECK_FOR_UPDATES_DAILY_TIME_PROP, 0);
    }

    /**
     * Stop the bundle. Nothing to stop for now.
     * @param bundleContext <tt>BundleContext</tt> provided by OSGi framework
     * @throws Exception if something goes wrong during stop
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Update checker [STOPPED]");

        if (mUpdateExecutor != null)
        {
            mUpdateExecutor.shutdown();
            mUpdateExecutor = null;
        }
    }
}
