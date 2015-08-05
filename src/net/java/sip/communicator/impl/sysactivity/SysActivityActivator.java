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
package net.java.sip.communicator.impl.sysactivity;

import net.java.sip.communicator.service.sysactivity.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Listens for system activity changes like sleep, network change, inactivity
 * and informs all its listeners.
 *
 * @author Damian Minkov
 */
public class SysActivityActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by this <tt>SysActivityActivator</tt> for
     * logging output.
     */
    private final Logger logger = Logger.getLogger(SysActivityActivator.class);

    /**
     * The OSGi <tt>BundleContext</tt>.
     */
    private static BundleContext bundleContext = null;

    /**
     * The system activity service impl.
     */
    private static SystemActivityNotificationsServiceImpl
        sysActivitiesServiceImpl;

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     * marked as stopped and the Framework will remove this bundle's listeners,
     * unregister all services registered by this bundle, and release all
     * services used by this bundle.
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        SysActivityActivator.bundleContext = bundleContext;

        if (logger.isDebugEnabled())
            logger.debug("Started.");

        sysActivitiesServiceImpl = new SystemActivityNotificationsServiceImpl();
        sysActivitiesServiceImpl.start();

        bundleContext.registerService(
                SystemActivityNotificationsService.class.getName(),
                sysActivitiesServiceImpl,
                null);
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * with.
     */
    public static SystemActivityNotificationsServiceImpl
        getSystemActivityService()
    {
        return sysActivitiesServiceImpl;
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bundleContext The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is still
     * marked as stopped, and the Framework will remove the bundle's listeners,
     * unregister all services registered by the bundle, and release all
     * services used by the bundle.
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if (sysActivitiesServiceImpl != null)
            sysActivitiesServiceImpl.stop();
    }

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * with.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }
}
