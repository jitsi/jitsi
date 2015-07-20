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
package net.java.sip.communicator.impl.browserlauncher;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the browserlauncher bundle.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Pawel Domas
 */
public class BrowserLauncherActivator
    extends SimpleServiceActivator<BrowserLauncherImpl>
{
    /**
     * The <tt>BundleContext</tt>
     */
    private static BundleContext bundleContext = null;

    /**
     * The <tt>ServiceConfiguration</tt> to be used by this service.
     */
    private static ConfigurationService configService = null;

    /**
     * Creates new instance of <tt>BrowserLauncherActivator</tt>.
     */
    public BrowserLauncherActivator()
    {
        super(BrowserLauncherService.class, "Browser Launcher Service");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BrowserLauncherImpl createServiceImpl()
    {
        return new BrowserLauncherImpl();
    }

    /**
     * {@inheritDoc}
     *
     * Saves <tt>bundleContext</tt> locally.
     */
    @Override
    public void start(BundleContext bundleContext)
            throws Exception
    {
        BrowserLauncherActivator.bundleContext = bundleContext;

        super.start(bundleContext);
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the
     * the <tt>BundleContext</tt>
     *
     * @return the <tt>ConfigurationService</tt> obtained from the
     * the <tt>BundleContext</tt>
     */
    public static ConfigurationService getConfigurationService()
    {
        if (configService == null && bundleContext != null)
        {
            configService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }

        return configService;
    }
}
