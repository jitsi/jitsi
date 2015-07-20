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
package net.java.sip.communicator.plugin.connectioninfo;

import java.util.*;

import net.java.sip.communicator.service.globaldisplaydetails.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

import org.osgi.framework.*;

/**
 * Starts the connection info bundle.
 *
 * @author Adam Glodstein
 * @author Marin Dzhigarov
 */
public class ConnectionInfoActivator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(ConnectionInfoActivator.class);

    /**
     * The OSGi bundle context.
     */
    public static BundleContext bundleContext;

    static ResourceManagementService R;

    /**
     * Property to disable connect info in tools menu.
     */
    private static final String CONNECT_INFO_TOOLS_MENU_DISABLED_PROP =
        "net.java.sip.communicator.plugin.connectioninfo" +
            ".CONNECT_INFO_TOOLS_MENU_DISABLED_PROP";

    /**
     * Property to disable connect info in account config.
     */
    private static final String CONNECT_INFO_ACC_CONFIG_DISABLED_PROP =
        "net.java.sip.communicator.plugin.connectioninfo" +
            ".CONNECT_INFO_ACC_CONFIG_DISABLED_PROP";

    private static GlobalDisplayDetailsService globalDisplayDetailsService;

    public void start(BundleContext bc) throws Exception
    {
        ConnectionInfoActivator.bundleContext = bc;

        R = ServiceUtils.getService(bc, ResourceManagementService.class);

        ConfigurationService config
            = ServiceUtils.getService(bc, ConfigurationService.class);

        if(!config.getBoolean(CONNECT_INFO_TOOLS_MENU_DISABLED_PROP, false))
        {
            Hashtable<String, String> containerFilter
                = new Hashtable<String, String>();
            containerFilter.put(
                Container.CONTAINER_ID,
                Container.CONTAINER_TOOLS_MENU.getID());

            bundleContext.registerService(
                PluginComponentFactory.class.getName(),
                new PluginComponentFactory(Container.CONTAINER_TOOLS_MENU)
                {
                    @Override
                    protected PluginComponent getPluginInstance()
                    {
                        return new ConnectionInfoMenuItemComponent(
                            getContainer(), this);
                    }
                },
                containerFilter);
        }

        if(!config.getBoolean(CONNECT_INFO_ACC_CONFIG_DISABLED_PROP, false))
        {
            Hashtable<String, String> containerFilter
                = new Hashtable<String, String>();
            containerFilter.put(
                Container.CONTAINER_ID,
                Container.CONTAINER_ACCOUNT_RIGHT_BUTTON_MENU.getID());

            bundleContext.registerService(
                PluginComponentFactory.class.getName(),
                new PluginComponentFactory(
                    Container.CONTAINER_ACCOUNT_RIGHT_BUTTON_MENU)
                {
                    @Override
                    protected PluginComponent getPluginInstance()
                    {
                        return new ConnectionInfoMenuItemComponent(
                            getContainer(), this);
                    }
                },
                containerFilter);
        }
    }

    public void stop(BundleContext bc) throws Exception {}

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
        Map<Object, ProtocolProviderFactory> providerFactoriesMap =
            new Hashtable<Object, ProtocolProviderFactory>();

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs =
                bundleContext.getServiceReferences(
                    ProtocolProviderFactory.class.getName(), null);

        }
        catch (InvalidSyntaxException e)
        {
            logger.error("LoginManager : " + e);
        }

        for (int i = 0; i < serRefs.length; i++)
        {

            ProtocolProviderFactory providerFactory =
                (ProtocolProviderFactory) bundleContext.getService(serRefs[i]);

            providerFactoriesMap
                .put(serRefs[i].getProperty(ProtocolProviderFactory.PROTOCOL),
                    providerFactory);
        }

        return providerFactoriesMap;
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
}
