/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
    /**
     * The service registration.
     */
    private ServiceRegistration serviceRegistration;

    /**
     * Initialize and start file service
     *
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if initializing and starting file service fails
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        serviceRegistration =
            bundleContext.registerService(FileAccessService.class.getName(),
                new FileAccessServiceImpl(), null);
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
