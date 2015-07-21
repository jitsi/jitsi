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

import org.jitsi.util.*;
import org.jivesoftware.smack.*;

import javax.net.ssl.*;
import java.security.*;

/**
 * Login to Jabber using a client certificate (as defined in the account
 * configuration)
 *
 * @author Stefan Sieber
 */
class LoginByClientCertificateStrategy
    implements JabberLoginStrategy
{
    private final static Logger logger
        = Logger.getLogger(LoginByClientCertificateStrategy.class);
    private AccountID accountID;

    /**
     * Creates a new instance of this class.
     *
     * @param accountID The account to use for the strategy.
     */
    public LoginByClientCertificateStrategy(AccountID accountID)
    {
        this.accountID = accountID;
    }

    /**
     * Does nothing.
     *
     * @param authority unused
     * @param reasonCode unused
     * @return always <tt>null</tt>
     */
    public UserCredentials prepareLogin(SecurityAuthority authority,
        int reasonCode)
    {
        // password is retrieved later when opening the key store.
        return null;
    }

    /**
     * Does nothing.
     *
     * @return always <tt>true</tt>
     */
    public boolean loginPreparationSuccessful()
    {
        return true;
    }

    /**
     * Always true as the authentication occurs with the TLS client
     * certificate.
     *
     * @return always <tt>true</tt>
     */
    public boolean isTlsRequired()
    {
        return true;
    }

    /**
     * Creates the SSLContext for the XMPP connection configured with a
     * customized TrustManager and a KeyManager based on the selected client
     * certificate.
     *
     * @param certificateService  certificate service to retrieve the
     *                            SSL context
     * @param trustManager Trust manager to use for the context
     * @return Configured and initialized SSL Context
     * @throws GeneralSecurityException
     */
    public SSLContext createSslContext(CertificateService cs,
            X509TrustManager trustManager)
        throws GeneralSecurityException
    {
        String certConfigName = accountID.getAccountPropertyString(
            ProtocolProviderFactory.CLIENT_TLS_CERTIFICATE);
        return cs.getSSLContext(certConfigName, trustManager);
    }

    /**
     * Performs the login on the XMPP connection using the SASL EXTERNAL
     * mechanism.
     *
     * @param connection The connection on which the login is performed.
     * @param userName The username for the login.
     * @param resource The XMPP resource.
     * @return true when the login succeeded, false when the certificate wasn't
     * accepted.
     * @throws XMPPException
     */
    public boolean login(XMPPConnection connection, String userName,
            String resource)
        throws XMPPException
    {
        SASLAuthentication.supportSASLMechanism("EXTERNAL", 0);

        // user/password MUST be empty. In fact they shouldn't be
        // necessary at all because the user name is derived from the
        // client certificate.
        try
        {
            connection.login("", "", resource);
            return true;
        }
        catch (XMPPException ex)
        {
            if (ex.getMessage().contains("EXTERNAL failed: not-authorized"))
            {
                logger.error("Certificate login failed", ex);
                return false;
            }

            throw ex;
        }
    }
}
