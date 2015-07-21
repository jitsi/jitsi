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

import gov.nist.core.net.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

import javax.net.ssl.*;

import net.java.sip.communicator.impl.protocol.sip.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.sip.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * Manages jain-sip socket creation. When dealing with ssl sockets we interact
 * with the user when the certificate for some reason is not trusted.
 *
 * @author Damian Minkov
 * @author Ingo Bauersachs
 * @author Sebastien Vincent
 */
public class SslNetworkLayer
    implements NetworkLayer
{
     /**
     * Our class logger.
     */
     private static final Logger logger =
         Logger.getLogger(SslNetworkLayer.class);

     /**
      * SIP DSCP configuration property name.
      */
     private static final String SIP_DSCP_PROPERTY =
         "net.java.sip.communicator.impl.protocol.SIP_DSCP";

    /**
     * The service we use to interact with user.
     */
    private CertificateService certificateVerification = null;

    /**
     * Creates the network layer.
     */
    public SslNetworkLayer()
    {
        ServiceReference guiVerifyReference =
            SipActivator.getBundleContext().getServiceReference(
                CertificateService.class.getName());

        if (guiVerifyReference != null)
            certificateVerification =
                (CertificateService) SipActivator.getBundleContext().getService(
                    guiVerifyReference);
    }

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
    public ServerSocket createServerSocket(int port, int backlog,
            InetAddress bindAddress)
            throws IOException
    {
        ServerSocket sock = new ServerSocket(port, backlog, bindAddress);
        // XXX apparently traffic class cannot be set on ServerSocket
        //setTrafficClass(sock);
        return sock;
    }

    /**
     * Creates a stream socket and connects it to the specified port number at
     * the specified IP address.
     *
     * @param address the address to connect.
     * @param port the port to connect.
     * @return the socket
     * @throws IOException problem creating socket.
     */
    public Socket createSocket(InetAddress address, int port)
        throws IOException
    {
        Socket sock = new Socket(address, port);
        setTrafficClass(sock);
        return sock;
    }

    /**
     * Constructs a datagram socket and binds it to any available port on the
     * local host machine. Comparable to "new java.net.DatagramSocket();"
     *
     * @return the datagram socket
     * @throws SocketException problem creating socket.
     */
    public DatagramSocket createDatagramSocket()
        throws SocketException
    {
        DatagramSocket sock = new DatagramSocket();
        setTrafficClass(sock);
        return sock;
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
        DatagramSocket sock = new DatagramSocket(port, laddr);
        setTrafficClass(sock);
        return sock;
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
    public SSLServerSocket createSSLServerSocket(int port, int backlog,
            InetAddress bindAddress)
        throws IOException
    {
        SSLServerSocket sock = (SSLServerSocket) getSSLServerSocketFactory()
            .createServerSocket(port, backlog, bindAddress);
        // XXX apparently traffic class cannot be set on ServerSocket
        // setTrafficClass(sock);
        return sock;
    }

    /**
     * Creates a ssl server socket factory.
     *
     * @return the server socket factory.
     * @throws IOException problem creating factory.
     */
    protected SSLServerSocketFactory getSSLServerSocketFactory()
        throws IOException
    {
        try
        {
            return certificateVerification.getSSLContext()
                .getServerSocketFactory();
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Creates ssl socket factory.
     *
     * @return the socket factory.
     * @throws IOException problem creating ssl socket factory.
     */
    private SSLSocketFactory getSSLSocketFactory(InetAddress address)
        throws IOException
    {
        ProtocolProviderServiceSipImpl provider = null;
        for (ProtocolProviderServiceSipImpl pps : ProtocolProviderServiceSipImpl
            .getAllInstances())
        {
            if (pps.getConnection() != null
                && pps.getConnection().isSameInetAddress(address))
            {
                provider = pps;
                break;
            }
        }
        if (provider == null)
            throw new IOException(
                "The provider that requested "
                + "the SSL Socket could not be found");
        try
        {
            ArrayList<String> identities = new ArrayList<String>(2);
            SipAccountID id = (SipAccountID) provider.getAccountID();
            // if the proxy is configured manually, the entered name is valid
            // for the X.509 certificate
            if(!id.getAccountPropertyBoolean(
                ProtocolProviderFactory.PROXY_AUTO_CONFIG, false))
            {
                String proxy = id.getAccountPropertyString(
                    ProtocolProviderFactory.PROXY_ADDRESS);
                if(proxy != null)
                    identities.add(proxy);
                if (logger.isDebugEnabled())
                    logger.debug("Added <" + proxy
                        + "> to list of valid SIP TLS server identities.");
            }
            // the domain part of the user id is always valid
            String userID =
                id.getAccountPropertyString(ProtocolProviderFactory.USER_ID);
            int index = userID.indexOf('@');
            if (index > -1)
            {
                identities.add(userID.substring(index + 1));
                if (logger.isDebugEnabled())
                    logger.debug("Added <" + userID.substring(index + 1)
                        + "> to list of valid SIP TLS server identities.");
            }

            return certificateVerification.getSSLContext(
                    id.getAccountPropertyString(
                        ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE),
                certificateVerification.getTrustManager(
                    identities,
                    null,
                    new RFC5922Matcher(provider)
                )).getSocketFactory();
        }
        catch (GeneralSecurityException e)
        {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Creates a stream SSL socket and connects it to the specified port number
     * at the specified IP address.
     *
     * @param address the address we are connecting to.
     * @param port the port we use.
     * @return the socket.
     * @throws IOException problem creating socket.
     */
    public SSLSocket createSSLSocket(InetAddress address, int port)
        throws IOException
    {
        SSLSocket sock = (SSLSocket) getSSLSocketFactory(address).createSocket(
            address, port);
        setTrafficClass(sock);
        return sock;
    }

    /**
     * Creates a stream SSL socket and connects it to the specified port number
     * at the specified IP address.
     *
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
        SSLSocket sock = (SSLSocket) getSSLSocketFactory(address).createSocket(
            address, port, myAddress, 0);
        setTrafficClass(sock);
        return sock;
    }

    /**
     * Creates a stream socket and connects it to the specified port number at
     * the specified IP address. Comparable to
     * "new java.net.Socket(address, port,localaddress);"
     *
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
        Socket sock = null;

        if (myAddress != null)
            sock = new Socket(address, port, myAddress, 0);
        else
            sock = new Socket(address, port);

        setTrafficClass(sock);

        return sock;
    }

    /**
     * Creates a new Socket, binds it to myAddress:myPort and connects it to
     * address:port.
     *
     * @param address the InetAddress that we'd like to connect to.
     * @param port the port that we'd like to connect to
     * @param myAddress the address that we are supposed to bind on or null for
     *            the "any" address.
     * @param myPort the port that we are supposed to bind on or 0 for a random
     *            one.
     *
     * @return a new Socket, bound on myAddress:myPort and connected to
     *         address:port.
     * @throws IOException if binding or connecting the socket fail for a reason
     *             (exception relayed from the corresponding Socket methods)
     */
    public Socket createSocket(InetAddress address, int port,
                    InetAddress myAddress, int myPort)
        throws IOException
    {
        Socket sock = null;

        if (myAddress != null)
        {
            sock = new Socket(address, port, myAddress, myPort);
        }
        else if (port != 0)
        {
            // myAddress is null (i.e. any) but we have a port number
            sock = new Socket();
            sock.bind(new InetSocketAddress(port));
            sock.connect(new InetSocketAddress(address, port));
        }
        else
        {
            sock = new Socket(address, port);
        }
        setTrafficClass(sock);
        return sock;
    }

    /**
     * Sets the traffic class for the <tt>Socket</tt>.
     *
     * @param s <tt>Socket</tt>
     */
    protected void setTrafficClass(Socket s)
    {
        int tc = getDSCP();

        try
        {
            s.setTrafficClass(tc);
        }
        catch (SocketException e)
        {
            logger.warn("Failed to set traffic class on Socket", e);
        }
    }

    /**
     * Sets the traffic class for the <tt>DatagramSocket</tt>.
     *
     * @param s <tt>DatagramSocket</tt>
     */
    protected void setTrafficClass(DatagramSocket s)
    {
        int tc = getDSCP();

        try
        {
            s.setTrafficClass(tc);
        }
        catch (SocketException e)
        {
            logger.warn("Failed to set traffic class on DatagramSocket", e);
        }
    }

    /**
     * Get the SIP traffic class from configuration.
     *
     * @return SIP traffic class or 0 if not configured
     */
    private int getDSCP()
    {
        ConfigurationService configService =
            SipActivator.getConfigurationService();

        String dscp =
            (String)configService.getProperty(SIP_DSCP_PROPERTY);

        if(dscp != null)
        {
            return Integer.parseInt(dscp) << 2;
        }

        return 0;
    }
}
