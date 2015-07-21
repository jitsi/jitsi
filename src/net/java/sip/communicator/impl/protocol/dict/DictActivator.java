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
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Loads the Dict provider factory and registers its services in the OSGI
 * bundle context.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class DictActivator
    implements BundleActivator
{
    private static final Logger logger = Logger.getLogger(DictActivator.class);

    /**
     * The currently valid bundle context.
     */
    private static BundleContext bundleContext = null;

    private ServiceRegistration dictPpFactoryServReg = null;
    private static ProtocolProviderFactoryDictImpl
                                 dictProviderFactory = null;

    private static ResourceManagementService resourceService;

    /**
     * Called when this bundle is started. In here we'll export the
     * dict ProtocolProviderFactory implementation so that it could be
     * possible to register accounts with it in SIP Communicator.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context)
        throws Exception
    {
        bundleContext = context;

        Hashtable<String,String> hashtable = new Hashtable<String,String>();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, ProtocolNames.DICT);

        dictProviderFactory = new ProtocolProviderFactoryDictImpl();

        //reg the dict provider factory.
        dictPpFactoryServReg = context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    dictProviderFactory,
                    hashtable);

        if (logger.isInfoEnabled())
            logger.info("DICT protocol implementation [STARTED].");
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
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context)
        throws Exception
    {

        dictProviderFactory.stop();
        dictPpFactoryServReg.unregister();

        if (logger.isInfoEnabled())
            logger.info("DICT protocol implementation [STOPPED].");
    }

    /**
     * Returns a reference to the protocol provider factory that we have
     * registered.
     * @return a reference to the <tt>ProtocolProviderFactoryDictImpl</tt>
     * instance that we have registered from this package.
     */
    public static ProtocolProviderFactoryDictImpl getProtocolProviderFactory()
    {
        return dictProviderFactory;
    }


    /**
     * Returns the <tt>ResourceManagementService</tt>.
     *
     * @return the <tt>ResourceManagementService</tt>.
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
