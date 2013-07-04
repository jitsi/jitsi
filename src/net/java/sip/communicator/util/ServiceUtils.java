/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
 * @author Pawel Domas
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

    /**
     * Gets an OSGi service references registered in a specific
     * <tt>BundleContext</tt> by its <tt>Class</tt>
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the services to
     * get have been registered
     * @param serviceClass the <tt>Class</tt> of the OSGi service references to get
     * @return the OSGi service references registered in <tt>bundleContext</tt>
     * with the specified <tt>serviceClass</tt> if such a services exists there;
     * otherwise, <tt>null</tt>
     */
    public static ServiceReference[] getServiceReferences(
            BundleContext bundleContext,
            Class serviceClass)
    {
        ServiceReference[] handlerRefs = null;
        try
        {
            handlerRefs = bundleContext.getServiceReferences(
                    serviceClass.getName(),
                    null);
        }
        catch (InvalidSyntaxException ex)
        {
            throw new RuntimeException(ex);
        }

        return handlerRefs;
    }

    /** Prevents the creation of <tt>ServiceUtils</tt> instances. */
    private ServiceUtils()
    {
    }
}
