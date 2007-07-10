/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.netaddr;

import java.net.*;
import java.io.*;

/**
 * The NetworkAddressManagerService takes care of problems such as
 * @author Emil Ivov
 */
public interface NetworkAddressManagerService
{
    /**
     * Returns an InetAddress instance that represents the localhost, and that
     * a socket can bind upon or distribute to peers as a contact address.
     * <p>
     * This method tries to make for the ambiguity in the implementation of the
     * InetAddress.getLocalHost() method.
     * (see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037).
     * <p>
     * To put it briefly, the issue is about choosing a local source
     * address to bind to or to distribute to peers. It is possible and even
     * quite probable to expect that a machine may dispose with multiple
     * addresses and each of them may be valid for a specific destination.
     * Example cases include:
     * <p>
     * 1) A dual stack IPv6/IPv4 box. <br>
     * 2) A double NIC box with a leg on the Internet and another one in a
     * private LAN <br>
     * 3) In the presence of a virtual interface over a VPN or a MobileIP(v6)
     * tunnel.
     * <p>
     * In all such cases a source local address needs to be chosen according to
     * the intended destination and after consulting the local routing table.
     * <p>
     * @param intendedDestination the address of the destination that we'd like
     * to access through the local address that we are requesting.
     *
     * @return an InetAddress instance representing the local host, and that
     * a socket can bind upon or distribute to peers as a contact address.
     */
    public InetAddress getLocalHost(InetAddress intendedDestination);

    /**
     * Tries to obtain a mapped/public address for the specified port. If the
     * STUN lib fails, tries to retrieve localhost, if that fails too, returns
     * null.
     *
     * @param intendedDestination the destination that we'd like to use this
     * address with.
     * @param port the port whose mapping we are interested in.
     *
     * @return a public address corresponding to the specified port or null if
     * all attempts to retrieve such an address have failed.
     *
     * @throws IOException if an error occurs while the underlying resolve lib
     * is using sockets.
     * @throws BindException if the port is already in use.
     */
    public InetSocketAddress getPublicAddressFor(
                                            InetAddress intendedDestination,
                                            int port)
        throws IOException,
               BindException;

    /**
     * Tries to obtain a mapped/public address for the specified port (possibly
     * by executing a STUN query).
     *
     * @param port the port whose mapping we are interested in.
     *
     * @return a public address corresponding to the specified port or null
     *   if all attempts to retrieve such an address have failed.
     *
     * @throws IOException if an error occurs while stun4j is using sockets.
     * @throws BindException if the port is already in use.
     */
    public InetSocketAddress getPublicAddressFor(int port)
        throws IOException,
               BindException;

    /**
      * Initializes the network address manager service implementation and
      * starts all processes/threads associated with this address manager, such
      * as a stun firewall/nat detector, keep alive threads, binding lifetime
      * discovery threads and etc. The method may also be used after a call to
      * stop() as a reinitialization technique.
      */
     public void start();

     /**
      * Kills all threads/processes lauched by this thread and prepares it for
      * shutdown. You may use this method as a reinitialization technique (
      * you'll have to call start afterwards)
      */
     public void stop();

}
