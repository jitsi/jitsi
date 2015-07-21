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
package net.java.sip.communicator.impl.netaddr;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.netaddr.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.NetworkUtils;

import org.ice4j.*;
import org.ice4j.ice.*;
import org.ice4j.ice.harvest.*;
import org.ice4j.security.*;
import org.ice4j.stack.*;
import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

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
     * The service name to use when discovering TURN servers through DNS using
     * SRV requests as per RFC 5766.
     */
    public static final String TURN_SRV_NAME = "turn";

    /**
     * The service name to use when discovering STUN servers through DNS using
     * SRV requests as per RFC 5389.
     */
    public static final String STUN_SRV_NAME = "stun";

     /**
      * Initializes this network address manager service implementation.
      */
     public void start()
     {
         this.localHostFinderSocket = initRandomPortSocket();

         // set packet logging to ice4j stack
         StunStack.setPacketLogger(new Ice4jPacketLogger());
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
    public synchronized InetAddress getLocalHost(
            InetAddress intendedDestination)
    {
        InetAddress localHost = null;

        if(logger.isTraceEnabled())
        {
            logger.trace(
                    "Querying for a localhost address"
                        + " for intended destination '"
                        + intendedDestination
                        + "'");
        }

        /* use native code (JNI) to find source address for a specific
         * destination address on Windows XP SP1 and over.
         *
         * For other systems, we used method based on DatagramSocket.connect
         * which will returns us source address. The reason why we cannot use it
         * on Windows is because its socket implementation returns the any
         * address...
         */
        String osVersion;

        if (OSUtils.IS_WINDOWS
                && !(osVersion = System.getProperty("os.version")).startsWith(
                        "4") /* 95/98/Me/NT */
                && !osVersion.startsWith("5.0")) /* 2000 */
        {
            byte[] src
                = Win32LocalhostRetriever.getSourceForDestination(
                        intendedDestination.getAddress());

            if (src == null)
            {
                logger.warn("Failed to get localhost ");
            }
            else
            {
                try
                {
                    localHost = InetAddress.getByAddress(src);
                }
                catch(UnknownHostException uhe)
                {
                    logger.warn("Failed to get localhost", uhe);
                }
            }
        }
        else if (OSUtils.IS_MAC)
        {
            try
            {
                localHost = BsdLocalhostRetriever
                    .getLocalSocketAddress(new InetSocketAddress(
                        intendedDestination, RANDOM_ADDR_DISC_PORT));
            }
            catch (IOException e)
            {
                logger.warn("Failed to get localhost", e);
            }
        }
        else
        {
            //no point in making sure that the localHostFinderSocket is
            //initialized.
            //better let it through a NullPointerException.
            localHostFinderSocket.connect(intendedDestination,
                                          RANDOM_ADDR_DISC_PORT);
            localHost = localHostFinderSocket.getLocalAddress();
            localHostFinderSocket.disconnect();
        }

        //windows socket implementations return the any address so we need to
        //find something else here ... InetAddress.getLocalHost seems to work
        //better on windows so let's hope it'll do the trick.
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
        if (localHost.isAnyLocalAddress())
        {
            if (logger.isTraceEnabled())
            {
                logger.trace(
                        "Socket returned the ANY local address."
                            + " Trying a workaround.");
            }
            try
            {
                //all that's inside the if is an ugly IPv6 hack
                //(good ol' IPv6 - always causing more problems than it solves.)
                if (intendedDestination instanceof Inet6Address)
                {
                    //return the first globally routable ipv6 address we find
                    //on the machine (and hope it's a good one)
                    boolean done = false;
                    Enumeration<NetworkInterface> ifaces
                        = NetworkInterface.getNetworkInterfaces();

                    while (!done && ifaces.hasMoreElements())
                    {
                        Enumeration<InetAddress> addresses
                            = ifaces.nextElement().getInetAddresses();

                        while (addresses.hasMoreElements())
                        {
                            InetAddress address = addresses.nextElement();

                            if ((address instanceof Inet6Address)
                                    && !address.isAnyLocalAddress()
                                    && !address.isLinkLocalAddress()
                                    && !address.isLoopbackAddress()
                                    && !address.isSiteLocalAddress())
                            {
                                localHost = address;
                                done = true;
                                break;
                            }
                        }
                    }
                }
                else
                // an IPv4 destination
                {
                    // Make sure we got an IPv4 address.
                    if (intendedDestination instanceof Inet4Address)
                    {
                        // return the first non-loopback interface we find.
                        boolean done = false;
                        Enumeration<NetworkInterface> ifaces
                            = NetworkInterface.getNetworkInterfaces();

                        while (!done && ifaces.hasMoreElements())
                        {
                            Enumeration<InetAddress> addresses
                                = ifaces.nextElement().getInetAddresses();

                            while (addresses.hasMoreElements())
                            {
                                InetAddress address = addresses.nextElement();

                                if ((address instanceof Inet4Address)
                                        && !address.isLoopbackAddress())
                                {
                                    localHost = address;
                                    done = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                //sigh ... ok return 0.0.0.0
                logger.warn("Failed to get localhost", e);
            }
        }

        if (logger.isTraceEnabled())
            logger.trace("Returning the localhost address '" + localHost + "'");
        return localHost;
    }

    /**
     * Returns the hardware address (i.e. MAC address) of the specified
     * interface name.
     *
     * @param iface the <tt>NetworkInterface</tt>
     * @return array of bytes representing the layer 2 address or null if
     * interface does not exist
     */
    public byte[] getHardwareAddress(NetworkInterface iface)
    {
        String ifName = null;
        byte hwAddress[] = null;

        /* try reflection */
        try
        {
            Method method = iface.getClass().
                getMethod("getHardwareAddress");

            if(method != null)
            {
                hwAddress = (byte[])method.invoke(iface, new Object[]{});
                return hwAddress;
            }
        }
        catch(Exception e)
        {
        }

        /* maybe getHardwareAddress not available on this JVM try
         * with our JNI
         */
        if(OSUtils.IS_WINDOWS)
        {
            ifName = iface.getDisplayName();
        }
        else
        {
            ifName = iface.getName();
        }

        hwAddress = HardwareAddressRetriever.getHardwareAddress(ifName);

        return hwAddress;
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
                {
                    logger.info(
                    "Retrying a bind because of a failure to bind to address "
                        + laddr + " and port " + port);
                    if (logger.isTraceEnabled())
                        logger.trace("Since you seem, here's a stack:", se);
                }

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
     public synchronized void addNetworkConfigurationChangeListener(
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
     public synchronized void removeNetworkConfigurationChangeListener(
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

     /**
      * Tries to discover a TURN or a STUN server for the specified
      * <tt>domainName</tt>. The method would first try to discover a TURN
      * server and then fall back to STUN only. In both cases we would only care
      * about a UDP transport.
      *
      * @param domainName the domain name that we are trying to discover a
      * TURN server for.
      * @param userName the name of the user we'd like to use when connecting to
      * a TURN server (we won't be using credentials in case we only have a STUN
      * server).
      * @param password the password that we'd like to try when connecting to
      * a TURN server (we won't be using credentials in case we only have a STUN
      * server).
      *
      * @return A {@link StunCandidateHarvester} corresponding to the TURN or
      * STUN server we discovered or <tt>null</tt> if there were no such records
      * for the specified <tt>domainName</tt>
      */
     public StunCandidateHarvester discoverStunServer(String domainName,
                                                      byte[] userName,
                                                      byte[] password)
     {
         String srvrAddress = null;
         int port = 0;

         try
         {
             SRVRecord srvRecord = NetworkUtils.getSRVRecord(
                     TURN_SRV_NAME, Transport.UDP.toString(), domainName);

             if(srvRecord != null)
             {
                 srvrAddress = srvRecord.getTarget();
             }

             if(srvrAddress != null)
             {
                 //yay! we seem to have a TURN server, so we'll be using it for
                 //both TURN and STUN harvesting.
                 return new TurnCandidateHarvester(
                             new TransportAddress(srvrAddress,
                                     srvRecord.getPort(),
                                     Transport.UDP),
                             new LongTermCredential(userName, password));
             }

             //srvrAddres was null. try for a STUN only server.
             srvRecord = NetworkUtils.getSRVRecord(
                         STUN_SRV_NAME, Transport.UDP.toString(), domainName);
             if(srvRecord != null)
             {
                 srvrAddress = srvRecord.getTarget();
                 port = srvRecord.getPort();
             }
         }
         catch (ParseException e)
         {
             logger.info(domainName + " seems to be causing parse problems", e);
             srvrAddress = null;
         }
        catch (DnssecException e)
        {
            logger.warn("DNSSEC validation for " + domainName
                + " STUN/TURN failed.", e);
        }

         if(srvrAddress != null)
         {
             return new StunCandidateHarvester(
                             new TransportAddress(
                                     srvrAddress,
                                     port,
                                     Transport.UDP));
         }

         //srvrAddress was still null. sigh ...
         return null;

     }

     /**
      * Creates an <tt>IceMediaStrean</tt> and adds to it an RTP and and RTCP
      * component, which also implies running the currently installed
      * harvesters so that they would.
      *
      * @param rtpPort the port that we should try to bind the RTP component on
      * (the RTCP one would automatically go to rtpPort + 1)
      * @param streamName the name of the stream to create
      * @param agent the <tt>Agent</tt> that should create the stream.
      *
      *@return the newly created <tt>IceMediaStream</tt>.
      *
      * @throws IllegalArgumentException if <tt>rtpPort</tt> is not a valid port
      * number.
      * @throws IOException if an error occurs while the underlying resolver
      * is using sockets.
      * @throws BindException if we couldn't find a free port between within the
      * default number of retries.
     */
    public IceMediaStream createIceStream( int    rtpPort,
                                           String streamName,
                                           Agent  agent)
        throws IllegalArgumentException,
               IOException,
               BindException
    {
        return createIceStream(2, rtpPort, streamName, agent);
    }

    /**
      * {@inheritDoc}
      */
    public IceMediaStream createIceStream( int    numComponents,
                                           int    portBase,
                                           String streamName,
                                           Agent  agent)
        throws IllegalArgumentException,
               IOException,
               BindException
    {
        if(numComponents < 1 || numComponents > 2)
            throw new IllegalArgumentException(
                "Invalid numComponents value: " + numComponents);

        IceMediaStream stream = agent.createMediaStream(streamName);

        agent.createComponent(
            stream, Transport.UDP,
            portBase, portBase, portBase + 100);

        if(numComponents > 1)
        {
            agent.createComponent(
                stream, Transport.UDP,
                portBase + 1, portBase + 1, portBase + 101);
        }

        return stream;
    }
}
