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

import java.net.*;
import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.netaddr.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Tests basic Network Address Manager Service behaviour.
 *
 * @author Emil Ivov
 * @author Pierre Floury
 */
public class TestNetworkAddressManagerService
    extends TestCase
{
    /**
     * The NetworkAddressManagerService that we will be testing.
     */
    private NetworkAddressManagerService networkAddressManagerService = null;

    private static final String PROP_STUN_SERVER_ADDR =
                        "net.java.sip.communicator.STUN_SERVER_ADDRESS";
    private static final String PROP_STUN_SERVER_PORT =
                        "net.java.sip.communicator.STUN_SERVER_PORT";
    private static final String PROP_PREFERRED_NET_IFACE =
                        "net.java.sip.communicator.PREFERRED_NETWORK_INTERFACE";
    private static final String PROP_PREFERRED_NET_ADDR =
                        "net.java.sip.communicator.PREFERRED_NETWORK_ADDRESS";


    /**
     * The ConfigurationService that we will be using.
     */
    private ConfigurationService configurationService = null;


    /**
     * Constructor.
     * get a reference to the configuration service
     *
     * @param name the name of the test
     */
    public TestNetworkAddressManagerService(String name)
    {
        super(name);

    }

    /**
     * Generic JUnit setUp method. That's where we get the configuration and
     * net address manager services
     * service.
     *
     * @throws Exception if anything goes wrong.
     */
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        BundleContext context = NetworkAddressManagerServiceLick.bc;
        // get the configuration service
        ServiceReference confRef = context
            .getServiceReference(ConfigurationService.class.getName());
        configurationService = (ConfigurationService) context
            .getService(confRef);
        // get the netaddr service
        ServiceReference netRef = context.getServiceReference(
            NetworkAddressManagerService.class.getName());
        networkAddressManagerService = (NetworkAddressManagerService)
            context.getService(netRef);
    }

    /**
     * Generic JUnit tearDown method.
     *
     * @throws Exception if anything goes wrong.
     */
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }


    private static boolean isLocalInterfaceAddress(InetAddress address)
    {
        try {
            Enumeration<NetworkInterface> intfs
                = NetworkInterface.getNetworkInterfaces();
            while (intfs.hasMoreElements())
            {
                NetworkInterface intf = intfs.nextElement();
                Enumeration<InetAddress> addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements())
                {
                    try {
                        InetAddress addr = addrs.nextElement();
                        if(addr.equals(address))
                            return true;
                    } catch (Exception e)
                    {
                    }
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isLocalInterfaceAddressIPv4(InetAddress address)
    {
        if(address instanceof Inet4Address)
            return isLocalInterfaceAddress(address);
        return false;
    }

    private static boolean isLocalInterfaceAddressIPv6(InetAddress address)
    {
        if(address instanceof Inet6Address)
            return isLocalInterfaceAddress(address);
        return false;
    }

    public void testDummyTest()
    {
        //we gotta have at least one test otherwise we have an error
    }

    /**
     * This test will specify all local address one by one as "preferred
     * address" and  test the returned address.
     *
     * @throws java.lang.Exception if anything goes wrong.
     */
    /**
    public void testPreferredNetAddressProperty() throws Exception
    {
        initProperties();
        //set the original properties
        String valuePropertyStunName="";
        String propertyStunPort="";

        //disable stun for this test. we are onlytesting simple logics here.
        configurationService.setProperty(PROP_STUN_SERVER_ADDR,"");
        configurationService.setProperty(PROP_STUN_SERVER_PORT,"");

        //restart the netaddress manager so the it takes the new settings into
        //account
        networkAddressManagerService.stop();
        networkAddressManagerService.start();

        Enumeration intfs = NetworkInterface.getNetworkInterfaces();
        while (intfs.hasMoreElements())
        {
            NetworkInterface iface = (NetworkInterface) intfs.nextElement();

            Enumeration addrs = iface.getInetAddresses();
            while (addrs.hasMoreElements())
            {
                InetAddress addr = (InetAddress) addrs.nextElement();

                //set the current address as the preferred one.
                configurationService.setProperty(PROP_PREFERRED_NET_IFACE,
                                                 iface.getName());
                configurationService.setProperty(PROP_PREFERRED_NET_ADDR,
                                                 addr.toString());

                InetAddress localHost =
                        networkAddressManagerService.getLocalHost();

                if( isRoutable(addr) )
                {
                    //if the address we set as prefered was routable then
                    //the net address manager should have returned it.
                    assertEquals(addr, localHost);
                }
                else
                {
                    assertFalse(networkAddressManagerService.
                                getLocalHost().isLoopbackAddress());
                }
            }
        }
    }


    public void testBadIpAddressNoIPversionSpecified() throws Exception
    {
        initProperties();
        //set the original properties
        //Integer valuePropertyPreferedIPVersion;
        String valuePropertyStunName="stun01.sipphone.com";
        Integer valuePropertyStunPort=new Integer(3478);
        String valuePropertyNetworkAddress="192.169.1.1";
        //String propertyNetworkInterface=""
        //valuePropertyPreferedIPVersion = new Integer(4);

        configurationService.setProperty(PROP_STUN_SERVER_ADDR,
                                         valuePropertyStunName);
        configurationService.setProperty(PROP_STUN_SERVER_PORT,
                                         valuePropertyStunPort);
        configurationService.setProperty(PROP_PREFERRED_NET_ADDR,
                                         valuePropertyNetworkAddress);
        InetAddress addr = networkAddressManagerService.getLocalHost();
        assertTrue(isLocalInterfaceAddressIPv4(addr)
                   || isLinkLocalIPv4Address(addr)
                   || isRoutable(addr)
                   || getStunAddress().equals(addr));

    }
    public void testBadIpAddressIPv4Specified() throws Exception
    {
        initProperties();
        //set th original properties
        //Integer valuePropertyPreferedIPVersion;
        String valuePropertyStunName="stun01.sipphone.com";
        Integer valuePropertyStunPort=new Integer(3478);
        String valuePropertyNetworkAddress="2001:660:4701:1001:800:1cff:fed1:51";
        Integer valuePropertyPreferedIPVersion=new Integer(4);
        //String propertyNetworkInterface=""
        //valuePropertyPreferedIPVersion = new Integer(4);

        configurationService.setProperty(PROP_STUN_SERVER_ADDR,
                                         valuePropertyStunName);
        configurationService.setProperty(PROP_STUN_SERVER_PORT,
                                         valuePropertyStunPort);
        configurationService.setProperty(PROP_PREFERRED_NET_ADDR,
                                         valuePropertyNetworkAddress);
        InetAddress addr = networkAddressManagerService.getLocalHost();
        assertTrue(addr instanceof Inet4Address);

    }

    public void testBadStun() throws Exception
    {
        initProperties();
        //set the original properties
        //Integer valuePropertyPreferedIPVersion;
        String valuePropertyStunName="stun01.sipphone.com";
        Integer valuePropertyStunPort=new Integer(3478);
        String valuePropertyNetworkAddress="192.169.1.1";
        //String propertyNetworkInterface=""
        //valuePropertyPreferedIPVersion = new Integer(4);

        configurationService.setProperty(PROP_STUN_SERVER_ADDR,
                                         valuePropertyStunName);
        configurationService.setProperty(PROP_STUN_SERVER_PORT,
                                         valuePropertyStunPort);
        configurationService.setProperty(PROP_PREFERRED_NET_ADDR,
                                         valuePropertyNetworkAddress);
        InetAddress addr = networkAddressManagerService.getLocalHost();
        assertTrue(isLocalInterfaceAddressIPv4(addr)
                   || isLinkLocalIPv4Address(addr)
                   || isRoutable(addr));

    }


*/

    /**
     * Set the configuration to IPv4 pref if the network is not behind a NAT
     * if not it does nothing
     * and watch if the result of "NetworkAddressManagerService.getlocalhost()
     * is a valid (public) IPv4 address
     */
    /*public void testIPv4Stack()
    {
        if(nat==true)
            return;
        // set properties
        Boolean propertyIpV6Pref= new Boolean(false);
        Boolean propertyIpV4Stack = new Boolean(true);
        String propertieStunValue="stun01.sipphone.com";
        Integer propertieStunPort=new Integer(3478);
        Boolean natBoolean = new Boolean(nat);

        try
        {
            configurationService.setProperty(propertyIpV4Stack, propertyIpV4Stack);
            configurationService.setProperty(propertyStunName,propertieStunValue );
            configurationService.setProperty(propertyStunPort,propertieStunPort );
            configurationService.setProperty(propertyV6,propertyIpV6Pref );
            configurationService.setProperty(propertyNat, natBoolean);
        }
        catch (Exception e) {}
        try {
            // get the locahost grom NetworkAddressManager Service
            InetAddress serviceAdress =
                                    networkAddressManagerService.getLocalHost();


            // test must crash if the localHost is an IPv6 Address
            if(serviceAdress instanceof Inet4Address)
                assertTrue(foundInetAddr(serviceAdress,
                                            propertyIpV6Pref.booleanValue()));
            else
                assertTrue(false);

        }
        catch (Exception e) {}

    }*/

    /**
     * Set the configuration to IPv4 pref if the network is behind a nat
     * if not it does nothing
     * and watch if the result of "NetworkAddressManagerService.getlocalhost()
     * is a valid (public) IPv4 address
     */
    /*public void testIPv4StackWithNat()
    {
        if(nat==false)
            return;
        // set properties
        Boolean propertyIpV6Pref= new Boolean(false);
        Boolean propertyIpV4Stack = new Boolean(true);
        String propertieStunValue="stun01.sipphone.com";
        Integer propertieStunPort=new Integer(3478);
        Boolean natBoolean = new Boolean(nat);

        try
        {
            configurationService.setProperty(propertIpV4Stack, propertyIpV4Stack);
            configurationService.setProperty(propertyStunName,propertieStunValue );
            configurationService.setProperty(propertyStunPort,propertieStunPort );
            configurationService.setProperty(propertyV6,propertyIpV6Pref );
            configurationService.setProperty(propertyNat, natBoolean);
        }
        catch (Exception e) {}
        try {
            // get the locahost grom NetworkAddressManager Service
            InetAddress serviceAdress =
                                    networkAddressManagerService.getLocalHost();


            // test must crash if the localHost is an IPv6 Address
            if(serviceAdress instanceof Inet4Address)
                assertTrue(!isLinkLocalIPv4Address(serviceAdress));
            else
                assertTrue(false);


        }
        catch (Exception e) {}

    }*/

    /**
     * Set the configuration to IPv6 pref
     * and watch if the result of "NetworkAddressManagerService.getlocalhost()
     * is a valid IPv6 address
     */
    /*public void testIPv6()
    {
        if(nat==true)
            return;
        // set properties
        Boolean propertyIpV6Pref= new Boolean(true);
        Boolean propertyIpV4Stack = new Boolean(false);
        String propertieStunValue="stun01.sipphone.com";
        Integer propertieStunPort=new Integer(3478);
        Boolean natBoolean = new Boolean(nat);
        try
        {
            configurationService.setProperty(propertIpV4Stack, propertyIpV4Stack);
            configurationService.setProperty(propertyStunName,propertieStunValue );
            configurationService.setProperty(propertyStunPort,propertieStunPort );
            configurationService.setProperty(propertyV6,propertyIpV6Pref );
            configurationService.setProperty(propertyNat, natBoolean);
        }
        catch (Exception e) {}

        try {
            // get localhost address from the NetworkAddressManager bundle
            InetAddress serviceAdress =
                                    networkAddressManagerService.getLocalHost();

            if(serviceAdress instanceof Inet6Address)
                assertTrue(foundInetAddr(serviceAdress,
                                         propertyIpV6Pref.booleanValue()));
            else
                assertTrue(false);

        }
        catch (Exception e) {}

    }*/

    /**
     * Set the configuration to IPv6 pref
     * and watch if the result of "NetworkAddressManagerService.getlocalhost()
     * is a valid IPv6 address
     */
    /*public void testIPv6Nat()
    {
        if(nat==false)
            return;

        // set properties
        Boolean propertyIpV6Pref= new Boolean(true);
        Boolean propertyIpV4Stack = new Boolean(false);
        String propertieStunValue="stun01.sipphone.com";
        Integer propertieStunPort=new Integer(3478);
        Boolean natBoolean = new Boolean(nat);
        try
        {
            configurationService.setProperty(propertIpV4Stack, propertyIpV4Stack);
            configurationService.setProperty(propertyStunName,propertieStunValue );
            configurationService.setProperty(propertyStunPort,propertieStunPort );
            configurationService.setProperty(propertyV6,propertyIpV6Pref );
            configurationService.setProperty(propertyNat, natBoolean);
        }
        catch (Exception e) {}

        try
        {
            // get localhost address from the NetworkAddressManager bundle
            InetAddress serviceAdress =
                                    networkAddressManagerService.getLocalHost();


            if(serviceAdress instanceof Inet6Address)
                assertTrue( isRoutable(serviceAdress));
            else
                assertTrue(false);
        }
        catch (Exception e) {}

    }*/

    /**
     * verify if the address returned by the service is valid by listing local
     * interfaces and compare them to the address retruned
     *
     * @param serviceAdress : address to check
     * @param ipv6version : if true verifie if this address is an ipv6 belong
     * to the local interface. if false, et do the same thing for
     * IPv4 Addresses
     * @return : true if it is an ipv4 address and if this addres belong to the
     *         local interface
     */
    /*boolean foundInetAddr(InetAddress serviceAdress, boolean ipv6version)
    {

        try {

            Enumeration intfs = NetworkInterface.getNetworkInterfaces();
            while (intfs.hasMoreElements()) {
                NetworkInterface intf = (NetworkInterface) intfs.nextElement();
                Enumeration addrs = intf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    try {
                        InetAddress addr = (InetAddress) addrs.nextElement();
                        if (!addr.isLoopbackAddress()
                        && serviceAdress.equals(addr)
                        && ((!ipv6version && addr.getClass().equals(Inet4Address.class)
                            ||
                            (ipv6version && addr.getClass().equals(Inet6Address.class)))))
                            return true;
                    } catch (Exception e) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }*/

    /**
     *  test the getPublicAddressFor methode,
     *  but I don't know how test that
     *  it is still TODO
     *
     */
    /*public void testgetPublicAddressFor()
    {
        // set properties
        Boolean propertyIpV6Pref= new Boolean(true);
        Boolean propertyIpV4Stack = new Boolean(false);
        String propertieStunValue="stun01.sipphone.com";
        Integer propertieStunPort=new Integer(3478);
        Boolean natBoolean = new Boolean(nat);
        try
        {
            configurationService.setProperty(propertIpV4Stack, propertyIpV4Stack);
            configurationService.setProperty(propertyStunName,propertieStunValue );
            configurationService.setProperty(propertyStunPort,propertieStunPort );
            configurationService.setProperty(propertyV6,propertyIpV6Pref );
            configurationService.setProperty(propertyNat, natBoolean);
        }
        catch (Exception ex) {}




        InetAddress serviceAddress = (networkAddressManagerService.
                                        getPublicAddressFor(1024)).getAddress();
        assertTrue(foundInetAddr(serviceAddress, propertyIpV6Pref.booleanValue()));
        propertyIpV4Stack = new Boolean(true);
        propertyIpV6Pref = new Boolean(false);
        try
        {
            configurationService.setProperty(propertIpV4Stack, propertyIpV4Stack);
            configurationService.setProperty(propertyV6,propertyIpV6Pref );
        }
        catch (Exception e) {}
        serviceAddress = (networkAddressManagerService.
                                        getPublicAddressFor(1024)).getAddress();
        assertTrue(foundInetAddr(serviceAddress, propertyIpV6Pref.booleanValue()));




    }*/

    /**
     * Determines whether the address is an IPv4 link local address. IPv4 link
     * local addresses are those in the following networks:
     *
     * 10.0.0.0    to 10.255.255.255
     * 172.16.0.0  to 172.31.255.255
     * 192.168.0.0 to 192.168.255.255
     *
     * @param add the address to inspect
     * @return true if add is a link local ipv4 address and false if not.
     */
    private static boolean isLinkLocalIPv4Address(InetAddress add)
    {
        if(add instanceof Inet4Address)
        {
            byte address[] = add.getAddress();
            if ( (address[0] & 0xFF) == 10)
                return true;
            if ( (address[0] & 0xFF) == 172
                && (address[1] & 0xFF) >= 16 && address[1] <= 31)
                return true;
            if ( (address[0] & 0xFF) == 192
                && (address[1] & 0xFF) == 168)
                return true;
            return false;
        }
        return false;
    }

    /**
     * Determines whether the address could be used in a VoIP session. Attention,
     * routable address as determined by this method are not only globally routable
     * addresses in the general sense of the term. Link local addresses such as
     * 192.168.x.x or fe80::xxxx are also considered usable.
     * @param address the address to test.
     * @return true if the address could be used in a VoIP session.
     */
    private static boolean isRoutable(InetAddress address)
    {
        if(address instanceof Inet6Address)
        {
            return !address.isLoopbackAddress()
                   && !address.isLinkLocalAddress();
        }
        else
        {
            return (!address.isLoopbackAddress())
                    && (!isWindowsAutoConfiguredIPv4Address(address));
        }
    }
     /**
     * Determines whether the address is the result of windows auto configuration.
     * (i.e. One that is in the 169.254.0.0 network)
     * @param add the address to inspect
     * @return true if the address is autoconfigured by windows, false otherwise.
     */
    private static boolean isWindowsAutoConfiguredIPv4Address(InetAddress add)
    {
        return (add.getAddress()[0] & 0xFF) == 169
            && (add.getAddress()[1] & 0xFF) == 254;
    }

    private void initProperties()
    {
        try
        {
            configurationService.setProperty(  PROP_STUN_SERVER_ADDR , null);
            configurationService.setProperty(  PROP_STUN_SERVER_PORT , null);
            configurationService.setProperty(  PROP_PREFERRED_NET_IFACE, null);
            configurationService.setProperty(  PROP_PREFERRED_NET_ADDR , null);
            configurationService.setProperty(  PROP_STUN_SERVER_ADDR , null);
        }
        catch(Exception ex){}
    }
}
