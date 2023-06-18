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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.protocol.*;
import org.jivesoftware.smack.*;
import org.jxmpp.jid.*;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;

/**
 * Is responsible to configure the login mechanism for smack
 * and later login to the XMPP server.
 *
 * @author Stefan Sieber
 */
public interface JabberLoginStrategy
{
    /**
     * Prepare the login by e.g. asking the user for his password.
     *
     * @param authority SecurityAuthority to obtain the password
     * @param reasonCode reason why we're preparing for login
     * @return UserCredentials in case they need to be cached for this session
     *         (i.e. password is not persistent)
     *
     * @see SecurityAuthority
     */
    UserCredentials prepareLogin(SecurityAuthority authority, int reasonCode);

    /**
     * Determines whether the login preparation was successful and the strategy
     * is ready to start connecting.
     *
     * @return true if prepareLogin was successful.
     */
    boolean loginPreparationSuccessful();

    /**
     * Performs the login for the specified connection.
     *
     * @param connection Connection  to login
     * @param jid the full JID to use
     * @return true to continue connecting, false to abort
     */
    boolean login(AbstractXMPPConnection connection, EntityFullJid jid)
        throws XMPPException, InterruptedException, IOException, SmackException;

    /**
     * Is TLS required for this login strategy / account?
     * @return true if TLS is required
     */
    boolean isTlsRequired();

    /**
     * Creates an SSLContext to use for the login strategy.
     * @param certificateService  certificate service to retrieve the
     *                            ssl context
     * @param trustManager Trust manager to use for the context
     *
     * @return the SSLContext
     */
    SSLContext createSslContext(CertificateService certificateService,
            X509ExtendedTrustManager trustManager)
            throws GeneralSecurityException;

    /**
     * Gets the KeyManagers to use for the login strategy.
     * @param cs  certificate service to retrieve the key managers
     * @return {@code null} if not implemented, or an array of KeyManagers
     * @throws GeneralSecurityException
     */
    default KeyManager[] getKeyManager(CertificateService cs)
        throws GeneralSecurityException
    {
        return null;
    }

    /**
     * Gets the connection configuration builder.
     * @return The connection configuration builder configured for this login
     *  strategy.
     */
    ConnectionConfiguration.Builder getConnectionConfigurationBuilder();
}
