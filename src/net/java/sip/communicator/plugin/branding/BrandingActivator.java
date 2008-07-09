/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

public class BrandingActivator
    implements  BundleActivator,
                BundleListener
{
    private Logger logger = Logger.getLogger(BrandingActivator.class);

    private WelcomeWindow welcomeWindow;

    private static BundleContext bundleContext;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        welcomeWindow = new WelcomeWindow();
        welcomeWindow.pack();
        welcomeWindow.setVisible(true);

        bundleContext.addBundleListener(this);
    }

    public void stop(BundleContext arg0) throws Exception
    {
    }

    public void bundleChanged(BundleEvent evt)
    {
        if (welcomeWindow != null && welcomeWindow.isShowing()
                && evt.getType() == BundleEvent.STARTED)
            welcomeWindow.setBundle(evt.getBundle().getHeaders().get(
                    "Bundle-Name").toString());

        ServiceReference[] services = evt.getBundle().getRegisteredServices();
        if (services == null)
        {
            return;
        }
        for (ServiceReference serviceRef : services)
        {
            ServiceReference uiServiceRef = bundleContext
                    .getServiceReference(UIService.class.getName());
            if (serviceRef == uiServiceRef)
            {
                int state = serviceRef.getBundle().getState();
                if (state == Bundle.ACTIVE)
                {
                    // UI-Service started.

                    // register the about dialog menu entry
                    registerMenuEntry(uiServiceRef);

                    welcomeWindow.close();
                }
            }
        }
    }

    private void registerMenuEntry(ServiceReference uiServiceRef)
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

}
