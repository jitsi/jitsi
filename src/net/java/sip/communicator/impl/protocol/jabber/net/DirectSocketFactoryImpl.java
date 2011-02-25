package net.java.sip.communicator.impl.protocol.jabber.net;

import net.java.sip.communicator.util.*;

import javax.net.*;
import java.io.*;
import java.net.*;

/**
 * Socket factory using NetworkUtils so we skip PTR dns requests
 * if the host is already resolved ip address.
 *
 * @author Damian Minkov
 */
public class DirectSocketFactoryImpl
    extends SocketFactory
{
    /**
     * Creates an unconnected socket.
     * @param host - the server host
     * @param port - the server port
     * @return the Socket
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws UnknownHostException if the host is not known
     */
    public Socket createSocket(String host, int port)
        throws IOException,
               UnknownHostException
    {
        Socket newSocket = new Socket(Proxy.NO_PROXY);

        newSocket.connect(new InetSocketAddress(
            NetworkUtils.getInetAddress(host), port));
        return newSocket;
    }

    /**
     * Creates a socket and connects it to the specified remote host on the
     * specified remote port. The socket will also be bound to the local
     * address and port supplied.
     *
     * @param host - the server host
     * @param port - the server port
     * @param localHost - the local address the socket is bound to
     * @param localPort - the local port the socket is bound to
     * @return the Socket
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws UnknownHostException if the host is not known
     */
    public Socket createSocket(String host, int port,
                               InetAddress localHost, int localPort)
        throws IOException,
               UnknownHostException
    {
        return new Socket(NetworkUtils.getInetAddress(host), port,
                          localHost, localPort);
    }

    /**
     * Creates a socket and connects it to the specified port number
     * at the specified address.
     * @param host - the server host
     * @param port - the server port
     * @return the Socket
     * @throws IOException if an I/O error occurs when creating the socket
     */
    public Socket createSocket(InetAddress host, int port)
        throws IOException
    {
        Socket newSocket = new Socket(Proxy.NO_PROXY);
        newSocket.connect(new InetSocketAddress(host, port));
        return newSocket;
    }

    /**
     * Creates a socket and connect it to the specified remote address on the
     * specified remote port. The socket will also be bound to the
     * local address and port supplied.
     * @param address - the server address
     * @param port - the server port
     * @param localAddress - the local address the socket is bound to
     * @param localPort - the local port the socket is bound to
     * @return the Socket
     * @throws IOException if an I/O error occurs when creating the socket
     * @throws UnknownHostException if the host is not known
     */
    public Socket createSocket(InetAddress address, int port,
                               InetAddress localAddress, int localPort)
        throws IOException
    {
        return new Socket(address, port, localAddress, localPort);
    }
}
