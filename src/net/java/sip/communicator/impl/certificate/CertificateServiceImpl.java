/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.certificate;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.*;

import javax.net.ssl.*;
import javax.swing.*;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.X509Extension;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of the CertificateService. It asks the user to trust a
 * certificate when the automatic verification fails.
 * 
 * @author Ingo Bauersachs
 */
public class CertificateServiceImpl
    implements CertificateService
{
    // services
    private static final Logger logger =
        Logger.getLogger(CertificateServiceImpl.class);

    private final ResourceManagementService R =
        CertificateVerificationActivator.getResources();

    private final ConfigurationService config =
        CertificateVerificationActivator.getConfigurationService();

    // properties
    /**
     * Base property name for the storage of certificate user preferences.
     */
    private final static String PNAME_CERT_TRUST_PREFIX =
        "net.java.sip.communicator.impl.certservice";

    /** Hash algorithm for the cert thumbprint*/
    private final static String THUMBPRINT_HASH_ALGORITHM = "SHA1";

    // variables
    /**
     * Stores the certificates that are trusted as long as this service lives.
     */
    private Map<String, String> sessionAllowedCertificates =
        new HashMap<String, String>();

    /*
     * (non-Javadoc)
     * 
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * addCertificateToTrust(java.security.cert.Certificate, java.lang.String,
     * int)
     */
    public void addCertificateToTrust(Certificate cert, String trustFor,
        int trustMode)
        throws CertificateException
    {
        switch (trustMode)
        {
        case DO_NOT_TRUST:
            throw new IllegalArgumentException(
                "Cannot add a certificate to trust when "
                + "no trust is requested.");
        case TRUST_ALWAYS:
            config.setProperty(PNAME_CERT_TRUST_PREFIX + ".param." + trustFor,
                getThumbprint(cert, THUMBPRINT_HASH_ALGORITHM));
            break;
        case TRUST_THIS_SESSION_ONLY:
            sessionAllowedCertificates.put(PNAME_CERT_TRUST_PREFIX + ".param."
                + trustFor, getThumbprint(cert, THUMBPRINT_HASH_ALGORITHM));
            break;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * getSSLContext()
     */
    public SSLContext getSSLContext() throws GeneralSecurityException
    {
        return getSSLContext(getTrustManager((Iterable<String>)null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.java.sip.communicator.service.certificate.CertificateService#
     * getSSLContext(javax.net.ssl.X509TrustManager)
     */
    public SSLContext getSSLContext(X509TrustManager trustManager)
        throws GeneralSecurityException
    {
        try
        {
            KeyStore ks =
                KeyStore.getInstance(System.getProperty(
                    "javax.net.ssl.keyStoreType", KeyStore.getDefaultType()));
            KeyManagerFactory kmFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());

            String keyStorePassword =
                System.getProperty("javax.net.ssl.keyStorePassword");
            if (System.getProperty("javax.net.ssl.keyStore") != null)
            {
                ks.load(
                    new FileInputStream(System
                        .getProperty("javax.net.ssl.keyStore")), null);
            }
            else
            {
                ks.load(null, null);
            }

            kmFactory.init(ks, keyStorePassword == null ? null
                : keyStorePassword.toCharArray());

            //TODO: inject our own socket factory to use our own DNS stuff
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmFactory.getKeyManagers(), new TrustManager[]
            { trustManager }, null);

            return sslContext;
        }
        catch (Exception e)
        {
            throw new GeneralSecurityException("Cannot init SSLContext: "
                + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.java.sip.communicator.service.certificate
     * .CertificateService#getTrustManager(java.lang.Iterable)
     */
    public X509TrustManager getTrustManager(Iterable<String> identitiesToTest)
        throws GeneralSecurityException
    {
        return getTrustManager(
            identitiesToTest,
            new EMailAddressMatcher(),
            new BrowserLikeHostnameMatcher()
        );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.java.sip.communicator.service.certificate.CertificateService
     * #getTrustManager(java.lang.String)
     */
    public X509TrustManager getTrustManager(String identityToTest)
        throws GeneralSecurityException
    {
        return getTrustManager(
            Arrays.asList(new String[]{identityToTest}),
            new EMailAddressMatcher(),
            new BrowserLikeHostnameMatcher()
        );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.java.sip.communicator.service.certificate.CertificateService
     * #getTrustManager(java.lang.String,
     * net.java.sip.communicator.service.certificate.CertificateMatcher,
     * net.java.sip.communicator.service.certificate.CertificateMatcher)
     */
    public X509TrustManager getTrustManager(
        String identityToTest,
        CertificateMatcher clientVerifier,
        CertificateMatcher serverVerifier)
        throws GeneralSecurityException
    {
        return getTrustManager(
            Arrays.asList(new String[]{identityToTest}),
            clientVerifier,
            serverVerifier
        );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.java.sip.communicator.service.certificate.CertificateService
     * #getTrustManager(java.lang.Iterable,
     * net.java.sip.communicator.service.certificate.CertificateMatcher,
     * net.java.sip.communicator.service.certificate.CertificateMatcher)
     */
    public X509TrustManager getTrustManager(
        final Iterable<String> identitiesToTest,
        final CertificateMatcher clientVerifier,
        final CertificateMatcher serverVerifier)
        throws GeneralSecurityException
    {
        // obtain the default X509 trust manager
        X509TrustManager defaultTm = null;
        TrustManagerFactory tmFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory
                .getDefaultAlgorithm());
        tmFactory.init((KeyStore) null);
        for (TrustManager m : tmFactory.getTrustManagers())
        {
            if (m instanceof X509TrustManager)
            {
                defaultTm = (X509TrustManager) m;
                break;
            }
        }
        if (defaultTm == null)
            throw new GeneralSecurityException(
                "No default X509 trust manager found");

        final X509TrustManager tm = defaultTm;

        return new X509TrustManager()
        {
            private boolean serverCheck;

            public X509Certificate[] getAcceptedIssuers()
            {
                return tm.getAcceptedIssuers();
            }

            public void checkServerTrusted(X509Certificate[] chain,
                String authType) throws CertificateException
            {
                serverCheck = true;
                checkCertTrusted(chain, authType);
            }

            public void checkClientTrusted(X509Certificate[] chain,
                String authType) throws CertificateException
            {
                serverCheck = false;
                checkCertTrusted(chain, authType);
            }

            private void checkCertTrusted(X509Certificate[] chain,
                String authType) throws CertificateException
            {
                if(config.getBoolean(PNAME_ALWAYS_TRUST, false))
                    return;

                try
                {
                    // check the certificate itself (issuer, validity)
                    try
                    {
                        chain = tryBuildChain(chain);
                    }
                    catch (Exception e)
                    {} // don't care and take the chain as is

                    if(serverCheck)
                        tm.checkServerTrusted(chain, authType);
                    else
                        tm.checkClientTrusted(chain, authType);

                    if(identitiesToTest == null
                        || !identitiesToTest.iterator().hasNext())
                        return;
                    else if(serverCheck)
                        serverVerifier.verify(identitiesToTest, chain[0]);
                    else
                        clientVerifier.verify(identitiesToTest, chain[0]);

                    // ok, globally valid cert
                }
                catch (CertificateException e)
                {
                    String thumbprint = getThumbprint(
                        chain[0], THUMBPRINT_HASH_ALGORITHM);
                    String propName = null;
                    String message = null;
                    String storedCert = null;
                    String appName =
                        R.getSettingsString("service.gui.APPLICATION_NAME");

                    if (identitiesToTest == null
                        || !identitiesToTest.iterator().hasNext())
                    {
                        propName =
                            PNAME_CERT_TRUST_PREFIX + ".server." + thumbprint;
                        message =
                            R.getI18NString("service.gui."
                                + "CERT_DIALOG_DESCRIPTION_TXT_NOHOST",
                                new String[] {
                                    appName
                                }
                            );

                        // get the thumbprint from the permanent allowances
                        storedCert = config.getString(propName);
                        // not found? check the session allowances
                        if (storedCert == null)
                            storedCert =
                                sessionAllowedCertificates.get(propName);
                    }
                    else
                    {
                        for (String identity : identitiesToTest)
                        {
                            if (serverCheck)
                            {
                                message =
                                    R.getI18NString(
                                        "service.gui."
                                        + "CERT_DIALOG_DESCRIPTION_TXT",
                                        new String[] {
                                            appName,
                                            identitiesToTest.toString()
                                        }
                                    );
                                propName =
                                    PNAME_CERT_TRUST_PREFIX + ".param."
                                        + identity;
                            }
                            else
                            {
                                message =
                                    R.getI18NString(
                                        "service.gui."
                                        + "CERT_DIALOG_PEER_DESCRIPTION_TXT",
                                        new String[] {
                                            appName,
                                            identitiesToTest.toString()
                                        }
                                    );
                                propName =
                                    PNAME_CERT_TRUST_PREFIX + ".param."
                                        + identity;
                            }

                            // get the thumbprint from the permanent allowances
                            storedCert = config.getString(propName);
                            // not found? check the session allowances
                            if (storedCert == null)
                                storedCert =
                                    sessionAllowedCertificates.get(propName);

                            // stop search for further saved allowances if we
                            // found a match
                            if (storedCert != null)
                                break;
                        }
                    }

                    if (!thumbprint.equals(storedCert))
                    {
                        switch (verify(chain, message))
                        {
                        case DO_NOT_TRUST:
                            throw new CertificateException(
                                "The peer provided certificate with Subject <"
                                    + chain[0].getSubjectDN()
                                    + "> is not trusted");
                        case TRUST_ALWAYS:
                            config.setProperty(propName, thumbprint);
                            break;
                        case TRUST_THIS_SESSION_ONLY:
                            sessionAllowedCertificates
                                .put(propName, thumbprint);
                            break;
                        }
                    }
                    // ok, we've seen this certificate before
                }
            }

            private X509Certificate[] tryBuildChain(X509Certificate[] chain)
                throws IOException,
                URISyntaxException,
                CertificateException
            {
                // Only try to build chains for servers that send only their
                // own cert, but no issuer. This also matches self signed (will
                // be ignored later) and Root-CA signed certs. In this case we
                // throw the Root-CA away after the lookup
                if (chain.length != 1)
                    return chain;

                // ignore self signed certs
                if (chain[0].getIssuerDN().equals(chain[0].getSubjectDN()))
                    return chain;

                // prepare for the newly created chain
                List<X509Certificate> newChain =
                    new ArrayList<X509Certificate>(chain.length + 4);
                for (X509Certificate cert : chain)
                {
                    newChain.add(cert);
                }

                // search from the topmost certificate upwards
                CertificateFactory certFactory =
                    CertificateFactory.getInstance("X.509");
                X509Certificate current = chain[chain.length - 1];
                boolean foundParent;
                int chainLookupCount = 0;
                do
                {
                    foundParent = false;
                    // extract the url(s) where the parent certificate can be
                    // found
                    byte[] aiaBytes =
                        current.getExtensionValue(
                            X509Extension.authorityInfoAccess.getId());
                    if (aiaBytes == null)
                        break;

                    DEROctetString octs =
                        (DEROctetString) ASN1Object.fromByteArray(aiaBytes);
                    ASN1InputStream as = new ASN1InputStream(octs.getOctets());
                    AuthorityInformationAccess aia =
                        AuthorityInformationAccess
                            .getInstance(as.readObject());
                    // the AIA may contain different URLs and types, try all
                    // of them
                    for (AccessDescription ad : aia.getAccessDescriptions())
                    {
                        // we are only interested in the issuer certificate,
                        // not in OCSP urls the like
                        if (!ad.getAccessMethod().equals(
                            AccessDescription.id_ad_caIssuers))
                            continue;

                        GeneralName gn = ad.getAccessLocation();
                        if (!(gn.getTagNo() ==
                                GeneralName.uniformResourceIdentifier
                            && gn.getName() instanceof DERIA5String))
                            continue;

                        URI uri =
                            new URI(((DERIA5String) gn.getName()).getString());
                        // only http(s) urls; LDAP is taken care of in the
                        // default implementation
                        if (!(uri.getScheme().equalsIgnoreCase("http") || uri
                            .getScheme().equals("https")))
                            continue;

                        if (logger.isDebugEnabled())
                            logger
                                .debug("Downloading parent certificate for <"
                                    + current.getSubjectDN()
                                    + "> from <"
                                    + uri
                                    + ">");

                        try
                        {
                            InputStream is =
                                HttpUtils.openURLConnection(uri.toString())
                                    .getContent();
                            X509Certificate cert =
                                (X509Certificate) certFactory
                                    .generateCertificate(is);
                            if(!cert.getIssuerDN().equals(cert.getSubjectDN()))
                            {
                                newChain.add(cert);
                                foundParent = true;
                                current = cert;
                                break; // an AD was valid, ignore others
                            }
                            else
                                logger.debug("Parent is self-signed, ignoring");
                        }
                        catch (Exception e)
                        {
                            logger.debug("Could not download from <" + uri
                                + ">");
                        }
                    }
                    chainLookupCount++;
                }
                while (foundParent && chainLookupCount < 10);
                chain = newChain.toArray(chain);
                return chain;
            }
        };
    }

    protected class BrowserLikeHostnameMatcher
        implements CertificateMatcher
    {
        public void verify(Iterable<String> identitiesToTest,
            X509Certificate cert) throws CertificateException
        {
            // check whether one of the hostname is present in the
            // certificate
            boolean oneMatched = false;
            for(String identity : identitiesToTest)
            {
                try
                {
                    org.apache.http.conn.ssl.SSLSocketFactory
                        .BROWSER_COMPATIBLE_HOSTNAME_VERIFIER
                        .verify(identity, cert);
                    oneMatched = true;
                    break;
                }
                catch (SSLException e)
                {}
            }

            if (!oneMatched)
                throw new CertificateException("None of <"
                    + identitiesToTest
                    + "> matched the cert with CN="
                    + cert.getSubjectDN());
        }
    }

    protected class EMailAddressMatcher
        implements CertificateMatcher
    {
        public void verify(Iterable<String> identitiesToTest,
            X509Certificate cert) throws CertificateException
        {
            // check if the certificate contains the E-Mail address(es)
            // in the SAN(s)
            //TODO: extract address from DN (E-field) too?
            boolean oneMatched = false;
            Iterable<String> emails = getSubjectAltNames(cert, 6);
            for(String identity : identitiesToTest)
            {
                for(String email : emails)
                {
                    if(identity.equalsIgnoreCase(email))
                    {
                        oneMatched = true;
                        break;
                    }
                }
            }
            if(!oneMatched)
                throw new CertificateException(
                    "The peer provided certificate with Subject <"
                    + cert.getSubjectDN()
                    + "> contains no SAN for <"
                    + identitiesToTest + ">");
        }
    }

    /**
     * Asks the user whether he trusts the supplied chain of certificates.
     * 
     * @param chain The chain of the certificates to check with user.
     * @param message A text that describes why the verification failed.
     * @return The result of the user interaction. One of
     *         {@link CertificateService#DO_NOT_TRUST},
     *         {@link CertificateService#TRUST_THIS_SESSION_ONLY},
     *         {@link CertificateService#TRUST_ALWAYS}
     */
    protected int verify(final X509Certificate[] chain, final String message)
    {
        if(config.getBoolean(PNAME_NO_USER_INTERACTION, false))
            return DO_NOT_TRUST;

        final VerifyCertificateDialog dialog =
            new VerifyCertificateDialog(chain, null, message);
        try
        {
            // show the dialog in the swing thread and wait for the user
            // choice
            SwingUtilities.invokeAndWait(new Runnable()
            {
                public void run()
                {
                    dialog.setVisible(true);
                }
            });
        }
        catch (Exception e)
        {
            logger.error("Cannot show certificate verification dialog", e);
            return DO_NOT_TRUST;
        }

        if(!dialog.isTrusted)
            return DO_NOT_TRUST;
        else if(dialog.alwaysTrustCheckBox.isSelected())
            return TRUST_ALWAYS;
        else
            return TRUST_THIS_SESSION_ONLY;
    }

    /**
     * Calculates the hash of the certificate known as the "thumbprint"
     * and returns it as a string representation.
     * 
     * @param cert The certificate to hash.
     * @param algorithm The hash algorithm to use.
     * @return The SHA-1 hash of the certificate.
     * @throws CertificateException
     */
    static String getThumbprint(Certificate cert, String algorithm)
        throws CertificateException
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new CertificateException(e);
        }
        byte[] encodedCert = cert.getEncoded();
        StringBuilder sb = new StringBuilder(encodedCert.length * 2);
        Formatter f = new Formatter(sb);
        for (byte b : digest.digest(encodedCert))
        {
            f.format("%02x", b);
        }
        return sb.toString();
    }

    /**
     * Gets the SAN (Subject Alternative Name) of the specified type.
     * 
     * @param cert the certificate to extract from
     * @param altNameType The type to be returned
     * @return SAN of the type
     * 
     * <PRE>
     * GeneralName ::= CHOICE {
     *                 otherName                   [0]   OtherName,
     *                 rfc822Name                  [1]   IA5String,
     *                 dNSName                     [2]   IA5String,
     *                 x400Address                 [3]   ORAddress,
     *                 directoryName               [4]   Name,
     *                 ediPartyName                [5]   EDIPartyName,
     *                 uniformResourceIdentifier   [6]   IA5String,
     *                 iPAddress                   [7]   OCTET STRING,
     *                 registeredID                [8]   OBJECT IDENTIFIER
     *              }
     * <PRE>
     */
    private static Iterable<String> getSubjectAltNames(X509Certificate cert,
        int altNameType)
    {
        Collection<List<?>> altNames = null;
        try
        {
            altNames = cert.getSubjectAlternativeNames();
        }
        catch (CertificateParsingException e)
        {
            return Collections.emptyList();
        }

        List<String> matchedAltNames = new LinkedList<String>();
        for (List<?> item : altNames)
        {
            if (item.contains(altNameType))
            {
                Integer type = (Integer) item.get(0);
                if (type.intValue() == altNameType)
                    matchedAltNames.add((String) item.get(1));
            }
        }
        return matchedAltNames;
    }
}
