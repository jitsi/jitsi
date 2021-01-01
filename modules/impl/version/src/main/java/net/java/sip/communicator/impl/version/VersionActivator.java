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

import org.jitsi.utils.version.*;
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
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VersionActivator.class);

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param context The execution context of the bundle being started.
     */
    public void start(BundleContext context)
    {
        VersionService vs = new VersionServiceImpl();
        context.registerService(VersionService.class, vs, null);

        String appName = vs.getCurrentVersion().getApplicationName();
        String version = vs.getCurrentVersion().toString();

        logger.info("Jitsi Version: " + appName + " " + version);

        // later stage.
        System.setProperty("sip-communicator.version", version);
        System.setProperty("sip-communicator.application.name", appName);
    }

    public void stop(BundleContext context)
    {
    }
}
