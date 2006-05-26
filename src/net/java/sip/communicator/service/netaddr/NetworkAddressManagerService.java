/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.netaddr;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * The NetworkAddressManagerService takes care of problems such as
 * @author Emil Ivov
 */
public interface NetworkAddressManagerService
{
    /**
     * Returns an InetAddress instance representing the local host or null if no
     * IP address for the host could be found
     * @return an InetAddress instance representing the local host or null if no
     * IP address for the host could be found
     */
    public InetAddress getLocalHost();

    /**
     * Returns a localhostAddress. The method uses the following algorithm to
     * choose among multiple addresses:
     * if stun is enabled - queries STUN server and saves returned address
     * Scans addresses for all network interfaces<br>
     *       if an address that matches the one returned by the STUN server is found - it is returned<br>
     *       else<br>
     *       if a non link local (starting with 172.16-31, 10, or 192.168) address is found it is returned<br>
     *       else<br>
     *       if a link local address is found it is returned<br>
     *       else<br>
     *       if the any address is accepted - it is returned<br>
     *       else<br>
     *       returns the InetAddress.getLocalHost()<br>
     *       if the InetAddress.getLocalHost() fails returns<br>
     *       the "any" local address - 0.0.0.0<br>
     *
     * @param anyAddressIsAccepted is 0.0.0.0 accepted as a return value.
     * @return the address that was detected the address of the localhost.
     */
    public InetAddress getLocalHost(boolean anyAddressIsAccepted);

    /**
     * Tries to obtain a mapped/public address for the specified port. If the
     * STUN lib fails, tries to retrieve localhost, if that fails too, returns
     * null.
     *
     * @param port the port whose mapping we are interested in.
     * @return a public address corresponding to the specified port or null if
     * all attempts to retrieve such an address have failed.
     */
    public InetSocketAddress getPublicAddressFor(int port);

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
