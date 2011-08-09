/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.certificate;

import java.io.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

/**
 * A service which implementors will ask the user for permission for the
 * certificates which are for some reason not valid and not globally trusted.
 *
 * @deprecated Use the new {@link CertificateService}
 *
 * @author Damian Minkov
 */
@Deprecated
public interface CertificateVerificationService
{
    /**
     * Property for always trust mode. When enabled certificate check is skipped.
     */
    public final static String ALWAYS_TRUST_MODE_ENABLED_PROP_NAME =
        "net.java.sip.communicator.service.gui.ALWAYS_TRUST_MODE_ENABLED";

    /**
     * Result of user interaction. User don't trust this certificate.
     */
    public static int DO_NOT_TRUST = 0;

    /**
     * Result of user interaction. User will always trust this certificate.
     */
    public static int TRUST_ALWAYS = 1;

    /**
     * Result of user interaction. User will trust this certificate
     * only during current session.
     */
    public static int TRUST_THIS_SESSION_ONLY = 2;

    /**
     * Add a certificate to the local trust store.
     * 
     * @param cert The certificate to add to the trust store.
     * @param trustMode Whether to trust the certificate permanently
     *  or only for the current session.
     * @throws GeneralSecurityException
     */
    public void addCertificateToTrust(X509Certificate cert, int trustMode)
        throws GeneralSecurityException;

    /**
     * Obtain custom trust manager, which tries to verify the certificate and
     * queries the user for acceptance when verification fails.
     * 
     * @param   message A text that describes why the verification failed.
     * @return the custom trust manager.
     * @throws GeneralSecurityException when there was a problem creating
     *         the trust manager
     */
    public X509TrustManager getTrustManager(String message)
        throws GeneralSecurityException;

    /**
     * Returns SSLContext instance initialized with the custom trust manager,
     * which will try verify the certificate and if verification fails
     * will query the user for acceptance.
     *
     * @param   toHost the host we are connecting.
     * @param   toPort the port used when connecting.
     * @return  the SSLContext
     * @throws IOException throws exception when unable to initialize the
     *  ssl context.
     */
    public SSLContext getSSLContext(String toHost, int toPort)
        throws IOException;

    /**
     * Returns SSLContext instance initialized with the custom trust manager,
     * which will try verify the certificate and if verification fails
     * will query the user for acceptance.
     *
     * @param message The message to show on the verification GUI if necessary
     * @return  the SSLContext
     * @throws IOException throws exception when unable to initialize the
     *  ssl context.
     */
    public SSLContext getSSLContext(String message)
        throws IOException;
}
