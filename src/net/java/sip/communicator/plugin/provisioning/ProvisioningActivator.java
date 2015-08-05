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
package net.java.sip.communicator.plugin.provisioning;

import java.util.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.provdisc.*;
import net.java.sip.communicator.service.provisioning.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.StringUtils;
import org.osgi.framework.*;

/**
 * Activator the provisioning system. It will gather provisioning URL depending
 * on the configuration (DHCP, manual, ...), retrieve configuration file and
 * push properties to the <tt>ConfigurationService</tt>.
 */
public class ProvisioningActivator
    implements BundleActivator
{
    /**
     * Logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(ProvisioningActivator.class);

    /**
     * The current BundleContext.
     */
    static BundleContext bundleContext = null;

    /**
     * A reference to the ConfigurationService implementation instance that
     * is currently registered with the bundle context.
     */
    private static ConfigurationService configurationService = null;

    /**
     * A reference to the CredentialsStorageService implementation instance
     * that is registered with the bundle context.
     */
    private static CredentialsStorageService credentialsService = null;

    /**
     * A reference to the NetworkAddressManagerService implementation instance
     * that is registered with the bundle context.
     */
    private static NetworkAddressManagerService netaddrService = null;

    /**
     * The user interface service.
     */
    private static UIService uiService;

    /**
     * The resource service.
     */
    private static ResourceManagementService resourceService;

    /**
     * Provisioning service.
     */
    private static ProvisioningServiceImpl provisioningService = null;

    /**
     * Indicates if the provisioning configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.provisionconfig.DISABLED";

    /**
     * Starts this bundle
     *
     * @param bundleContext BundleContext
     * @throws Exception if anything goes wrong during the start of the bundle
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Provisioning discovery [STARTED]");

        ProvisioningActivator.bundleContext = bundleContext;
        String url = null;

        provisioningService = new ProvisioningServiceImpl();

        // Show/hide provisioning configuration form.
        if(!getConfigurationService().getBoolean(DISABLED_PROP, false))
        {
            Dictionary<String, String> properties
                = new Hashtable<String, String>();
            properties.put( ConfigurationForm.FORM_TYPE,
                            ConfigurationForm.ADVANCED_TYPE);

            bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    "net.java.sip.communicator.plugin.provisioning.ProvisioningForm",
                    getClass().getClassLoader(),
                    "plugin.provisioning.PLUGIN_ICON",
                    "plugin.provisioning.PROVISIONING",
                    2000, true),
                properties);
        }

        String method = provisioningService.getProvisioningMethod();

        if(StringUtils.isNullOrEmpty(method, true) || method.equals("NONE"))
        {
            return;
        }

        ServiceReference serviceReferences[] = bundleContext.
            getServiceReferences(ProvisioningDiscoveryService.class.getName(),
                    null);

        /* search the provisioning discovery implementation that correspond to
         * the method name
         */
        if(serviceReferences != null)
        {
            for(ServiceReference ref : serviceReferences)
            {
                ProvisioningDiscoveryService provdisc =
                    (ProvisioningDiscoveryService)bundleContext.getService(ref);

                if(provdisc.getMethodName().equals(method))
                {
                    /* may block for sometime depending on the method used */
                    url = provdisc.discoverURL();
                    break;
                }
            }
        }

        provisioningService.start(url);

        bundleContext.registerService(
            ProvisioningService.class.getName(), provisioningService, null);

        if (logger.isDebugEnabled())
            logger.debug("Provisioning discovery [REGISTERED]");
    }

    /**
     * Stops this bundle
     *
     * @param bundleContext BundleContext
     * @throws Exception if anything goes wrong during the stop of the bundle
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        ProvisioningActivator.bundleContext = null;

        if (logger.isDebugEnabled())
            logger.debug("Provisioning discovery [STOPPED]");
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the bundle context.
     *
     * @return the <tt>UIService</tt> obtained from the bundle context
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            ServiceReference uiReference =
                bundleContext.getServiceReference(UIService.class.getName());

            uiService =
                (UIService) bundleContext
                    .getService(uiReference);
        }

        return uiService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> obtained from the
     * bundle context.
     *
     * @return the <tt>ResourceManagementService</tt> obtained from the
     * bundle context
     */
    public static ResourceManagementService getResourceService()
    {
        if (resourceService == null)
        {
            ServiceReference resourceReference
                = bundleContext.getServiceReference(
                    ResourceManagementService.class.getName());

            resourceService =
                (ResourceManagementService) bundleContext
                    .getService(resourceReference);
        }

        return resourceService;
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configurationService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService)bundleContext.getService(confReference);
        }
        return configurationService;
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

    /**
     * Returns a reference to a NetworkAddressManagerService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * NetworkAddressManagerService.
     */
    public static NetworkAddressManagerService getNetworkAddressManagerService()
    {
        if (netaddrService == null)
        {
            ServiceReference netaddrReference
                = bundleContext.getServiceReference(
                    NetworkAddressManagerService.class.getName());
            netaddrService
                = (NetworkAddressManagerService) bundleContext
                                        .getService(netaddrReference);
        }
        return netaddrService;
    }

    /**
     * Returns a reference to a <tt>ProvisioningService</tt> implementation.
     *
     * @return a currently valid implementation of <tt>ProvisioningService</tt>
     */
    public static ProvisioningServiceImpl getProvisioningService()
    {
        return provisioningService;
    }
}
