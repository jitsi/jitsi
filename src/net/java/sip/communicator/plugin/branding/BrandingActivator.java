/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.branding;

import java.awt.event.*;

import javax.swing.JMenuItem;

import net.java.sip.communicator.service.gui.UIService;

import org.osgi.framework.*;

public class BrandingActivator implements BundleActivator, BundleListener
{
    private WelcomeWindow welcomeWindow;
    private static BundleContext bundleContext;
    private JMenuItem aboutEntry;

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
        unRegisterMenuEntry();
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
        final UIService uiService = (UIService) bundleContext
                .getService(uiServiceRef);

        // add menu entry to file menu
        // Add your menu item to the help menu
        if (aboutEntry == null)
        {
            aboutEntry = new JMenuItem(Resources.getString("aboutMenuEntry"));
            aboutEntry.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {

                    AboutWindow aboutWindow = new AboutWindow(null);
                    aboutWindow.setVisible(true);
                }
            });
        }
        // Check if the help menu is a supported container.
        if (uiService.isContainerSupported(UIService.CONTAINER_HELP_MENU))
        {
            uiService.addComponent(UIService.CONTAINER_HELP_MENU, aboutEntry);
        }
        // Check if the help menu is a supported container.
        if (uiService.isContainerSupported(UIService.CONTAINER_CHAT_HELP_MENU))
        {
            uiService.addComponent(UIService.CONTAINER_CHAT_HELP_MENU,
                    aboutEntry);
        }
    }

    private void unRegisterMenuEntry()
    {
        // Obtain the UI Service
        ServiceReference uiServiceRef = bundleContext
                .getServiceReference(UIService.class.getName());

        if (uiServiceRef == null
                || uiServiceRef.getBundle().getState() != Bundle.ACTIVE)
        {
            return;
        }

        UIService uiService = (UIService) bundleContext
                .getService(uiServiceRef);

        // Check if the tools menu is a supported container.
        boolean isContainerSupported = uiService
                .isContainerSupported(UIService.CONTAINER_HELP_MENU);

        if (isContainerSupported)
        {
            // add menu entry to file menu
            // Add your menu item to the help menu
            uiService
                    .removeComponent(UIService.CONTAINER_HELP_MENU, aboutEntry);
        }
    }

    static BundleContext getBundleContext()
    {
        return bundleContext;
    }

}
