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
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Loads the  ICQ provider factory and registers it with  service in the OSGI
 * bundle context.
 *
 * @author Emil Ivov
 */
public class IcqActivator
    implements BundleActivator
{
    private        ServiceRegistration  icqPpFactoryServReg   = null;
    private        ServiceRegistration  aimPpFactoryServReg   = null;
            static BundleContext        bundleContext         = null;
    private static ConfigurationService configurationService  = null;

    private static ProtocolProviderFactoryIcqImpl icqProviderFactory = null;
    private static ProtocolProviderFactoryIcqImpl aimProviderFactory = null;

    private static ResourceManagementService resourceService;

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context) throws Exception
    {
        IcqActivator.bundleContext = context;

        Hashtable<String, String> icqHashtable = new Hashtable<String, String>();
        icqHashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.ICQ);

        Hashtable<String, String> aimHashtable = new Hashtable<String, String>();
        aimHashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.AIM);

        icqProviderFactory = new ProtocolProviderFactoryIcqImpl(false);
        aimProviderFactory = new ProtocolProviderFactoryIcqImpl(true);

        //reg the icq account man.
        icqPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    icqProviderFactory,
                    icqHashtable);

        aimPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    aimProviderFactory,
                    aimHashtable);
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return ConfigurationService a currently valid implementation of the
     * configuration service.
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configurationService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    ConfigurationService.class.getName());
            configurationService
                = (ConfigurationService) bundleContext.getService(confReference);
        }
        return configurationService;
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * witn.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Retrurns a reference to the protocol provider factory that we have
     * registered for icq accounts.
     * @return a reference to the <tt>ProtocolProviderFactoryIcqImpl</tt>
     * instance that we have registered from this package.
     */
    static ProtocolProviderFactoryIcqImpl getIcqProtocolProviderFactory()
    {
        return icqProviderFactory;
    }

    /**
     * Retrurns a reference to the protocol provider factory that we have
     * registered for aim accounts.
     * @return a reference to the <tt>ProtocolProviderFactoryIcqImpl</tt>
     * instance that we have registered from this package.
     */
    static ProtocolProviderFactoryIcqImpl getAimProtocolProviderFactory()
    {
        return aimProviderFactory;
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context) throws Exception
    {
        icqProviderFactory.stop();
        icqPpFactoryServReg.unregister();

        aimPpFactoryServReg.unregister();
    }

    /**
     * Returns an instance of the <tt>ResourceManagementService<tt>.
     *
     * @return an instance of the <tt>ResourceManagementService<tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourceService = (ResourceManagementService) bundleContext
                .getService(serviceReference);
        }

        return resourceService;
    }
}
