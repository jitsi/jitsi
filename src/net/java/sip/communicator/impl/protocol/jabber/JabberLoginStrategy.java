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

import javax.net.ssl.*;
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
    public UserCredentials prepareLogin(SecurityAuthority authority,
        int reasonCode);

    /**
     * Determines whether the login preparation was successful and the strategy
     * is ready to start connecting.
     *
     * @return true if prepareLogin was successful.
     */
    public boolean loginPreparationSuccessful();

    /**
     * Performs the login for the specified connection.
     *
     * @param connection Connection  to login
     * @param userName userName to be used for the login.
     * @param resource the XMPP resource
     * @return true to continue connecting, false to abort
     */
    public boolean login(XMPPConnection connection, String userName,
            String resource)
            throws XMPPException;

    /**
     * Is TLS required for this login strategy / account?
     * @return true if TLS is required
     */
    public boolean isTlsRequired();

    /**
     * Creates an SSLContext to use for the login strategy.
     * @param certificateService  certificate service to retrieve the
     *                            ssl context
     * @param trustManager Trust manager to use for the context
     *
     * @return the SSLContext
     */
    public SSLContext createSslContext(CertificateService certificateService,
            X509TrustManager trustManager)
            throws GeneralSecurityException;
}
