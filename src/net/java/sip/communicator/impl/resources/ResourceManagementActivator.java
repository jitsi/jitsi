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
