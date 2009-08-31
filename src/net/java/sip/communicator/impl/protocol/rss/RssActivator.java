/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.rss;

import org.osgi.framework.*;

import java.util.*;

import javax.net.ssl.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Loads the Rss provider factory and registers its services in the OSGI
 * bundle context.
 *
 * @author Jean-Albert Vescovo
 * @author Mihai Balan
 * @author Emil Ivov
 */
public class RssActivator
    implements BundleActivator
{
    private static final Logger logger
        = Logger.getLogger(RssActivator.class);

    /**
     * A reference to the registration of our Rss protocol provider
     * factory.
     */
    private ServiceRegistration  rssPpFactoryServReg   = null;

    /**
     * A reference to the Rss protocol provider factory.
     */
    private static ProtocolProviderFactoryRssImpl
                                    rssProviderFactory = null;

    /**
     * The currently valid bundle context.
     */
    static BundleContext bundleContext = null;

    /**
     * The <tt>ResourceManagementService</tt> that we use in this provider.
     */
    private static ResourceManagementService resourcesService = null;

    /**
     * The <tt>UIService</tt> that we use in this provider.
     */
    private static UIService uiService = null;

    /**
     * The uri handler that would be handling all feed:// links.
     */
    private UriHandlerRssImpl uriHandler = null;

    /**
     * Called when this bundle is started. In here we'll export the
     * rss ProtocolProviderFactory implementation so that it could be
     * possible to register accounts with it in SIP Communicator.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    public void start(BundleContext context)
        throws Exception
    {
        this.bundleContext = context;

        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        hashtable.put(ProtocolProviderFactory.PROTOCOL, "RSS");

        rssProviderFactory = new ProtocolProviderFactoryRssImpl();

        //reg the rss provider factory.
        rssPpFactoryServReg =  context.registerService(
                    ProtocolProviderFactory.class.getName(),
                    rssProviderFactory,
                    hashtable);

        logger.info("RSS protocol implementation [STARTED].");

        System.setProperty(
            "http.agent",
            System.getProperty("sip-communicator.application.name")
            + "/"
            + System.getProperty("sip-communicator.version"));
        logger.debug("User-Agent set to " + System.getProperty("http.agent"));

        installCustomSSLTrustManager();

        uriHandler = new UriHandlerRssImpl();
        bundleContext.addServiceListener(uriHandler);
        uriHandler.registerHandlerService();
    }

    /**
     * Installs a trust manager that would accept all certificates so that
     * we could install rss feeds from sites with expired/self-signed
     * certificates.
     */
    private void installCustomSSLTrustManager() throws Exception
    {
        // Let us create the factory where we can set some parameters for the
        //connection
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null,
                new TrustManager[]{ new TrustlessManager()},
                new java.security.SecureRandom());

        // Create the socket connection and open it to the secure remote web server
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    }

    /**
     * Returns a reference to the bundle context that we were started with.
     * @return a reference to the BundleContext instance that we were started
     * witn.
     */
    public static BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Retrurns a reference to the protocol provider factory that we have
     * registered.
     * @return a reference to the <tt>ProtocolProviderFactoryJabberImpl</tt>
     * instance that we have registered from this package.
     */
    public static ProtocolProviderFactoryRssImpl getProtocolProviderFactory()
    {
        return rssProviderFactory;
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context)
        throws Exception
    {
        this.rssProviderFactory.stop();
        rssPpFactoryServReg.unregister();

        context.removeServiceListener(uriHandler);

        logger.info("RSS protocol implementation [STOPPED].");
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>.
     * @return the <tt>ResourceManagementService</tt>.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService = (ResourceManagementService) bundleContext
                .getService(serviceReference);
        }

        return resourcesService;
    }

    /**
     * Returns a reference to the <tt>UIService</tt> instance that is currently
     * in use.
     * @return a reference to the currently valid <tt>UIService</tt>.
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(UIService.class.getName());

            if(serviceReference == null)
                return null;

            uiService = (UIService) bundleContext
                .getService(serviceReference);
        }

        return uiService;
    }

    /**
     * A trust manager that would accept all certificates so that we would be
     * able to add rss feeds from sites with expired/self-signed certificates.
     */
    private static class TrustlessManager implements X509TrustManager
    {

        public java.security.cert.X509Certificate[] getAcceptedIssuers()
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
         * @param chain the peer certificate chain
         * @param authType the authentication type based on the client
         * certificate
         *
         * @throws IllegalArgumentException - if null or zero-length chain is
         * passed in for the chain parameter or if null or zero-length string
         * is passed in for the authType parameter
         * @throws CertificateException - if the certificate chain is not
         * trusted by this TrustManager.
         */
        public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs,
                        String authType)
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
         * @param chain the peer certificate chain
         * @param authType the key exchange algorithm used
         *
         * @throws IllegalArgumentException if null or zero-length chain is
         * passed in for the chain parameter or if null or zero-length string
         * is passed in for the authType parameter
         * @throws CertificateException if the certificate chain is not trusted
         * by this TrustManager.
         */
        public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs,
                        String authType)
        {
        }
    }
}
