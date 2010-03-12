/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.login;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import java.security.cert.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.impl.gui.*;


/**
 * Asks the user for permission for the
 * certificates which are for some reason not valid and not globally trusted.
 *
 * @author Damian Minkov
 */
public class CertificateVerificationServiceImpl
    implements CertificateVerificationService
{

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
    public int verificationNeeded(Certificate[] chain, String toHost, int toPort)
    {
        VerifyCertificateDialog dialog = new VerifyCertificateDialog(
            chain, toHost, toPort);

        dialog.setVisible(true);

        if(!dialog.isTrusted)
            return DO_NOT_TRUST;
        else if(dialog.alwaysTrustCheckBox.isSelected())
            return TRUST_ALWAYS;
        else
            return TRUST_THIS_SESSION_ONLY;
    }

    /**
     * The dialog that is shown to user.
     */
    private static class VerifyCertificateDialog
        extends JDialog
    {
        /**
         * The certificate to show.
         */
        Certificate cert;

        /**
         * The host we are connecting to.
         */
        String host;

        /**
         * The port we use.
         */
        int port;

        /**
         * The certificate panel.
         */
        JPanel certPanel;

        /**
         * Whether certificate description is shown.
         */
        boolean certOpened = false;

        /**
         * The button to show certificate description.
         */
        JButton certButton;

        /**
         * The check box if checked permanently stored the certificate
         * which will be always trusted.
         */
        JCheckBox alwaysTrustCheckBox = new JCheckBox(
            GuiActivator.getResources().getI18NString("service.gui.ALWAYS_TRUST"),
            false);

        /**
         * Whether the user trusts this certificate.
         */
        boolean isTrusted = false;

        /**
         * Creates the dialog.
         * @param certs
         * @param host 
         * @param port
         */
        public VerifyCertificateDialog(Certificate[] certs, String host, int port)
        {
            super(GuiActivator.getUIService().getMainFrame(),
                  GuiActivator.getResources().getI18NString(
                        "service.gui.CERT_DIALOG_TITLE"),
                  true);

            // for now shows only the first certificate from the chain
            this.cert = certs[0];
            this.host = host;
            this.port = port;

            setMinimumSize(new Dimension(600, 200));

            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            init();

            setLocationRelativeTo(getParent());
        }

        /**
         * Inits the dialog initial display.
         */
        private void init()
        {
            this.getContentPane().setLayout(new BorderLayout(5, 5));

            JLabel imgLabel = new JLabel(
                GuiActivator.getResources().getImage(
                    "impl.media.security.zrtp.CONF_ICON"));
            imgLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            this.getContentPane().add(imgLabel, BorderLayout.WEST);

            String descriptionTxt = GuiActivator.getResources()
                .getI18NString("service.gui.CERT_DIALOG_DESCRIPTION_TXT",
                               new String[]{host, String.valueOf(port)});

            JTextArea descriptionLabel = new JTextArea();
            descriptionLabel.setEditable(false);
            descriptionLabel.setWrapStyleWord(true);
            descriptionLabel.setLineWrap(true);
            descriptionLabel.setOpaque(false);
            descriptionLabel.setText(descriptionTxt);
            descriptionLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            this.getContentPane().add(descriptionLabel, BorderLayout.CENTER);

            JPanel southPanel = new JPanel(new BorderLayout());
            southPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

            certPanel = new JPanel();
            southPanel.add(certPanel, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new BorderLayout());
            southPanel.add(buttonPanel, BorderLayout.SOUTH);

            certButton = new JButton();
            certButton.setText("Show Certificate");
            certButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e)
                {
                    actionShowCertificate();
                }
            });
            JPanel firstButonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            firstButonPanel.add(certButton);
            buttonPanel.add(firstButonPanel, BorderLayout.WEST);

            JPanel secondButonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancelButton = new JButton(
                GuiActivator.getResources().getI18NString("service.gui.CANCEL"));
            cancelButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e)
                {
                    actionCancel();
                }
            });
            JButton continueButton = new JButton(
                GuiActivator.getResources().getI18NString("service.gui.CONTINUE"));
            continueButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e)
                {
                    actionContinue();
                }
            });
            secondButonPanel.add(continueButton);
            secondButonPanel.add(cancelButton);
            buttonPanel.add(secondButonPanel, BorderLayout.EAST);

            this.getContentPane().add(southPanel, BorderLayout.SOUTH);

            pack();
        }

        /**
         * Action when shoe certificate button is clicked.
         */
        private void actionShowCertificate()
        {
            if(certOpened)
            {
                certPanel.removeAll();
                certButton.setText(GuiActivator.getResources()
                    .getI18NString("service.gui.SHOW_CERT"));

                certPanel.revalidate();
                certPanel.repaint();
                pack();
                certOpened = false;
                setLocationRelativeTo(getParent());
                return;
            }

            certPanel.setLayout(new BorderLayout());
            certPanel.add(alwaysTrustCheckBox, BorderLayout.NORTH);

            JTextArea certInfoPane = new JTextArea();
            certInfoPane.setEditable(false);
            certInfoPane.setText(cert.toString());
            final JScrollPane certScroll = new JScrollPane(certInfoPane);
            certScroll.setPreferredSize(new Dimension(200, 200));
            certPanel.add(certScroll, BorderLayout.CENTER);

            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    certScroll.getVerticalScrollBar().setValue(0);
                }
            });

            certButton.setText(GuiActivator.getResources()
                .getI18NString("service.gui.HIDE_CERT"));

            certPanel.revalidate();
            certPanel.repaint();
            pack();
            certOpened = true;
            setLocationRelativeTo(getParent());
        }

        /**
         * Action when cancel button is clicked.
         */
        private void actionCancel()
        {
            isTrusted = false;
            dispose();
        }

        /**
         * Action when continue is clicked.
         */
        private void actionContinue()
        {
            isTrusted = true;
            dispose();
        }
    }
}
