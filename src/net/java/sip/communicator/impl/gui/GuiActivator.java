/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import java.util.*;

import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.desktop.*;
import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.service.replacement.smilies.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.audionotifier.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import javax.swing.*;

/**
 * The GUI Activator class.
 *
 * @author Yana Stamcheva
 */
public class GuiActivator implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>GuiActivator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(GuiActivator.class);

    private static UIServiceImpl uiService = null;

    /**
     * OSGi bundle context.
     */
    public static BundleContext bundleContext;

    private static ConfigurationService configService;

    private static MetaHistoryService metaHistoryService;

    private static MetaContactListService metaCListService;

    private static CallHistoryService callHistoryService;

    private static AudioNotifierService audioNotifierService;

    private static BrowserLauncherService browserLauncherService;

    private static SystrayService systrayService;

    private static ResourceManagementService resourcesService;

    private static KeybindingsService keybindingsService;

    private static FileAccessService fileAccessService;

    private static DesktopService desktopService;

    private static MediaService mediaService;

    private static SmiliesReplacementService smiliesService;

    private static GlobalStatusService globalStatusService;

    private static AccountManager accountManager;

    private static NotificationService notificationService;

    private static List<ContactSourceService> contactSources;

    private static List<CustomContactActionsService<?>> contactActionsServices;

    private static SecurityAuthority securityAuthority;

    private static DemuxContactSourceService demuxContactSourceService;

    private static GlobalDisplayDetailsService globalDisplayDetailsService;

    private static AlertUIService alertUIService;
    
    private static CredentialsStorageService credentialsService;

    private static final Map<Object, ProtocolProviderFactory>
        providerFactoriesMap = new Hashtable<Object, ProtocolProviderFactory>();

    private static final Map<String, ReplacementService>
        replacementSourcesMap = new Hashtable<String, ReplacementService>();

    /**
     * Indicates if this bundle has been started.
     */
    public static boolean isStarted = false;

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

        ConfigurationUtils.loadGuiConfigurations();

        try
        {
            alertUIService = new AlertUIServiceImpl();
            // Registers an implementation of the AlertUIService.
            bundleContext.registerService(  AlertUIService.class.getName(),
                                            alertUIService,
                                            null);

            // Registers an implementation of the ImageLoaderService.
            bundleContext.registerService(  ImageLoaderService.class.getName(),
                                            new ImageLoaderServiceImpl(),
                                            null);

            // Create the ui service
            uiService = new UIServiceImpl();

            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    uiService.loadApplicationGui();

                    GuiActivator.getConfigurationService()
                                .addPropertyChangeListener(uiService);

                    bundleContext.addServiceListener(uiService);

                    // don't block the ui thread
                    // with registering services, as they are executed
                    // in the same thread as registering
                    new Thread(new Runnable()
                    {
                        public void run()
                        {
                            if (logger.isInfoEnabled())
                                logger.info("UI Service...[  STARTED ]");

                            bundleContext.registerService(
                                UIService.class.getName(),
                                uiService,
                                null);

                            if (logger.isInfoEnabled())
                                logger.info("UI Service ...[REGISTERED]");

                            // UIServiceImpl also implements ShutdownService.
                            bundleContext.registerService(
                                ShutdownService.class.getName(),
                                uiService,
                                null);
                        }
                    }).start();
                }
            });

            logger.logEntry();
        }
        finally
        {
            logger.logExit();
        }
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
        if (logger.isInfoEnabled())
            logger.info("UI Service ...[STOPPED]");
        isStarted = false;

        GuiActivator.getConfigurationService()
            .removePropertyChangeListener(uiService);

        bContext.removeServiceListener(uiService);
        alertUIService.dispose();
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
            serRefs
                = bundleContext.getServiceReferences(
                        ProtocolProviderFactory.class.getName(),
                        null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("LoginManager : " + e);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory
                    = (ProtocolProviderFactory)
                        bundleContext.getService(serRef);

                providerFactoriesMap.put(
                        serRef.getProperty(ProtocolProviderFactory.PROTOCOL),
                        providerFactory);
            }
        }
        return providerFactoriesMap;
    }

    /**
     * Returns the <tt>AccountManager</tt> obtained from the bundle context.
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        if(accountManager == null)
        {
            accountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);
        }
        return accountManager;
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
            configService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
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
            metaHistoryService
                = ServiceUtils.getService(
                        bundleContext,
                        MetaHistoryService.class);
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
            metaCListService
                = ServiceUtils.getService(
                        bundleContext,
                        MetaContactListService.class);
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
            callHistoryService
                = ServiceUtils.getService(
                        bundleContext,
                        CallHistoryService.class);
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
            audioNotifierService
                = ServiceUtils.getService(
                        bundleContext,
                        AudioNotifierService.class);
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
            browserLauncherService
                = ServiceUtils.getService(
                        bundleContext,
                        BrowserLauncherService.class);
        }
        return browserLauncherService;
    }

    /**
     * Returns the <tt>GlobalStatusService</tt> obtained from the bundle
     * context.
     * @return the <tt>GlobalStatusService</tt> obtained from the bundle
     * context
     */
    public static GlobalStatusService getGlobalStatusService()
    {
        if (globalStatusService == null)
        {
            globalStatusService
                = ServiceUtils.getService(
                        bundleContext,
                        GlobalStatusService.class);
        }
        return globalStatusService;
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
     * Returns the implementation of the <tt>AlertUIService</tt>.
     * @return the implementation of the <tt>AlertUIService</tt>
     */
    public static AlertUIService getAlertUIService()
    {
        return alertUIService;
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
            systrayService
                = ServiceUtils.getService(bundleContext, SystrayService.class);
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
            keybindingsService
                = ServiceUtils.getService(
                        bundleContext,
                        KeybindingsService.class);
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
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
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
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
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
            desktopService
                = ServiceUtils.getService(bundleContext, DesktopService.class);
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
            mediaService
                = ServiceUtils.getService(bundleContext, MediaService.class);
        }
        return mediaService;
    }

    /**
     * Returns the <tt>DemuxContactSourceService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>DemuxContactSourceService</tt> obtained from the bundle
     * context
     */
    public static DemuxContactSourceService getDemuxContactSourceService()
    {
        if (demuxContactSourceService == null)
        {
            demuxContactSourceService
                = ServiceUtils.getService(
                        bundleContext,
                        DemuxContactSourceService.class);
        }
        return demuxContactSourceService;
    }

    /**
     * Returns the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>GlobalDisplayDetailsService</tt> obtained from the bundle
     * context
     */
    public static GlobalDisplayDetailsService getGlobalDisplayDetailsService()
    {
        if (globalDisplayDetailsService == null)
        {
            globalDisplayDetailsService
                = ServiceUtils.getService(
                        bundleContext,
                        GlobalDisplayDetailsService.class);
        }
        return globalDisplayDetailsService;
    }

    /**
     * Returns a list of all registered contact sources.
     * @return a list of all registered contact sources
     */
    public static List<ContactSourceService> getContactSources()
    {
        contactSources = new Vector<ContactSourceService>();

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs =
                bundleContext.getServiceReferences(
                    ContactSourceService.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {
            logger.error("GuiActivator : " + e);
        }

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                ContactSourceService contactSource
                    = (ContactSourceService) bundleContext.getService(serRef);

                contactSources.add(contactSource);
            }
        }
        return contactSources;
    }

    /**
     * Returns all <tt>ReplacementService</tt>s obtained from the bundle
     * context.
     *
     * @return all <tt>ReplacementService</tt> implementation obtained from the
     *         bundle context
     */
    public static Map<String, ReplacementService> getReplacementSources()
    {
        ServiceReference[] serRefs = null;
        try
        {
            // get all registered sources
            serRefs
                = bundleContext.getServiceReferences(ReplacementService.class
                    .getName(), null);

        }
        catch (InvalidSyntaxException e)
        {
            logger.error("Error : " + e);
        }

        if (serRefs != null)
        {
            for (int i = 0; i < serRefs.length; i++)
            {
                ReplacementService replacementSources =
                    (ReplacementService) bundleContext.getService(serRefs[i]);

                replacementSourcesMap.put((String)serRefs[i]
                    .getProperty(ReplacementService.SOURCE_NAME),
                    replacementSources);
            }
        }
        return replacementSourcesMap;
    }

    /**
     * Returns the <tt>SmiliesReplacementService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>SmiliesReplacementService</tt> implementation obtained
     * from the bundle context
     */
    public static SmiliesReplacementService getSmiliesReplacementSource()
    {
        if (smiliesService == null)
        {
            smiliesService
                = ServiceUtils.getService(bundleContext,
                    SmiliesReplacementService.class);
        }
        return smiliesService;
    }

    /**
     * Returns the <tt>SecurityAuthority</tt> implementation registered to
     * handle security authority events.
     *
     * @return the <tt>SecurityAuthority</tt> implementation obtained
     * from the bundle context
     */
    public static SecurityAuthority getSecurityAuthority()
    {
        if (securityAuthority == null)
        {
            securityAuthority
                = ServiceUtils.getService(bundleContext,
                    SecurityAuthority.class);
        }
        return securityAuthority;
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
            notificationService
                = ServiceUtils.getService(
                        bundleContext,
                        NotificationService.class);
        }
        return notificationService;
    }

    /**
     * Returns the <tt>SecurityAuthority</tt> implementation registered to
     * handle security authority events.
     *
     * @param protocolName protocol name
     * @return the <tt>SecurityAuthority</tt> implementation obtained
     * from the bundle context
     */
    public static SecurityAuthority getSecurityAuthority(String protocolName)
    {
        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + protocolName + ")";

        SecurityAuthority securityAuthority = null;
        try
        {
            ServiceReference[] serRefs
                = bundleContext.getServiceReferences(
                    SecurityAuthority.class.getName(), osgiFilter);

            if (serRefs != null && serRefs.length > 0)
                securityAuthority
                    = (SecurityAuthority) bundleContext
                        .getService(serRefs[0]);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("GuiActivator : " + ex);
        }

        return securityAuthority;
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

    /**
     * Returns the list of wrapped protocol providers.
     *
     * @param providers the list of protocol providers
     * @return an array of wrapped protocol providers
     */
    public static Account[] getAccounts(List<ProtocolProviderService> providers)
    {
        Iterator<ProtocolProviderService> accountsIter = providers.iterator();
        List<Account> accounts = new ArrayList<Account>();

        while (accountsIter.hasNext())
            accounts.add(new Account(accountsIter.next()));

        return accounts.toArray(new Account[accounts.size()]);
    }

    /**
     * Returns the preferred account if there's one.
     *
     * @return the <tt>ProtocolProviderService</tt> corresponding to the
     * preferred account
     */
    public static ProtocolProviderService getPreferredAccount()
    {
        // check for preferred wizard
        String prefWName = GuiActivator.getResources().
            getSettingsString("impl.gui.PREFERRED_ACCOUNT_WIZARD");
        if(prefWName == null || prefWName.length() <= 0)
            return null;

        ServiceReference[] accountWizardRefs = null;
        try
        {
            accountWizardRefs = GuiActivator.bundleContext
                .getServiceReferences(
                    AccountRegistrationWizard.class.getName(),
                    null);
        }
        catch (InvalidSyntaxException ex)
        {
            // this shouldn't happen since we're providing no parameter string
            // but let's log just in case.
            logger.error(
                "Error while retrieving service refs", ex);
            return null;
        }

        // in case we found any, add them in this container.
        if (accountWizardRefs != null)
        {
            if (logger.isDebugEnabled())
                logger.debug("Found "
                         + accountWizardRefs.length
                         + " already installed providers.");

            for (int i = 0; i < accountWizardRefs.length; i++)
            {
                AccountRegistrationWizard wizard
                    = (AccountRegistrationWizard) GuiActivator.bundleContext
                        .getService(accountWizardRefs[i]);

                // is it the preferred protocol ?
                if(wizard.getClass().getName().equals(prefWName))
                {
                    for (ProtocolProviderFactory providerFactory : GuiActivator
                            .getProtocolProviderFactories().values())
                    {
                        ServiceReference serRef;
                        ProtocolProviderService protocolProvider;

                        for (AccountID accountID
                                : providerFactory.getRegisteredAccounts())
                        {
                            serRef = providerFactory.getProviderForAccount(
                                        accountID);

                            protocolProvider = (ProtocolProviderService)
                                GuiActivator.bundleContext
                                    .getService(serRef);

                            if (protocolProvider.getAccountID()
                                    .getProtocolDisplayName()
                                        .equals(wizard.getProtocolName()))
                            {
                                return protocolProvider;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns a reference to a CredentialsStorageService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * CredentialsStorageService.
     */
    public static CredentialsStorageService getCredentialsStorageService()
    {
        if (credentialsService == null)
        {
            ServiceReference credentialsReference
                = bundleContext.getServiceReference(
                    CredentialsStorageService.class.getName());
            credentialsService
                = (CredentialsStorageService) bundleContext
                                        .getService(credentialsReference);
        }
        return credentialsService;
    }
}
