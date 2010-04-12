/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.net;

import java.io.*;
import java.net.*;
import java.security.*;

import javax.net.ssl.*;

import gov.nist.core.net.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * Manages jain-sip socket creating. When dealing with ssl sockets we interact
 * with the user when the certificate for some reason is not trusted.
 *
 * @author Damian Minkov
 */
public class SslNetworkLayer
    implements NetworkLayer,
               ServiceListener
{
    /**
     * Our class logger.
     */
    private static final Logger logger =
        Logger.getLogger(SslNetworkLayer.class);

    /**
     * The service we use to interact with user.
     */
    private CertificateVerificationService certificateVerification;

    /**
     * Creates the network layer.
     * 
     * @throws GeneralSecurityException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public SslNetworkLayer()
        throws GeneralSecurityException, 
        FileNotFoundException,
        IOException
    {
        SipActivator.getBundleContext().addServiceListener(this);

        ServiceReference guiVerifyReference
            = SipActivator.getBundleContext().getServiceReference(
                CertificateVerificationService.class.getName());

        if(guiVerifyReference != null)
            certificateVerification
                = (CertificateVerificationService)SipActivator.getBundleContext()
                    .getService(guiVerifyReference);
    }

    /**
     * Creates a server with the specified port, listen backlog,
     * and local IP address to bind to.
     * Comparable to "new java.net.ServerSocket(port,backlog,bindAddress);"
     * 
     * @param port the port
     * @param backlog backlog
     * @param bindAddress local address to use
     * @return the newly created server socket.
     * @throws IOException problem creating socket.
     */
    public ServerSocket createServerSocket(int port, int backlog,
            InetAddress bindAddress) 
            throws IOException
    {
        return new ServerSocket(port, backlog, bindAddress);
    }

    /**
     * Creates a stream socket and connects it to the specified
     * port number at the specified IP address.
     *
     * @param address the address to connect.
     * @param port the port to connect.
     * @return the socket 
     * @throws IOException problem creating socket.
     */
    public Socket createSocket(InetAddress address, int port)
        throws IOException
    {
        return new Socket(address, port);
    }

    /**
     * Constructs a datagram socket and binds it to any available port on the
     * local host machine.
     * Comparable to "new java.net.DatagramSocket();"
     *
     * @return the datagram socket
     * @throws SocketException problem creating socket.
     */
    public DatagramSocket createDatagramSocket()
        throws SocketException
    {
        return new DatagramSocket();
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
    public DatagramSocket createDatagramSocket(int port, InetAddress laddr)
        throws SocketException
    {
        return new DatagramSocket(port, laddr);
    }

    /**
     * Creates an SSL server with the specified port, listen backlog, 
     * and local IP address to bind to.
     * 
     * @param port the port to listen to
     * @param backlog backlog
     * @param bindAddress the address to listen to
     * @return the server socket.
     * @throws IOException problem creating socket.
     */
    public SSLServerSocket createSSLServerSocket(int port, int backlog,
            InetAddress bindAddress) 
        throws IOException
    {
        return (SSLServerSocket) getSSLServerSocketFactory(
            bindAddress.getHostName(), port).createServerSocket(
                port, backlog, bindAddress);
    }

    /**
     * Creates a ssl server socket factory.
     * @param address the address.
     * @param port the port 
     * @return the server socket factory.
     * @throws IOException problem creating factory.
     */
    private SSLServerSocketFactory getSSLServerSocketFactory(
        String address, int port)
        throws IOException
    {
        return getSSLContext(address, port).getServerSocketFactory();
    }

    /**
     * Creates the ssl context used to create ssl socket factories. Used
     * to install our custom trust manager which knows the address
     * we are connecting to.
     * @param address the address we are connecting to.
     * @param port the port
     * @return the ssl context.
     * @throws IOException problem creating ssl context.
     */
    private SSLContext getSSLContext(String address, int port)
        throws IOException
    {
        if(certificateVerification != null)
            return certificateVerification.getSSLContext(address, port);
        else
        {
            try
            {
                SSLContext sslContext;
                sslContext = SSLContext.getInstance("TLS");
                String algorithm = KeyManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmFactory =
                    TrustManagerFactory.getInstance(algorithm);
                KeyManagerFactory kmFactory =
                    KeyManagerFactory.getInstance(algorithm);
                SecureRandom secureRandom   = new SecureRandom();
                secureRandom.nextInt();
                tmFactory.init((KeyStore)null);
                kmFactory.init(null, null);
                sslContext.init(kmFactory.getKeyManagers(),
                    tmFactory.getTrustManagers(), secureRandom);

                return sslContext;
            } catch (Throwable e)
            {
                throw new IOException("Cannot init SSLContext: " +
                    e.getMessage());
            }
        }
    }

    /**
     * Creates ssl socket factory.
     * @param address the address we are connecting to.
     * @param port the port we use.
     * @return the socket factory.
     * @throws IOException problem creating ssl socket factory.
     */
    private SSLSocketFactory getSSLSocketFactory(String address, int port)
        throws IOException
    {
        return getSSLContext(address, port).getSocketFactory();
    }

    /**
     * Creates a stream SSL socket and connects it to the specified
     * port number at the specified IP address.
     * @param address the address we are connecting to.
     * @param port the port we use.
     * @return the socket.
     * @throws IOException problem creating socket.
     */
    public SSLSocket createSSLSocket(InetAddress address, int port)
        throws IOException
    {
        return (SSLSocket) getSSLSocketFactory(
            address.getCanonicalHostName(), port).createSocket(address, port);
    }

    /**
     * Creates a stream SSL socket and connects it to the specified
     * port number at the specified IP address.
     * @param address the address we are connecting to.
     * @param port the port we use.
     * @param myAddress the local address to use
     * @return the socket.
     * @throws IOException problem creating socket.
     */
    public SSLSocket createSSLSocket(InetAddress address, int port,
            InetAddress myAddress) 
        throws IOException
    {
        return (SSLSocket) getSSLSocketFactory(
            address.getCanonicalHostName(), port).createSocket(address, port,
                myAddress, 0);
    }

    /**
     * Creates a stream socket and connects it to the specified port number at
     * the specified IP address.
     * Comparable to "new java.net.Socket(address, port,localaddress);"
     * @param address the address to connect to.
     * @param port the port we use.
     * @param myAddress the local address to use.
     * @return the created socket.
     * @throws IOException problem creating socket.
     */
    public Socket createSocket(InetAddress address, int port,
            InetAddress myAddress)
        throws IOException
    {
        if (myAddress != null)
            return new Socket(address, port, myAddress, 0);
        else
            return new Socket(address, port);
    }

    /**
     * Creates a new Socket, binds it to myAddress:myPort and connects it to
     * address:port.
     *
     * @param address the InetAddress that we'd like to connect to.
     * @param port the port that we'd like to connect to
     * @param myAddress the address that we are supposed to bind on or null
     *        for the "any" address.
     * @param myPort the port that we are supposed to bind on or 0 for a random
     * one.
     *
     * @return a new Socket, bound on myAddress:myPort and connected to
     * address:port.
     * @throws IOException if binding or connecting the socket fail for a reason
     * (exception relayed from the corresponding Socket methods)
     */
    public Socket createSocket(InetAddress address, int port,
                    InetAddress myAddress, int myPort)
        throws IOException
    {
        if (myAddress != null)
            return new Socket(address, port, myAddress, myPort);
        else if (port != 0)
        {
            //myAddress is null (i.e. any)  but we have a port number
            Socket sock = new Socket();
            sock.bind(new InetSocketAddress(port));
            sock.connect(new InetSocketAddress(address, port));
            return sock;
        }
        else
            return new Socket(address, port);
    }

    /**
     * Listens for newly registered services. Looking for
     * CertificateVerificationService.
     * 
     * @param event the new event.
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sService = SipActivator.getBundleContext().getService(
            event.getServiceReference());

        // we don't care if the source service is not a plugin component
        if (! (sService instanceof CertificateVerificationService))
        {
            return;
        }

        if(event.getType() == ServiceEvent.REGISTERED)
            certificateVerification = (CertificateVerificationService)sService;
        else if(event.getType() == ServiceEvent.UNREGISTERING)
            certificateVerification = null;
    }
}
