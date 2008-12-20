/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.fileaccess;

import org.osgi.framework.*;

import net.java.sip.communicator.service.fileaccess.*;

/**
 * Invoke "Service Binder" to parse the service XML and register all services.
 * 
 * @author Alexander Pelov
 * @author Lubomir Marinov
 */
public class FileAccessActivator
    implements BundleActivator
{
    private ServiceRegistration serviceRegistration;

    public void start(BundleContext bundleContext)
    {
        serviceRegistration =
            bundleContext.registerService(FileAccessService.class.getName(),
                new FileAccessServiceImpl(), null);
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
