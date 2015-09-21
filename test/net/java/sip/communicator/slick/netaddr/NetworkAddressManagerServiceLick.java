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
package net.java.sip.communicator.slick.netaddr;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * This class launches the bundle of which tests the NetworkManagerBundle this
 * bundle is a set of (j)unit tests. It aim to be launch by the cruisecontrol
 * module to verify that an implementation of the NetworkAddressManagerService
 * interface is good.
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
        NetworkAddressManagerServiceLick.bc = bundleContext;
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
