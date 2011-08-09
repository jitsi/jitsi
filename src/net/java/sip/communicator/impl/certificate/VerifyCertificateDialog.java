/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.certificate;

import java.awt.*;
import java.awt.event.*;
import java.security.cert.*;
import java.security.interfaces.*;
import java.text.*;
import java.util.Formatter;

import javax.naming.*;
import javax.naming.ldap.*;
import javax.security.auth.x500.*;
import javax.swing.*;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Dialog that is shown to the user when a certificate verification failed.
 */
class VerifyCertificateDialog
    extends SIPCommDialog
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private ResourceManagementService R = CertificateVerificationActivator
        .getResources();

    /**
     * Date formatter.
     */
    private DateFormat dateFormatter = DateFormat
        .getDateInstance(DateFormat.MEDIUM);

    /**
     * The maximum width that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_WIDTH = 600;

    /**
     * The maximum height that we allow message dialogs to have.
     */
    private static final int MAX_MSG_PANE_HEIGHT = 800;

    /**
     * The certificate to show.
     */
    Certificate cert;

    /**
     * A text that describes why the verification failed.
     */
    String message;

    /**
     * The certificate panel.
     */
    TransparentPanel certPanel;

    /**
     * This dialog content pane.
     */
    TransparentPanel contentPane;

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
    SIPCommCheckBox alwaysTrustCheckBox = new SIPCommCheckBox(
        R.getI18NString("service.gui.ALWAYS_TRUST"),
        false);

    /**
     * Whether the user trusts this certificate.
     */
    boolean isTrusted = false;

    /**
     * Creates the dialog.
     *
     * @param certs the certificates list
     * @param title The title of the dialog; when null the resource
     * <tt>service.gui.CERT_DIALOG_TITLE</tt> is loaded.
     * @param message A text that describes why the verification failed.
     */
    public VerifyCertificateDialog( Certificate[] certs,
                                    String title, String message)
    {
        super(false);

        setTitle(title != null ? title : 
            R.getI18NString("service.gui.CERT_DIALOG_TITLE"));
        setModal(true);

        // for now shows only the first certificate from the chain
        this.cert = certs[0];
        this.message = message;

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        init();

        setLocationRelativeTo(getParent());
    }

    /**
     * Inits the dialog initial display.
     */
    private void init()
    {
        this.getContentPane().setLayout(new BorderLayout());

        contentPane =
            new TransparentPanel(new BorderLayout(5, 5));

        TransparentPanel northPanel =
            new TransparentPanel(new BorderLayout(5, 5));
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));

        JLabel imgLabel = new JLabel(
            R.getImage("service.gui.icons.WARNING_ICON"));
        imgLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        northPanel.add(imgLabel, BorderLayout.WEST);

        StyledHTMLEditorPane descriptionPane = new StyledHTMLEditorPane();
        descriptionPane.setOpaque(false);
        descriptionPane.setEditable(false);
        descriptionPane.setContentType("text/html");
        descriptionPane.setText(message);
        descriptionPane.setSize(
                    new Dimension(MAX_MSG_PANE_WIDTH, MAX_MSG_PANE_HEIGHT));
        int height = descriptionPane.getPreferredSize().height;
        descriptionPane.setPreferredSize(
                    new Dimension(MAX_MSG_PANE_WIDTH, height));

        northPanel.add(descriptionPane, BorderLayout.CENTER);
        contentPane.add(northPanel, BorderLayout.NORTH);

        certPanel = new TransparentPanel();
        contentPane.add(certPanel, BorderLayout.CENTER);

        TransparentPanel southPanel =
            new TransparentPanel(new BorderLayout());
        contentPane.add(southPanel, BorderLayout.SOUTH);

        certButton = new JButton();
        certButton.setText(R.getI18NString("service.gui.SHOW_CERT"));
        certButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                actionShowCertificate();
            }
        });
        TransparentPanel firstButonPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
        firstButonPanel.add(certButton);
        southPanel.add(firstButonPanel, BorderLayout.WEST);

        TransparentPanel secondButonPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton(
            R.getI18NString("service.gui.CANCEL"));

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                actionCancel();
            }
        });
        JButton continueButton = new JButton(
            R.getI18NString("service.gui.CONTINUE_ANYWAY"));

        continueButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                actionContinue();
            }
        });
        secondButonPanel.add(continueButton);
        secondButonPanel.add(cancelButton);
        southPanel.add(secondButonPanel, BorderLayout.EAST);

        this.getContentPane().add(contentPane, BorderLayout.CENTER);

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
            certButton.setText(R.getI18NString("service.gui.SHOW_CERT"));

            certPanel.revalidate();
            certPanel.repaint();
            pack();
            certOpened = false;
            setLocationRelativeTo(getParent());
            return;
        }

        certPanel.setLayout(new BorderLayout());
        certPanel.add(alwaysTrustCheckBox, BorderLayout.NORTH);

        Component certInfoPane = null;
        if(cert instanceof X509Certificate)
        {
            certInfoPane = getX509DisplayComponent((X509Certificate)cert);
        }
        else
        {
            JTextArea textArea = new JTextArea();
            textArea.setOpaque(false);
            textArea.setEditable(false);
            textArea.setText(cert.toString());
            certInfoPane = textArea;
        }

        final JScrollPane certScroll = new JScrollPane(certInfoPane);
        certScroll.setPreferredSize(new Dimension(300, 300));
        certPanel.add(certScroll, BorderLayout.CENTER);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                certScroll.getVerticalScrollBar().setValue(0);
            }
        });

        certButton.setText(R.getI18NString("service.gui.HIDE_CERT"));

        certPanel.revalidate();
        certPanel.repaint();
        // restore default values for prefered size,
        // as we have resized its components let it calculate
        // that size
        setPreferredSize(null);
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

    /**
     * Called when dialog closed or escape pressed.
     * @param isEscaped is escape button pressed.
     */
    protected void close(boolean isEscaped)
    {
        actionCancel();
    }

    /**
     * Returns the display component for X509 certificate.
     *
     * @param certificate the certificate to show
     * @return the created component
     */
    private Component getX509DisplayComponent(
        X509Certificate certificate)
    {
        Insets valueInsets = new Insets(2,10,0,0);
        Insets titleInsets = new Insets(10,5,0,0);

        TransparentPanel certDisplayPanel
            = new TransparentPanel(new GridBagLayout());

        int currentRow = 0;

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(2,5,0,0);
        constraints.gridx = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.gridy = currentRow++;

        X500Principal issuer = certificate.getIssuerX500Principal();
        X500Principal subject = certificate.getSubjectX500Principal();

        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_ISSUED_TO")),
            constraints);

        // subject
        constraints.insets = valueInsets;
        try
        {
            for(Rdn name : new LdapName(subject.getName()).getRdns())
            {
                constraints.gridy = currentRow++;
                constraints.gridx = 0;
                String lbl =
                    R.getI18NString("service.gui.CERT_INFO_" + name.getType());
                if (lbl
                    .equals("!service.gui.CERT_INFO_" + name.getType() + "!"))
                    lbl = name.getType();
                certDisplayPanel.add(new JLabel(lbl), constraints);
                constraints.gridx = 1;
                certDisplayPanel.add(
                    new JLabel(
                        name.getValue() instanceof byte[] ?
                            getHex((byte[])name.getValue()) + " ("
                                + new String((byte[]) name.getValue()) + ")"
                                : name.getValue().toString()),
                    constraints);
            }
        }
        catch (InvalidNameException e1)
        {
            constraints.gridy = currentRow++;
            certDisplayPanel.add(new JLabel(
                R.getI18NString("service.gui.CERT_INFO_CN")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(subject.getName()),
                constraints);
        }

        // issuer
        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        constraints.insets = titleInsets;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_ISSUED_BY")),
            constraints);
        constraints.insets = valueInsets;
        try
        {
            for(Rdn name : new LdapName(issuer.getName()).getRdns())
            {
                constraints.gridy = currentRow++;
                constraints.gridx = 0;
                constraints.gridx = 0;
                String lbl =
                    R.getI18NString("service.gui.CERT_INFO_" + name.getType());
                if (lbl
                    .equals("!service.gui.CERT_INFO_" + name.getType() + "!"))
                    lbl = name.getType();
                certDisplayPanel.add(new JLabel(lbl), constraints);
                constraints.gridx = 1;
                certDisplayPanel.add(
                    new JLabel(
                        name.getValue() instanceof byte[] ?
                            getHex((byte[])name.getValue()) + " ("
                                + new String((byte[]) name.getValue()) + ")"
                                : name.getValue().toString()),
                    constraints);
            }
        }
        catch (InvalidNameException e1)
        {
            constraints.gridy = currentRow++;
            certDisplayPanel.add(new JLabel(
                R.getI18NString("service.gui.CERT_INFO_CN")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(issuer.getName()),
                constraints);
        }

        // validity
        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        constraints.insets = titleInsets;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_VALIDITY")),
            constraints);
        constraints.insets = valueInsets;

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_ISSUED_ON")),
            constraints);
        constraints.gridx = 1;
        certDisplayPanel.add(
            new JLabel(dateFormatter.format(certificate.getNotBefore())),
            constraints);

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_EXPIRES_ON")),
            constraints);
        constraints.gridx = 1;
        certDisplayPanel.add(
            new JLabel(dateFormatter.format(certificate.getNotAfter())),
            constraints);

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        constraints.insets = titleInsets;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_FINGERPRINTS")),
            constraints);
        constraints.insets = valueInsets;

        try
        {
            String sha1String = CertificateServiceImpl.getThumbprint(certificate, "SHA1");
            String md5String = CertificateServiceImpl.getThumbprint(certificate, "MD5");

            JTextArea sha1Area = new JTextArea(sha1String);
            sha1Area.setLineWrap(false);
            sha1Area.setOpaque(false);
            sha1Area.setWrapStyleWord(true);
            sha1Area.setEditable(false);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel("SHA1:"),
                constraints);

            constraints.gridx = 1;
            certDisplayPanel.add(
                sha1Area,
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel("MD5:"),
                constraints);

            JTextArea md5Area = new JTextArea(md5String);
            md5Area.setLineWrap(false);
            md5Area.setOpaque(false);
            md5Area.setWrapStyleWord(true);
            md5Area.setEditable(false);

            constraints.gridx = 1;
            certDisplayPanel.add(
                md5Area,
                constraints);
        }
        catch (Exception e)
        {
            // do nothing as we cannot show this value
        }

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        constraints.insets = titleInsets;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_CERT_DETAILS")),
            constraints);
        constraints.insets = valueInsets;

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_SER_NUM")),
            constraints);
        constraints.gridx = 1;
        certDisplayPanel.add(
            new JLabel(certificate.getSerialNumber().toString()),
            constraints);

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_VER")),
            constraints);
        constraints.gridx = 1;
        certDisplayPanel.add(
            new JLabel(String.valueOf(certificate.getVersion())),
            constraints);

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_SIGN_ALG")),
            constraints);
        constraints.gridx = 1;
        certDisplayPanel.add(
            new JLabel(String.valueOf(certificate.getSigAlgName())),
            constraints);

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        constraints.insets = titleInsets;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_PUB_KEY_INFO")),
            constraints);
        constraints.insets = valueInsets;

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_ALG")),
            constraints);
        constraints.gridx = 1;
        certDisplayPanel.add(
            new JLabel(certificate.getPublicKey().getAlgorithm()),
            constraints);

        if(certificate.getPublicKey().getAlgorithm().equals("RSA"))
        {
            RSAPublicKey key = (RSAPublicKey)certificate.getPublicKey();

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                R.getI18NString("service.gui.CERT_INFO_PUB_KEY")),
                constraints);

            JTextArea pubkeyArea = new JTextArea(
                R.getI18NString(
                    "service.gui.CERT_INFO_KEY_BYTES_PRINT",
                    new String[]{
                        String.valueOf(key.getModulus().toByteArray().length - 1),
                        key.getModulus().toString(16)
                    }));
            pubkeyArea.setLineWrap(false);
            pubkeyArea.setOpaque(false);
            pubkeyArea.setWrapStyleWord(true);
            pubkeyArea.setEditable(false);

            constraints.gridx = 1;
            certDisplayPanel.add(
                pubkeyArea,
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                R.getI18NString("service.gui.CERT_INFO_EXP")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(key.getPublicExponent().toString()),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                R.getI18NString("service.gui.CERT_INFO_KEY_SIZE")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(R.getI18NString(
                    "service.gui.CERT_INFO_KEY_BITS_PRINT",
                    new String[]{
                        String.valueOf(key.getModulus().bitLength())})),
                constraints);
        }
        else if(certificate.getPublicKey().getAlgorithm().equals("DSA"))
        {
            DSAPublicKey key =
                (DSAPublicKey)certificate.getPublicKey();

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel("Y:"), constraints);

            JTextArea yArea = new JTextArea(key.getY().toString(16));
            yArea.setLineWrap(false);
            yArea.setOpaque(false);
            yArea.setWrapStyleWord(true);
            yArea.setEditable(false);

            constraints.gridx = 1;
            certDisplayPanel.add(
                yArea,
                constraints);
        }

        constraints.gridy = currentRow++;
        constraints.gridx = 0;
        certDisplayPanel.add(new JLabel(
            R.getI18NString("service.gui.CERT_INFO_SIGN")),
            constraints);

        JTextArea signArea = new JTextArea(
            R.getI18NString(
                    "service.gui.CERT_INFO_KEY_BYTES_PRINT",
                    new String[]{
                        String.valueOf(certificate.getSignature().length),
                        getHex(certificate.getSignature())
                    }));
        signArea.setLineWrap(false);
        signArea.setOpaque(false);
        signArea.setWrapStyleWord(true);
        signArea.setEditable(false);

        constraints.gridx = 1;
        certDisplayPanel.add(
            signArea,
            constraints);

        return certDisplayPanel;
    }

    /**
     * Converts the byte array to hex string.
     * @param raw the data.
     * @return the hex string.
     */
    public String getHex( byte [] raw )
    {
        if (raw == null)
            return null;

        StringBuilder hex = new StringBuilder(2 * raw.length);
        Formatter f = new Formatter(hex);
        for (byte b : raw)
        {
            f.format("%02x", b);
        }
        return hex.toString();
    }
}
