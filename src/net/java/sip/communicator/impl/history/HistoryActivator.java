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
package net.java.sip.communicator.impl.history;

import net.java.sip.communicator.service.history.*;

import org.osgi.framework.*;

/**
 * Invoke "Service Binder" to parse the service XML and register all services.
 *
 * @author Alexander Pelov
 * @author Lubomir Marinov
 */
public class HistoryActivator
    implements BundleActivator
{
    /**
     * The service registration.
     */
    private ServiceRegistration serviceRegistration;

    /**
     * Initialize and start history service
     *
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if initializing and starting history service fails
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        serviceRegistration =
            bundleContext.registerService(HistoryService.class.getName(),
                new HistoryServiceImpl(bundleContext), null);
    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if the stop operation goes wrong
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if (serviceRegistration != null)
        {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
