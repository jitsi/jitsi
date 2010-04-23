/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import java.util.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.desktop.*;
import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The GUI Activator class.
 *
 * @author Yana Stamcheva
 */
public class GuiActivator implements BundleActivator
{
    private static final Logger logger = Logger.getLogger(GuiActivator.class);

    private static UIServiceImpl uiService = null;

    public static BundleContext bundleContext;

    private static ConfigurationService configService;

    private static MetaHistoryService metaHistoryService;

    private static MetaContactListService metaCListService;

    private static CallHistoryService callHistoryService;

    private static AudioNotifierService audioNotifierService;

    private static BrowserLauncherService browserLauncherService;

    private static NotificationService notificationService;
    private        NotificationServiceListener notificationServiceListener;

    private static SystrayService systrayService;

    private static ResourceManagementService resourcesService;

    private static KeybindingsService keybindingsService;

    private static FileAccessService fileAccessService;

    private static DesktopService desktopService;

    private static MediaService mediaService;

    private static final Map<Object, ProtocolProviderFactory>
        providerFactoriesMap = new Hashtable<Object, ProtocolProviderFactory>();

    /**
     * Indicates if this bundle has been started.
     */
    public  static boolean isStarted = false;

    /**
     * The contact list object.
     */
    private static TreeContactList contactList;

    /**
     * Called when this bundle is started.
     *
     * @param bContext The execution context of the bundle being started.
     * @throws Exception if the bundle is not correctly started
     */
    public void start(BundleContext bContext)
        throws Exception
    {
        isStarted = true;
        GuiActivator.bundleContext = bContext;

        NotificationManager.registerGuiNotifications();
        notificationServiceListener = new NotificationServiceListener();
        bundleContext.addServiceListener(notificationServiceListener);

        ConfigurationManager.loadGuiConfigurations();

        try
        {
            // Create the ui service
            uiService = new UIServiceImpl();

            logger.info("UI Service...[  STARTED ]");

            bundleContext.registerService(UIService.class.getName(),
                                          uiService,
                                          null);
            logger.info("UI Service ...[REGISTERED]");

            // UIServiceImpl also implements ShutdownService.
            bundleContext.registerService(ShutdownService.class.getName(),
                                          (ShutdownService) uiService,
                                          null);

            bundleContext.registerService(
                CertificateVerificationService.class.getName(),
                new CertificateVerificationServiceImpl(),
                null);

            uiService.loadApplicationGui();

            logger.logEntry();
        }
        finally
        {
            logger.logExit();
        }

        GuiActivator.getConfigurationService()
            .addPropertyChangeListener(uiService);

        bundleContext.addServiceListener(uiService);
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bContext The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext bContext) throws Exception
    {
        logger.info("UI Service ...[STOPPED]");
        isStarted = false;

        GuiActivator.getConfigurationService()
            .removePropertyChangeListener(uiService);

        bContext.removeServiceListener(uiService);
        bContext.removeServiceListener(notificationServiceListener);
    }

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     * 
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     *         context
     */
    public static Map<Object, ProtocolProviderFactory>
        getProtocolProviderFactories()
    {

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);

        }
        catch (InvalidSyntaxException e)
        {
            logger.error("LoginManager : " + e);
        }

        if (serRefs != null) 
        {
            for (int i = 0; i < serRefs.length; i++) 
            {

                ProtocolProviderFactory providerFactory
                    = (ProtocolProviderFactory) bundleContext
                        .getService(serRefs[i]);

                providerFactoriesMap.put(serRefs[i]
                        .getProperty(ProtocolProviderFactory.PROTOCOL),
                        providerFactory);
            }
        }
        return providerFactoriesMap;
    }

    /**
     * Returns a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider.
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * factory we're looking for
     * @return a <tt>ProtocolProviderFactory</tt> for a given protocol
     * provider
     */
    public static ProtocolProviderFactory getProtocolProviderFactory(
            ProtocolProviderService protocolProvider)
    {
        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "="+protocolProvider.getProtocolName()+")";

        ProtocolProviderFactory protocolProviderFactory = null;
        try
        {
            ServiceReference[] serRefs = GuiActivator.bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
            protocolProviderFactory = (ProtocolProviderFactory) GuiActivator
                    .bundleContext.getService(serRefs[0]);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("GuiActivator : " + ex);
        }

        return protocolProviderFactory;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            ServiceReference configReference = bundleContext
                .getServiceReference(ConfigurationService.class.getName());

            configService = (ConfigurationService) bundleContext
                .getService(configReference);
        }

        return configService;
    }

    /**
     * Returns the <tt>MetaHistoryService</tt> obtained from the bundle
     * context.
     * @return the <tt>MetaHistoryService</tt> obtained from the bundle
     * context
     */
    public static MetaHistoryService getMetaHistoryService()
    {
        if (metaHistoryService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(MetaHistoryService.class.getName());

            if (serviceReference != null)
                metaHistoryService = (MetaHistoryService) bundleContext
                    .getService(serviceReference);
        }

        return metaHistoryService;
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
            ServiceReference clistReference = bundleContext
                .getServiceReference(MetaContactListService.class.getName());

            metaCListService = (MetaContactListService) bundleContext
                    .getService(clistReference);
        }

        return metaCListService;
    }

    /**
     * Returns the <tt>CallHistoryService</tt> obtained from the bundle
     * context.
     * @return the <tt>CallHistoryService</tt> obtained from the bundle
     * context
     */
    public static CallHistoryService getCallHistoryService()
    {
        if (callHistoryService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(CallHistoryService.class.getName());

            callHistoryService = (CallHistoryService) bundleContext
                .getService(serviceReference);
        }

        return callHistoryService;
    }

    /**
     * Returns the <tt>AudioNotifierService</tt> obtained from the bundle
     * context.
     * @return the <tt>AudioNotifierService</tt> obtained from the bundle
     * context
     */
    public static AudioNotifierService getAudioNotifier()
    {
        if (audioNotifierService == null)
        {
            ServiceReference serviceReference
                = bundleContext
                    .getServiceReference(AudioNotifierService.class.getName());

            if (serviceReference != null)
                audioNotifierService
                    = (AudioNotifierService)
                        bundleContext.getService(serviceReference);
        }
        return audioNotifierService;
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
     * Returns the current implementation of the <tt>UIService</tt>.
     * @return the current implementation of the <tt>UIService</tt>
     */
    public static UIServiceImpl getUIService()
    {
        return uiService;
    }

    /**
     * Returns the <tt>SystrayService</tt> obtained from the bundle context.
     *
     * @return the <tt>SystrayService</tt> obtained from the bundle context
     */
    public static SystrayService getSystrayService()
    {
        if (systrayService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(SystrayService.class.getName());

            if(serviceReference == null)
                return null;

            systrayService = (SystrayService) bundleContext
                .getService(serviceReference);
        }

        return systrayService;
    }

    /**
     * Returns the <tt>KeybindingsService</tt> obtained from the bundle context.
     *
     * @return the <tt>KeybindingsService</tt> obtained from the bundle context
     */
    public static KeybindingsService getKeybindingsService()
    {
        if (keybindingsService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(KeybindingsService.class.getName());

            keybindingsService = (KeybindingsService) bundleContext
                .getService(serviceReference);
        }

        return keybindingsService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
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

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        if (notificationService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(NotificationService.class.getName());

            notificationService = (NotificationService) bundleContext
                .getService(serviceReference);
        }

        return notificationService;
    }

    /**
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(FileAccessService.class.getName());

            fileAccessService = (FileAccessService) bundleContext
                .getService(serviceReference);
        }

        return fileAccessService;
    }

    /**
     * Returns the <tt>DesktopService</tt> obtained from the bundle context.
     *
     * @return the <tt>DesktopService</tt> obtained from the bundle context
     */
    public static DesktopService getDesktopService()
    {
        if (desktopService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(DesktopService.class.getName());

            desktopService = (DesktopService) bundleContext
                .getService(serviceReference);
        }

        return desktopService;
    }

    /**
     * Returns an instance of the <tt>MediaService</tt> obtained from the
     * bundle context.
     * @return an instance of the <tt>MediaService</tt> obtained from the
     * bundle context
     */
    public static MediaService getMediaService()
    {
        if (mediaService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(MediaService.class.getName());

            mediaService = (MediaService) bundleContext
                .getService(serviceReference);
        }

        return mediaService;
    }

    /**
     * Implements the <tt>ServiceListener</tt>. Verifies whether the
     * passed event concerns a <tt>NotificationService</tt> and if so
     * initiates the user interface NotificationManager.
     */
    private static class NotificationServiceListener implements ServiceListener
    {
        /**
         * Implements the <tt>ServiceListener</tt> method. Verifies whether the
         * passed event concerns a <tt>NotificationService</tt> and if so
         * initiates the NotificationManager.
         *
         * @param event The <tt>ServiceEvent</tt> object.
         */
        public void serviceChanged(ServiceEvent event)
        {
            // if the event is caused by a bundle being stopped, we don't want
            // to know
            if (event.getServiceReference().getBundle().getState()
                    == Bundle.STOPPING)
            {
                return;
            }

            Object service = GuiActivator.bundleContext.getService(event
                .getServiceReference());

            // we don't care if the source service is not a notification service
            if (!(service instanceof NotificationService))
            {
                return;
            }

            if (event.getType() == ServiceEvent.REGISTERED)
            {
                NotificationManager.registerGuiNotifications();
            }
        }
    }

    /**
     * Sets the <tt>contactList</tt> component currently used to show the
     * contact list.
     * @param list the contact list object to set
     */
    public static void setContactList(TreeContactList list)
    {
        contactList = list;
    }

    /**
     * Returns the component used to show the contact list.
     * @return the component used to show the contact list
     */
    public static TreeContactList getContactList()
    {
        return contactList;
    }
}
