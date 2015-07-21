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
package net.java.sip.communicator.plugin.dnsconfig;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.dns.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.dns.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Configuration of the DNSSEC validating resolver.
 *
 * @author Ingo Bauersachs
 */
public class DnssecPanel
    extends TransparentPanel
    implements ActionListener, FocusListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final static Logger logger = Logger.getLogger(DnssecPanel.class);

    //UI Controls
    private JComboBox cboDefault;
    private JCheckBox chkEnabled;
    private JCheckBox chkAbsolute;
    private JTable tblDomains;
    private JTextField txtNameservers;

    //service references
    private ResourceManagementService R;
    private ConfigurationService config;
    private TableModel data = new DnssecTableModel();
    private final ParallelDnsPanel parallelDnsPanel;

    /**
     * Create a new instance of this class.
     * @param parallelDnsPanel the panel configuring the parallel resolver
     */
    public DnssecPanel(ParallelDnsPanel parallelDnsPanel)
    {
        this.parallelDnsPanel = parallelDnsPanel;
        initServices();
        initComponents();
        loadData();
        updateState();
        chkAbsolute.addActionListener(this);
        chkEnabled.addActionListener(this);
        cboDefault.addActionListener(this);
        txtNameservers.addFocusListener(this);
    }

    /**
     * Loads all service references
     */
    private void initServices()
    {
        BundleContext bc = DnsConfigActivator.bundleContext;
        R = ServiceUtils.getService(bc, ResourceManagementService.class);
        config = ServiceUtils.getService(bc, ConfigurationService.class);
    }

    /**
     * Create the UI components
     */
    private void initComponents()
    {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 0));

        JPanel pnlCommon = new TransparentPanel(new GridBagLayout());
        pnlCommon.setAlignmentX(LEFT_ALIGNMENT);
        GridBagConstraints cl = new GridBagConstraints();
        GridBagConstraints cr = new GridBagConstraints();
        cl.gridy = cr.gridy = 0;
        cl.anchor = cr.anchor = GridBagConstraints.LINE_START;
        cl.gridx = 0;
        cl.fill = GridBagConstraints.HORIZONTAL;
        cl.weightx = 0;
        cl.insets = new Insets(0, 0, 0, 10);
        cr.gridx = 1;
        cr.fill = GridBagConstraints.HORIZONTAL;
        cr.gridwidth = GridBagConstraints.REMAINDER;
        cr.weightx = 1;
        add(pnlCommon, BorderLayout.NORTH);


        //always use absolute names
        chkAbsolute = new SIPCommCheckBox(
            R.getI18NString("plugin.dnsconfig.dnssec.chkAbsolute"));
        cl.gridwidth = 2;
        pnlCommon.add(chkAbsolute, cl);
        cl.gridwidth = 1;

        //dnssec enable/disable
        cl.gridy = ++cr.gridy;
        chkEnabled = new SIPCommCheckBox(
            R.getI18NString("plugin.dnsconfig.dnssec.chkEnabled"));
        cl.gridwidth = 2;
        pnlCommon.add(chkEnabled, cl);
        cl.gridwidth = 1;

        cl.gridy = ++cr.gridy;
        JLabel lblRestart = new JLabel(
            R.getI18NString("plugin.dnsconfig.dnssec.RESTART_WARNING",
            new String[]{
                R.getSettingsString("service.gui.APPLICATION_NAME")
            }));
        lblRestart.setBorder(BorderFactory.createEmptyBorder(0, 22, 10, 0));
        cl.gridwidth = GridBagConstraints.REMAINDER;
        pnlCommon.add(lblRestart, cl);
        cl.gridwidth = 1;

        //custom nameservers
        cl.gridy = ++cr.gridy;
        JLabel lblNameserver = new JLabel(
            R.getI18NString("plugin.dnsconfig.dnssec.lblNameservers"));
        lblNameserver.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
        pnlCommon.add(lblNameserver, cl);

        txtNameservers = new JTextField();
        pnlCommon.add(txtNameservers, cr);
        cl.gridy = ++cr.gridy;
        JLabel lblNsHint = new JLabel(
            R.getI18NString("plugin.dnsconfig.dnssec.lblNameserversHint"));
        lblNsHint.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        pnlCommon.add(lblNsHint, cr);

        //default dnssec handling
        cl.gridy = ++cr.gridy;
        JLabel lblDefault = new JLabel(
            R.getI18NString("plugin.dnsconfig.dnssec.lblDefault"));
        lblDefault.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
        pnlCommon.add(lblDefault, cl);
        cboDefault = new JComboBox(SecureResolveMode.values());
        cboDefault.setRenderer(getResolveModeRenderer());
        pnlCommon.add(cboDefault, cr);

        //domain list table
        tblDomains = new JTable(data);
        tblDomains.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblDomains.setRowHeight(20);
        tblDomains.getColumnModel().getColumn(1).setCellRenderer(
            new DefaultTableCellRenderer()
            {
                /**
                 * Serial version UID.
                 */
                private static final long serialVersionUID = 0L;

                @Override
                protected void setValue(Object value)
                {
                    if (value instanceof SecureResolveMode)
                        setText(R.getI18NString(
                            "net.java.sip.communicator.util.dns."
                            + SecureResolveMode.class.getSimpleName()
                            + "."
                            + ((SecureResolveMode) value).name()));
                    else
                        super.setValue(value);
                }
            }
        );
        JComboBox cboTblModeEditor = new JComboBox(SecureResolveMode.values());
        cboTblModeEditor.setRenderer(getResolveModeRenderer());
        tblDomains.getColumnModel().getColumn(1).setCellEditor(
            new DefaultCellEditor(cboTblModeEditor));
        JScrollPane pnlScroller = new JScrollPane(tblDomains);
        pnlScroller.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 0));
        pnlScroller.setOpaque(false);
        add(pnlScroller, BorderLayout.CENTER);
    }

    /**
     * Reads the configured properties or their defaults into the UI controls.
     */
    private void loadData()
    {
        cboDefault.setSelectedItem(Enum.valueOf(SecureResolveMode.class,
            config.getString(
                ConfigurableDnssecResolver.PNAME_DNSSEC_VALIDATION_MODE,
                SecureResolveMode.WarnIfBogus.name())
            )
        );
        chkEnabled.setSelected(config.getBoolean(
            CustomResolver.PNAME_DNSSEC_RESOLVER_ENABLED,
            CustomResolver.PDEFAULT_DNSSEC_RESOLVER_ENABLED
        ));
        chkAbsolute.setSelected(config.getBoolean(
            NetworkUtils.PNAME_DNS_ALWAYS_ABSOLUTE,
            NetworkUtils.PDEFAULT_DNS_ALWAYS_ABSOLUTE
        ));
        txtNameservers.setText(
            config.getString(DnsUtilActivator.PNAME_DNSSEC_NAMESERVERS));
    }

    /**
     * Action has occurred in the config form.
     * @param e the action event
     */
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == cboDefault)
        {
            if(cboDefault.getSelectedItem() == null)
                return;

            SecureResolveMode oldMode = Enum.valueOf(SecureResolveMode.class,
                config.getString(
                    ConfigurableDnssecResolver.PNAME_DNSSEC_VALIDATION_MODE,
                    SecureResolveMode.WarnIfBogus.name())
                );
            config.setProperty(
                ConfigurableDnssecResolver.PNAME_DNSSEC_VALIDATION_MODE,
                ((SecureResolveMode)cboDefault.getSelectedItem()).name());

            //update all values that had the default to the new default
            for(int i = 0; i < tblDomains.getModel().getRowCount(); i++)
            {
                SecureResolveMode m = (SecureResolveMode)data.getValueAt(i, 1);
                if(m == oldMode)
                    data.setValueAt(cboDefault.getSelectedItem(), i, 1);
            }
            tblDomains.repaint();
            return;
        }
        if(e.getSource() == chkEnabled)
        {
            File f;
            try
            {
                f = DnsConfigActivator.getFileAccessService()
                    .getPrivatePersistentFile(".usednsjava",
                        FileCategory.PROFILE);
                if(chkEnabled.isSelected())
                {
                    if(!f.createNewFile() && !f.exists())
                        chkEnabled.setSelected(UnboundApi.isAvailable());
                }
                else
                {
                    if(!f.delete() && f.exists())
                        chkEnabled.setSelected(true);
                }
                config.setProperty(
                    CustomResolver.PNAME_DNSSEC_RESOLVER_ENABLED,
                    chkEnabled.isSelected());
            }
            catch (Exception ex)
            {
                logger.error("failed to enable DNSSEC", ex);
                ErrorDialog ed = new ErrorDialog(
                    null,
                    R.getI18NString("plugin.dnsconfig.dnssec.ENABLE_FAILED"),
                    R.getI18NString("plugin.dnsconfig.dnssec.ENABLE_FAILED_MSG"),
                    ex);
                ed.showDialog();
            }
            updateState();
        }
        if(e.getSource() == chkAbsolute)
        {
            config.setProperty(
                NetworkUtils.PNAME_DNS_ALWAYS_ABSOLUTE,
                chkAbsolute.isSelected());
            updateState();
        }
    }

    /**
     * Updates the form behavior when the resolver is enabled or disabled.
     */
    private void updateState()
    {
        cboDefault.setEnabled(chkEnabled.isSelected());
        txtNameservers.setEnabled(chkEnabled.isSelected());
        tblDomains.setEnabled(chkEnabled.isSelected());
        parallelDnsPanel.updateDnssecState();
    }

    /**
     * Creates a ComboBox renderer for the resolve mode column. The non-edit
     * text is based on the selected value of the row for which the renderer is
     * created.
     *
     * @return ComboBox render for the SecureResolveMode enum.
     */
    private BasicComboBoxRenderer getResolveModeRenderer()
    {
        return new BasicComboBoxRenderer()
        {
            /**
             * Serial version UID.
             */
            private static final long serialVersionUID = 0L;

            @Override
            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus)
            {
                Component c =
                    super.getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
                setText(R.getI18NString("net.java.sip.communicator.util.dns."
                    + SecureResolveMode.class.getSimpleName()
                    + "."
                    + value));
                return c;
            }
        };
    }

    /**
     * A text field has lost the focus in the config form.
     * @param e the action event
     */
    public void focusLost(FocusEvent e)
    {
        if(e.getSource() == txtNameservers)
        {
            config.setProperty(
                DnsUtilActivator.PNAME_DNSSEC_NAMESERVERS,
                txtNameservers.getText());
            DnsUtilActivator.reloadDnsResolverConfig();
        }
    }

    public void focusGained(FocusEvent e)
    {}
}
