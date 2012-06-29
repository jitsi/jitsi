/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    implements BundleActivator
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

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

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
        final WelcomeWindow welcomeWindow;
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

        bundleContext.addBundleListener(new BundleListener()
        {

            /**
             * The indicator which determines whether the job of this listener
             * is done and no other <tt>BundleEvent</tt>s should be handled.
             * <p>
             * After
             * {@link BrandingActivator#bundleChanged(BundleEvent, WelcomeWindow)}
             * reports it's done, it's not enough to remove this listener from
             * the notifying <tt>BundleContext</tt> in order to not handle more
             * <tt>BundleEvent</tt>s because the notifications get triggered on
             * packs of events without consulting the currently registered
             * <tt>BundleListener</tt> and, if an event in the pack concludes
             * the job of this listener, the subsequent events from the same
             * pack will still be handled without the indicator.
             * </p>
             */
            private boolean done;

            public void bundleChanged(BundleEvent evt)
            {
                if (!done
                        && !BrandingActivator
                                .this.bundleChanged(evt, welcomeWindow))
                {

                    /*
                     * Don't let bundleContext retain a reference to this
                     * listener because it'll retain a reference to
                     * welcomeWindow. Besides, we're no longer interested in
                     * handling events so it doesn't make sense to even retain
                     * this listener.
                     */
                    bundleContext.removeBundleListener(this);
                    done = true;
                }
            }
        });
    }

    public void stop(BundleContext arg0) throws Exception
    {
    }

    private boolean bundleChanged(BundleEvent evt, WelcomeWindow welcomeWindow)
    {
        if ((welcomeWindow != null)
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

        ServiceReference uiServiceRef =
            bundleContext.getServiceReference(UIService.class.getName());
        if ((uiServiceRef != null)
                && (Bundle.ACTIVE == uiServiceRef.getBundle().getState()))
        {
            // UI-Service started.

            // register the about dialog menu entry
            registerMenuEntry(uiServiceRef);

            if (welcomeWindow != null)
                welcomeWindow.close();

            /*
             * We've just closed the WelcomeWindow so there'll be no other
             * updates to it and we should stop listening to events which
             * trigger them.
             */
            return false;
        }

        return true;
    }

    private void registerMenuEntry(ServiceReference uiServiceRef)
    {
        UIService uiService
            = (UIService) bundleContext.getService(uiServiceRef);

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

        bundleContext.registerService(  PluginComponent.class.getName(),
                                        new AboutWindowPluginComponent(
                                            Container.CONTAINER_HELP_MENU),
                                        helpMenuFilter);

        if (logger.isInfoEnabled())
            logger.info("ABOUT WINDOW ... [REGISTERED]");

        // Register the about window plugin component in the chat help menu.
        Hashtable<String, String> chatHelpMenuFilter
            = new Hashtable<String, String>();
        chatHelpMenuFilter.put( Container.CONTAINER_ID,
                                Container.CONTAINER_CHAT_HELP_MENU.getID());

        bundleContext.registerService(  PluginComponent.class.getName(),
                                        new AboutWindowPluginComponent(
                                            Container.CONTAINER_CHAT_HELP_MENU),
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
