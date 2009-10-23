/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.netaddr;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.configuration.event.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.util.*;
import net.java.stun4j.*;
import net.java.stun4j.client.*;

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
 */
public class NetworkAddressManagerServiceImpl
    implements NetworkAddressManagerService,  VetoableChangeListener
{
    private static  Logger logger =
        Logger.getLogger(NetworkAddressManagerServiceImpl.class);

    /**
     * The name of the property containing the stun server address.
     */
    private static final String PROP_STUN_SERVER_ADDRESS
                = "net.java.sip.communicator.impl.netaddr.STUN_SERVER_ADDRESS";
    /**
     * The port number of the stun server to use for NAT traversal
     */
    private static final String PROP_STUN_SERVER_PORT
                = "net.java.sip.communicator.impl.netaddr.STUN_SERVER_PORT";

    /**
     * A stun4j address resolver
     */
    private SimpleAddressDetector detector = null;

    /**
     * Specifies whether or not STUN should be used for NAT traversal
     */
    private boolean useStun = false;

    /**
     * The address of the stun server that we're currently using.
     */
    private StunAddress stunServerAddress = null;


    /**
     * The socket that we use for dummy connections during selection of a local
     * address that has to be used when communicating with a specific location.
     */
    DatagramSocket localHostFinderSocket = null;

    /**
     * A random (unused)local port to use when trying to select a local host
     * address to use when sending messages to a specific destination.
     */
    private static final int RANDOM_ADDR_DISC_PORT = 55721;

    /**
     * The prefix used for Dynamic Configuration of IPv4 Link-Local Addresses.
     * <br>
     * {@link http://ietf.org/rfc/rfc3927.txt}
     */
    // TODO Remove after confirmation that it is not used
//    private static final String DYNAMIC_CONF_FOR_IPV4_ADDR_PREFIX = "169.254";

    /**
     * The name of the property containing the number of binds that we should
     * should execute in case a port is already bound to (each retry would be on
     * a new random port).
     */
    public static final String BIND_RETRIES_PROPERTY_NAME
        = "net.java.sip.communicator.service.netaddr.BIND_RETRIES";

    /**
     * Default STUN server port.
     */
    public static final int DEFAULT_STUN_SERVER_PORT = 3478;


     /**
      * Initializes this network address manager service implementation and
      * starts all processes/threads associated with this address manager, such
      * as a stun firewall/nat detector, keep alive threads, binding lifetime
      * discovery threads and etc. The method may also be used after a call to
      * stop() as a reinitialization technique.
      */
     public void start()
     {
         // init stun
         ConfigurationService configurationService
             = NetaddrActivator.getConfigurationService();
         String stunAddressStr
             = configurationService.getString(PROP_STUN_SERVER_ADDRESS);
         String portStr
             = configurationService.getString(PROP_STUN_SERVER_PORT);

         this.localHostFinderSocket = initRandomPortSocket();

         if ((stunAddressStr == null) || (portStr == null))
         {
            useStun = false;

            logger.info("Stun server address("
                        +stunAddressStr+")/port("
                        +portStr
                        +") not set (or invalid). Disabling STUN.");

         }
         else
         {
             int port = -1;

             try
             {
                port = Integer.parseInt(portStr);
             }
             catch (NumberFormatException ex)
             {
                 logger.info(portStr + " is not a valid port number. "
                             +"Defaulting to 3478",
                             ex);
                 port = DEFAULT_STUN_SERVER_PORT;
             }

             stunServerAddress = new StunAddress(stunAddressStr, port);
             detector = new SimpleAddressDetector(stunServerAddress);

             if (logger.isDebugEnabled())
             {
                 logger.debug(
                     "Created a STUN Address detector for the following "
                     + "STUN server: "
                     + stunAddressStr + ":" + port);
             }
             detector.start();
             logger.debug("STUN server detector started;");

             //make sure that someone doesn't set invalid stun address and port
             configurationService
                 .addVetoableChangeListener(PROP_STUN_SERVER_ADDRESS, this);
             configurationService
                 .addVetoableChangeListener(PROP_STUN_SERVER_PORT, this);

             //now start a thread query to the stun server and only set the
             //useStun flag to true if it succeeds.
             launchStunServerTest();
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
             if (detector != null)
             {
                 try
                 {
                     detector.shutDown();
                 }
                 catch (Exception ex)
                 {
                     logger
                         .debug(
                             "Failed to properly shutdown a stun detector: "
                                 + ex.getMessage());

                 }
                 detector = null;
             }
             useStun = false;

             //remove the listeners
             ConfigurationService configurationService
                 = NetaddrActivator.getConfigurationService();
             configurationService
                 .removeVetoableChangeListener( PROP_STUN_SERVER_ADDRESS, this);
             configurationService
                 .removeVetoableChangeListener( PROP_STUN_SERVER_PORT, this);

         }
         finally
         {
             logger.logExit();
         }
     }

    /**
     * Returns an InetAddress instance that represents the localhost, and that
     * a socket can bind upon or distribute to peers as a contact address.
     *
     * @param intendedDestination the destination that we'd like to use the
     * localhost address with.
     *
     * @return an InetAddress instance representing the local host, and that
     * a socket can bind upon or distribute to peers as a contact address.
     */
    public synchronized InetAddress getLocalHost(InetAddress intendedDestination)
    {
        InetAddress localHost = null;
        
        if(logger.isTraceEnabled())
        {
            logger.trace("Querying a localhost addr for dst="
                            + intendedDestination);

        }

        /* use native code (JNI) to find source address for a specific destination
         * address on Windows. 
         *
         * For other systems, we used method based on DatagramSocket.connect 
         * which will returns us source address. The reason why we cannot use it 
         * on Windows is because its socket implementation returns the any address...
         */
        if(System.getProperty("os.name").startsWith("Windows"))
        {
            try
            {
logger.info("eeeeeeeee");
              localHost = Win32LocalhostRetriever.getSourceForDestination(intendedDestination);
logger.info("eeeeeeeee 2 " + localHost);
            }
            catch(Exception e)
            {
              logger.warn("Failed to get localhost ", e);
            }
        }
        else
        {

            //no point in making sure that the localHostFinderSocket is initialized.
            //better let it through a NullPointerException.
            localHostFinderSocket.connect(intendedDestination,
                                          RANDOM_ADDR_DISC_PORT);
            localHost = localHostFinderSocket.getLocalAddress();
            localHostFinderSocket.disconnect();
        }
/*
        //windows socket implementations return the any address so we need to
        //find something else here ... InetAddress.getLocalHost seems to work
        //better on windows so lets hope it'll do the trick.
        if( localHost.isAnyLocalAddress())
        {
            logger.trace("Socket returned the AnyLocalAddress. "+
                            "Trying a workaround.");
            try
            {
                //all that's inside the if is an ugly IPv6 hack
                //(good ol' IPv6 - always causing more problems than it solves.)
                if (intendedDestination instanceof Inet6Address)
                {
                    //return the first globally routable ipv6 address we find
                    //on the machine (and hope it's a good one)
                    Enumeration<NetworkInterface> interfaces
                        = NetworkInterface.getNetworkInterfaces();

                    while (interfaces.hasMoreElements())
                    {
                        NetworkInterface iface = interfaces.nextElement();
                        Enumeration<InetAddress> addresses =
                            iface.getInetAddresses();
                        while(addresses.hasMoreElements())
                        {
                            InetAddress address
                                = addresses.nextElement();
                            if(address instanceof Inet6Address)
                            {
                                if(!address.isAnyLocalAddress()
                                    && !address.isLinkLocalAddress()
                                    && !address.isSiteLocalAddress()
                                    && !address.isLoopbackAddress())
                                {
                                    if(logger.isTraceEnabled())
                                    {
                                        logger.trace("will return ipv6 addr "
                                                    + address);
                                    }
                                    return address;
                                }
                            }
                        }
                    }
                }
                else
                // an IPv4 destination
                {
                    // first try the easy way
                    localHost = InetAddress.getLocalHost();

                    // Make sure we got an IPv4 address.
                    if (!(localHost instanceof Inet4Address))
                    {
                        // return the first non localhost interface we find.
                        Enumeration<NetworkInterface> interfaces = NetworkInterface
                                        .getNetworkInterfaces();

                        while (interfaces.hasMoreElements())
                        {
                            NetworkInterface iface = interfaces.nextElement();
                            Enumeration<InetAddress> addresses = iface
                                            .getInetAddresses();
                            while (addresses.hasMoreElements())
                            {
                                InetAddress address = addresses.nextElement();
                                if (address instanceof Inet4Address)
                                {
                                    if (!address.isLoopbackAddress())
                                    {
                                        if (logger.isTraceEnabled())
                                        {
                                            logger.trace(
                                                "will return ipv6 addr "
                                                + address);
                                        }
                                        return address;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                //sigh ... ok return 0.0.0.0
                logger.warn("Failed to get localhost ", ex);
            }
        }
*/
        if(logger.isTraceEnabled())
        {
            logger.trace("Will return the following localhost address: "
                        + localHost);
        }
        return localHost;
    }


    /**
     * The method queries a Stun server for a binding for the specified port.
     * @param port the port to resolve (the stun message gets sent trhough that
     * port)
     * @return StunAddress the address returned by the stun server or null
     * if an error occurred or no address was returned
     *
     * @throws IOException if an error occurs while stun4j is using sockets.
     * @throws BindException if the port is already in use.
     */
    private StunAddress queryStunServer(int port)
        throws IOException, BindException
    {
        StunAddress mappedAddress = null;
        if (detector != null && useStun)
        {
            mappedAddress = detector.getMappingFor(port);
            if (logger.isDebugEnabled())
                logger.debug("For port:"
                             + port + "a Stun server returned the "
                             + "following mapping [" + mappedAddress);
        }
        return mappedAddress;
    }

    /**
     * The method queries a Stun server for a binding for the port and address
     * that <tt>sock</tt> is bound on.
     * @param sock the socket whose port and address we'dlike to resolve (the
     * stun message gets sent trhough that socket)
     *
     * @return StunAddress the address returned by the stun server or null
     * if an error occurred or no address was returned
     *
     * @throws IOException if an error occurs while stun4j is using sockets.
     * @throws BindException if the port is already in use.
     */
    private StunAddress queryStunServer(DatagramSocket sock)
        throws IOException, BindException
    {
        StunAddress mappedAddress = null;
        if (detector != null && useStun)
        {
            mappedAddress = detector.getMappingFor(sock);
            if (logger.isTraceEnabled())
            {
                logger.trace("For socket with address "
                             + sock.getLocalAddress().getHostAddress()
                             + " and port "
                             + sock.getLocalPort()
                             + " the stun server returned the "
                             + "following mapping [" + mappedAddress + "]");
            }
        }
        return mappedAddress;
    }


    /**
     * Tries to obtain a mapped/public address for the specified port (possibly
     * by executing a STUN query).
     *
     * @param dst the destination that we'd like to use this address with.
     * @param port the port whose mapping we are interested in.
     * @return a public address corresponding to the specified port or null
     *   if all attempts to retrieve such an address have failed.
     *
     * @throws IOException if an error occurs while stun4j is using sockets.
     * @throws BindException if the port is already in use.
     */
    public InetSocketAddress getPublicAddressFor(InetAddress dst, int port)
        throws IOException, BindException
    {
        if (!useStun || (dst instanceof Inet6Address))
        {
            logger.debug(
                "Stun is disabled for destination "
                + dst
                +", skipping mapped address recovery (useStun="
                +useStun
                +", IPv6@="
                +(dst instanceof Inet6Address)
                +").");
            //we'll still try to bind though so that we could notify the caller
            //if the port has been taken already.
            DatagramSocket bindTestSocket = new DatagramSocket(port);
            bindTestSocket.close();

            //if we're here then the port was free.
            return new InetSocketAddress(getLocalHost(dst), port);
        }
        StunAddress mappedAddress = queryStunServer(port);
        InetSocketAddress result = null;
        if (mappedAddress != null)
            result = mappedAddress.getSocketAddress();
        else
        {
            //Apparently STUN failed. Let's try to temporarily disable it
            //and use algorithms in getLocalHost(). ... We should probably
            //even think about completely disabling stun, and not only
            //temporarily.
            //Bug report - John J. Barton - IBM
            InetAddress localHost = getLocalHost(dst);
            result = new InetSocketAddress(localHost, port);
        }
        if (logger.isDebugEnabled())
            logger.debug("Returning mapping for port:"
                         + port +" as follows: " + result);
        return result;
    }

    /**
     * This method gets called when a bound property is changed.
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
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
     * @param evt a <tt>PropertyChangeEvent</tt> object describing the
     *            event source and the property that will change.
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

    /**
     * Initializes and binds a socket that on a random port number. The method
     * would try to bind on a random port and retry 5 times until a free port
     * is found.
     *
     * @return the socket that we have initialized on a randomport number.
     */
    private DatagramSocket initRandomPortSocket()
    {
        DatagramSocket resultSocket = null;
        String bindRetriesStr
            = NetaddrActivator.getConfigurationService().getString(
                BIND_RETRIES_PROPERTY_NAME);

        int bindRetries = 5;

        if (bindRetriesStr != null)
        {
            try
            {
                bindRetries = Integer.parseInt(bindRetriesStr);
            }
            catch (NumberFormatException ex)
            {
                logger.error(bindRetriesStr
                             + " does not appear to be an integer. "
                             + "Defaulting port bind retries to " + bindRetries
                             , ex);
            }
        }

        int currentlyTriedPort = NetworkUtils.getRandomPortNumber();

        //we'll first try to bind to a random port. if this fails we'll try
        //again (bindRetries times in all) until we find a free local port.
        for (int i = 0; i < bindRetries; i++)
        {
            try
            {
                resultSocket = new DatagramSocket(currentlyTriedPort);
                //we succeeded - break so that we don't try to bind again
                break;
            }
            catch (SocketException exc)
            {
                if (exc.getMessage().indexOf("Address already in use") == -1)
                {
                    logger.fatal("An exception occurred while trying to create"
                                 + "a local host discovery socket.", exc);
                    resultSocket = null;
                    return null;
                }
                //port seems to be taken. try another one.
                logger.debug("Port " + currentlyTriedPort
                             + " seems in use.");
                currentlyTriedPort
                    = NetworkUtils.getRandomPortNumber();
                logger.debug("Retrying bind on port "
                             + currentlyTriedPort);
            }
        }

        return resultSocket;
    }

    /**
     * Runs a test query agains the stun server. If it works we set useStun to
     * true, otherwise we set it to false.
     */
    private void launchStunServerTest()
    {
        Thread stunServerTestThread
            = new Thread("StunServerTestThread")
        {
            public void run()
            {
                DatagramSocket randomSocket = initRandomPortSocket();
                try
                {
                    StunAddress stunAddress
                        = detector.getMappingFor(randomSocket);
                    randomSocket.disconnect();
                    if (stunAddress != null)
                    {
                        useStun = true;
                        logger.trace(
                            "StunServer check succeeded for server: "
                            + detector.getServerAddress()
                            + " and local port: "
                            + randomSocket.getLocalPort());
                    }
                    else
                    {
                        useStun = false;
                        logger.trace(
                            "StunServer check failed for server: "
                            + detector.getServerAddress()
                            + " and local port: "
                            + randomSocket.getLocalPort()
                            + ". No address returned by server.");
                    }
                }
                catch (Throwable ex)
                {
                    logger.error("Failed to run a stun query against "
                                 + "server :" + detector.getServerAddress(),
                                 ex);
                    if (randomSocket.isConnected())
                        randomSocket.disconnect();
                    useStun = false;
                }
            }
        };
        stunServerTestThread.setDaemon(true);
        stunServerTestThread.start();

    }
}
