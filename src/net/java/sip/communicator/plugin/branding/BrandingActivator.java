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
package net.java.sip.communicator.plugin.branding;

import java.lang.reflect.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Branding bundle activator.
 */
public class BrandingActivator
    extends AbstractServiceDependentActivator
    implements BundleListener
{
    private final Logger logger = Logger.getLogger(BrandingActivator.class);

    /**
     * The name of the boolean property which indicates whether the splash
     * screen (i.e. <code>WelcomeWindow</code>) is to be shown or to not be
     * utilized for the sake of better memory consumption and faster startup.
     */
    private static final String PNAME_SHOW_SPLASH_SCREEN
        = "net.java.sip.communicator.plugin.branding.SHOW_SPLASH_SCREEN";

    private static BundleContext bundleContext;

    private static ResourceManagementService resourcesService;

    /**
     * The welcome window.
     */
    private WelcomeWindow welcomeWindow;

    @Override
    public void start(BundleContext bc) throws Exception
    {
        super.start(bc);

        ConfigurationService config = getConfigurationService();
        boolean showSplashScreen
            = (config == null)
                ? true /*
                        * Having no ConfigurationService reference is not good
                        * for the application so we are better off with the
                        * splash screen to actually see which bundles get loaded
                        * and maybe be able to debug the problem.
                        */
                : config.getBoolean(PNAME_SHOW_SPLASH_SCREEN, false);

        /*
         * WelcomeWindow is huge because it has a large image spread all over it
         * so, given it's only necessary before the UIService gets activated, we
         * certainly don't want to keep it around (e.g. as an instance field or
         * as a final variable used inside a BundleListener which never gets
         * removed).
         */
        if (showSplashScreen)
        {
            welcomeWindow = new WelcomeWindow();
            welcomeWindow.pack();
            welcomeWindow.setVisible(true);
        }
        else
            welcomeWindow = null;

        if (getResources().getSettingsString(
                "service.gui.APPLICATION_NAME").equals("SIP Communicator"))
            new JitsiWarningWindow(null).setVisible(true);

        bundleContext.addBundleListener(this);
    }

    /**
     * Bundle has been started if welcome window is available and visible
     * update it to show the bundle activity.
     * @param evt
     */
    public synchronized void bundleChanged(BundleEvent evt)
    {
        if (welcomeWindow != null
            && welcomeWindow.isShowing()
            && (evt.getType() == BundleEvent.STARTED))
        {
            /*
             * The IBM JRE on GNU/Linux reports the Bundle-Name as null while
             * the SUN JRE reports it as non-null. Just prevent the throwing of
             * a NullPointerException because displaying the Bundle-Name isn't
             * vital anyway.
             */
            Object bundleName = evt.getBundle().getHeaders().get("Bundle-Name");

            welcomeWindow.setBundle(
                (bundleName == null) ? null : bundleName.toString());
        }
    }

    /**
     * Setting context to the activator, as soon as we have one.
     *
     * @param context the context to set.
     */
    @Override
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }

    /**
     * This activator depends on UIService.
     * @return the class name of uiService.
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * The dependent service is available and the bundle will start.
     * @param dependentService the UIService this activator is waiting.
     */
    @Override
    public void start(Object dependentService)
    {
        // UI-Service started.

        /*
         * Don't let bundleContext retain a reference to this
         * listener because it'll retain a reference to
         * welcomeWindow. Besides, we're no longer interested in
         * handling events so it doesn't make sense to even retain
         * this listener.
         */
        bundleContext.removeBundleListener(this);

        // register the about dialog menu entry
        registerMenuEntry((UIService)dependentService);

        if (welcomeWindow != null)
        {
            synchronized(this)
            {
                welcomeWindow.close();
                welcomeWindow = null;
            }
        }
    }

    public void stop(BundleContext arg0) throws Exception
    {
    }

    /**
     * Register the about menu entry.
     * @param uiService
     */
    private void registerMenuEntry(UIService uiService)
    {
        if ((uiService == null)
                || !uiService.useMacOSXScreenMenuBar()
                || !registerMenuEntryMacOSX(uiService))
        {
            registerMenuEntryNonMacOSX(uiService);
        }
    }

    private boolean registerMenuEntryMacOSX(UIService uiService)
    {
        Exception exception = null;
        try
        {
            Class<?> clazz =
                Class
                    .forName("net.java.sip.communicator.plugin.branding.MacOSXAboutRegistration");
            Method method = clazz.getMethod("run", (Class<?>[]) null);
            Object result = method.invoke(null, (Object[]) null);

            if (result instanceof Boolean)
                return ((Boolean) result).booleanValue();
        }
        catch (ClassNotFoundException ex)
        {
            exception = ex;
        }
        catch (IllegalAccessException ex)
        {
            exception = ex;
        }
        catch (InvocationTargetException ex)
        {
            exception = ex;
        }
        catch (NoSuchMethodException ex)
        {
            exception = ex;
        }
        if (exception != null)
            logger.error(
                "Failed to register Mac OS X-specific About handling.",
                exception);
        return false;
    }

    private void registerMenuEntryNonMacOSX(UIService uiService)
    {
        // Register the about window plugin component in the main help menu.
        Hashtable<String, String> helpMenuFilter
            = new Hashtable<String, String>();
        helpMenuFilter.put( Container.CONTAINER_ID,
                            Container.CONTAINER_HELP_MENU.getID());

        bundleContext.registerService(
            PluginComponentFactory.class.getName(),
            new PluginComponentFactory(
                    Container.CONTAINER_HELP_MENU)
            {
                @Override
                protected PluginComponent getPluginInstance()
                {
                    return new AboutWindowPluginComponent(getContainer(), this);
                }
            },
            helpMenuFilter);

        if (logger.isInfoEnabled())
            logger.info("ABOUT WINDOW ... [REGISTERED]");

        // Register the about window plugin component in the chat help menu.
        Hashtable<String, String> chatHelpMenuFilter
            = new Hashtable<String, String>();
        chatHelpMenuFilter.put( Container.CONTAINER_ID,
                                Container.CONTAINER_CHAT_HELP_MENU.getID());

        bundleContext.registerService(
            PluginComponentFactory.class.getName(),
            new PluginComponentFactory(
                    Container.CONTAINER_CHAT_HELP_MENU)
            {
                @Override
                protected PluginComponent getPluginInstance()
                {
                    return new AboutWindowPluginComponent(getContainer(), this);
                }
            },
            chatHelpMenuFilter);

        if (logger.isInfoEnabled())
            logger.info("CHAT ABOUT WINDOW ... [REGISTERED]");
    }

    static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    private static ConfigurationService getConfigurationService()
    {
        ServiceReference serRef
            = bundleContext
                .getServiceReference(ConfigurationService.class.getName());
        return
            (serRef == null)
                ? null
                : (ConfigurationService) bundleContext.getService(serRef);
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
            resourcesService
                = ResourceManagementServiceUtils.getService(bundleContext);
        return resourcesService;
    }
}
