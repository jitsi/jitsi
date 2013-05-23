/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.thunderbird;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Bundle-Activator for the Thunderbird address book contact source plug-in.
 *
 * @author Ingo Bauersachs
 */
public class ThunderbirdActivator
    implements BundleActivator
{
    /** OSGi context. */
    private static BundleContext bundleContext;

    /** Active address book registrations. */
    private static Map<ThunderbirdContactSourceService, ServiceRegistration>
        registrations;

    /**
     * Gets the configuration service.
     * @return the configuration service.
     */
    static ConfigurationService getConfigService()
    {
        return ServiceUtils.getService(bundleContext,
            ConfigurationService.class);
    }

    /**
     * Gets the resource service.
     * @return the resource service.
     */
    static ResourceManagementService getResources()
    {
        return ServiceUtils.getService(bundleContext,
            ResourceManagementService.class);
    }

    /**
     * Gets all registered Thunderbird address book services.
     * @return all registered Thunderbird address book services.
     */
    static List<ThunderbirdContactSourceService> getActiveServices()
    {
        return new LinkedList<ThunderbirdContactSourceService>(
            registrations.keySet());
    }

    /**
     * Loads and registers an address book service.
     * @param config the name of the base property of the service to load.
     */
    static void add(String config)
    {
        ThunderbirdContactSourceService service
            = new ThunderbirdContactSourceService(config);
        registrations.put(service,
            bundleContext.registerService(
                ContactSourceService.class.getName(), service, null));
    }

    /**
     * Stops an address book service and deletes the corresponding configuration
     * data.
     *
     * @param service the address book instance to remove.
     */
    static void remove(ThunderbirdContactSourceService service)
    {
        registrations.get(service).unregister();
        registrations.remove(service);
        ConfigurationService config = getConfigService();
        config.removeProperty(service.getBaseConfigProperty());
        for (String prop : config.getPropertyNamesByPrefix(
            service.getBaseConfigProperty(), false))
        {
            config.removeProperty(prop);
        }
    }

    /**
     * Searches the configuration for Thunderbird address books and registers a
     * {@link ContactSourceService} for each found config.
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        ThunderbirdActivator.bundleContext = bundleContext;

        ConfigurationService config = getConfigService();
        List<String> configs =
            config.getPropertyNamesByPrefix(
                ThunderbirdContactSourceService.PNAME_BASE_THUNDERBIRD_CONFIG,
                false);

        registrations = new HashMap
            <ThunderbirdContactSourceService, ServiceRegistration>();
        for (String cfg : configs)
        {
            String value = config.getString(cfg);
            if (value != null && cfg.endsWith(value))
            {
                add(cfg);
            }
        }

        /* registers the configuration form */
        Dictionary<String, String> properties
            = new Hashtable<String, String>();
        properties.put(
            ConfigurationForm.FORM_TYPE,
            ConfigurationForm.CONTACT_SOURCE_TYPE);

        bundleContext.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                ThunderbirdConfigForm.class.getName(),
                getClass().getClassLoader(),
                null,
                "plugin.thunderbird.CONFIG_FORM_TITLE"),
            properties);
    }

    /**
     * Unregisters all {@link ContactSourceService}s that were registered by
     * this activator.
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        for (ServiceRegistration sr : registrations.values())
        {
            sr.unregister();
        }

        registrations = null;
    }
}
