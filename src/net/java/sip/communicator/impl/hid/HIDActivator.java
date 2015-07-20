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
package net.java.sip.communicator.impl.hid;

import net.java.sip.communicator.service.hid.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * OSGi activator for the HID service.
 *
 * @author Sebastien Vincent
 */
public class HIDActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>HIDActivator</tt> class and its
     * instances for logging output.
     */
    private final Logger logger = Logger.getLogger(HIDActivator.class);

    /**
     * The OSGi <tt>ServiceRegistration</tt> of <tt>HIDServiceImpl</tt>.
     */
    private ServiceRegistration serviceRegistration;

    /**
     * Starts the execution of the <tt>hid</tt> bundle in the specified context.
     *
     * @param bundleContext the context in which the <tt>hid</tt> bundle is to
     * start executing
     * @throws Exception if an error occurs while starting the execution of the
     * <tt>hid</tt> bundle in the specified context
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Started.");

        serviceRegistration =
            bundleContext.registerService(HIDService.class.getName(),
                new HIDServiceImpl(), null);

        if (logger.isDebugEnabled())
            logger.debug("HID Service ... [REGISTERED]");
    }

    /**
     * Stops the execution of the <tt>hid</tt> bundle in the specified context.
     *
     * @param bundleContext the context in which the <tt>hid</tt> bundle is to
     * stop executing
     * @throws Exception if an error occurs while stopping the execution of the
     * <tt>hid</tt> bundle in the specified context
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if (serviceRegistration != null)
        {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }
}
