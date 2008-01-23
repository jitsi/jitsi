package net.java.sip.communicator.plugin.contactinfo;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

/**
 * The Activator of the Contact Info bundle.
 * 
 * @author Adam Goldstein
 * @author Yana Stamcheva
 */
public class ContactInfoActivator implements BundleActivator
{
    private static BrowserLauncherService browserLauncherService;

    private static BundleContext bundleContext;

    /**
     * Starts this bundle.
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        ServiceReference uiServiceRef
            = bc.getServiceReference(UIService.class.getName());

        UIService uiService
            = (UIService) bc.getService(uiServiceRef);

        // Check if the desired place, where we would like to add 
        // our menu item is supported from the current UIService implementation.
        if (uiService.isContainerSupported(
            UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
        {
            ContactInfoMenuItem cinfoMenuItem = new ContactInfoMenuItem();

            // We add the example plugin menu item in the right button menu
            // for a contact.
            uiService.addComponent(
                UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU,
                cinfoMenuItem);
        }
    }

    public void stop(BundleContext bc) throws Exception
    {
    }

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    public static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncherService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(BrowserLauncherService.class.getName());

            browserLauncherService = (BrowserLauncherService) bundleContext
                .getService(serviceReference);
        }

        return browserLauncherService;
    }
}