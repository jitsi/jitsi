/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.certificate;

import java.security.GeneralSecurityException;
import java.security.cert.*;

import javax.net.ssl.*;

/**
 * A service which implementors will ask the user for permission for the
 * certificates which are for some reason not valid and not globally trusted.
 *
 * @author Damian Minkov
 * @author Ingo Bauersachs
 */
public interface CertificateService
{
    /**
     * Property for always trust mode. When enabled certificate check is
     * skipped.
     */
    public final static String PNAME_ALWAYS_TRUST =
        "net.java.sip.communicator.service.gui.ALWAYS_TRUST_MODE_ENABLED";

    /**
     * When set to true, the certificate check is performed. If the check fails
     * the user is not asked and the error is directly reported to the calling
     * service.
     */
    public final static String PNAME_NO_USER_INTERACTION =
        "net.java.sip.communicator.service.tls.NO_USER_INTERACTION";

    /**
     * Result of user interaction. User does not trust this certificate.
     */
    public final static int DO_NOT_TRUST = 0;

    /**
     * Result of user interaction. User will always trust this certificate.
     */
    public final static int TRUST_ALWAYS = 1;

    /**
     * Result of user interaction. User will trust this certificate
     * only for the current session.
     */
    public final static int TRUST_THIS_SESSION_ONLY = 2;

    /**
     * Get an SSL Context that validates certificates based on the JRE default
     * check and asks the user when the JRE check fails.
     * 
     * CAUTION: Only the certificate itself is validated, no check is performed
     * whether it is valid for a specific server or client.
     * 
     * @return An SSL context based on a user confirming trust manager.
     * @throws GeneralSecurityException
     */
    public SSLContext getSSLContext() throws GeneralSecurityException;

    /**
     * Get an SSL Context with the specified trustmanager.
     * 
     * @param trustManager The trustmanager that will be used by the created
     *            SSLContext
     * @return An SSL context based on the supplied trust manager.
     * @throws GeneralSecurityException
     */
    public SSLContext getSSLContext(X509TrustManager trustManager)
        throws GeneralSecurityException;

    /**
     * Creates a trustmanager that validates the certificate based on the JRE
     * default check and asks the user when the JRE check fails. When
     * <tt>null</tt> is passed as the <tt>identityToTest</tt> then no check is
     * performed whether the certificate is valid for a specific server or
     * client. The passed identities are checked by applying a behavior similar
     * to the on regular browsers use.
     * 
     * @param identitiesToTest when not <tt>null</tt>, the values are assumed
     *            to be hostnames for invocations of checkServerTrusted and
     *            e-mail addresses for invocations of checkClientTrusted
     * @return TrustManager to use in an SSLContext
     * @throws GeneralSecurityException
     */
    public X509TrustManager getTrustManager(Iterable<String> identitiesToTest)
        throws GeneralSecurityException;

    /**
     * @see #getTrustManager(Iterable)
     * 
     * @param identityToTest when not <tt>null</tt>, the value is assumed to
     *            be a hostname for invocations of checkServerTrusted and an
     *            e-mail address for invocations of checkClientTrusted
     * @return TrustManager to use in an SSLContext
     * @throws GeneralSecurityException
     */
    public X509TrustManager getTrustManager(String identityToTest)
        throws GeneralSecurityException;

    /**
     * @see #getTrustManager(Iterable, CertificateMatcher, CertificateMatcher)
     * 
     * @param identityToTest The identity to match against the supplied
     *            verifiers.
     * @param clientVerifier The verifier to use in calls to checkClientTrusted
     * @param serverVerifier The verifier to use in calls to checkServerTrusted
     * @return TrustManager to use in an SSLContext
     * @throws GeneralSecurityException
     */
    public X509TrustManager getTrustManager(
        final String identityToTest,
        final CertificateMatcher clientVerifier,
        final CertificateMatcher serverVerifier)
        throws GeneralSecurityException;

    /**
     * Creates a trustmanager that validates the certificate based on the JRE
     * default check and asks the user when the JRE check fails. When
     * <tt>null</tt> is passed as the <tt>identityToTest</tt> then no check is
     * performed whether the certificate is valid for a specific server or
     * client.
     * 
     * @param identitiesToTest The identities to match against the supplied
     *            verifiers.
     * @param clientVerifier The verifier to use in calls to checkClientTrusted
     * @param serverVerifier The verifier to use in calls to checkServerTrusted
     * @return TrustManager to use in an SSLContext
     * @throws GeneralSecurityException
     */
    public X509TrustManager getTrustManager(
        final Iterable<String> identitiesToTest,
        final CertificateMatcher clientVerifier,
        final CertificateMatcher serverVerifier)
        throws GeneralSecurityException;

    /**
     * Adds a certificate to the local trust store.
     * 
     * @param cert The certificate to add to the trust store.
     * @param trustMode Whether to trust the certificate permanently or only
     *            for the current session.
     * @throws CertificateException when the thumbprint could not be calculated
     */
    public void addCertificateToTrust(Certificate cert, String trustFor,
        int trustMode) throws CertificateException;
}