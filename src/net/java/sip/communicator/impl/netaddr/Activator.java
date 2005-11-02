/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.netaddr;

import org.osgi.framework.*;
import net.java.sip.communicator.service.configuration.ConfigurationService;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.util.*;

/**
 * The activator manage the the bundles between OSGi framework and the
 * Network address manager
 *
 * @author Emil Ivov
 * @author Pierre Floury
 */
public class Activator
    implements BundleActivator
{
    private static Logger logger =
        Logger.getLogger(NetworkAddressManagerServiceImpl.class);

    private NetworkAddressManagerServiceImpl networkAMS = null;

    /**
     * Creates a NetworkAddressManager, starts it, and registers it as a
     * NetworkAddressManagerService.
     *
     * @param bundleContext  OSGI bundle context
     * @throws Exception if starting the NetworkAddressManagerFails.
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        try{

            logger.logEntry();
            // get the config service
            ServiceReference refConfig = bundleContext.getServiceReference(
                ConfigurationService.class.getName());

            ConfigurationService configurationService = (ConfigurationService)
                bundleContext.getService(refConfig);

            //Create and start the network address manager.
            networkAMS =
                new NetworkAddressManagerServiceImpl(configurationService);

            // give references to the NetworkAddressManager implementation
            networkAMS.start();

            logger.info("Network Address Manager         ...[  STARTED ]");

            bundleContext.registerService(
                NetworkAddressManagerService.class.getName(), networkAMS, null);

            logger.info("Network Address Manager Service ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }
    }

    /**
     * Stops the Networ Address Manager bundle
     *
     * @param bundleContext  the OSGI bundle context
     *
     */
    public void stop(BundleContext bundleContext)
    {
        if(networkAMS != null)
            networkAMS.stop();
        logger.info("Network Address Manager Service ...[STOPED]");
    }
}
