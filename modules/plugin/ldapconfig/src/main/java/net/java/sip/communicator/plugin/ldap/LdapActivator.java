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
package net.java.sip.communicator.plugin.ldap;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.ldap.*;

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the LDAP plug-in which provides
 * support for LDAP contact sources.
 *
 * @author Sebastien Vincent
 */
public class LdapActivator extends DependentActivator
{
    /**
     * LDAP service.
     */
    private static LdapService ldapService = null;

    /**
     * Reference to the resource management service
     */
    private static ResourceManagementService resourceService;

    public LdapActivator()
    {
        super(
            ResourceManagementService.class,
            LdapService.class
        );
    }

    /**
     * Starts the LDAP plug-in.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the LDAP
     * plug-in is to be started
     */
    @Override
    public void startWithServices(BundleContext bundleContext)
    {
        ldapService = getService(LdapService.class);
        resourceService = getService(ResourceManagementService.class);

        /* registers the configuration form */
        Dictionary<String, String> properties =
            new Hashtable<String, String>();
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.CONTACT_SOURCE_TYPE);

        bundleContext.registerService(
            ConfigurationForm.class.getName(),
            new LazyConfigurationForm(
                "net.java.sip.communicator.plugin.ldap.configform.LdapConfigForm",
                getClass().getClassLoader(),
                "impl.ldap.PLUGIN_ICON",
                "impl.ldap.CONFIG_FORM_TITLE",
                2000, false),
            properties);
    }

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
     * Returns a reference to a ResourceManagementService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * ResourceManagementService.
     */
    public static ResourceManagementService getResourceManagementService()
    {
        return resourceService;
    }
}
