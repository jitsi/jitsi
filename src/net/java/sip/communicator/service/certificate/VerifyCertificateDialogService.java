/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.certificate;

import java.security.cert.*;

/**
 * Service that creates dialog that is shown to the user when
 * a certificate verification failed.
 *
 * @author Damian Minkov
 */
public interface VerifyCertificateDialogService
{
    /**
     * Creates the dialog.
     *
     * @param certs the certificates list
     * @param title The title of the dialog; when null the resource
     * <tt>service.gui.CERT_DIALOG_TITLE</tt> is loaded and used.
     * @param message A text that describes why the verification failed.
     */
    public VerifyCertificateDialog createDialog(
        Certificate[] certs, String title, String message);

    /**
     * The dialog implementers should return <tt>VerifyCertificateDialog</tt>.
     */
    public interface VerifyCertificateDialog
    {
        /**
         * Shows or hides the dialog and waits for user response.
         * @param isVisible whether we should show or hide the dialog.
         */
        public void setVisible(boolean isVisible);

        /**
         * Whether the user has accepted the certificate or not.
         * @return whether the user has accepted the certificate or not.
         */
        public boolean isTrusted();

        /**
         * Whether the user has selected to note the certificate so we always
         * trust it.
         * @return whether the user has selected to note the certificate so
         * we always trust it.
         */
        public boolean isAlwaysTrustSelected();
    }
}
