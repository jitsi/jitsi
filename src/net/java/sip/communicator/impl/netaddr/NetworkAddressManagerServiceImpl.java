/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.netaddr;

import java.net.*;
import java.util.Enumeration;

import net.java.sip.communicator.util.*;
import net.java.stun4j.client.SimpleAddressDetector;
import net.java.stun4j.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.configuration.event.*;
import java.text.*;


/**
 * This implementation of the Network Address Manager allows you to
 * intelligently retrieve the address of your localhost according to preferences
 * specified in a number of properties like:
 * <br>
 * net.java.sip.communicator.STUN_SERVER_ADDRESS - the address of the stun
 * server to use for NAT traversal
 * <br>
 * net.java.sip.communicator.STUN_SERVER_PORT - the port of the stun server
 * to use for NAT traversal
 * <br>
 * java.net.preferIPv6Addresses - a system property specifying weather ipv6
 * addresses are to be preferred in address resolution (default is false for
 * backward compatibility)
 * <br>
 * net.java.sip.communicator.common.PREFERRED_NETWORK_ADDRESS - the address
 * that the user would like to use. (If this is a valid address it will be
 * returned in getLocalhost() calls)
 * <br>
 * net.java.sip.communicator.common.PREFERRED_NETWORK_INTERFACE - the network
 * interface that the user would like to use for fommunication (addresses
 * belonging to that interface will be prefered when selecting a localhost
 * address)
 *
 * @todo further explain the way the service works. explain address selection
 * algorithms and priorities.
 *
 * @author Emil Ivov
 * @author Pierre Floury
 */
public class NetworkAddressManagerServiceImpl
    implements NetworkAddressManagerService, VetoableChangeListener
{
    private static  Logger logger =
        Logger.getLogger(NetworkAddressManagerServiceImpl.class);

    /**
     * The name of the property containing the stun server address.
     */
    private static final String PROP_STUN_SERVER_ADDRESS
                            = "net.java.sip.communicator.STUN_SERVER_ADDRESS";
    /**
     * the port number of the stun server to use for NAT traversal
     */
    private static final String PROP_STUN_SERVER_PORT
                            = "net.java.sip.communicator.STUN_SERVER_PORT";
    /**
     * a system property specifying weather ipv6
     * addresses are to be preferred in address resolution (default is false for
     * backward compatibility)
     */
    private static final String PROP_PREF_IPV6_ADDRS
                            =  "java.net.preferIPv6Addresses";

    /**
     * If an application has a preference to only use IPv4 sockets then this
     * property can be set to true.
     */
    private static final String PROP_PREF_IPV4_STACK
                            =  "java.net.preferIPv4Stack";

    /**
     * the address that the user would like to use. (If this is a valid address
     * it will be returned in getLocalhost() calls)
     */
    private static final String PROP_PREFERRED_NET_ADDRESS
               = "net.java.sip.communicator.common.PREFERRED_NETWORK_ADDRESS";

    /**
     * the network interface that the user would like to use for fommunication
     * (addresses belonging to that interface will be prefered when selecting a
     * localhost address)
     */
    private static final String PROP_PREFERRED_NET_IFACE
               = "net.java.sip.communicator.common.PREFERRED_NETWORK_INTERFACE";

    /** The configuration service to use when retrieving conf property values*/
    private ConfigurationService configurationService = null;

    /** A stun4j address resolver */
    private SimpleAddressDetector detector = null;

    /** Specifies whether or not STUN should be used for NAT traversal */
    private boolean useStun = true;
    private static final int RANDOM_PORT = 55055;
    private static final String WINDOWS_AUTO_CONFIGURED_ADDRESS_PREFIX = "169";

    /**
     * A default constructor.
     *
     * @param configurationService the configruation service that this address
     * manager should use for retrieving configuration properties.
     */
     NetworkAddressManagerServiceImpl(ConfigurationService configurationService)
     {
        this.configurationService = configurationService;
     }

     /**
      * Initializes this network address manager service implementation and
      * starts all processes/threads associated with this address manager, such
      * as a stun firewall/nat detector, keep alive threads, binding lifetime
      * discovery threads and etc. The method may also be used after a call to
      * stop() as a reinitialization technique.
      */
     public void start()
     {
        try
        {
            logger.logEntry();

            // init stun
            String stunAddressStr = null;
            int port = -1;
            stunAddressStr = configurationService.getString(
                                                    PROP_STUN_SERVER_ADDRESS);
            String portStr = configurationService.getString(
                                                    PROP_STUN_SERVER_PORT);

            //in case the user prefers ipv6 addresses we don't want to use
            //stun
            boolean preferIPv6Addresses =  Boolean.getBoolean(
                                                    PROP_PREF_IPV6_ADDRS);

            if (stunAddressStr == null
                || portStr == null
                || preferIPv6Addresses)
            {
                useStun = false;
                //user doesn't want stun - bail out
                return;
            }

            port = Integer.valueOf(portStr).intValue();

            detector = new SimpleAddressDetector(
                new StunAddress(stunAddressStr, port));

            if (logger.isDebugEnabled())
                logger.debug(
                    "Created a STUN Address detector for the following "
                    + "STUN server: "
                    + stunAddressStr + ":" + port);


            try
            {
                detector.start();
                logger.debug("STUN server started;");
            }
            catch (StunException ex)
            {
                logger.error(
                    "Failed to start the STUN Address Detector. " +
                    detector.toString());
                logger.debug("Disabling stun and continuing bravely!");
                detector = null;
                useStun = false;
            }

            //make sure that someone doesn't set invalid stun address and port
            configurationService.addVetoableChangeListener(
                PROP_STUN_SERVER_ADDRESS, this);
            configurationService.addVetoableChangeListener(
                PROP_STUN_SERVER_PORT, this);

            //don't register a property listener. reinitialization is supposed
            //to only happen after a stop(), start() call seq
        }
        finally
        {
            logger.logExit();
        }
     }

     /**
      * Kills all threads/processes lauched by this thread and prepares it for
      * shutdown. You may use this method as a reinitialization technique (
      * you'll have to call start afterwards)
      */
     public void stop()
     {
         try
         {
            try{
                detector.shutDown();
            }catch (Exception ex){
                logger.debug("Failed to properly shutdown a stun detector: "
                    +ex.getMessage());

            }
             detector = null;
             useStun = false;

             //remove the listeners
             configurationService.removeVetoableChangeListener(
                 PROP_STUN_SERVER_ADDRESS, this);

             configurationService.removeVetoableChangeListener(
                 PROP_STUN_SERVER_PORT, this);

         }
         finally
         {
             logger.logExit();
         }

     }

    /**
     * Returns an InetAddress instance that represents the localhost, and that
     * a socket can bind upon.
     *
     * @return an InetAddress instance representing the local host. The returned
     * value may also contain the "any" inet address (i.e. 0.0.0.0 or ::0)
     */
    public InetAddress getLocalHost()
    {
        return getLocalHost(true);
    }


    /**
     * Returns an InetAddress instance that represents the localhost, and that
     * a socket can bind upon.
     *
     * @param anyAddressIsAccepted are (0.0.0.0 / ::0) addresses accepted as a
     * return value.
     * @return the address that was detected the address of the localhost.
     */
    public InetAddress getLocalHost(boolean anyAddressIsAccepted)
    {
        try
        {
            logger.logEntry();
            InetAddress localHost = null;
            InetAddress mappedAddress = null;
            InetAddress stunConfirmedAddress = null;
            InetAddress linkLocalAddress = null;
            InetAddress publicAddress = null;
            String      selectedInterface = null;

            //let's check whether the user has any preferences concerning addrs
            String preferredAddr =
                configurationService.getString(PROP_PREFERRED_NET_ADDRESS);
            String preferredIface =
                configurationService.getString(PROP_PREFERRED_NET_IFACE);

            boolean preferIPv4Stack = Boolean.getBoolean(PROP_PREF_IPV4_STACK);
            boolean preferIPv6Addrs = Boolean.getBoolean(PROP_PREF_IPV6_ADDRS);

            try
            {
                //check whether we have a public address that matches one of
                //the local interfaces if not - return the first one that
                //is not the loopback

                //retrieve and store a STUN binding if possible
                if (useStun)
                {
                    StunAddress stunMappedAddress =
                        queryStunServer(RANDOM_PORT);

                    mappedAddress =  (stunMappedAddress == null)
                        ? null
                        : stunMappedAddress.getSocketAddress().getAddress();
                }

                Enumeration localIfaces =
                        NetworkInterface.getNetworkInterfaces();

                //do a loop over all addresses of all interfaces and return
                //the first that we judge correct.

                //interfaces loop
                interfaces_loop:
                while (localIfaces.hasMoreElements())
                {
                    NetworkInterface iFace =
                                (NetworkInterface) localIfaces.nextElement();

                    Enumeration addresses = iFace.getInetAddresses();

                    //addresses loop
                    addresses_loop:
                    while (addresses.hasMoreElements()) {
                        InetAddress address =
                                (InetAddress) addresses.nextElement();
                        //ignore link local addresses
                        if (address.isAnyLocalAddress()
                            || address.isLinkLocalAddress()
                            || address.isLoopbackAddress()
                            || isWindowsAutoConfiguredIPv4Address(address))
                        {
                            //address is phony - go on to the next one
                            continue addresses_loop;
                        }
                        //see whether this is the address used in STUN communic.
                        if (mappedAddress != null
                            && mappedAddress.equals(address)) {
                            if (logger.isDebugEnabled())
                                logger.debug("Returninng localhost: Mapped "
                                             + "address = Public address = "
                                             + address);
                            //the addr matches the one seen by the STUN server
                            //no doubt that it's a working public
                            //address.

                            stunConfirmedAddress = address;
                        }
                        //link local addr
                        else if (isLinkLocalIPv4Address(address))
                        {
                            if (logger.isDebugEnabled())
                                logger.debug("Found Linklocal ipv4 address "
                                             + address);
                                linkLocalAddress = address;
                        }
                        //publicly routable addr
                        else {
                            if (logger.isDebugEnabled())
                                logger.debug("Found a public address "
                                 + address);

                            //now befo we store this address, make sure we don't
                            //already have one that suits us better and bail out
                            //if this is the case

                            if (//we already have the address prefferred by user
                                (   publicAddress != null
                                    && preferredAddr!= null
                                    && preferredAddr.equals(publicAddress.
                                                            getHostAddress()))
                                   //we already have an address on an iface
                                   //preferred by the user
                                 ||(publicAddress != null
                                    && selectedInterface != null
                                    && preferredIface != null
                                    && preferredIface.equals(selectedInterface))
                                   //in case we have an ipv4 addr and don't
                                   //want to change it for an ipv6
                                 ||(publicAddress != null
                                    && publicAddress instanceof Inet4Address
                                    && address instanceof Inet6Address
                                    && preferIPv4Stack)
                                    //in case we have an ipv6 addr and don't
                                    //want to change it for an ipv4
                                 ||(publicAddress != null
                                    && publicAddress instanceof Inet6Address
                                    && address instanceof Inet4Address
                                    && !preferIPv4Stack)
                                )
                            {
                                continue;
                            }
                            publicAddress = address;
                            selectedInterface = iFace.getDisplayName();
                        }
                    }//addresses loop
                }//interfaces loop

                //if we have an address confirmed by STUN msg exchanges - we'll
                //return it unless the user had really insisted on IPv6 addresses.
                if(stunConfirmedAddress != null
                    && ! preferIPv6Addrs){
                     logger.debug("Returning stun confirmed address");
                     return stunConfirmedAddress;
                }
                //return the address that was selected during the loop above.
                if (publicAddress != null) {
                    logger.debug("Returning public address");
                     return publicAddress;
                }
                if (linkLocalAddress != null) {
                    logger.debug("Returning link local address");
                    return linkLocalAddress;
                }
                if (anyAddressIsAccepted)
                    localHost = new InetSocketAddress(RANDOM_PORT).getAddress();
                else
                    localHost = InetAddress.getLocalHost();
            }
            catch (Exception ex) {
                logger.error("Failed to determine the localhost address, "
                             +"returning the any address (0.0.0.0/::0)", ex);
                //get the address part of an InetSocketAddress for a random port.
                localHost = new InetSocketAddress(RANDOM_PORT).getAddress();
            }
            if (logger.isDebugEnabled())
                logger.debug("Returning localhost address=" + localHost);
            return localHost;
        }
        finally
        {
            logger.logExit();
        }
    }

    /**
     * The method queries a Stun server for a binding for the specified port.
     * @param port the port to resolve (the stun message gets sent trhough that
     * port)
     * @return StunAddress the address returned by the stun server or null
     * if an error occurred or no address was returned
     */
    private StunAddress queryStunServer(int port)
    {

        try{
            logger.logEntry();
            StunAddress mappedAddress = null;
            if (detector != null && useStun) {
                try {
                    mappedAddress = detector.getMappingFor(port);
                    if (logger.isDebugEnabled())
                        logger.debug("For port:"
                                     + port + "a Stun server returned the "
                                     +"following mapping [" + mappedAddress);
                }
                catch (StunException ex) {
                    logger.error(
                        "Failed to retrive mapped address port:" +port, ex);
                    mappedAddress = null;
                }
            }
            return mappedAddress;
        }
        finally{
            logger.logExit();
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
     * Tries to obtain a mapped/public address for the specified port (possibly
     * by executing a STUN query).
     *
     * @param port the port whose mapping we are interested in.
     * @return a public address corresponding to the specified port or null
     *   if all attempts to retrieve such an address have failed.
     */
    public InetSocketAddress getPublicAddressFor(int port)
    {
        try {
            logger.logEntry();
            if (!useStun) {
                logger.debug(
                    "Stun is disabled, skipping mapped address recovery.");
                return new InetSocketAddress(getLocalHost(), port);
            }
            StunAddress mappedAddress = queryStunServer(port);
            InetSocketAddress result = null;
            if (mappedAddress != null)
                result = mappedAddress.getSocketAddress();
            else {
                //Apparently STUN failed. Let's try to temporarily disble it
                //and use algorithms in getLocalHost(). ... We should probably
                //eveng think about completely disabling stun, and not only
                //temporarily.
                //Bug report - John J. Barton - IBM
                InetAddress localHost = getLocalHost(false);
                result = new InetSocketAddress(localHost, port);
            }
            if (logger.isDebugEnabled())
                logger.debug("Returning mapping for port:"
                             + port +" as follows: " + result);
            return result;
        }
        finally {
            logger.logExit();
        }
    }

    /**
     * This method gets called when a bound property is changed.
     * @param evt A PropertyChangeEvent object describing the event source
     *   	and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        //there's no point in implementing this method as we have no way of
        //knowing whether the current property change event is the only event
        //we're going to get or whether another one is going to follow..

        //in the case of a STUN_SERVER_ADDRESS property change for example
        //there's no way of knowing whether a STUN_SERVER_PORT property change
        //will follow or not.

        //Reinitializaion will therefore only happen if the reinitialize()
        //method is called.
    }

    /**
     * This method gets called when a property we're interested in is about to
     * change. In case we don't like the new value we throw a
     * PropertyVetoException to prevent the actual change from happening.
     *
     * @param     evt a <code>PropertyChangeEvent</code> object describing the
     *   	      event source and the property that will change.
     * @exception PropertyVetoException if we don't want the change to happen.
     */
    public void vetoableChange(PropertyChangeEvent evt) throws
        PropertyVetoException
    {
        if (evt.getPropertyName().equals(PROP_STUN_SERVER_ADDRESS))
        {
            //make sure that we have a valid fqdn or ip address.

            //null or empty port is ok since it implies turning STUN off.
            if (evt.getNewValue() == null)
                return;

            String host = evt.getNewValue().toString();
            if (host.trim().length() == 0)
                return;

            boolean ipv6Expected = false;
            if (host.charAt(0) == '[')
            {
                // This is supposed to be an IPv6 litteral
                if (host.length() > 2 &&
                    host.charAt(host.length() - 1) == ']')
                {
                    host = host.substring(1, host.length() - 1);
                    ipv6Expected = true;
                }
                else
                {
                    // This was supposed to be a IPv6 address, but it's not!
                    throw new PropertyVetoException(
                        "Invalid address string" + host, evt);
                }
            }

            for(int i = 0; i < host.length(); i++)
            {
                char c = host.charAt(i);
                if( Character.isLetterOrDigit(c))
                    continue;

                if( (c != '.' && c!= ':')
                    ||( c == '.' && ipv6Expected)
                    ||( c == ':' && !ipv6Expected))
                    throw new PropertyVetoException(
                                host + " is not a valid address nor host name",
                                evt);
            }

        }//is prop_stun_server_address
        else if (evt.getPropertyName().equals(PROP_STUN_SERVER_PORT)){

            //null or empty port is ok since it implies turning STUN off.
            if (evt.getNewValue() == null)
                return;

            String port = evt.getNewValue().toString();
            if (port.trim().length() == 0)
                return;

            try
            {
                Integer.valueOf(evt.getNewValue().toString());
            }
            catch (NumberFormatException ex)
            {
                throw new PropertyVetoException(
                    port + " is not a valid port! " + ex.getMessage(), evt);
            }


        }

    }
}
