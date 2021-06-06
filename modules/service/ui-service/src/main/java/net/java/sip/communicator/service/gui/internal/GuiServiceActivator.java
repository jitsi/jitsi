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
package net.java.sip.communicator.service.gui.internal;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.java.sip.communicator.service.protocol.AccountManager;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
@Slf4j
public class GuiServiceActivator
    extends DependentActivator
{
    /**
     * The <tt>BundleContext</tt> of the service.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>ResourceManagementService</tt>, which gives access to application
     * resources.
     */
    private static ResourceManagementService resourceService;

    private static AccountManager accountManager;

    /**
     * Returns the <tt>BundleContext</tt>.
     *
     * @return bundle context
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    public GuiServiceActivator()
    {
        super(
            ResourceManagementService.class
        );
    }

    /**
     * Initialize and start GUI service
     *
     * @param bundleContext the <tt>BundleContext</tt>
     */
    @Override
    public void startWithServices(BundleContext bundleContext)
    {
        GuiServiceActivator.bundleContext = bundleContext;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            resourceService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourceService;
    }

    /**
     * Returns all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     * context.
     *
     * @return all <tt>ProtocolProviderFactory</tt>s obtained from the bundle
     *         context
     */
    public static Map<Object, ProtocolProviderFactory>
    getProtocolProviderFactories(BundleContext bundleContext)
    {
        Collection<ServiceReference<ProtocolProviderFactory>> serRefs;
        Map<Object, ProtocolProviderFactory> providerFactoriesMap
            = new Hashtable<>();

        try
        {
            // get all registered provider factories
            serRefs
                = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class,null);
        }
        catch (InvalidSyntaxException ex)
        {
            serRefs = null;
            logger.error("getProtocolProviderFactories", ex);
        }

        if ((serRefs != null) && !serRefs.isEmpty())
        {
            for (ServiceReference<ProtocolProviderFactory> serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory
                    = bundleContext.getService(serRef);

                providerFactoriesMap.put(
                    serRef.getProperty(ProtocolProviderFactory.PROTOCOL),
                    providerFactory);
            }
        }
        return providerFactoriesMap;
    }
}
