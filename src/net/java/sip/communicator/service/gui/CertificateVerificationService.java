/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.security.cert.*;

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
     * connecting to the server and port.
     *
     * @param   chain the chain of the certificates to check with user.
     * @param   toHost the host we are connecting.
     * @param   toPort the port used when connecting.
     * @return  the result of user interaction on of DO_NOT_TRUST, TRUST_ALWAYS,
     *          TRUST_THIS_SESSION_ONLY.
     */
    int verificationNeeded(Certificate[] chain, String toHost, int toPort);
}
