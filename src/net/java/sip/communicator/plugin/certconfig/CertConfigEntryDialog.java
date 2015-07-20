/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.certconfig;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;

import javax.security.auth.callback.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * Dialog window to add/edit client certificate configuration entries.
 *
 * @author Ingo Bauersachs
 */
public class CertConfigEntryDialog
    extends SIPCommDialog
    implements ActionListener, ItemListener, ChangeListener
{
    // ------------------------------------------------------------------------
    // Fields and services
    // ------------------------------------------------------------------------
    private static final long serialVersionUID = 8361336563239745007L;
    private static final Logger logger = Logger
        .getLogger(CertConfigEntryDialog.class);
    private ResourceManagementService R = CertConfigActivator.R;
    private CertificateService cs = CertConfigActivator.getCertService();
    private CertificateConfigEntry entry;
    private boolean success = false;

    // ------------------------------------------------------------------------
    // GUI members
    // ------------------------------------------------------------------------
    private JButton cmdOk;
    private JButton cmdCancel;
    private JButton cmdBrowse;
    private JTextField txtDisplayName;
    private JTextField txtKeyStore;
    private JComboBox cboKeyStoreTypes;
    private JCheckBox chkSavePassword;
    private JPasswordField txtKeyStorePassword;
    private JComboBox cboAlias;
    private JButton cmdShowCert;
    private KeyStore keyStore;

    // ------------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param e the <tt>CertificateConfigEntry</tt>
     */
    public CertConfigEntryDialog(CertificateConfigEntry e)
    {
        super(false);
        entry = e;
        initComponents();
        setPreferredSize(new Dimension(650, 270));

        try
        {
            if(entry.getKeyStore() != null)
            {
                txtKeyStorePassword.setText(entry.getKeyStorePassword());
                chkSavePassword.setSelected(entry.isSavePassword());
                cboKeyStoreTypes.setEnabled(true);
                cboKeyStoreTypes.setSelectedItem(entry.getKeyStoreType());
                if(keyStore == null)
                    keyStore = loadKeyStore();
                cboAlias.setEnabled(true);
                loadAliases();
                cboAlias.setSelectedItem(entry.getAlias());
            }
        }
        catch (KeyStoreException ex)
        {
            logger.error("Unable to load all data", ex);
            showGenericError("plugin.certconfig.KEYSTORE_EXCEPTION", ex);
        }
        catch (ProviderException ex)
        {
            logger.error("Unable to load all data", ex);
            showGenericError("plugin.certconfig.KEYSTORE_EXCEPTION", ex);
        }
    }

    private void initComponents()
    {
        setTitle(R.getI18NString("plugin.certconfig.EDIT_ENTRY"));
        setLayout(new BorderLayout());
        JPanel fields = new TransparentPanel();
        fields.setLayout(new GridBagLayout());

        JLabel lblDisplayName = new JLabel();
        lblDisplayName.setText(R.getI18NString("service.gui.DISPLAY_NAME"));
        txtDisplayName = new JTextField();
        txtDisplayName.setText(entry.getDisplayName());

        JLabel lblKeyStore = new JLabel();
        lblKeyStore.setText(R.getI18NString("plugin.certconfig.KEYSTORE"));
        txtKeyStore = new JTextField();
        txtKeyStore.setText(entry.getKeyStore());
        txtKeyStore.setEditable(false);

        cmdBrowse = new JButton();
        cmdBrowse.setText(R.getI18NString("service.gui.BROWSE"));
        cmdBrowse.addActionListener(this);

        JLabel lblKeyStorePassword = new JLabel();
        lblKeyStorePassword.setText(
            R.getI18NString("plugin.certconfig.KEYSTORE_PASSWORD"));
        txtKeyStorePassword = new JPasswordField();
        txtKeyStorePassword.setEditable(false);

        chkSavePassword = new SIPCommCheckBox();
        chkSavePassword.setText(
            R.getI18NString("service.gui.REMEMBER_PASSWORD"));
        chkSavePassword.addChangeListener(this);
        chkSavePassword.setEnabled(false);

        JLabel lblKeyStoreType = new JLabel();
        lblKeyStoreType.setText(
            R.getI18NString("plugin.certconfig.KEYSTORE_TYPE"));
        cboKeyStoreTypes =
            new JComboBox(cs.getSupportedKeyStoreTypes().toArray());
        cboKeyStoreTypes.addItemListener(this);
        cboKeyStoreTypes.setEnabled(false);

        JLabel lblAlias = new JLabel();
        lblAlias.setText(R.getI18NString("plugin.certconfig.ALIAS"));
        cboAlias = new JComboBox();
        cboAlias.addItemListener(this);
        cboAlias.setEnabled(false);

        cmdShowCert = new JButton();
        cmdShowCert.setText(R.getI18NString("service.gui.SHOW_CERT") + "...");
        cmdShowCert.addActionListener(this);
        cmdShowCert.setEnabled(false);

        cmdCancel = new JButton();
        cmdCancel.setText(R.getI18NString("service.gui.CANCEL"));
        cmdCancel.addActionListener(this);

        cmdOk = new JButton();
        cmdOk.setText(R.getI18NString("service.gui.OK"));
        cmdOk.addActionListener(this);
        cmdOk.setPreferredSize(cmdCancel.getPreferredSize());

        TransparentPanel buttons = new TransparentPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cmdOk);
        buttons.add(cmdCancel);

        GridBagConstraints first = new GridBagConstraints();
        first.gridx = 0;
        first.gridy = 0;
        first.weightx = 0;
        first.anchor = GridBagConstraints.LINE_START;
        first.gridwidth = 1;
        first.insets = new Insets(2,4,2,4);
        first.fill = GridBagConstraints.HORIZONTAL;

        GridBagConstraints second = new GridBagConstraints();
        second.gridx = 1;
        second.gridy = 0;
        second.weightx = 2;
        second.anchor = GridBagConstraints.LINE_START;
        second.gridwidth = 1; //GridBagConstraints.REMAINDER;
        second.insets = first.insets;
        second.fill = GridBagConstraints.HORIZONTAL;

        GridBagConstraints third = new GridBagConstraints();
        third.gridx = 2;
        third.gridy = 0;
        third.weightx = 1;
        third.anchor = GridBagConstraints.LINE_END;
        third.gridwidth = 1;
        third.insets = first.insets;
        third.fill = GridBagConstraints.HORIZONTAL;

        fields.add(lblDisplayName, first);
        fields.add(txtDisplayName, second);

        first.gridy = second.gridy = ++third.gridy;
        fields.add(lblKeyStore, first);
        fields.add(txtKeyStore, second);
        fields.add(cmdBrowse, third);

        first.gridy = second.gridy = ++third.gridy;
        fields.add(lblKeyStoreType, first);
        fields.add(cboKeyStoreTypes, second);

        first.gridy = second.gridy = ++third.gridy;
        fields.add(lblKeyStorePassword, first);
        fields.add(txtKeyStorePassword, second);

        first.gridy = second.gridy = ++third.gridy;
        fields.add(chkSavePassword, second);

        first.gridy = second.gridy = ++third.gridy;
        fields.add(lblAlias, first);
        fields.add(cboAlias, second);
        fields.add(cmdShowCert, third);

        add(fields, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------------
    // Event handling
    // ------------------------------------------------------------------------
    @Override
    protected void close(boolean escaped)
    {
        cmdCancel.doClick();
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == cmdOk)
        {
            if(cboAlias.getSelectedItem() == null
                || StringUtils.isNullOrEmpty(txtDisplayName.getText())
                || StringUtils.isNullOrEmpty(txtKeyStore.getText()))
            {
                JOptionPane.showMessageDialog(this,
                    R.getI18NString("plugin.certconfig.INCOMPLETE"),
                    R.getI18NString("service.gui.ERROR"),
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            entry.setAlias(cboAlias.getSelectedItem().toString());
            entry.setDisplayName(txtDisplayName.getText());
            entry.setSavePassword(chkSavePassword.isSelected());
            entry.setKeyStorePassword(
                new String(txtKeyStorePassword.getPassword()));
            entry.setKeyStoreType(
                (KeyStoreType) cboKeyStoreTypes.getSelectedItem());
            entry.setKeyStore(txtKeyStore.getText());
            success = true;
            dispose();
        }
        if(e.getSource() == cmdCancel)
        {
            dispose();
        }
        if(e.getSource() == cmdBrowse)
        {
            browseKeyStore();
        }
        if(e.getSource() == cmdShowCert)
        {
            showSelectedCertificate();
        }
    }

    private void showSelectedCertificate()
    {
        try
        {
            @SuppressWarnings("serial")
            SIPCommDialog dlg = new SIPCommDialog(this, false)
            {
                private JButton cmdClose;
                {
                    setTitle(cboAlias.getSelectedItem().toString());
                    setLayout(new BorderLayout());
                    final JScrollPane certScroll =
                        new JScrollPane(new X509CertificatePanel(
                            (X509Certificate) keyStore.getCertificate(cboAlias
                                .getSelectedItem().toString())));
                    certScroll.setPreferredSize(new Dimension(600, 300));
                    certScroll.getVerticalScrollBar().setValue(0);
                    add(certScroll, BorderLayout.CENTER);

                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            certScroll.getVerticalScrollBar().setValue(0);
                        }
                    });

                    cmdClose = new JButton();
                    cmdClose.setText(R.getI18NString("service.gui.CLOSE"));
                    cmdClose.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            dispose();
                        }
                    });

                    TransparentPanel buttons =
                        new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
                    buttons.add(cmdClose);
                    add(buttons, BorderLayout.SOUTH);

                    setLocationRelativeTo(cmdShowCert);
                }

                @Override
                protected void close(boolean escaped)
                {
                    cmdClose.doClick();
                }
            };
            dlg.setModal(true);
            dlg.setVisible(true);
        }
        catch (KeyStoreException e1)
        {
            logger.error("Unable to show the selected certificate", e1);
            showGenericError("plugin.certconfig.SHOW_CERT_EXCEPTION", e1);
        }
    }

    /**
     * Opens a FileChoserDialog to let the user pick a keystore and tries to
     * auto-detect the keystore type using the file extension
     */
    private void browseKeyStore()
    {
        SipCommFileChooser dlg =
            GenericFileDialog.create(null,
                R.getI18NString("plugin.certconfig.BROWSE_KEYSTORE"),
                SipCommFileChooser.LOAD_FILE_OPERATION);
        dlg.setSelectionMode(SipCommFileChooser.FILES_ONLY);
        dlg.addFilter(new SipCommFileFilter()
        {
            @Override
            public String getDescription()
            {
                return R
                    .getI18NString("plugin.certconfig.FILE_TYPE_DESCRIPTION");
            }

            @Override
            public boolean accept(File f)
            {
                for(KeyStoreType kt : cs.getSupportedKeyStoreTypes())
                    for(String ext : kt.getFileExtensions())
                        if(f.getName().endsWith(ext))
                            return true;

                return false;
            }
        });
        File f = dlg.getFileFromDialog();
        if(f != null)
        {
            cboKeyStoreTypes.setEnabled(true);
            cboKeyStoreTypes.setSelectedItem(null);
            cboAlias.setEnabled(true);

            txtKeyStore.setText(f.getAbsolutePath());
            for(KeyStoreType kt: cs.getSupportedKeyStoreTypes())
                for(String ext : kt.getFileExtensions())
                    if(f.getName().endsWith(ext))
                        cboKeyStoreTypes.setSelectedItem(kt);
        }
    }

    /**
     * Open the keystore selected by the user. If the type is set as PKCS#11,
     * the file is loaded as a provider. If the store is protected by a
     * password, the user is being asked by an authentication dialog.
     *
     * @return The loaded keystore
     * @throws KeyStoreException when something goes wrong
     */
    private KeyStore loadKeyStore() throws KeyStoreException
    {
        final File f = new File(txtKeyStore.getText());
        final KeyStoreType kt =
            (KeyStoreType) cboKeyStoreTypes.getSelectedItem();
        if("PKCS11".equals(kt.getName()))
        {
            String config =
                "name=" + f.getName() + "\nlibrary=" + f.getAbsoluteFile();
            try
            {
                Class<?> pkcs11c =
                    Class.forName("sun.security.pkcs11.SunPKCS11");
                Constructor<?> c = pkcs11c.getConstructor(InputStream.class);
                Provider p =
                    (Provider) c.newInstance(new ByteArrayInputStream(config
                        .getBytes()));
                Security.insertProviderAt(p, 0);
            }
            catch (Exception e)
            {
                logger.error("Tried to access the PKCS11 provider on an "
                    + "unsupported platform or the load failed", e);
            }
        }
        KeyStore.Builder ksBuilder = KeyStore.Builder.newInstance(
            kt.getName(),
            null,
            f,
            new KeyStore.CallbackHandlerProtection(new CallbackHandler()
            {
                public void handle(Callback[] callbacks)
                    throws IOException,
                    UnsupportedCallbackException
                {
                    for(Callback cb : callbacks)
                    {
                        if(!(cb instanceof PasswordCallback))
                            throw new UnsupportedCallbackException(cb);
                        PasswordCallback pwcb = (PasswordCallback)cb;
                        if(
                            (
                                txtKeyStorePassword.getPassword() != null
                                && txtKeyStorePassword.getPassword().length>0
                            )
                            || chkSavePassword.isSelected())
                        {
                            pwcb.setPassword(txtKeyStorePassword.getPassword());
                            return;
                        }
                        AuthenticationWindow aw = new AuthenticationWindow(
                            CertConfigEntryDialog.this,
                            f.getName(),
                            null,
                            kt.getName(),
                            false,
                            null
                        );
                        aw.setAllowSavePassword(!"PKCS11".equals(kt.getName()));
                        aw.setVisible(true);
                        if(!aw.isCanceled())
                        {
                                pwcb.setPassword(aw.getPassword());
                                if (!"PKCS11".equals(kt.getName())
                                    && aw.isRememberPassword())
                                {
                                    txtKeyStorePassword.setText(new String(aw
                                        .getPassword()));
                                }
                                chkSavePassword.setSelected(aw
                                    .isRememberPassword());
                        }
                        else
                            throw new IOException("User cancel");
                    }
                }
            }));
        return ksBuilder.getKeyStore();
    }

    /**
     * Load the certificate entry aliases from the chosen keystore.
     */
    private void loadAliases()
    {
        String currentDisplayName = txtDisplayName.getText();
        String currentAlias =
            cboAlias.getSelectedItem() == null ? null : cboAlias
                .getSelectedItem().toString();
        try
        {
            cboAlias.removeAllItems();
            Enumeration<String> e = keyStore.aliases();
            while(e.hasMoreElements())
            {
                cboAlias.addItem(e.nextElement());
            }
            // if the display name is empty or identical to the alias, set it
            // to the alias of the newly selected cert
            if(
                (
                    StringUtils.isNullOrEmpty(currentDisplayName)
                    || (
                        currentDisplayName != null
                        && currentDisplayName.equals(currentAlias)
                        )
                )
                && cboAlias.getSelectedItem() != null)
            {
                txtDisplayName.setText(cboAlias.getSelectedItem().toString());
            }

        }
        catch (KeyStoreException e)
        {
            cboAlias.removeAllItems();
            logger.error("Unable to obtain aliases from keystore", e);
            showGenericError("plugin.certconfig.ALIAS_LOAD_EXCEPTION", e);
        }
    }

    private void showGenericError(String msg, Throwable e)
    {
        JOptionPane.showMessageDialog(
            this,
            R.getI18NString(msg, new String[]{e.getMessage()}),
            R.getI18NString("service.gui.ERROR"),
            JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Show this dialog.
     *
     * @return true if OK has been pressed, false otherwise
     */
    public boolean showDialog()
    {
        setModal(true);
        setVisible(true);
        setVisible(false);
        return success;
    }

    public void itemStateChanged(ItemEvent e)
    {
        if(e.getStateChange() != ItemEvent.SELECTED)
            return;
        if(e.getSource() == cboKeyStoreTypes)
        {
            KeyStoreType kt = (KeyStoreType)cboKeyStoreTypes.getSelectedItem();
            if(kt == null)
                return;
            try
            {
                if(!"PKCS11".equals(kt.getName()))
                    chkSavePassword.setEnabled(true);
                txtKeyStorePassword.setEditable(kt.hasKeyStorePassword()
                    && chkSavePassword.isSelected());

                keyStore = loadKeyStore();
                loadAliases();
            }
            catch (KeyStoreException ex)
            {
                cboAlias.removeAllItems();
                showGenericError("plugin.certconfig.INVALID_KEYSTORE_TYPE", ex);
            }
        }
        if(e.getSource() == cboAlias)
        {
            cmdShowCert.setEnabled(cboAlias.getSelectedItem() != null);
        }
    }

    public void stateChanged(ChangeEvent e)
    {
        if(e.getSource() == chkSavePassword)
        {
            txtKeyStorePassword.setEditable(
                chkSavePassword.isSelected()
                && ((KeyStoreType) cboKeyStoreTypes.getSelectedItem())
                    .hasKeyStorePassword()
            );
        }
    }
}
