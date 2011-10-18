/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.ldap;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.contactsource.ContactSourceService;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.protocol.PhoneNumberI18nService;
import net.java.sip.communicator.service.resources.ResourceManagementService;

/**
 * Activates the LdapService
 *
 * @author Sebastien Mazy
 */
public class LdapActivator
    implements BundleActivator
{
    /**
     * the logger for this class
     */
    private static Logger logger =
        Logger.getLogger(LdapActivator.class);

    /**
     * The <tt>BundleContext</tt> in which the LDAP plug-in is started.
     */
    private static BundleContext bundleContext = null;

    /**
     * instance of the service
     */
    private static LdapServiceImpl ldapService = null;

    /**
     * The service through which we access resources.
     */
    private static ResourceManagementService resourceService = null;

    /**
     * Get LDAP service.
     *
     * @return LDAP service
     */
    public static LdapService getLdapService()
    {
        return ldapService;
    }

    /**
     * The cached reference to the <tt>PhoneNumberI18nService</tt> instance used
     * by the functionality of the LDAP plug-in and fetched from its
     * <tt>BundleContext</tt>.
     */
    private static PhoneNumberI18nService phoneNumberI18nService;

    /**
     * List of contact source service registrations.
     */
    private static Map<LdapContactSourceService, ServiceRegistration> cssList =
        new HashMap<LdapContactSourceService, ServiceRegistration>();

    /**
     * Starts the LDAP service
     *
     * @param bundleContext BundleContext
     * @throws Exception if something goes wrong when starting service
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        LdapActivator.bundleContext = bundleContext;

        try
        {
            logger.logEntry();

            /* Creates and starts the LDAP service. */
            ldapService =
                new LdapServiceImpl();

            ldapService.start(bundleContext);

            bundleContext.registerService(
                    LdapService.class.getName(), ldapService, null);

            logger.trace("LDAP Service ...[REGISTERED]");

            if(ldapService.getServerSet().size() == 0)
            {
                return;
            }

            for(LdapDirectory ldapDir : getLdapService().getServerSet())
            {
                if(!ldapDir.getSettings().isEnabled())
                {
                    continue;
                }

                registerContactSource(ldapDir);
            }
        }
        finally
        {
            logger.logExit();
        }
    }

    /**
     * Stops the LDAP service
     *
     * @param bundleContext BundleContext
     * @throws Exception if something goes wrong when stopping service
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if(ldapService != null)
            ldapService.stop(bundleContext);

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
     * Returns a reference to a ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * ResourceManagementService.
     */
    public static ResourceManagementService getResourceService()
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
     * Enable contact source service with specified LDAP directory.
     *
     * @param ldapDir LDAP directory
     */
    public static ContactSourceService registerContactSource(
                                                        LdapDirectory ldapDir)
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

        return css;
    }

    /**
     * Disable contact source service with specified LDAP directory.
     *
     * @param ldapDir LDAP directory
     */
    public static void unregisterContactSource(LdapDirectory ldapDir)
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
}
