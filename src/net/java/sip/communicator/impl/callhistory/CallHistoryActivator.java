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
package net.java.sip.communicator.impl.callhistory;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activates the <tt>CallHistoryService</tt>.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class CallHistoryActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>CallHistoryActivator</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(CallHistoryActivator.class);

    /**
     * The bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The <tt>CallHistoryServiceImpl</tt> instantiated in the start method
     * of this bundle.
     */
    private static CallHistoryServiceImpl callHistoryService = null;

    /**
     * The service responsible for resources.
     */
    private static ResourceManagementService resourcesService;

    /**
     * The map containing all registered
     */
    private static final Map<Object, ProtocolProviderFactory>
        providerFactoriesMap = new Hashtable<Object, ProtocolProviderFactory>();

    /**
     * Initialize and start call history
     *
     * @param bc the <tt>BundleContext</tt>
     * @throws Exception if initializing and starting call history fails
     */
    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;

        try{
            logger.logEntry();

            HistoryService historyService
                = ServiceUtils.getService(bundleContext, HistoryService.class);

            //Create and start the call history service.
            callHistoryService =
                new CallHistoryServiceImpl();
            // set the configuration and history service
            callHistoryService.setHistoryService(historyService);

            callHistoryService.start(bundleContext);

            bundleContext.registerService(
                CallHistoryService.class.getName(), callHistoryService, null);

            bundleContext.registerService(
                ContactSourceService.class.getName(),
                new CallHistoryContactSource(), null);

            if (logger.isInfoEnabled())
                logger.info("Call History Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }

    }

    /**
     * Stops this bundle.
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if the stop operation goes wrong
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        if(callHistoryService != null)
            callHistoryService.stop(bundleContext);
    }

    /**
     * Returns the instance of <tt>CallHistoryService</tt> created in this
     * activator.
     * @return the instance of <tt>CallHistoryService</tt> created in this
     * activator
     */
    public static CallHistoryService getCallHistoryService()
    {
        return callHistoryService;
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
        if (resourcesService == null)
        {
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
    }

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
        Collection<ServiceReference<ProtocolProviderFactory>> serRefs
            = ServiceUtils.getServiceReferences(
                    bundleContext,
                    ProtocolProviderFactory.class);

        if (!serRefs.isEmpty())
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
