package net.java.sip.communicator.plugin.contactinfo;

import java.util.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
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

    /**
     * Indicates if the contact info button is enabled in the chat window.
     */
    private static final String ENABLED_IN_CHAT_WINDOW_PROP
        = "net.java.sip.communicator.plugin.contactinfo." +
            "ENABLED_IN_CHAT_WINDOW_PROP";

    /**
     * Indicates if the contact info button is enabled in the call window.
     */
    private static final String ENABLED_IN_CALL_WINDOW_PROP
        = "net.java.sip.communicator.plugin.contactinfo." +
            "ENABLED_IN_CALL_WINDOW_PROP";

    private static BrowserLauncherService browserLauncherService;

    /**
     * The image loader service implementation.
     */
    private static ImageLoaderService imageLoaderService = null;

    /**
     * The contact list service implementation.
     */
    private static MetaContactListService metaCListService;

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

        if(getConfigService().getBoolean(ENABLED_IN_CHAT_WINDOW_PROP, false))
        {
            containerFilter = new Hashtable<String, String>();
            containerFilter.put(
                    Container.CONTAINER_ID,
                    Container.CONTAINER_CHAT_TOOL_BAR.getID());

            bundleContext.registerService(
                PluginComponent.class.getName(),
                new ContactInfoMenuItem(Container.CONTAINER_CHAT_TOOL_BAR),
                containerFilter);
        }

        if(getConfigService().getBoolean(ENABLED_IN_CALL_WINDOW_PROP, false))
        {
            containerFilter = new Hashtable<String, String>();
            containerFilter.put(
                    Container.CONTAINER_ID,
                    Container.CONTAINER_CALL_DIALOG.getID());

            bundleContext.registerService(
                PluginComponent.class.getName(),
                new ContactInfoMenuItem(Container.CONTAINER_CALL_DIALOG),
                containerFilter);
        }

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

    /**
     * Returns the imageLoaderService instance, if missing query osgi for it.
     * @return the imageLoaderService.
     */
    public static ImageLoaderService getImageLoaderService()
    {
        if(imageLoaderService == null)
        {
            imageLoaderService =
                ServiceUtils.getService(
                    bundleContext,
                    ImageLoaderService.class);
        }

        return imageLoaderService;
    }

    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     * @return the <tt>MetaContactListService</tt> obtained from the bundle
     * context
     */
    public static MetaContactListService getContactListService()
    {
        if (metaCListService == null)
        {
            metaCListService
                = ServiceUtils.getService(
                        bundleContext,
                        MetaContactListService.class);
        }
        return metaCListService;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigService()
    {
        return ServiceUtils.getService(bundleContext,
            ConfigurationService.class);
    }

}
