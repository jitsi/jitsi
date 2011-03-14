/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ldap;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the LDAP plug-in which provides
 * support for LDAP contact sources.
 *
 * @author Sebastien Vincent
 */
public class LdapActivator implements BundleActivator
{
    /**
     * The <tt>BundleContext</tt> in which the LDAP plug-in is started.
     */
    private static BundleContext bundleContext = null;

    /**
     * LDAP service.
     */
    private static LdapService ldapService = null;

    /**
     * The cached reference to the <tt>PhoneNumberI18nService</tt> instance used
     * by the functionality of the LDAP plug-in and fetched from its
     * <tt>BundleContext</tt>.
     */
    private static PhoneNumberI18nService phoneNumberI18nService;

    /**
     * Reference to the resource management service
     */
    private static ResourceManagementService resourceService;

    /**
     * List of contact source service registrations.
     */
    private static Map<LdapContactSourceService, ServiceRegistration> cssList =
        new HashMap<LdapContactSourceService, ServiceRegistration>();

    /**
     * Starts the LDAP plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the LDAP
     * plug-in is to be started
     * @throws Exception if anything goes wrong while starting the LDAP
     * plug-in
     * @see BundleActivator#start(BundleContext)
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        LdapActivator.bundleContext = bundleContext;

        /* registers the configuration form */
        Dictionary<String, String> properties =
            new Hashtable<String, String>();
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.ADVANCED_TYPE);

        bundleContext.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                "net.java.sip.communicator.plugin.ldap.configform.LdapConfigForm",
                getClass().getClassLoader(),
                "impl.ldap.PLUGIN_ICON",
                "impl.ldap.CONFIG_FORM_TITLE",
                2000, true),
            properties);

        if(getLdapService().getServerSet().size() == 0)
        {
            return;
        }

        for(LdapDirectory ldapDir : getLdapService().getServerSet())
        {
            if(!ldapDir.getSettings().isEnabled())
            {
                continue;
            }

            enableContactSource(ldapDir);
        }
    }

    /**
     * Get LDAP service.
     *
     * @return LDAP service
     */
    public static LdapService getLdapService()
    {
        if(ldapService == null)
        {
            ldapService = ServiceUtils.getService(bundleContext,
                    LdapService.class);
        }

        return ldapService;
    }

    /**
     * Stops the LDAP plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the LDAP
     * plug-in is to be stopped
     * @throws Exception if anything goes wrong while stopping the LDAP
     * plug-in
     * @see BundleActivator#stop(BundleContext)
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        for(Map.Entry<LdapContactSourceService, ServiceRegistration> entry :
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
    }

    /**
     * Enable contact source service with specified LDAP directory.
     *
     * @param ldapDir LDAP directory
     */
    public static void enableContactSource(LdapDirectory ldapDir)
    {
        LdapContactSourceService css = new LdapContactSourceService(
                ldapDir);
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
    }

    /**
     * Disable contact source service with specified LDAP directory.
     *
     * @param ldapDir LDAP directory
     */
    public static void disableContactSource(LdapDirectory ldapDir)
    {
        LdapContactSourceService found = null;

        for(Map.Entry<LdapContactSourceService, ServiceRegistration> entry :
            cssList.entrySet())
        {
            String cssName =
                entry.getKey().getLdapDirectory().getSettings().getName();
            String name = ldapDir.getSettings().getName();
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
}
