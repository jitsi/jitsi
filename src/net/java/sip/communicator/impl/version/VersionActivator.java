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
package net.java.sip.communicator.impl.version;

import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.version.*;
import org.jitsi.service.version.Version;
import org.osgi.framework.*;

/**
 * The entry point to the Version Service Implementation. We register the
 * VersionServiceImpl instance on the OSGi BUS.
 *
 * @author Emil Ivov
 */
public class VersionActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by this <tt>VersionActivator</tt> instance for
     * logging output.
     */
    private final Logger logger = Logger.getLogger(VersionActivator.class);

    /**
     * The OSGi <tt>BundleContext</tt>.
     */
    private static BundleContext bundleContext;

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Started.");

        VersionActivator.bundleContext = context;

        context.registerService(
                VersionService.class.getName(),
                new VersionServiceImpl(),
                null);

        if (logger.isDebugEnabled())
            logger.debug("Jitsi Version Service ... [REGISTERED]");

        Version version = VersionImpl.currentVersion();
        String applicationName = version.getApplicationName();
        String versionString = version.toString();

        if (logger.isInfoEnabled())
        {
            logger.info(
                    "Jitsi Version: " + applicationName + " " + versionString);
        }

        //register properties for those that would like to use them
        ConfigurationService cfg = getConfigurationService();

        cfg.setProperty(Version.PNAME_APPLICATION_NAME, applicationName, true);
        cfg.setProperty(Version.PNAME_APPLICATION_VERSION, versionString, true);
    }

    /**
     * Gets a <tt>ConfigurationService</tt> implementation currently
     * registered in the <tt>BundleContext</tt> in which this bundle has been
     * started or <tt>null</tt> if no such implementation was found.
     *
     * @return a <tt>ConfigurationService</tt> implementation currently
     * registered in the <tt>BundleContext</tt> in which this bundle has been
     * started or <tt>null</tt> if no such implementation was found
     */
    private static ConfigurationService getConfigurationService()
    {
        return
            ServiceUtils.getService(bundleContext, ConfigurationService.class);
    }

    /**
     * Gets the <tt>BundleContext</tt> instance within which this bundle has
     * been started.
     *
     * @return the <tt>BundleContext</tt> instance within which this bundle has
     * been started
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context) throws Exception
    {
    }
}
