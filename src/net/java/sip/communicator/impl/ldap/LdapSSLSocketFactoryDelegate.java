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
package net.java.sip.communicator.impl.ldap;

import java.io.*;
import java.net.*;
import java.security.*;

import javax.net.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.util.*;

/**
 * Utility class to delegate the creation of sockets to LDAP servers to our
 * {@link CertificateService}.
 * <p>
 * Note that the documentation says to extend {@link SocketFactory}, but the
 * LDAP directory context tries to create an unconnected socket without a
 * hostname first by calling <tt>createSocket</tt>. It would be impossible to
 * validate the hostname against the certificate, which leads to an insecure
 * communication. It only calls {@link #createSocket(String, int)} when
 * <tt>createSocket</tt> is not found
 *
 * @author Ingo Bauersachs
 */
public class LdapSSLSocketFactoryDelegate
{
    /**
     * Logger for this class.
     */
    private final static Logger logger =
        Logger.getLogger(LdapSSLSocketFactoryDelegate.class);

    /**
     * Get default SSL socket factory delegate.
     *
     * @return default SSL socket factory delegate.
     */
    public static Object getDefault()
    {
        return new LdapSSLSocketFactoryDelegate();
    }

    /**
     * Creates a socket for the specified destination host and port.
     *
     * @param host The hostname that the socket connects to.
     * @param port The port that the socket connects to.
     * @return The created socket.
     * @throws IOException
     * @throws UnknownHostException When the hostname cannot be resolved to an
     *             IP address.
     */
    public Socket createSocket(String host, int port)
        throws IOException,
        UnknownHostException
    {
        try
        {
            return LdapServiceImpl
                .getCertificateService()
                .getSSLContext(
                    LdapServiceImpl.getCertificateService().getTrustManager(
                        host)).getSocketFactory().createSocket(host, port);
        }
        catch (GeneralSecurityException e)
        {
            logger.error(
                "unable to create socket through the certificate service", e);
            throw new IOException(e.getMessage());
        }
    }
}
