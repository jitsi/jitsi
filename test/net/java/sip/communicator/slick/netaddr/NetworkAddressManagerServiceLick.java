package net.java.sip.communicator.slick.netaddr;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;
/**
 * This class launche the bundle of which test the NetworkManagerBundle
 * this bundle is a set of (j)unit tests. It aim to be launch by the
 * cruisecontrol module to verify tha an implementation of the
 * NetworkAddressManagerService interface is good.
 *
 * @author Emil Ivov
 * @author Pierre Floury
 */
public class NetworkAddressManagerServiceLick
    extends TestSuite
    implements BundleActivator
{
    private Logger logger = Logger.getLogger(getClass().getName());

    protected static NetworkAddressManagerService networkAddressManagerService
                                                                        = null;
    protected static BundleContext bc = null;
    public static TestCase tcase = new TestCase(){};

    /**
     * Start the Network Address Manager Sevice Implementation Compatibility Kit.
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        this.bc = bundleContext;
        setName("NetworkAddressManagerServiceLick");
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

//        addTestSuite(TestNetworkAddressManagerService.class);
//        addTestSuite(TestAddressPool.class);

        bundleContext.registerService(getClass().getName(), this, properties);

        logger.debug("Successfully registered " + getClass().getName());
    }

    /**
     * stop
     *
     * @param bundlecontext BundleContext
     * @throws Exception
     */
    public void stop(BundleContext bundlecontext) throws Exception
    {
    }

}
