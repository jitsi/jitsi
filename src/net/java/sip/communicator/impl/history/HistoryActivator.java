/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.history;

import org.osgi.framework.*;

import net.java.sip.communicator.service.history.*;

/**
 * Invoke "Service Binder" to parse the service XML and register all services.
 * 
 * @author Alexander Pelov
 * @author Lubomir Marinov
 */
public class HistoryActivator
    implements BundleActivator
{
    private ServiceRegistration serviceRegistration;

    public void start(BundleContext bundleContext) throws Exception
    {
        serviceRegistration =
            bundleContext.registerService(HistoryService.class.getName(),
                new HistoryServiceImpl(bundleContext), null);
    }

    public void stop(BundleContext bundleContext)
    {
        if (serviceRegistration != null)
        {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
