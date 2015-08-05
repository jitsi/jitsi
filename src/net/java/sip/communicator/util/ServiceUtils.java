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
package net.java.sip.communicator.util;

import java.util.*;

import org.osgi.framework.*;

/**
 * Gathers utility functions related to OSGi services such as getting a service
 * registered in a BundleContext.
 *
 * @author Lyubomir Marinov
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
    public static <T> T getService(
            BundleContext bundleContext,
            Class<T> serviceClass)
    {
        ServiceReference<T> serviceReference
            = bundleContext.getServiceReference(serviceClass);

        return
            (serviceReference == null)
                ? null
                : bundleContext.getService(serviceReference);
    }

    /**
     * Gets an OSGi service references registered in a specific
     * <tt>BundleContext</tt> by its <tt>Class</tt>.
     *
     * @param bundleContext the <tt>BundleContext</tt> in which the services to
     * get have been registered
     * @param serviceClass the <tt>Class</tt> of the OSGi service references to
     * get
     * @return the OSGi service references registered in <tt>bundleContext</tt>
     * with the specified <tt>serviceClass</tt> if such a services exists there;
     * otherwise, an empty <tt>Collection</tt>
     */
    public static <T> Collection<ServiceReference<T>> getServiceReferences(
            BundleContext bundleContext,
            Class<T> serviceClass)
    {
        Collection<ServiceReference<T>> serviceReferences;

        try
        {
            serviceReferences
                = bundleContext.getServiceReferences(
                        serviceClass,
                        null);
        }
        catch (InvalidSyntaxException ex)
        {
            serviceReferences = null;
        }
        if (serviceReferences == null)
            serviceReferences = Collections.emptyList();
        return serviceReferences;
    }

    /** Prevents the creation of <tt>ServiceUtils</tt> instances. */
    private ServiceUtils()
    {
    }
}
