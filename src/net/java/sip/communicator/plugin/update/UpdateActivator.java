/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.update;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.update.UpdateService;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the update plug-in.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class UpdateActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>UpdateActivator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(UpdateActivator.class);

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
        = "net.java.sip.communicator.plugin.update.CHECK_FOR_UPDATES_MENU_DISABLED";

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
    private static UpdateService updateService;

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

    /**
     * Starts this bundle
     *
     * @param bundleContext <tt>BundleContext</tt> provided by OSGi framework
     * @throws Exception if something goes wrong during start
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Update checker [STARTED]");

        UpdateActivator.bundleContext = bundleContext;

        if (OSUtils.IS_WINDOWS)
        {
            updateService = new Update();

            bundleContext.registerService(
                UpdateService.class.getName(),
                updateService,
                null);

            ConfigurationService cfg = getConfiguration();

            // Register the "Check for Updates" menu item if
            // the "Check for Updates" property isn't disabled.
            if(!cfg.getBoolean(CHECK_FOR_UPDATES_MENU_DISABLED_PROP, false))
            {
                // Register the "Check for Updates" menu item.
                CheckForUpdatesMenuItemComponent
                    checkForUpdatesMenuItemComponent
                    = new CheckForUpdatesMenuItemComponent(
                            Container.CONTAINER_HELP_MENU);

                Hashtable<String, String> toolsMenuFilter
                    = new Hashtable<String, String>();
                toolsMenuFilter.put(
                        Container.CONTAINER_ID,
                        Container.CONTAINER_HELP_MENU.getID());

                bundleContext.registerService(
                        PluginComponent.class.getName(),
                        checkForUpdatesMenuItemComponent,
                        toolsMenuFilter);
            }

            // Check for software update upon startup if enabled.
            if(cfg.getBoolean(UPDATE_ENABLED, true))
                updateService.checkForUpdates(false);
        }

        if (logger.isDebugEnabled())
            logger.debug("Update checker [REGISTERED]");
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
    }

    /**
     * Returns the update service instance.
     *
     * @return the update service instance
     */
    static UpdateService getUpdateService()
    {
        return updateService;
    }
}
