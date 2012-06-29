/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import java.security.*;
import java.security.cert.*;
import java.util.*;

import javax.net.ssl.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The Rss protocol provider factory creates instances of the Rss
 * protocol provider service. One Service instance corresponds to one account.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class ProtocolProviderFactoryRssImpl
    extends ProtocolProviderFactory
{
    /**
     * The <tt>Logger</tt> used by the <tt>ProtocolProviderFactoryRssImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ProtocolProviderFactoryRssImpl.class);

    /**
     * The indicator which determines whether the delayed execution of
     * {@link #installCustomSSLTrustManager()} which has to happen only once has
     * been performed.
     */
    private static boolean customSSLTrustManagerIsInstalled = false;

    /**
     * Creates an instance of the ProtocolProviderFactoryRssImpl.
     */
    public ProtocolProviderFactoryRssImpl()
    {
        super(RssActivator.getBundleContext(),
            ProtocolProviderServiceRssImpl.RSS_PROTOCOL_NAME);
    }

    /**
     * Initialized and creates an account corresponding to the specified
     * accountProperties and registers the resulting ProtocolProvider in the
     * <tt>context</tt> BundleContext parameter.
     *
     * @param userIDStr tha/a user identifier uniquely representing the newly
     *   created account within the protocol namespace.
     * @param accountProperties a set of protocol (or implementation)
     *   specific properties defining the new account.
     * @return the AccountID of the newly created account.
     */
    public AccountID installAccount( String userIDStr,
                                     Map<String, String> accountProperties)
    {
        BundleContext context
            = RssActivator.getBundleContext();
        if (context == null)
            throw new NullPointerException("The specified BundleContext was null");

        if (userIDStr == null)
            throw new NullPointerException("The specified AccountID was null");

        if (accountProperties == null)
            throw new NullPointerException("The specified property map was null");

        accountProperties.put(USER_ID, userIDStr);

        AccountID accountID = new RssAccountID(userIDStr, accountProperties);

        //make sure we haven't seen this account id before.
        if (registeredAccounts.containsKey(accountID))
            throw new IllegalStateException(
                "An account for id " + userIDStr + " was already installed!");

        //first store the account and only then load it as the load generates
        //an osgi event, the osgi event triggers (through the UI) a call to the
        //ProtocolProviderService.register() method and it needs to access
        //the configuration service and check for a stored password.
        this.storeAccount(accountID, false);

        accountID = loadAccount(accountProperties);

        return accountID;
    }

    protected AccountID createAccountID(
            String userID,
            Map<String, String> accountProperties)
    {
        return new RssAccountID(userID, accountProperties);
    }

    protected ProtocolProviderService createService(String userID,
        AccountID accountID)
    {
        synchronized (ProtocolProviderFactoryRssImpl.class)
        {
            if (!customSSLTrustManagerIsInstalled)
            {
                System.setProperty(
                    "http.agent",
                    System.getProperty("sip-communicator.application.name")
                        + "/"
                        + System.getProperty("sip-communicator.version"));
                logger
                    .debug(
                        "User-Agent set to "
                            + System.getProperty("http.agent"));

                try
                {
                    installCustomSSLTrustManager();
                    customSSLTrustManagerIsInstalled = true;
                }
                catch (java.security.GeneralSecurityException gsex)
                {
                    logger.error(gsex);
                }
            }
        }

        ProtocolProviderServiceRssImpl service =
            new ProtocolProviderServiceRssImpl();

        service.initialize(userID, accountID);
        return service;
    }

    /**
     * Installs a trust manager that would accept all certificates so that
     * we could install rss feeds from sites with expired/self-signed
     * certificates.
     */
    private static void installCustomSSLTrustManager()
        throws GeneralSecurityException
    {
        // Let us create the factory where we can set some parameters for the
        // connection
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null,
                new TrustManager[] { new TrustlessManager()},
                new SecureRandom());

        // Create the socket connection and open it to the secure remote web server
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    @Override
    public void modifyAccount(  ProtocolProviderService protocolProvider,
                                Map<String, String> accountProperties)
        throws NullPointerException
    {
        // TODO Auto-generated method stub
    }

    /**
     * A trust manager that would accept all certificates so that we would be
     * able to add rss feeds from sites with expired/self-signed certificates.
     */
    private static class TrustlessManager
        implements X509TrustManager
    {
        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }

        /**
         * Given the partial or complete certificate chain provided by the peer,
         * build a certificate path to a trusted root and return if it can be
         * validated and is trusted for client SSL authentication based on the
         * authentication type. The authentication type is determined by the
         * actual certificate used. For instance, if RSAPublicKey is used, the
         * authType should be "RSA". Checking is case-sensitive.
         *
         * @param certs the peer certificate chain
         * @param authType the authentication type based on the client
         * certificate
         *
         * @throws IllegalArgumentException - if null or zero-length chain is
         * passed in for the chain parameter or if null or zero-length string
         * is passed in for the authType parameter
         * @throws CertificateException - if the certificate chain is not
         * trusted by this TrustManager.
         */
        public void checkClientTrusted(X509Certificate[] certs, String authType)
            throws CertificateException
        {
        }

        /**
         * Given the partial or complete certificate chain provided by the peer,
         * build a certificate path to a trusted root and return if it can be
         * validated and is trusted for server SSL authentication based on the
         * authentication type. The authentication type is the key exchange
         * algorithm portion of the cipher suites represented as a String, such
         * as "RSA", "DHE_DSS". Note: for some exportable cipher suites, the
         * key exchange algorithm is determined at run time during the
         * handshake. For instance, for TLS_RSA_EXPORT_WITH_RC4_40_MD5, the
         * authType should be RSA_EXPORT when an ephemeral RSA key is used for
         * the key exchange, and RSA when the key from the server certificate
         * is used. Checking is case-sensitive.
         *
         * @param certs the peer certificate chain
         * @param authType the key exchange algorithm used
         *
         * @throws IllegalArgumentException if null or zero-length chain is
         * passed in for the chain parameter or if null or zero-length string
         * is passed in for the authType parameter
         * @throws CertificateException if the certificate chain is not trusted
         * by this TrustManager.
         */
        public void checkServerTrusted(X509Certificate[] certs, String authType)
            throws CertificateException
        {
        }
    }
}
