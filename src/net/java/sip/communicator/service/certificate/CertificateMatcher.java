/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.certificate;

import java.security.cert.*;

/**
 * Interface to verify X.509 certificate
 */
public interface CertificateMatcher
{
    /**
     * Implementations check whether one of the supplied identities is
     * contained in the certificate.
     *
     * @param identitiesToTest The that are compared against the certificate.
     * @param cert The X.509 certificate that was supplied by the server or
     *            client.
     * @throws CertificateException When any certificate parsing fails.
     */
    public void verify(Iterable<String> identitiesToTest, X509Certificate cert)
        throws CertificateException;
}
