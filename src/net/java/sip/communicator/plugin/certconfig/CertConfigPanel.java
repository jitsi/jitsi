/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.certconfig;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;

import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
        this.setLayout(new BorderLayout());

        //TODO change to OSUtils.IS_WINDOWS as soon as we ship with JRE 1.7
        if (OSUtils.IS_WINDOWS32
            || (OSUtils.IS_WINDOWS
                && System.getProperty("java.version").startsWith("1.7")))
        {
            JPanel pnlCertConfig = new TransparentPanel(new GridLayout(2, 1));
            pnlCertConfig.setBorder(BorderFactory.createTitledBorder(
                R.getI18NString("plugin.certconfig.TRUSTSTORE_CONFIG")));
            add(pnlCertConfig, BorderLayout.NORTH);

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

        JPanel pnlCertList = new TransparentPanel(new BorderLayout());
        pnlCertList.setBorder(BorderFactory.createTitledBorder(
            R.getI18NString("plugin.certconfig.CERT_LIST_TITLE")));
        add(pnlCertList, BorderLayout.CENTER);

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
            CertConfigActivator.getConfigService().removeProperty(
                CertificateService.PNAME_TRUSTSTORE_TYPE);
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
