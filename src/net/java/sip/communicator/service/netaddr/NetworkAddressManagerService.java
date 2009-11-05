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
     * The default number of binds that a <tt>NetworkAddressManagerService</tt>
     * implementation should execute in case a port is already bound to (each
     * retry would be on a different port).
     */
    public static final int BIND_RETRIES_DEFAULT_VALUE = 50;

    /**
     * The name of the property containing number of binds that a
     * <tt>NetworkAddressManagerService</tt> implementation should execute in
     * case a port is already bound to (each retry would be on a different
     * port).
     */
    public static final String BIND_RETRIES_PROPERTY_NAME
        = "net.java.sip.communicator.service.netaddr.BIND_RETRIES";

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
     * @throws IOException if an error occurs while the underlying resolver lib
     * is using sockets.
     * @throws BindException if the port is already in use.
     */
    public InetSocketAddress getPublicAddressFor(
                                            InetAddress intendedDestination,
                                            int port)
        throws IOException,
               BindException;

     /**
      * Creates a datagram socket and binds it to on the specified
      * <tt>localAddress</tt> and a port in the range specified by the
      * <tt>minPort</tt> and <tt>maxPort</tt> parameters. The implementation
      * would first try to bind the socket on the <tt>minPort</tt> port number
      * and then proceed incrementally upwards until it succeeds or reaches
      * <tt>maxPort</tt>.
      *
      * @param laddr the address that we'd like to bind the socket on.
      * @param minPort the port number where we should first try to bind before
      * moving to the next one (i.e. <tt>minPort + 1</tt>)
      * @param maxPort the maximum port number where we should try binding
      * before giving up and throwinG an exception.
      *
      * @return the newly created <tt>DatagramSocket</tt>.
      *
      * @throws IllegalArgumentException if either <tt>minPort</tt> or
      * <tt>maxPort</tt> is not a valid port number.
      * @throws IOException if an error occurs while the underlying resolver lib
      * is using sockets.
      * @throws BindException if the port is already in use.
      */
     public DatagramSocket createDatagramSocket(InetAddress laddr,
                                                int         minPort,
                                                int         maxPort)
         throws IllegalArgumentException,
                IOException,
                BindException;

}
