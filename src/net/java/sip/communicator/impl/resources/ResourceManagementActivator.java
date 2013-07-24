/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.resources;

import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Starts Resource Management Service.
 * @author Damian Minkov
 * @author Pawel Domas
 */
public class ResourceManagementActivator
    extends SimpleServiceActivator<ResourceManagementServiceImpl>
{
    static BundleContext bundleContext;

    /**
     * Creates new instance of <tt>ResourceManagementActivator</tt>
     */
    public ResourceManagementActivator()
    {
        super(ResourceManagementService.class, "Resource manager");
    }

    @Override
    public void start(BundleContext bc)
            throws Exception
    {
        bundleContext = bc;

        super.start(bc);
    }

    /**
     * Stops this bundle.
     *
     * @param bc the osgi bundle context
     * @throws Exception
     */
    public void stop(BundleContext bc) throws Exception
    {
        bc.removeServiceListener(serviceImpl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ResourceManagementServiceImpl createServiceImpl()
    {
        return new ResourceManagementServiceImpl();
    }
}
