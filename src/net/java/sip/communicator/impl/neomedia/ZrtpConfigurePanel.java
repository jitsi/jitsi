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
package net.java.sip.communicator.impl.neomedia;

import gnu.java.zrtp.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 * @author Werner Dittmann
 * @author Lubomir Marinov
 */
@SuppressWarnings("serial")
public class ZrtpConfigurePanel
    extends TransparentPanel
{
    /**
     * The width and height of the margin to be applied to UI containers created
     * by <tt>ZrtpConfigurePanel</tt>.
     */
    private static final int MARGIN = 5;

    private static final String TRUSTED_PROP
        = "net.java.sip.communicator.gnu.java.zrtp.trustedmitm";
    private static final String SASSIGN_PROP
        = "net.java.sip.communicator.gnu.java.zrtp.sassignature";

    private ZrtpConfigure active = new ZrtpConfigure();

    private ZrtpConfigure inActive = new ZrtpConfigure();

    PublicKeyControls pkc = new PublicKeyControls();
    HashControls hc = new HashControls();
    CipherControls cc = new CipherControls();
    SasControls sc = new SasControls();
    LengthControls lc = new LengthControls();

    /**
     * Creates an instance of <tt>ZrtpConfigurePanel</tt>.
     */
    public ZrtpConfigurePanel()
    {
        super(new BorderLayout());

        ResourceManagementService resources = NeomediaActivator.getResources();

        JPanel mainPanel = new TransparentPanel(new BorderLayout(0, 10));

        final JButton stdButton
            = new JButton(
                    resources.getI18NString(
                            "impl.media.security.zrtp.STANDARD"));
        stdButton.setOpaque(false);

        final JButton mandButton
            = new JButton(
                    resources.getI18NString(
                            "impl.media.security.zrtp.MANDATORY"));
        mandButton.setOpaque(false);

        final JButton saveButton
            = new JButton(resources.getI18NString("service.gui.SAVE"));
        saveButton.setOpaque(false);

        JPanel buttonBar = new TransparentPanel(new GridLayout(1, 7));
        buttonBar.add(stdButton);
        buttonBar.add(mandButton);
        buttonBar.add(Box.createHorizontalStrut(10));
        buttonBar.add(saveButton);

        ConfigurationService cfg = NeomediaActivator.getConfigurationService();
        boolean trusted = cfg.getBoolean(TRUSTED_PROP, false);
        boolean sasSign = cfg.getBoolean(SASSIGN_PROP, false);

        JPanel checkBar = new TransparentPanel(new GridLayout(1,2));
        final JCheckBox trustedMitM
            = new SIPCommCheckBox(
                    resources.getI18NString("impl.media.security.zrtp.TRUSTED"),
                    trusted);
        final JCheckBox sasSignature
            = new SIPCommCheckBox(
                    resources.getI18NString(
                            "impl.media.security.zrtp.SASSIGNATURE"),
                    sasSign);
        checkBar.add(trustedMitM);
        checkBar.add(sasSignature);
        mainPanel.add(checkBar, BorderLayout.NORTH);

        ActionListener buttonListener = new ActionListener()
        {
            public void actionPerformed(ActionEvent event)
            {
                Object source = event.getSource();
                if (source == stdButton)
                {
                    inActive.clear();
                    active.setStandardConfig();
                    pkc.setStandard();
                    hc.setStandard();
                    sc.setStandard();
                    cc.setStandard();
                    lc.setStandard();
                }
                else if (source == mandButton)
                {
                    inActive.clear();
                    active.setMandatoryOnly();
                    pkc.setStandard();
                    hc.setStandard();
                    sc.setStandard();
                    cc.setStandard();
                    lc.setStandard();
                }
                else if (source == saveButton)
                {
                    ConfigurationService cfg
                        = NeomediaActivator.getConfigurationService();

                    cfg.setProperty(
                            TRUSTED_PROP,
                            String.valueOf(active.isTrustedMitM()));
                    cfg.setProperty(
                            SASSIGN_PROP,
                            String.valueOf(active.isSasSignature()));
                    pkc.saveConfig();
                    hc.saveConfig();
                    sc.saveConfig();
                    cc.saveConfig();
                    lc.saveConfig();
                }
                else
                    return;
            }
        };
        stdButton.addActionListener(buttonListener);
        mandButton.addActionListener(buttonListener);
        saveButton.addActionListener(buttonListener);

        ItemListener itemListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Object source = e.getItemSelectable();

                if (source == trustedMitM) {
                     active.setTrustedMitM(trustedMitM.isSelected());
                } else if (source == sasSignature) {
                    active.setSasSignature(sasSignature.isSelected());
                }
            }
        };
        trustedMitM.addItemListener(itemListener);
        sasSignature.addItemListener(itemListener);

        JTabbedPane algorithmsPane = new SIPCommTabbedPane();

        algorithmsPane.addTab(
                resources.getI18NString("impl.media.security.zrtp.PUB_KEYS"),
                pkc);
        algorithmsPane.addTab(
                resources.getI18NString("impl.media.security.zrtp.HASHES"),
                hc);
        algorithmsPane.addTab(
                resources.getI18NString("impl.media.security.zrtp.SYM_CIPHERS"),
                cc);
        algorithmsPane.addTab(
                resources.getI18NString("impl.media.security.zrtp.SAS_TYPES"),
                sc);
        algorithmsPane.addTab(
                resources.getI18NString(
                        "impl.media.security.zrtp.SRTP_LENGTHS"),
                lc);

        algorithmsPane.setMinimumSize(new Dimension(400, 100));
        algorithmsPane.setPreferredSize(new Dimension(400, 200));
        mainPanel.add(algorithmsPane, BorderLayout.CENTER);

        mainPanel.add(buttonBar, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private <T extends Enum<T>>String getPropertyValue(T algo)
    {
        StringBuilder strb = new StringBuilder();

        for (T it : active.algos(algo)) {
            strb.append(it.name());
            strb.append(';');
        }
        return strb.toString();
    }

    private class PublicKeyControls extends TransparentPanel
    {
        private final ZrtpConfigureTableModel<ZrtpConstants.SupportedPubKeys>
            dataModel;

        PublicKeyControls()
        {
            String id = getPropertyID(ZrtpConstants.SupportedPubKeys.DH2K);

            String savedConf
                = NeomediaActivator.getConfigurationService().getString(id);
            if (savedConf == null)
                savedConf = "DH3K;MULT;";

            dataModel
                = new ZrtpConfigureTableModel<ZrtpConstants.SupportedPubKeys>(
                    ZrtpConstants.SupportedPubKeys.DH2K,
                    active,
                    inActive,
                    savedConf);
            createControls(this, dataModel);
        }

        void setStandard()
        {
            dataModel.setStandardConfig();
        }

        void saveConfig()
        {
            String value = getPropertyValue(ZrtpConstants.SupportedPubKeys.DH2K);
            String id = getPropertyID(ZrtpConstants.SupportedPubKeys.DH2K);

            NeomediaActivator.getConfigurationService()
                .setProperty(id, value);
        }
    }

    private class HashControls extends TransparentPanel
    {

        private final ZrtpConfigureTableModel<ZrtpConstants.SupportedHashes>
            dataModel;

        HashControls()
        {
            String id = getPropertyID(ZrtpConstants.SupportedHashes.S256);

            String savedConf
                = NeomediaActivator.getConfigurationService()
                    .getString(id);
            if (savedConf == null)
                savedConf = "S256";

            dataModel
                = new ZrtpConfigureTableModel<ZrtpConstants.SupportedHashes>(
                    ZrtpConstants.SupportedHashes.S256,
                    active,
                    inActive,
                    savedConf);
            createControls(this, dataModel);
        }

        void setStandard()
        {
            dataModel.setStandardConfig();
        }

        void saveConfig()
        {
            String value = getPropertyValue(ZrtpConstants.SupportedHashes.S256);
            String id = getPropertyID(ZrtpConstants.SupportedHashes.S256);
            NeomediaActivator.getConfigurationService()
                .setProperty(id, value);
        }
    }

    private class CipherControls
        extends TransparentPanel
    {
        private final ZrtpConfigureTableModel<ZrtpConstants.SupportedSymCiphers>
            dataModel;

        CipherControls()
        {
            String id = getPropertyID(ZrtpConstants.SupportedSymCiphers.AES1);
            String savedConf
                = NeomediaActivator.getConfigurationService().getString(id);
            if (savedConf == null)
                savedConf = "AES1";

            dataModel
                = new ZrtpConfigureTableModel<ZrtpConstants.SupportedSymCiphers>(
                    ZrtpConstants.SupportedSymCiphers.AES1,
                    active,
                    inActive,
                    savedConf);
            createControls(this, dataModel);
        }

        void setStandard()
        {
            dataModel.setStandardConfig();
        }

        void saveConfig()
        {
            String value
                = getPropertyValue(ZrtpConstants.SupportedSymCiphers.AES1);
            String id = getPropertyID(ZrtpConstants.SupportedSymCiphers.AES1);
            NeomediaActivator.getConfigurationService()
                .setProperty(id, value);
        }
    }

    private class SasControls extends TransparentPanel
    {
        private final ZrtpConfigureTableModel<ZrtpConstants.SupportedSASTypes>
            dataModel;

        SasControls()
        {
            String id = getPropertyID(ZrtpConstants.SupportedSASTypes.B32);
            String savedConf
                = NeomediaActivator.getConfigurationService()
                    .getString(id);
            if (savedConf == null)
                savedConf = "B32";

            dataModel
                = new ZrtpConfigureTableModel<ZrtpConstants.SupportedSASTypes>(
                    ZrtpConstants.SupportedSASTypes.B32,
                    active,
                    inActive,
                    savedConf);
            createControls(this, dataModel);
        }

        void setStandard()
        {
            dataModel.setStandardConfig();
        }

        void saveConfig()
        {
            String value = getPropertyValue(ZrtpConstants.SupportedSASTypes.B32);
            String id = getPropertyID(ZrtpConstants.SupportedSASTypes.B32);
            NeomediaActivator.getConfigurationService()
                .setProperty(id, value);
        }
    }

    private class LengthControls
        extends TransparentPanel
    {
        private final ZrtpConfigureTableModel<ZrtpConstants.SupportedAuthLengths>
            dataModel;

        LengthControls()
        {
            String id = getPropertyID(ZrtpConstants.SupportedAuthLengths.HS32);
            String savedConf
                = NeomediaActivator.getConfigurationService().getString(id);
            if (savedConf == null)
                savedConf = "HS32;HS80;";

            dataModel
                = new ZrtpConfigureTableModel<ZrtpConstants.SupportedAuthLengths>(
                    ZrtpConstants.SupportedAuthLengths.HS32,
                    active,
                    inActive,
                    savedConf);
            createControls(this, dataModel);
        }

        void setStandard()
        {
            dataModel.setStandardConfig();
        }

        void saveConfig()
        {
            String value
                = getPropertyValue(ZrtpConstants.SupportedAuthLengths.HS32);
            String id = getPropertyID(ZrtpConstants.SupportedAuthLengths.HS32);
            NeomediaActivator.getConfigurationService()
                .setProperty(id, value);
        }
    }

    private <T extends Enum<T>> void createControls(
            JPanel panel,
            ZrtpConfigureTableModel<T> model)
    {
        ResourceManagementService resources = NeomediaActivator.getResources();

        final JButton upButton
            = new JButton(resources.getI18NString("impl.media.configform.UP"));
        upButton.setOpaque(false);

        final JButton downButton
            = new JButton(
                    resources.getI18NString("impl.media.configform.DOWN"));
        downButton.setOpaque(false);

        Container buttonBar = new TransparentPanel(new GridLayout(0, 1));
        buttonBar.add(upButton);
        buttonBar.add(downButton);

        panel.setBorder(
                BorderFactory.createEmptyBorder(
                        MARGIN,
                        MARGIN,
                        MARGIN,
                        MARGIN));
        panel.setLayout(new GridBagLayout());

        final JTable table = new JTable(model.getRowCount(), 2);
        table.setShowGrid(false);
        table.setTableHeader(null);
        table.setModel(model);
        // table.setFillsViewportHeight(true); // Since 1.6 only - nicer view

        /*
         * The first column contains the check boxes which enable/disable their
         * associated encodings and it doesn't make sense to make it wider than
         * the check boxes.
         */
        TableColumnModel tableColumnModel = table.getColumnModel();
        TableColumn tableColumn = tableColumnModel.getColumn(0);
        tableColumn.setMaxWidth(tableColumn.getMinWidth() + 5);
        table.doLayout();

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 1;
        panel.add(new JScrollPane(table), constraints);

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.weightx = 0;
        constraints.weighty = 0;
        panel.add(buttonBar, constraints);

        ListSelectionListener tableSelectionListener
            = new ListSelectionListener() {
            @SuppressWarnings("unchecked")
            public void valueChanged(ListSelectionEvent event) {
                if (table.getSelectedRowCount() == 1) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow > -1) {
                        ZrtpConfigureTableModel<T> model
                            = (ZrtpConfigureTableModel<T>) table
                                .getModel();
                        upButton.setEnabled(selectedRow > 0
                                && model.checkEnableUp(selectedRow));
                        downButton.setEnabled(selectedRow < (table
                                .getRowCount() - 1)
                                && model.checkEnableDown(selectedRow));
                        return;
                    }
                }
                upButton.setEnabled(false);
                downButton.setEnabled(false);
            }
        };
        table.getSelectionModel().addListSelectionListener(
                tableSelectionListener);

        TableModelListener tableListener = new TableModelListener() {
            @SuppressWarnings("unchecked")
            public void tableChanged(TableModelEvent e) {
                if (table.getSelectedRowCount() == 1) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow > -1) {
                        ZrtpConfigureTableModel<T> model
                            = (ZrtpConfigureTableModel<T>) table
                                .getModel();
                        upButton.setEnabled(selectedRow > 0
                                && model.checkEnableUp(selectedRow));
                        downButton.setEnabled(selectedRow < (table
                                .getRowCount() - 1)
                                && model.checkEnableDown(selectedRow));
                        return;
                    }
                }
                upButton.setEnabled(false);
                downButton.setEnabled(false);
            }
        };
        table.getModel().addTableModelListener(tableListener);

        tableSelectionListener.valueChanged(null);

        ActionListener buttonListener = new ActionListener() {
            @SuppressWarnings("unchecked")
            public void actionPerformed(ActionEvent event) {
                Object source = event.getSource();
                boolean up;
                if (source == upButton)
                    up = true;
                else if (source == downButton)
                    up = false;
                else
                    return;

                int index = ((ZrtpConfigureTableModel<T>) table.getModel())
                        .move(table.getSelectedRow(), up, up);
                table.getSelectionModel().setSelectionInterval(index, index);
            }
        };
        upButton.addActionListener(buttonListener);
        downButton.addActionListener(buttonListener);
    }

    public static <T extends Enum<T>>String getPropertyID(T algo)
    {
        Class<T> clazz = algo.getDeclaringClass();
        return "net.java.sip.communicator." + clazz.getName().replace('$', '_');
    }
}
