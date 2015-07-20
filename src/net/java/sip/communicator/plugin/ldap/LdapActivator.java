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
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
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
     * Reference to the resource management service
     */
    private static ResourceManagementService resourceService;

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
