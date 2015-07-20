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
import java.security.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.gui.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * Advanced configuration form to define client TLS certificate templates.
 *
 * @author Ingo Bauersachs
 */
public class CertConfigPanel
    extends TransparentPanel
    implements ConfigurationForm, ActionListener, ListSelectionListener
{
    // ------------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------------
    private static final long serialVersionUID = 2324122652952574574L;
    private ResourceManagementService R;
    private CertConfigTableModel model;

    // ------------------------------------------------------------------------
    // GUI members
    // ------------------------------------------------------------------------
    private JButton cmdAdd;
    private JButton cmdRemove;
    private JButton cmdEdit;
    private JTable tblCertList;
    private JRadioButton rdoUseWindows;
    private JRadioButton rdoUseJava;
    private SIPCommCheckBox chkEnableRevocationCheck;
    private SIPCommCheckBox chkEnableOcsp;

    // ------------------------------------------------------------------------
    // initialization
    // ------------------------------------------------------------------------
    /**
     * Creates a new instance of this class.
     */
    public CertConfigPanel()
    {
        R = CertConfigActivator.R;
        model = new CertConfigTableModel();
        initComponents();
        valueChanged(null);
    }

    private void initComponents()
    {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // trusted root CA source selection
        if (OSUtils.IS_WINDOWS)
        {
            JPanel pnlCertConfig = new TransparentPanel(new GridLayout(2, 1));
            pnlCertConfig.setBorder(BorderFactory.createTitledBorder(
                R.getI18NString("plugin.certconfig.TRUSTSTORE_CONFIG")));
            add(pnlCertConfig);

            ButtonGroup grpTrustStore = new ButtonGroup();

            rdoUseJava = new SIPCommRadioButton();
            rdoUseJava.setText(
                R.getI18NString("plugin.certconfig.JAVA_TRUSTSTORE"));
            rdoUseJava.addActionListener(this);
            grpTrustStore.add(rdoUseJava);
            pnlCertConfig.add(rdoUseJava);

            rdoUseWindows = new SIPCommRadioButton();
            rdoUseWindows.setText(
                R.getI18NString("plugin.certconfig.WINDOWS_TRUSTSTORE"));
            rdoUseWindows.addActionListener(this);
            grpTrustStore.add(rdoUseWindows);
            pnlCertConfig.add(rdoUseWindows);

            if ("Windows-ROOT".equals(CertConfigActivator.getConfigService()
                .getProperty(CertificateService.PNAME_TRUSTSTORE_TYPE)))
            {
                rdoUseWindows.setSelected(true);
            }
            else
            {
                rdoUseJava.setSelected(true);
            }
        }

        // revocation options
        JPanel pnlRevocation = new TransparentPanel(new GridLayout(2, 1));
        pnlRevocation.setBorder(BorderFactory.createTitledBorder(
            R.getI18NString("plugin.certconfig.REVOCATION_TITLE")));
        add(pnlRevocation);

        chkEnableRevocationCheck = new SIPCommCheckBox(
            R.getI18NString("plugin.certconfig.REVOCATION_CHECK_ENABLED"));
        chkEnableRevocationCheck.addActionListener(this);
        chkEnableRevocationCheck.setSelected(
            "true".equals(
                System.getProperty("com.sun.net.ssl.checkRevocation")));
        pnlRevocation.add(chkEnableRevocationCheck);

        chkEnableOcsp = new SIPCommCheckBox(
            R.getI18NString("plugin.certconfig.REVOCATION_OCSP_ENABLED"));
        chkEnableOcsp.addActionListener(this);
        chkEnableOcsp.setSelected(
            "true".equals(Security.getProperty("ocsp.enable")));
        chkEnableOcsp.setEnabled(chkEnableRevocationCheck.isSelected());
        pnlRevocation.add(chkEnableOcsp);

        // Client certificate authentication list
        JPanel pnlCertList = new TransparentPanel(new BorderLayout());
        pnlCertList.setBorder(BorderFactory.createTitledBorder(
            R.getI18NString("plugin.certconfig.CERT_LIST_TITLE")));
        add(pnlCertList);

        JLabel lblNote = new JLabel();
        lblNote.setText(
            R.getI18NString("plugin.certconfig.CERT_LIST_DESCRIPTION"));
        lblNote.setBorder(new EmptyBorder(7, 7, 7, 7));
        pnlCertList.add(lblNote, BorderLayout.NORTH);

        tblCertList = new JTable();
        tblCertList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCertList.getSelectionModel().addListSelectionListener(this);
        tblCertList.setModel(model);
        pnlCertList.add(new JScrollPane(tblCertList), BorderLayout.CENTER);

        TransparentPanel buttons = new TransparentPanel();
        buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        pnlCertList.add(buttons, BorderLayout.SOUTH);

        cmdAdd = new JButton();
        cmdAdd.setText(R.getI18NString("service.gui.ADD"));
        cmdAdd.addActionListener(this);
        buttons.add(cmdAdd);

        cmdRemove = new JButton();
        cmdRemove.setText(R.getI18NString("service.gui.REMOVE"));
        cmdRemove.addActionListener(this);
        buttons.add(cmdRemove);

        cmdEdit = new JButton();
        cmdEdit.setText(R.getI18NString("service.gui.EDIT"));
        cmdEdit.addActionListener(this);
        buttons.add(cmdEdit);
    }

    // ------------------------------------------------------------------------
    // event handling
    // ------------------------------------------------------------------------
    public void valueChanged(ListSelectionEvent e)
    {
        int row = tblCertList.getSelectedRow();
        cmdRemove.setEnabled(row > -1);
        cmdEdit.setEnabled(row > -1);
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == cmdAdd)
        {
            CertificateConfigEntry newEntry = new CertificateConfigEntry();
            CertConfigEntryDialog dlg = new CertConfigEntryDialog(newEntry);
            if (dlg.showDialog())
                CertConfigActivator.getCertService()
                    .setClientAuthCertificateConfig(newEntry);
        }
        if (e.getSource() == cmdRemove)
        {
            CertConfigActivator.getCertService()
                .removeClientAuthCertificateConfig(
                    model.getItem(tblCertList.getSelectedRow()).getId());
        }
        if (e.getSource() == cmdEdit)
        {
            CertificateConfigEntry entry =
                model.getItem(tblCertList.getSelectedRow());
            CertConfigEntryDialog dlg = new CertConfigEntryDialog(entry);
            if (dlg.showDialog())
                CertConfigActivator.getCertService()
                    .setClientAuthCertificateConfig(entry);
        }
        if (e.getSource() == rdoUseJava)
        {
            CertConfigActivator.getConfigService().setProperty(
                CertificateService.PNAME_TRUSTSTORE_TYPE,
                "meta:default");
            CertConfigActivator.getConfigService().removeProperty(
                CertificateService.PNAME_TRUSTSTORE_FILE);
            CertConfigActivator.getCredService().removePassword(
                CertificateService.PNAME_TRUSTSTORE_PASSWORD);
        }
        if (e.getSource() == rdoUseWindows)
        {
            CertConfigActivator.getConfigService().setProperty(
                CertificateService.PNAME_TRUSTSTORE_TYPE, "Windows-ROOT");
            CertConfigActivator.getConfigService().removeProperty(
                CertificateService.PNAME_TRUSTSTORE_FILE);
            CertConfigActivator.getCredService().removePassword(
                CertificateService.PNAME_TRUSTSTORE_PASSWORD);
        }
        if (e.getSource() == chkEnableRevocationCheck)
        {
            CertConfigActivator.getConfigService().setProperty(
                CertificateService.PNAME_REVOCATION_CHECK_ENABLED,
                chkEnableRevocationCheck.isSelected());

            String enabled = new Boolean(
                chkEnableRevocationCheck.isSelected()).toString();
            System.setProperty("com.sun.security.enableCRLDP", enabled);
            System.setProperty("com.sun.net.ssl.checkRevocation", enabled);
            chkEnableOcsp.setEnabled(chkEnableRevocationCheck.isSelected());
        }
        if (e.getSource() == chkEnableOcsp)
        {
            CertConfigActivator.getConfigService().setProperty(
                CertificateService.PNAME_OCSP_ENABLED,
                chkEnableOcsp.isSelected());

            Security.setProperty("ocsp.enable",
                new Boolean(chkEnableOcsp.isSelected()).toString());
        }
    }

    // ------------------------------------------------------------------------
    // Configuration form members
    // ------------------------------------------------------------------------
    public String getTitle()
    {
        return CertConfigActivator.R.getI18NString("plugin.certconfig.TITLE");
    }

    public byte[] getIcon()
    {
        return null;
    }

    public Object getForm()
    {
        return this;
    }

    public int getIndex()
    {
        return -1;
    }

    public boolean isAdvanced()
    {
        return true;
    }

}
