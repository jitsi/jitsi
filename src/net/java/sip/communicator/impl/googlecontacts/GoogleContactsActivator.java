/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.googlecontacts;

import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.googlecontacts.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activates the Google Contacts Service
 *
 * @author Sebastien Vincent
 */
public class GoogleContactsActivator implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>GoogleContactsActivator</tt> class
     * and its instances for logging output.
     */
    private final Logger logger = Logger.getLogger(
            GoogleContactsActivator.class);

    /**
     * The OSGi <tt>ServiceRegistration</tt> of
     * <tt>GoogleContactsServiceImpl</tt>.
     */
    private ServiceRegistration serviceRegistration = null;

    /**
     * BundleContext from the OSGI bus.
     */
    private static BundleContext bundleContext;

    /**
     * Reference to the configuration service
     */
    private static ConfigurationService configService;

    /**
     * Reference to the credentials service
     */
    private static CredentialsStorageService credentialsService;

    /**
     * Reference to the resource management service
     */
    private static ResourceManagementService resourceService;

    /**
     * The cached reference to the <tt>PhoneNumberI18nService</tt> instance used
     * by the functionality of the Google Contacts plug-in and fetched from its
     * <tt>BundleContext</tt>.
     */
    private static PhoneNumberI18nService phoneNumberI18nService;

    /**
     * Google contacts service.
     */
    private static GoogleContactsServiceImpl googleContactsService;

    /**
     * List of contact source service registrations.
     */
    private static Map<GoogleContactsSourceService, ServiceRegistration> cssList
        = new HashMap<GoogleContactsSourceService, ServiceRegistration>();

    /**
     * Gets the <tt>PhoneNumberI18nService</tt> to be used by the functionality
     * of the addrbook plug-in.
     *
     * @return the <tt>PhoneNumberI18nService</tt> to be used by the
     * functionality of the addrbook plug-in
     */
    public static PhoneNumberI18nService getPhoneNumberI18nService()
    {
        if (phoneNumberI18nService == null)
        {
            phoneNumberI18nService
                = ServiceUtils.getService(
                        bundleContext,
                        PhoneNumberI18nService.class);
        }
        return phoneNumberI18nService;
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
        if(configService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                        ConfigurationService.class.getName());
            configService
                = (ConfigurationService) bundleContext.getService(
                        confReference);
        }
        return configService;
    }

    /**
     * Returns a reference to a GoogleContactsService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the GoogleContactsService
     */
    public static GoogleContactsServiceImpl getGoogleContactsService()
    {
        return googleContactsService;
    }

    /**
     * Returns a reference to a CredentialsStorageConfigurationService
     * implementation currently registered in the bundle context or null if no
     * such implementation was found.
     *
     * @return a currently valid implementation of the
     * CredentialsStorageService.
     */
    public static CredentialsStorageService getCredentialsService()
    {
        if(credentialsService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                        CredentialsStorageService.class.getName());
            credentialsService
                = (CredentialsStorageService) bundleContext.getService(
                        confReference);
        }
        return credentialsService;
    }

    /**
     * Returns a reference to a ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * ResourceManagementService.
     */
    public static ResourceManagementService getResourceManagementService()
    {
        if(resourceService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                        ResourceManagementService.class.getName());
            resourceService
                = (ResourceManagementService) bundleContext.getService(
                        confReference);
        }
        return resourceService;
    }

    /**
     * Starts the Google Contacts service
     *
     * @param bundleContext BundleContext
     * @throws Exception if something goes wrong when starting service
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Started.");

        GoogleContactsActivator.bundleContext = bundleContext;
        googleContactsService =
            new GoogleContactsServiceImpl();
        serviceRegistration =
            bundleContext.registerService(GoogleContactsService.class.getName(),
                googleContactsService, null);

        /* registers the configuration form */
        Dictionary<String, String> properties =
            new Hashtable<String, String>();
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.CONTACT_SOURCE_TYPE);

        bundleContext.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                "net.java.sip.communicator.impl.googlecontacts.configform.GoogleContactsConfigForm",
                getClass().getClassLoader(),
                "impl.googlecontacts.PLUGIN_ICON",
                "impl.googlecontacts.CONFIG_FORM_TITLE",
                2000, false),
            properties);

        bundleContext.addServiceListener(new ServiceListener()
        {
            public void serviceChanged(ServiceEvent serviceEvent)
            {
                GoogleContactsActivator.this.serviceChanged(serviceEvent);
            }
        });
        if (logger.isDebugEnabled())
            logger.debug("Google Contacts Service ... [REGISTERED]");
    }

    /**
     * Implements the <tt>ServiceListener</tt> method. Verifies whether the
     * passed event concerns a <tt>ProtocolProviderService</tt> and adds the
     * corresponding UI controls.
     *
     * @param event The <tt>ServiceEvent</tt> object.
     */
    private void serviceChanged(ServiceEvent event)
    {
        ServiceReference serviceRef = event.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to
        // know
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
        {
            return;
        }

        Object service = bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
        {
            return;
        }

        // we don't care if the protocol provider is not a Jabber ones
        if(((ProtocolProviderService)service).getProtocolName() !=
            ProtocolNames.JABBER)
        {
            return;
        }

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            this.handleProviderAdded((ProtocolProviderService) service);
            break;
        case ServiceEvent.UNREGISTERING:
            this.handleProviderRemoved((ProtocolProviderService) service);
            break;
        }
    }

    /**
     * Notifies this manager that a specific
     * <tt>ProtocolProviderService</tt> has been registered as a service.
     *
     * @param provider the <tt>ProtocolProviderService</tt> which has been
     * registered as a service.
     */
    private void handleProviderAdded(final ProtocolProviderService provider)
    {
        // don't block felix, can be blocked if master password is set
        new Thread(new Runnable()
        {
            public void run()
            {
                String className = provider.getClass().getName();
                className = className.substring(0, className.lastIndexOf('.'));
                String acc = ProtocolProviderFactory.findAccountPrefix(
                        bundleContext, provider.getAccountID(), className);
                String password = getCredentialsService().loadPassword(acc);

                if(configService.getBoolean(acc + ".GOOGLE_CONTACTS_ENABLED",
                    true))
                {
                    enableContactSource(
                        provider.getAccountID().getAccountAddress(),
                        password);
                }
            }
        }).start();
    }

    /**
     * Notifies this manager that a specific
     * <tt>ProtocolProviderService</tt> has been unregistered as a service.
     *
     * @param provider the <tt>ProtocolProviderService</tt> which has been
     * unregistered as a service.
     */
    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        disableContactSource(provider.getAccountID().getAccountAddress());
    }

    /**
     * Stops the Google Contacts service.
     *
     * @param bundleContext BundleContext
     * @throws Exception if something goes wrong when stopping service
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if (serviceRegistration != null)
        {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }

        /* remove contact source services */
        for(Map.Entry<GoogleContactsSourceService, ServiceRegistration> entry :
            cssList.entrySet())
        {
            if (entry.getValue() != null)
            {
                try
                {
                    entry.getValue().unregister();
                }
                finally
                {
                    entry.getKey().stop();
                }
            }
        }
        cssList.clear();

        GoogleContactsActivator.bundleContext = null;
    }

    /**
     * Enable contact source service with specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param login login
     * @param password password
     * @return a <tt>GoogleContactsSourceService</tt> instance
     */
    public static GoogleContactsSourceService enableContactSource(
                                                String login, String password)
    {
        GoogleContactsSourceService css = new GoogleContactsSourceService(
                login, password);
        ServiceRegistration cssServiceRegistration = null;

        try
        {
            cssServiceRegistration
                = bundleContext.registerService(
                        ContactSourceService.class.getName(),
                        css,
                        null);
        }
        finally
        {
            if (cssServiceRegistration == null)
            {
                css.stop();
                css = null;
            }
            else
            {
                cssList.put(css, cssServiceRegistration);
            }
        }

        return css;
    }

    /**
     * Enable contact source service with specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param cnx <tt>GoogleContactsConnection</tt>
     * @return a <tt>GoogleContactsSourceService</tt> instance
     */
    public static GoogleContactsSourceService enableContactSource(
                                                GoogleContactsConnection cnx)
    {
        GoogleContactsSourceService css = new GoogleContactsSourceService(cnx);
        ServiceRegistration cssServiceRegistration = null;

        try
        {
            cssServiceRegistration
                = bundleContext.registerService(
                        ContactSourceService.class.getName(),
                        css,
                        null);
        }
        finally
        {
            if (cssServiceRegistration == null)
            {
                css.stop();
                css = null;
            }
            else
            {
                cssList.put(css, cssServiceRegistration);
            }
        }

        return css;
    }

    /**
     * Disable contact source service with specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param login login
     */
    public static void disableContactSource(String login)
    {
        GoogleContactsSourceService found = null;

        for(Map.Entry<GoogleContactsSourceService, ServiceRegistration> entry :
            cssList.entrySet())
        {
            String cssName = entry.getKey().getLogin();

            if(cssName.equals(login))
            {
                try
                {
                    entry.getValue().unregister();
                }
                finally
                {
                    entry.getKey().stop();
                }
                found = entry.getKey();
                break;
            }
        }

        if(found != null)
        {
            cssList.remove(found);
        }
    }

    /**
     * Disable contact source service with specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param cnx <tt>GoogleContactsConnection</tt>.
     */
    public static void disableContactSource(GoogleContactsConnection cnx)
    {
        GoogleContactsSourceService found = null;

        if(cnx == null)
        {
            return;
        }

        for(Map.Entry<GoogleContactsSourceService, ServiceRegistration> entry :
            cssList.entrySet())
        {
            String cssName = entry.getKey().getLogin();
            String name = cnx.getLogin();

            if(cssName.equals(name))
            {
                try
                {
                    entry.getValue().unregister();
                }
                finally
                {
                    entry.getKey().stop();
                }
                found = entry.getKey();
                break;
            }
        }

        if(found != null)
        {
            cssList.remove(found);
        }
    }
}
