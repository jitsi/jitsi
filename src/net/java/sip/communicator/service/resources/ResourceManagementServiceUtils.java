/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.resources;

import org.osgi.framework.*;

/**
 * @author Lubomir Marinov
 */
public final class ResourceManagementServiceUtils
{
    public static ResourceManagementService getService(
        BundleContext bundleContext)
    {
        ServiceReference ref =
            bundleContext.getServiceReference(ResourceManagementService.class
                .getName());

        return (ref == null) ? null : (ResourceManagementService) bundleContext
            .getService(ref);
    }

    private ResourceManagementServiceUtils()
    {
    }
}
