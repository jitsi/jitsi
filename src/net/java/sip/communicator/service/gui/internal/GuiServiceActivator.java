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
