/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.provdisc.dhcp;

import org.osgi.framework.*;

import net.java.sip.communicator.service.provdisc.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>BundleActivator</tt> for the DHCP provisioning bundle.
 *
 * @author Sebastien Vincent
 */
public class ProvisioningDiscoveryDHCPActivator
    implements BundleActivator
{
   /**
    * <tt>Logger</tt> used by this <tt>DHCPProvisioningDiscoveryActivator</tt>
    * instance for logging output.
    */
   private final Logger logger
       = Logger.getLogger(ProvisioningDiscoveryDHCPActivator.class);

   /**
    * DHCP provisioning service.
    */
   private static ProvisioningDiscoveryServiceDHCPImpl provisioningService =
       new ProvisioningDiscoveryServiceDHCPImpl();

   /**
    * A reference to the currently valid {@link NetworkAddressManagerService}.
    */
   private static NetworkAddressManagerService
                                       networkAddressManagerService = null;

   /**
    * Bundle context from OSGi.
    */
   private static BundleContext bundleContext = null;

    /**
     * Starts the DHCP provisioning service
     *
     * @param bundleContext the <tt>BundleContext</tt> as provided by the OSGi
     * framework.
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("DHCP provisioning discovery Service [STARTED]");

        bundleContext.registerService(
                ProvisioningDiscoveryService.class.getName(),
                provisioningService,
                null);

        ProvisioningDiscoveryDHCPActivator.bundleContext = bundleContext;

        if (logger.isDebugEnabled())
            logger.debug("DHCP provisioning discovery Service [REGISTERED]");
    }

    /**
     * Stops the DHCP provisioning service.
     *
     *  @param bundleContext the <tt>BundleContext</tt> as provided by the OSGi
     * framework.
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        ProvisioningDiscoveryDHCPActivator.bundleContext = null;

        if (logger.isInfoEnabled())
            logger.info("DHCP provisioning discovery Service ...[STOPPED]");
    }

    /**
     * Returns a reference to a NetworkAddressManagerService implementation
     * currently registered in the bundle context or null if no such
     * implementation was found.
     *
     * @return a currently valid implementation of the
     * NetworkAddressManagerService .
     */
    public static NetworkAddressManagerService getNetworkAddressManagerService()
    {
        if(networkAddressManagerService == null)
        {
            ServiceReference confReference
                = bundleContext.getServiceReference(
                    NetworkAddressManagerService.class.getName());
            networkAddressManagerService = (NetworkAddressManagerService)
                bundleContext.getService(confReference);
        }
        return networkAddressManagerService;
    }
}
