/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import org.osgi.framework.*;

/**
 * Gathers utility functions related to OSGi services such as getting a service
 * registered in a BundleContext.
 *
 * @author Lubomir Marinov
 */
public class ServiceUtils
{
    /**
     * Gets an OSGi service registered in a specific <tt>BundleContext</tt> by
     * its <tt>Class</tt>
     *
     * @param <T> the very type of the OSGi service to get
     * @param bundleContext the <tt>BundleContext</tt> in which the service to
     * get has been registered
     * @param serviceClass the <tt>Class</tt> with which the service to get has
     * been registered in the <tt>bundleContext</tt>
     * @return the OSGi service registered in <tt>bundleContext</tt> with the
     * specified <tt>serviceClass</tt> if such a service exists there;
     * otherwise, <tt>null</tt>
     */
    @SuppressWarnings("unchecked")
    public static <T> T getService(
            BundleContext bundleContext,
            Class<T> serviceClass)
    {
        ServiceReference serviceReference
            = bundleContext.getServiceReference(serviceClass.getName());

        return
            (serviceReference == null)
                ? null
                : (T) bundleContext.getService(serviceReference);
    }

    /** Prevents the creation of <tt>ServiceUtils</tt> instances. */
    private ServiceUtils()
    {
    }
}
