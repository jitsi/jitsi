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
package net.java.sip.communicator.impl.provdisc.mdns;

import net.java.sip.communicator.service.provdisc.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the mDNS provisioning bundle.
 *
 * @author Sebastien Vincent
 */
public class ProvisioningDiscoveryMDNSActivator
    implements BundleActivator
{
    /**
     * <tt>Logger</tt> used by this <tt>ProvisioningDiscoveryMDNSActivator</tt>
     * instance for logging output.
     */
    private final Logger logger
        = Logger.getLogger(ProvisioningDiscoveryMDNSActivator.class);

    /**
     * MDNS provisioning service.
     */
    private static ProvisioningDiscoveryServiceMDNSImpl provisioningService =
        new ProvisioningDiscoveryServiceMDNSImpl();

     /**
      * Starts the mDNS provisioning service
      *
      * @param bundleContext the <tt>BundleContext</tt> as provided by the OSGi
      * framework.
      * @throws Exception if anything goes wrong
      */
     public void start(BundleContext bundleContext)
         throws Exception
     {
         if (logger.isDebugEnabled())
             logger.debug("mDNS provisioning discovery Service [STARTED]");

         bundleContext.registerService(
                 ProvisioningDiscoveryService.class.getName(),
                 provisioningService,
                 null);

         if (logger.isDebugEnabled())
             logger.debug("mDNS provisioning discovery Service [REGISTERED]");
     }

     /**
      * Stops the mDNS provisioning service.
      *
      *  @param bundleContext the <tt>BundleContext</tt> as provided by the OSGi
      * framework.
      * @throws Exception if anything goes wrong
      */
     public void stop(BundleContext bundleContext) throws Exception
     {
         if (logger.isInfoEnabled())
             logger.info("mDNS provisioning discovery Service ...[STOPPED]");
     }
}
