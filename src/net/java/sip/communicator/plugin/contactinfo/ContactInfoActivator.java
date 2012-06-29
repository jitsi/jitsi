package net.java.sip.communicator.plugin.contactinfo;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The Activator of the Contact Info bundle.
 * 
 * @author Adam Goldstein
 * @author Yana Stamcheva
 */
public class ContactInfoActivator implements BundleActivator
{
    private Logger logger = Logger.getLogger(ContactInfoActivator.class);

    private static BrowserLauncherService browserLauncherService;

    static BundleContext bundleContext;

    /**
     * Starts this bundle.
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        ContactInfoMenuItem cinfoMenuItem = new ContactInfoMenuItem();

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();
        containerFilter.put(
                Container.CONTAINER_ID,
                Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID());

        bundleContext.registerService(  PluginComponent.class.getName(),
                                        cinfoMenuItem,
                                        containerFilter);

        if (logger.isInfoEnabled())
            logger.info("CONTACT INFO... [REGISTERED]");
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
