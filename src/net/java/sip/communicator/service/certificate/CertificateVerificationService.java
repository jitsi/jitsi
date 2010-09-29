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
 * @author Damian Minkov
 */
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
     * Checks does the user trust the supplied chain of certificates, when
     * connecting to the server and port. If needed shows dialog to confirm.
     *
     * @param   chain the chain of the certificates to check with user.
     * @param   toHost the host we are connecting.
     * @param   toPort the port used when connecting.
     * @return  the result of user interaction on of DO_NOT_TRUST, TRUST_ALWAYS,
     *          TRUST_THIS_SESSION_ONLY.
     */
    public int verify(X509Certificate[] chain, String toHost, int toPort);

    /**
     * Obtain custom trust manager, which will try verify the certificate and
     * if verification fails will query the user for acceptance.
     *
     * @param   toHost the host we are connecting.
     * @param   toPort the port used when connecting.
     * @return the custom trust manager.
     * @throws GeneralSecurityException when there is problem creating
     *         the trust manager
     */
    public X509TrustManager getTrustManager(String toHost, int toPort)
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
}
