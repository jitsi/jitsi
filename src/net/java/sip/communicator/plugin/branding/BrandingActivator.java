/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

import com.apple.eawt.*;

public class BrandingActivator
    implements  BundleActivator
{
    private final Logger logger = Logger.getLogger(BrandingActivator.class);

    private static BundleContext bundleContext;
    
    private static ResourceManagementService resourcesService;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        /*
         * WelcomeWindow is huge because it has a large image spread all over it
         * so, given it's only necessary before the UIService gets activated, we
         * certainly don't want to keep it around (e.g. as an instance field or
         * as a final variable used inside a BundleListener which never gets
         * removed).
         */
        final WelcomeWindow welcomeWindow = new WelcomeWindow();
        welcomeWindow.pack();
        welcomeWindow.setVisible(true);

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
                    && !BrandingActivator.this
                        .bundleChanged(evt, welcomeWindow))
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
        if (welcomeWindow != null && welcomeWindow.isShowing()
                && evt.getType() == BundleEvent.STARTED)
            welcomeWindow.setBundle(evt.getBundle().getHeaders().get(
                    "Bundle-Name").toString());

        ServiceReference uiServiceRef =
            bundleContext.getServiceReference(UIService.class.getName());
        if ((uiServiceRef != null)
            && (Bundle.ACTIVE == uiServiceRef.getBundle().getState()))
        {
            // UI-Service started.

            // register the about dialog menu entry
            registerMenuEntry(uiServiceRef);

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
        UIService uiService =
            (UIService) bundleContext.getService(uiServiceRef);
        if ((uiService == null) || !uiService.useMacOSXScreenMenuBar()
            || !registerMenuEntryMacOSX(uiService))
        {
            registerMenuEntryNonMacOSX(uiService);
        }
    }

    private boolean registerMenuEntryMacOSX(UIService uiService)
    {
//        Application application = Application.getApplication();
//        if (application != null)
//        {
//            application.addAboutMenuItem();
//            if (application.isAboutMenuItemPresent())
//            {
//                application.setEnabledAboutMenu(true);
//                application.addApplicationListener(new ApplicationAdapter()
//                {
//                    public void handleAbout(ApplicationEvent event)
//                    {
//                        AboutWindowPluginComponent.actionPerformed();
//                        event.setHandled(true);
//                    }
//                });
//                return true;
//            }
//        }
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

        logger.info("CHAT ABOUT WINDOW ... [REGISTERED]");
    }

    static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;
            
            resourcesService = (ResourceManagementService) bundleContext
                .getService(serviceReference);
        }

        return resourcesService;
    }
}
