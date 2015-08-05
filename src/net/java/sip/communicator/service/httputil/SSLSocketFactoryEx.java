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
package net.java.sip.communicator.service.httputil;

import org.apache.http.conn.*;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.*;

/**
 * Wrapper for SSLSocketFactory to use the constructor which is available
 * in android. The certificate validation is handled by the SSLContext
 * which we use to create sockets for this factory.
 *
 * TODO: wrap the SSLSocketFactory to use our own DNS resolution
 * TODO: register socketfactory for http to use our own DNS resolution
 *
 * @author Damian Minkov
 */
public class SSLSocketFactoryEx
    extends SSLSocketFactory
{
    /**
     * The context that will be used to create sockets.
     */
    private SSLContext context;

    /**
     * Constructor using the super constructor available for android.
     *
     * @param context the context to use
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     */
    public SSLSocketFactoryEx(SSLContext context)
        throws
        UnrecoverableKeyException,
        KeyStoreException,
        KeyManagementException,
        NoSuchAlgorithmException
    {
        super((KeyStore) null);

        this.context = context;
    }

    /**
     * Creates socket.
     * @param params
     * @return
     * @throws IOException
     */
    @Override
    public Socket createSocket(final HttpParams params)
        throws
        IOException
    {
        return this.context.getSocketFactory().createSocket();
    }

    /**
     * @since 4.2
     */
    @Override
    public Socket createLayeredSocket(
        final Socket socket,
        final String host,
        final int port,
        final HttpParams params)
            throws IOException,
                   UnknownHostException
    {
        return this.context.getSocketFactory()
            .createSocket(
                socket,
                host,
                port,
                true);
    }

    /**
     * @since 4.1
     */
    @Override
    public Socket connectSocket(
        final Socket socket,
        final InetSocketAddress remoteAddress,
        final InetSocketAddress localAddress,
        final HttpParams params)
            throws IOException,
                   UnknownHostException,
                   ConnectTimeoutException
    {
        if(remoteAddress == null)
        {
            throw new IllegalArgumentException("Remote address may not be null");
        }
        if(params == null)
        {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
        Socket sock = socket != null ?
            socket : this.context.getSocketFactory().createSocket();
        if(localAddress != null)
        {
            sock.setReuseAddress(HttpConnectionParams.getSoReuseaddr(params));
            sock.bind(localAddress);
        }

        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);

        try
        {
            sock.setSoTimeout(soTimeout);
            sock.connect(remoteAddress, connTimeout);
        }
        catch(SocketTimeoutException ex)
        {
            throw new ConnectTimeoutException(
                    "Connect to " + remoteAddress + " timed out");
        }

        String hostname;
        if(remoteAddress instanceof HttpInetSocketAddress)
        {
            hostname = ((HttpInetSocketAddress) remoteAddress)
                .getHttpHost().getHostName();
        }
        else
        {
            hostname = remoteAddress.getHostName();
        }

        SSLSocket sslsock;
        // Setup SSL layering if necessary
        if(sock instanceof SSLSocket)
        {
            sslsock = (SSLSocket) sock;
        }
        else
        {
            int port = remoteAddress.getPort();
            sslsock = (SSLSocket) this.context.getSocketFactory()
                .createSocket(sock, hostname, port, true);
        }

        return sslsock;
    }
}
