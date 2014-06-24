/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ciscodirectory;

import static net.java.sip.communicator.util.ServiceUtils.*;

import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Activator for the Cisco Directory plugin.
 *
 * @author Fabien Cortina <fabien.cortina@gmail.com>
 */
public final class CiscoDirectoryActivator implements BundleActivator
{
    private final static String PROP_BASE =
            "net.java.sip.communicator.plugin.ciscodirectory";

    private static BundleContext bundleContext;

    private static PhoneNumberI18nService phoneNumberI18nService;

    private DirectorySettings settings;
    private ServiceRegistration<?> contactSourceRegistration;
    private ServiceRegistration<?> configFormRegistration;

    /**
     * @return an instance of ConfigurationService valid for that bundle.
     */
    static ConfigurationService getConfiguration()
    {
        return getService(bundleContext, ConfigurationService.class);
    }

    /**
     * @return an instance of ResourceManagementService valid for that bundle.
     */
    static ResourceManagementService getResources()
    {
        return getService(bundleContext, ResourceManagementService.class);
    }

    /**
     * @return an instance of PhoneNumberI18nService valid for that bundle.
     */
    static PhoneNumberI18nService getPhoneNumberI18nService()
    {
        if (phoneNumberI18nService == null)
        {
            phoneNumberI18nService =
                    getService(bundleContext, PhoneNumberI18nService.class);
        }
        return phoneNumberI18nService;
    }

    /**
     * @param resourceId the identifier of a localized resource
     * @return the localized string in the set locale.
     */
    static String _txt(String resourceId)
    {
        return getResources().getI18NString(resourceId);
    }

    /**
     * Starts the bundle.
     *
     * @param context the context of the bundle.
     */
    @Override
    public void start(BundleContext context)
    {
        bundleContext = context;

        loadSettings();
        registerContactSource();
        registerConfigForm();
    }

    /**
     * Loads the settings and store them in {@link #settings}.
     */
    private void loadSettings()
    {
        ConfigurationService configuration = getConfiguration();
        settings = new DirectorySettings(configuration, PROP_BASE);
    }

    /**
     * Registers an instance of ContactSourceService that queries the Cisco
     * directory, using the configuration stored in {@link #settings}.
     */
    private void registerContactSource()
    {
        contactSourceRegistration = bundleContext.registerService(
                ContactSourceService.class.getName(),
                new CiscoDirectoryContactSourceService(settings),
                null);
    }

    /**
     * Registers an instance of ConfigurationForm to manage the configuration
     * stored in [@link #settings}.
     */
    private void registerConfigForm()
    {
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConfigurationForm.FORM_TYPE,
                ConfigurationForm.CONTACT_SOURCE_TYPE);

        configFormRegistration = bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new CiscoDirectoryConfigForm(settings),
                properties);
    }

    /**
     * Stops the bundle and unregisters the configuration form and contact
     * source.
     * <p/>
     * Note: this does NOT remove the configuration itself.
     *
     * @param bundleContext the context of the bundle.
     */
    @Override
    public void stop(BundleContext bundleContext)
    {
        if (contactSourceRegistration != null)
        {
            contactSourceRegistration.unregister();
            contactSourceRegistration = null;
        }
        if (configFormRegistration != null)
        {
            configFormRegistration.unregister();
            configFormRegistration = null;
        }
        settings = null;
    }
}
