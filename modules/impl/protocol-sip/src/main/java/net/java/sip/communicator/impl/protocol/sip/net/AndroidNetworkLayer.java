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
package net.java.sip.communicator.impl.protocol.sip.net;

import java.io.*;
import java.net.*;

import javax.net.ssl.*;

/**
 * Manages jain-sip socket creation. Handling android and its ipv6 support.
 * When using ICS you cannot currently bind to :: or ::0, without scope id.
 *
 * @author Damian Minkov
 */
public class AndroidNetworkLayer
    extends SslNetworkLayer
{
    /**
     * A string containing the "any" local address for IPv6.
     */
    public static final String IN6_ADDR_ANY = "::";

    /**
     * Creates a server with the specified port, listen backlog, and local IP
     * address to bind to. Comparable to
     * "new java.net.ServerSocket(port,backlog,bindAddress);"
     *
     * @param port the port
     * @param backlog backlog
     * @param bindAddress local address to use
     * @return the newly created server socket.
     * @throws IOException problem creating socket.
     */
    @Override
    public ServerSocket createServerSocket(int port, int backlog,
            InetAddress bindAddress)
            throws IOException
    {
        if(bindAddress.getHostAddress().equals(IN6_ADDR_ANY))
        {
            return new ServerSocket(port, backlog);
        }
        else
        {
            return super.createServerSocket(port, backlog, bindAddress);
        }
    }

    /**
     * Creates a datagram socket, bound to the specified local address.
     * Comparable to "new java.net.DatagramSocket(port,laddr);"
     *
     * @param port local port to use
     * @param laddr local address to bind
     * @return the datagram socket
     * @throws SocketException problem creating socket.
     */
    @Override
    public DatagramSocket createDatagramSocket(int port, InetAddress laddr)
        throws SocketException
    {
        DatagramSocket sock;
        if(laddr.getHostAddress().equals(IN6_ADDR_ANY))
        {
            sock = new DatagramSocket(port);
            setTrafficClass(sock);
            return sock;
        }
        else
        {
            return super.createDatagramSocket(port, laddr);
        }
    }

    /**
     * Creates an SSL server with the specified port, listen backlog, and local
     * IP address to bind to.
     *
     * @param port the port to listen to
     * @param backlog backlog
     * @param bindAddress the address to listen to
     * @return the server socket.
     * @throws IOException problem creating socket.
     */
    @Override
    public SSLServerSocket createSSLServerSocket(int port, int backlog,
            InetAddress bindAddress)
        throws IOException
    {
        if(bindAddress.getHostAddress().equals(IN6_ADDR_ANY))
        {
            return (SSLServerSocket) getSSLServerSocketFactory()
                        .createServerSocket(port, backlog);
        }
        else
        {
            return super.createSSLServerSocket(port, backlog, bindAddress);
        }
    }
}
