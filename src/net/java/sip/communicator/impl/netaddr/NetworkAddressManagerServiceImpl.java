/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.netaddr;

import java.beans.*;
import java.io.*;
import java.net.*;
import java.util.*;

import org.ice4j.ice.*;

import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.NetworkUtils;//disambiguates with ice4j's

/**
 * This implementation of the Network Address Manager allows you to
 * intelligently retrieve the address of your localhost according to the
 * destinations that you will be trying to reach. It also provides an interface
 * to the ICE implementation in ice4j.
 *
 * @author Emil Ivov
 */
public class NetworkAddressManagerServiceImpl
    implements NetworkAddressManagerService
{
    /**
     * Our class logger.
     */
    private static  Logger logger =
        Logger.getLogger(NetworkAddressManagerServiceImpl.class);

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
     * A thread which periodically scans network interfaces and reports
     * changes in network configuration.
     */
    private NetworkConfigurationWatcher networkConfigurationWatcher = null;

     /**
      * Initializes this network address manager service implementation.
      */
     public void start()
     {
         this.localHostFinderSocket = initRandomPortSocket();
     }

     /**
      * Kills all threads/processes launched by this thread (if any) and
      * prepares it for shutdown. You may use this method as a reinitialization
      * technique (you'll have to call start afterwards)
      */
     public void stop()
     {
         try
         {
             if(networkConfigurationWatcher != null)
                 networkConfigurationWatcher.stop();
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
        String osVersion = System.getProperty("os.version");

        if(logger.isTraceEnabled())
            logger.trace(
                    "Querying a localhost addr for dst=" + intendedDestination);

        /* use native code (JNI) to find source address for a specific destination
         * address on Windows XP SP1 and over.
         *
         * For other systems, we used method based on DatagramSocket.connect
         * which will returns us source address. The reason why we cannot use it
         * on Windows is because its socket implementation returns the any address...
         */
        if(OSUtils.IS_WINDOWS &&
           !osVersion.startsWith("4") && /* 95/98/Me/NT */
           !osVersion.startsWith("5.0")) /* 2000 */
        {
            byte[] src = Win32LocalhostRetriever.getSourceForDestination(intendedDestination.getAddress());

            if(src == null)
            {
                logger.warn("Failed to get localhost ");
            }
            else
            {
                try
                {
                    localHost = InetAddress.getByAddress(src);
                }
                catch(UnknownHostException e)
                {
                    logger.warn("Failed to get localhost ", e);
                }
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
        //windows socket implementations return the any address so we need to
        //find something else here ... InetAddress.getLocalHost seems to work
        //better on windows so lets hope it'll do the trick.

        if (localHost == null)
        {
            try
            {
                localHost = InetAddress.getLocalHost();
            }
            catch (UnknownHostException e)
            {
                logger.warn("Failed to get localhost ", e);
            }
        }
        if( localHost.isAnyLocalAddress())
        {
            if (logger.isTraceEnabled())
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
        if(logger.isTraceEnabled())
        {
            logger.trace("Will return the following localhost address: "
                        + localHost);
        }
        return localHost;
    }


    /**
     * Tries to obtain an for the specified port.
     *
     * @param dst the destination that we'd like to use this address with.
     * @param port the port whose mapping we are interested in.
     * @return a public address corresponding to the specified port or null
     *   if all attempts to retrieve such an address have failed.
     *
     * @throws IOException if an error occurs while creating the socket.
     * @throws BindException if the port is already in use.
     */
    public InetSocketAddress getPublicAddressFor(InetAddress dst, int port)
        throws IOException, BindException
    {
        //we'll try to bind so that we could notify the caller
        //if the port has been taken already.
        DatagramSocket bindTestSocket = new DatagramSocket(port);
        bindTestSocket.close();

        //if we're here then the port was free.
        return new InetSocketAddress(getLocalHost(dst), port);
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
                    return null;
                }
                //port seems to be taken. try another one.
                if (logger.isDebugEnabled())
                    logger.debug("Port " + currentlyTriedPort
                             + " seems in use.");
                currentlyTriedPort
                    = NetworkUtils.getRandomPortNumber();
                if (logger.isDebugEnabled())
                    logger.debug("Retrying bind on port "
                             + currentlyTriedPort);
            }
        }

        return resultSocket;
    }

    /**
     * Creates a <tt>DatagramSocket</tt> and binds it to the specified
     * <tt>localAddress</tt> and a port in the range specified by the
     * <tt>minPort</tt> and <tt>maxPort</tt> parameters. We first try to bind
     * the newly created socket on the <tt>preferredPort</tt> port number
     * (unless it is outside the <tt>[minPort, maxPort]</tt> range in which case
     * we first try the <tt>minPort</tt>) and then proceed incrementally upwards
     * until we succeed or reach the bind retries limit. If we reach the
     * <tt>maxPort</tt> port number before the bind retries limit, we will then
     * start over again at <tt>minPort</tt> and keep going until we run out of
     * retries.
     *
     * @param laddr the address that we'd like to bind the socket on.
     * @param preferredPort the port number that we should try to bind to first.
     * @param minPort the port number where we should first try to bind before
     * moving to the next one (i.e. <tt>minPort + 1</tt>)
     * @param maxPort the maximum port number where we should try binding
     * before giving up and throwinG an exception.
     *
     * @return the newly created <tt>DatagramSocket</tt>.
     *
     * @throws IllegalArgumentException if either <tt>minPort</tt> or
     * <tt>maxPort</tt> is not a valid port number or if <tt>minPort >
     * maxPort</tt>.
     * @throws IOException if an error occurs while the underlying resolver lib
     * is using sockets.
     * @throws BindException if we couldn't find a free port between
     * <tt>minPort</tt> and <tt>maxPort</tt> before reaching the maximum allowed
     * number of retries.
     */
    public DatagramSocket createDatagramSocket(InetAddress laddr,
                                               int preferredPort,
                                               int minPort,
                                               int maxPort)
        throws IllegalArgumentException,
               IOException,
               BindException
    {
        // make sure port numbers are valid
        if (!NetworkUtils.isValidPortNumber(minPort)
                        || !NetworkUtils.isValidPortNumber(maxPort))
        {
            throw new IllegalArgumentException("minPort (" + minPort
                            + ") and maxPort (" + maxPort + ") "
                            + "should be integers between 1024 and 65535.");
        }

        // make sure minPort comes before maxPort.
        if (minPort > maxPort)
        {
            throw new IllegalArgumentException("minPort (" + minPort
                            + ") should be less than or "
                            + "equal to maxPort (" + maxPort + ")");
        }

        // if preferredPort is not  in the allowed range, place it at min.
        if (minPort > preferredPort || preferredPort > maxPort)
        {
            throw new IllegalArgumentException("preferredPort ("+preferredPort
                            +") must be between minPort (" + minPort
                            + ") and maxPort (" + maxPort + ")");
        }

        ConfigurationService config = NetaddrActivator
                        .getConfigurationService();

        int bindRetries = config.getInt(BIND_RETRIES_PROPERTY_NAME,
                        BIND_RETRIES_DEFAULT_VALUE);

        int port = preferredPort;
        for (int i = 0; i < bindRetries; i++)
        {

            try
            {
                return new DatagramSocket(port, laddr);
            }
            catch (SocketException se)
            {
                if (logger.isInfoEnabled())
                    logger.info(
                    "Retrying a bind because of a failure to bind to address "
                        + laddr + " and port " + port, se);
            }

            port ++;

            if (port > maxPort)
                port = minPort;
        }

        throw new BindException("Could not bind to any port between "
                        + minPort + " and " + (port -1));
    }

    /**
      * Adds new <tt>NetworkConfigurationChangeListener</tt> which will
      * be informed for network configuration changes.
      *
      * @param listener the listener.
      */
     public void addNetworkConfigurationChangeListener(
         NetworkConfigurationChangeListener listener)
     {
         if(networkConfigurationWatcher == null)
             networkConfigurationWatcher = new NetworkConfigurationWatcher();

         networkConfigurationWatcher
             .addNetworkConfigurationChangeListener(listener);
     }

     /**
      * Remove <tt>NetworkConfigurationChangeListener</tt>.
      *
      * @param listener the listener.
      */
     public void removeNetworkConfigurationChangeListener(
         NetworkConfigurationChangeListener listener)
     {
        if(networkConfigurationWatcher != null)
            networkConfigurationWatcher
                .removeNetworkConfigurationChangeListener(listener);
     }

     /**
      * Creates and returns an ICE agent that a protocol could use for the
      * negotiation of media transport addresses. One ICE agent should only be
      * used for a single session negotiation.
      *
      * @return the newly created ICE Agent.
      */
     public Agent createIceAgent()
     {
         return new Agent();
     }
}
