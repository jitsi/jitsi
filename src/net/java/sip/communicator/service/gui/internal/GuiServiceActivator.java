/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.internal;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class GuiServiceActivator
    implements BundleActivator
{
    /**
     * The <tt>BundleContext</tt> of the service.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>ResourceManagementService</tt>, which gives access to application
     * resources.
     */
    private static ResourceManagementService resourceService;

    /**
     * Returns the <tt>BundleContext</tt>.
     *
     * @return bundle context
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Initialize and start GUI service
     *
     * @param bundleContext the <tt>BundleContext</tt>
     */
    public void start(BundleContext bundleContext)
    {
        GuiServiceActivator.bundleContext = bundleContext;
    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     */
    public void stop(BundleContext bundleContext)
    {
        if (GuiServiceActivator.bundleContext == bundleContext)
            GuiServiceActivator.bundleContext = null;
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
        if (resourceService == null)
        {
            resourceService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourceService;
    }
}
