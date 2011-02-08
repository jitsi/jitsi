/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.certificate;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.*;
import java.security.interfaces.*;
import java.text.*;
import java.util.*;

import javax.naming.*;
import javax.swing.*;
import javax.naming.ldap.*;
import javax.net.ssl.*;
import javax.security.auth.x500.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Asks the user for permission for the
 * certificates which are for some reason not valid and not globally trusted.
 *
 * @author Damian Minkov
 * @author Yana Stamcheva
 */
public class CertificateVerificationServiceImpl
    implements CertificateVerificationService
{
    /**
     * The property for the configuration value to store the
     * KeyStore file location.
     */
    private static final String KEYSTORE_FILE_PROP =
        "net.java.sip.communicator.impl.protocol.sip.net.KEYSTORE";

    /**
     * The default name of the keystore file.
     */
    private static final String KEY_STORE_FILE_NAME = "jssecacerts";

    /**
     * The key store holding stored certificate during previous sessions.
     */
    private KeyStore keyStore;

    /**
     * This are the certificates which are temporally allowed
     * only for this session.
     */
    private ArrayList<X509Certificate> temporalyAllowed =
            new ArrayList<X509Certificate>();

    /**
     * The default password used for the keystore.
     */
    private char[] defaultPassword = new char[0];

    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(CertificateVerificationServiceImpl.class);

    /**
     * Return the File object for the keystore we will use. It can be
     * a full path existing keystore set from user. If it is set from user
     * but not existing we ignore it and return the default one.
     * If it is not set return just the default one.
     *
     * @return the file which will be used by KeyStore.
     * @throws Exception exception on creating file.
     */
    private File getKeyStoreLocation()
        throws Exception
    {
        String keyStoreFile = CertificateVerificationActivator.
            getConfigurationService().getString(KEYSTORE_FILE_PROP);

        if(keyStoreFile == null || keyStoreFile.length() == 0)
            return CertificateVerificationActivator.getFileAccessService()
                    .getPrivatePersistentFile(KEY_STORE_FILE_NAME);

        File f = new File(keyStoreFile);

        if(f.exists())
            return f;
        else
        {
            // Hum a keystore file is set but is not existing
            // lets remove the wrong config and return the default file.
            // An old version used to store the whole path to the file
            // and if the user changes location of its home dir
            // it breaks things.
            CertificateVerificationActivator.getConfigurationService()
                .removeProperty(KEYSTORE_FILE_PROP);

            return CertificateVerificationActivator.getFileAccessService()
                    .getPrivatePersistentFile(KEY_STORE_FILE_NAME);
        }
    }

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
    public int verify(
        final X509Certificate[] chain, final String toHost, final int toPort)
    {
        final VerifyCertificateDialog dialog = new VerifyCertificateDialog(
                        chain, toHost, toPort);
        try
        {
            // show the dialog in the swing thread and wait for the user
            // choice
            SwingUtilities.invokeAndWait(new Runnable() {
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
        {
            try
            {
                KeyStore kStore = getKeyStore();

                synchronized(kStore)
                {
                    for (X509Certificate c : chain)
                        kStore.setCertificateEntry(
                            String.valueOf(System.currentTimeMillis()), c);

                    kStore.store(
                        new FileOutputStream(getKeyStoreLocation()),
                        defaultPassword);
                }
            } catch (Throwable e)
            {
                logger.error("Error saving keystore.", e);
            }

            return TRUST_ALWAYS;
        }
        else
        {
            for (X509Certificate c : chain)
                temporalyAllowed.add(c);

            return TRUST_THIS_SESSION_ONLY;
        }
    }

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
        throws GeneralSecurityException
    {
        TrustManagerFactory tmFactory =
            TrustManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        tmFactory.init((KeyStore)null);

        return new HostTrustManager(
                    (X509TrustManager)tmFactory.getTrustManagers()[0],
                    toHost, toPort);
    }

    /**
     * Returns SSLContext instance initialized with the custom trust manager,
     * which will try verify the certificate and if verification fails
     * will query the user for acceptance.
     *
     * @param   toHost the host we are connecting.
     * @param   toPort the port used when connecting.
     * @return SSL context object
     *
     * @throws IOException if the SSLContext could not be initialized
     */
    public SSLContext getSSLContext(String toHost, int toPort)
        throws IOException
    {
        try
        {
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLS");
            String algorithm = KeyManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmFactory =
                TrustManagerFactory.getInstance(algorithm);
            KeyManagerFactory kmFactory =
                KeyManagerFactory.getInstance(algorithm);
            SecureRandom secureRandom   = new SecureRandom();
            secureRandom.nextInt();

            // init the default truststore
            tmFactory.init((KeyStore)null);
            kmFactory.init(null, null);

            sslContext.init(kmFactory.getKeyManagers(),
                new TrustManager[]{new HostTrustManager(
                    (X509TrustManager)tmFactory.getTrustManagers()[0],
                    toHost, toPort)}
                , secureRandom);

            return sslContext;
        } catch (Exception e)
        {
            throw new IOException("Cannot init SSLContext: " + e.getMessage());
        }
    }

    /**
     * Obtain and if null initialize the keystore.
     * @return the key store with certificates saved in previous sessions.
     */
    private KeyStore getKeyStore()
    {
        if(keyStore == null)
        {
            try
            {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                synchronized(keyStore)
                {
                    keyStore.load(null, defaultPassword);

                    File keyStoreFile = getKeyStoreLocation();

                    if(!keyStoreFile.exists())
                    {
                        // just store an empty keystore
                        // so the file to be created
                        keyStore.store(
                            new FileOutputStream(keyStoreFile),
                            defaultPassword);
                    }
                    else
                    {
                        keyStore.load(new FileInputStream(keyStoreFile), null);
                    }
                }

            } catch (Exception e)
            {
                logger.error("Cannot init keystore file.", e);
            }
        }

        return keyStore;
    }

    /**
     * The dialog that is shown to user.
     */
    private static class VerifyCertificateDialog
        extends SIPCommDialog
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * Date formatter.
         */
        private static SimpleDateFormat dateFormatter =
            new SimpleDateFormat("MM/dd/yyyy");

        /**
         * Used for converting bytes to HEX.
         */
        private static final String HEXES = "0123456789ABCDEF";

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
            CertificateVerificationActivator.getResources()
                .getI18NString("service.gui.ALWAYS_TRUST"),
            false);

        /**
         * Whether the user trusts this certificate.
         */
        boolean isTrusted = false;

        /**
         * Creates the dialog.
         *
         * @param certs the certificates list
         * @param host the host
         * @param port the port
         */
        public VerifyCertificateDialog( Certificate[] certs,
                                        String host,
                                        int port)
        {
            super(false);

            setTitle(CertificateVerificationActivator.getResources()
                .getI18NString("service.gui.CERT_DIALOG_TITLE"));
            setModal(true);

            // for now shows only the first certificate from the chain
            this.cert = certs[0];
            this.host = host;
            this.port = port;

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
                CertificateVerificationActivator.getResources().getImage(
                    "impl.media.security.zrtp.CONF_ICON"));
            imgLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            northPanel.add(imgLabel, BorderLayout.WEST);

            String descriptionTxt = CertificateVerificationActivator
                .getResources().getI18NString(
                    "service.gui.CERT_DIALOG_DESCRIPTION_TXT",
                    new String[]{
                        CertificateVerificationActivator.getResources()
                        .getSettingsString("service.gui.APPLICATION_NAME"),
                        host,
                        String.valueOf(port)});

            StyledHTMLEditorPane descriptionPane = new StyledHTMLEditorPane();
            descriptionPane.setOpaque(false);
            descriptionPane.setEditable(false);
            descriptionPane.setContentType("text/html");
            descriptionPane.setText(descriptionTxt);
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
            certButton.setText(CertificateVerificationActivator
                .getResources().getI18NString("service.gui.SHOW_CERT"));
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
                CertificateVerificationActivator.getResources()
                    .getI18NString("service.gui.CANCEL"));

            cancelButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e)
                {
                    actionCancel();
                }
            });
            JButton continueButton = new JButton(
                CertificateVerificationActivator.getResources()
                    .getI18NString("service.gui.CONTINUE"));

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
                certButton.setText(
                    CertificateVerificationActivator.getResources()
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

                certButton.setText(
                    CertificateVerificationActivator.getResources()
                    .getI18NString("service.gui.HIDE_CERT"));

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
        private static Component getX509DisplayComponent(
            X509Certificate certificate)
        {
            Insets valueInsets = new Insets(2,10,0,0);
            Insets titleInsets = new Insets(10,5,0,0);

            ResourceManagementService resources
                = CertificateVerificationActivator.getResources();

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
                resources.getI18NString("service.gui.CERT_INFO_ISSUED_TO")),
                constraints);

            constraints.gridy = currentRow++;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_CN")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(getCertificateValue(subject.getName(), "CN")),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_O")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(getCertificateValue(subject.getName(), "O")),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_C")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(getCertificateValue(subject.getName(), "C")),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_ST")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(getCertificateValue(subject.getName(), "ST")),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_L")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(getCertificateValue(subject.getName(), "L")),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            constraints.insets = titleInsets;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_ISSUED_BY")),
                constraints);
            constraints.insets = valueInsets;

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_C")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(getCertificateValue(issuer.getName(), "C")),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_O")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(getCertificateValue(issuer.getName(), "O")),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_OU")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(getCertificateValue(issuer.getName(), "OU")),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            constraints.insets = titleInsets;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_VALIDITY")),
                constraints);
            constraints.insets = valueInsets;

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_ISSUED_ON")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(dateFormatter.format(certificate.getNotBefore())),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_EXPIRES_ON")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(dateFormatter.format(certificate.getNotAfter())),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            constraints.insets = titleInsets;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_FINGERPRINTS")),
                constraints);
            constraints.insets = valueInsets;

            try
            {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(certificate.getEncoded());
                String sha1String = getHex(md.digest());

                md = MessageDigest.getInstance("MD5");
                md.update(certificate.getEncoded());
                String md5String = getHex(md.digest());

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
                logger.warn("Error in certificate, cannot show fingerprints", e);
            }

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            constraints.insets = titleInsets;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_CERT_DETAILS")),
                constraints);
            constraints.insets = valueInsets;

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_SER_NUM")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(certificate.getSerialNumber().toString()),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_VER")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(String.valueOf(certificate.getVersion())),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_SIGN_ALG")),
                constraints);
            constraints.gridx = 1;
            certDisplayPanel.add(
                new JLabel(String.valueOf(certificate.getSigAlgName())),
                constraints);

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            constraints.insets = titleInsets;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_PUB_KEY_INFO")),
                constraints);
            constraints.insets = valueInsets;

            constraints.gridy = currentRow++;
            constraints.gridx = 0;
            certDisplayPanel.add(new JLabel(
                resources.getI18NString("service.gui.CERT_INFO_ALG")),
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
                    resources.getI18NString("service.gui.CERT_INFO_PUB_KEY")),
                    constraints);

                JTextArea pubkeyArea = new JTextArea(
                    resources.getI18NString(
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
                    resources.getI18NString("service.gui.CERT_INFO_EXP")),
                    constraints);
                constraints.gridx = 1;
                certDisplayPanel.add(
                    new JLabel(key.getPublicExponent().toString()),
                    constraints);

                constraints.gridy = currentRow++;
                constraints.gridx = 0;
                certDisplayPanel.add(new JLabel(
                    resources.getI18NString("service.gui.CERT_INFO_KEY_SIZE")),
                    constraints);
                constraints.gridx = 1;
                certDisplayPanel.add(
                    new JLabel(resources.getI18NString(
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
                resources.getI18NString("service.gui.CERT_INFO_SIGN")),
                constraints);

            JTextArea signArea = new JTextArea(
                resources.getI18NString(
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
         * Extract values from certificate DNs(Distinguished Names).
         * @param rfc2253String the certificate string.
         * @param attributeName the DN attribute name to search for.
         * @return empty string or the found value.
         */
        private static String getCertificateValue(
            String rfc2253String, String attributeName)
        {
            try
            {
                LdapName issuerDN = new LdapName(rfc2253String);
                java.util.List<Rdn> l = issuerDN.getRdns();
                for (int i = 0; i < l.size(); i++)
                {
                    Rdn rdn = l.get(i);
                    if (rdn.getType().equals(attributeName))
                    {
                        return (String) rdn.getValue();
                    }
                }
            }
            catch (InvalidNameException ex)
            {
                // do nothing
                logger.warn("Wrong DN:" + rfc2253String, ex);
            }

            return "";
        }

        /**
         * Converts the byte array to hex string.
         * @param raw the data.
         * @return the hex string.
         */
        public static String getHex( byte [] raw )
        {
            if ( raw == null )
            {
                return null;
            }
            final StringBuilder hex = new StringBuilder( 2 * raw.length );
            for ( final byte b : raw )
            {
                hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)));
            }
            return hex.toString();
        }
    }

    /**
     * The trust manager which asks the client whether to trust particular
     * certificate which is not globally trusted.
     */
    private class HostTrustManager
        implements X509TrustManager
    {
        /**
         * The address we connect to.
         */
        String address;

        /**
         * The port we connect to.
         */
        int port;

        /**
         * The default trust manager.
         */
        private final X509TrustManager tm;

        /**
         * Creates the custom trust manager.
         * @param tm the default trust manager.
         * @param address the address we are connecting to.
         * @param port the port.
         */
        HostTrustManager(X509TrustManager tm, String address, int port)
        {
            this.tm = tm;
            this.port = port;
            this.address = address;
        }

        /**
         * Not used.
         * @return UnsupportedOperationException
         */
        public X509Certificate[] getAcceptedIssuers()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Not used.
         * @param chain the cert chain.
         * @param authType authentication type like: RSA.
         * @throws CertificateException if something went wrong with
         * certificate verification
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Check whether a certificate is trusted, if not as user whether he
         * trust it.
         * @param chain the certificate chain.
         * @param authType authentication type like: RSA.
         * @throws CertificateException not trusted.
         */
        public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException
        {
            // check and default configurations for property
            // if missing default is null - false
            String defaultAlwaysTrustMode =
                CertificateVerificationActivator.getResources()
                    .getSettingsString(CertificateVerificationService
                        .ALWAYS_TRUST_MODE_ENABLED_PROP_NAME);

            if(CertificateVerificationActivator.getConfigurationService()
                .getBoolean(CertificateVerificationService
                    .ALWAYS_TRUST_MODE_ENABLED_PROP_NAME,
                    Boolean.parseBoolean(defaultAlwaysTrustMode)))
            {
                return;
            }

            try
            {
                tm.checkServerTrusted(chain, authType);
                // everything is fine certificate is globally trusted
            }
            catch (CertificateException certificateException)
            {
                KeyStore kStore = getKeyStore();

                if(kStore == null)
                    throw certificateException;

                try
                {
                    for (int i = 0; i < chain.length; i++)
                    {
                        X509Certificate c = chain[i];

                        // check for temporaly allowed certs
                        if(temporalyAllowed.contains(c))
                        {
                            return;
                        }

                        // now check for permanent allow of certs
                        String alias = kStore.getCertificateAlias(c);
                        if(alias != null)
                        {
                            return;
                        }
                    }

                    if(verify(chain, address, port)
                        == CertificateVerificationService.DO_NOT_TRUST)
                    {
                        throw certificateException;
                    }
                } catch (Throwable e)
                {
                    // something happend
                    logger.error("Error trying to " +
                        "show certificate to user", e);

                    throw certificateException;
                }
            }
        }
    }
}
