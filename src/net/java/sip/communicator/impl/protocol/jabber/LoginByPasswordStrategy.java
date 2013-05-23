/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.impl.protocol.jabber.sasl.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import org.jivesoftware.smack.*;

import javax.net.ssl.*;
import java.security.*;

/**
 * Login to Jabber using username & password.
 *
 * @author Stefan Sieber
 */
public class LoginByPasswordStrategy
    implements JabberLoginStrategy
{
    private final AbstractProtocolProviderService protocolProvider;
    private final AccountID accountID;

    private String password;

    /**
     * Disables use of custom digest md5 per account.
     */
    private String DISABLE_CUSTOM_DIGEST_MD5_ACCOUNT_PROP =
        "DISABLE_CUSTOM_DIGEST_MD5";

    /**
     * Disables use of custom digest md5.
     */
    private String DISABLE_CUSTOM_DIGEST_MD5_CONFIG_PROP =
        "net.java.sip.communicator.impl.protocol" +
            ".jabber.DISABLE_CUSTOM_DIGEST_MD5";

    /**
     * Create a login strategy that logs in using user credentials (username
     * and password)
     * @param protocolProvider  protocol provider service to fire registration
     *                          change events.
     * @param accountID The accountID to use for the login.
     */
    public LoginByPasswordStrategy(
            AbstractProtocolProviderService protocolProvider,
            AccountID accountID)
    {
        this.protocolProvider = protocolProvider;
        this.accountID = accountID;
    }

    /**
     * Loads the account passwords as preparation for the login.
     *
     * @param authority SecurityAuthority to obtain the password
     * @param reasonCode reason why we're preparing for login
     * @return UserCredentials in case they need to be cached for this session
     *         (i.e. password is not persistent)
     */
    public UserCredentials prepareLogin(SecurityAuthority authority,
            int reasonCode)
    {
       return loadPassword(authority, reasonCode);
    }

    /**
     * Determines whether the strategy is ready to perform the login.
     *
     * @return True when the password was sucessfully loaded.
     */
    public boolean loginPreparationSuccessful()
    {
        return password != null;
    }


    /**
     * Performs the login on an XMPP connection using SASL PLAIN.
     *
     * @param connection The connection on which the login is performed.
     * @param userName The username for the login.
     * @param resource The XMPP resource.
     * @return always true.
     * @throws XMPPException
     */
    public boolean login(XMPPConnection connection, String userName,
            String resource)
            throws XMPPException
    {
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);

        // Insert our sasl mechanism implementation
        // in order to support some incompatible servers
        boolean disableCustomDigestMD5
            = accountID.getAccountPropertyBoolean(
            DISABLE_CUSTOM_DIGEST_MD5_ACCOUNT_PROP,
            false)
            || JabberActivator.getConfigurationService().getBoolean(
                DISABLE_CUSTOM_DIGEST_MD5_CONFIG_PROP, false);

        if(!disableCustomDigestMD5)
        {
            SASLAuthentication.unregisterSASLMechanism("DIGEST-MD5");
            SASLAuthentication.registerSASLMechanism("DIGEST-MD5",
                SASLDigestMD5Mechanism.class);
            SASLAuthentication.supportSASLMechanism("DIGEST-MD5");
        }

        connection.login(userName, password, resource);
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see net.java.sip.communicator.impl.protocol.jabber.JabberLoginStrategy#
     * isTlsRequired()
     */
    public boolean isTlsRequired()
    {
        // requires TLS by default (i.e. it will not connect to a non-TLS server
        // and will not fallback to cleartext)
        return !accountID.getAccountPropertyBoolean(
                ProtocolProviderFactory.IS_ALLOW_NON_SECURE, false);
    }

    /**
     * Prepares an SSL Context that is customized SSL context.
     *
     * @param cs The certificate service that provides the context.
     * @param trustManager The TrustManager to use within the context.
     * @return An initialized context for the current provider.
     * @throws GeneralSecurityException
     */
    public SSLContext createSslContext(CertificateService cs,
            X509TrustManager trustManager)
            throws GeneralSecurityException
    {
        return cs.getSSLContext(trustManager);
    }

    /**
     * Load the password from the account configuration or ask the user.
     *
     * @param authority SecurityAuthority
     * @param reasonCode the authentication reason code. Indicates the reason of
     *            this authentication.
     * @return The UserCredentials in case they should be cached for this
     *         session (i.e. are not persistent)
     */
    private UserCredentials loadPassword(SecurityAuthority authority,
            int reasonCode)
    {
        UserCredentials cachedCredentials = null;
        //verify whether a password has already been stored for this account
        password = JabberActivator.
                getProtocolProviderFactory().loadPassword(accountID);

        //decode
        if (password == null)
        {
            //create a default credentials object
            UserCredentials credentials = new UserCredentials();
            credentials.setUserName(accountID.getUserID());

            //request a password from the user
            credentials = authority.obtainCredentials(
                    accountID.getDisplayName(),
                    credentials,
                    reasonCode);

            // in case user has canceled the login window
            if(credentials == null)
            {
                protocolProvider.fireRegistrationStateChanged(
                        protocolProvider.getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST,
                        "No credentials provided");
                return null;
            }

            //extract the password the user passed us.
            char[] pass = credentials.getPassword();

            // the user didn't provide us a password (canceled the operation)
            if(pass == null)
            {
                protocolProvider.fireRegistrationStateChanged(
                        protocolProvider.getRegistrationState(),
                        RegistrationState.UNREGISTERED,
                        RegistrationStateChangeEvent.REASON_USER_REQUEST,
                        "No password entered");
                return null;
            }
            password = new String(pass);

            if (credentials.isPasswordPersistent())
            {
                JabberActivator.getProtocolProviderFactory()
                        .storePassword(accountID, password);
            }
            else
                cachedCredentials = credentials;
        }
        return cachedCredentials;
    }
}
