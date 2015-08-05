/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.ldap;

import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

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
     * List of contact source service registrations.
     */
    private static Map<LdapContactSourceService, ServiceRegistration> cssList =
        new HashMap<LdapContactSourceService, ServiceRegistration>();

    /**
     * The registered PhoneNumberI18nService.
     */
    private static PhoneNumberI18nService phoneNumberI18nService;

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
     * Enable contact source service with specified LDAP directory.
     *
     * @param ldapDir LDAP directory
     * @return an LDAP <tt>ContactSourceService</tt> instance
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

    /**
     * Returns the PhoneNumberI18nService.
     * @return returns the PhoneNumberI18nService.
     */
    public static PhoneNumberI18nService getPhoneNumberI18nService()
    {
        if(phoneNumberI18nService == null)
        {
            phoneNumberI18nService = ServiceUtils.getService(
                bundleContext,
                PhoneNumberI18nService.class);
        }

        return phoneNumberI18nService;
    }
}
