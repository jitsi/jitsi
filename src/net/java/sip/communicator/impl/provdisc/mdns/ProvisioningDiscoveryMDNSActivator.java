/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
