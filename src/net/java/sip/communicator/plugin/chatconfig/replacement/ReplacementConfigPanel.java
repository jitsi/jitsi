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
package net.java.sip.communicator.plugin.chatconfig.replacement;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.replacement.smiley.*;
import net.java.sip.communicator.plugin.chatconfig.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.replacement.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added in the chat configuration
 * window.
 *
 * @author Purvesh Sahoo
 */
public class ReplacementConfigPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Checkbox to enable/disable smiley replacement.
     */
    private JCheckBox enableSmiley;

    /**
     * Checkbox to enable/disable replacements other than smileys.
     */
    private JRadioButton enableReplacement;

    /**
     * Checkbox to enable/disable proposal messages for image/video replacement.
     */
    private JRadioButton enableReplacementProposal;

    /**
     * Checkbox to disable image/video replacement.
     */
    private JRadioButton disableReplacement;

    /**
     * Jtable to list all the available replacement sources.
     */
    private JTable table;

    /**
     * Create an instance of Replacement Config
     */
    public ReplacementConfigPanel()
    {
        super(new BorderLayout());

        add(ChatConfigActivator
            .createConfigSectionComponent(ChatConfigActivator.getResources()
                .getI18NString("plugin.chatconfig.replacement.TITLE")),
            BorderLayout.WEST);
        add(createMainPanel());

        initValues();
    }

    /**
     * Init the main panel.
     *
     * @return the created component
     */
    private Component createMainPanel()
    {
        JPanel mainPanel = new TransparentPanel();

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        ResourceManagementService R = ChatConfigActivator.getResources();

        enableSmiley =
            new SIPCommCheckBox(R.getI18NString(
                    "plugin.chatconfig.replacement.ENABLE_SMILEY_STATUS"));

        mainPanel.add(enableSmiley);

        enableSmiley.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                saveData();
            }
        });

        mainPanel.add(Box.createVerticalStrut(10));

        JPanel replacementPanel = new TransparentPanel();
        replacementPanel.setLayout(new BoxLayout(replacementPanel, BoxLayout.Y_AXIS));
        replacementPanel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(R.getI18NString(
                    "plugin.chatconfig.replacement.REPLACEMENT_TITLE")),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)));

        enableReplacement =
            new SIPCommRadioButton(R.getI18NString(
                    "plugin.chatconfig.replacement.ENABLE_REPLACEMENT_STATUS"));

        replacementPanel.add(enableReplacement);

        enableReplacement.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                saveData();
            }
        });

        enableReplacementProposal =
            new SIPCommRadioButton(R.getI18NString(
                    "plugin.chatconfig.replacement.ENABLE_REPLACEMENT_PROPOSAL"
                ));

        replacementPanel.add(enableReplacementProposal);

        enableReplacementProposal.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent arg0)
            {
                saveData();
            }
        });
        disableReplacement = new SIPCommRadioButton(R.getI18NString(
            "plugin.chatconfig.replacement.DISABLE_REPLACEMENT"));

        replacementPanel.add(disableReplacement);

        disableReplacement.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent arg0)
            {
                saveData();
            }
        });

        ButtonGroup replacementGroup = new ButtonGroup();
        replacementGroup.add(enableReplacement);
        replacementGroup.add(enableReplacementProposal);
        replacementGroup.add(disableReplacement);

        // the JTable to list all the available sources
        table = new JTable();
        table.setShowGrid(false);
        table.setTableHeader(null);

        table.setOpaque(true);
        table.setBackground(Color.white);

        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setOpaque(false);
        tablePane.setPreferredSize(
            new Dimension(replacementPanel.getWidth(), 150));
        tablePane.setAlignmentX(LEFT_ALIGNMENT);

        JPanel container = new TransparentPanel(new BorderLayout());
        container.setPreferredSize(
            new Dimension(replacementPanel.getWidth(), 200));
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JLabel label =
            new JLabel(R.getI18NString(
                "plugin.chatconfig.replacement.REPLACEMENT_SOURCES"));
        label.setDisplayedMnemonic(R.getI18nMnemonic(
                "plugin.chatconfig.replacement.REPLACEMENT_SOURCES"));
        label.setLabelFor(table);

        container.add(label);
        container.add(Box.createRigidArea(new Dimension(0, 5)));
        container.add(tablePane, BorderLayout.EAST);

        /*
         * list of the source names. Removing 'Smiley' as it shouldn't show up in
         * the table.
         */
        Set<String> keys = ChatConfigActivator.getReplacementSources().keySet();
        ArrayList<String> sourceList = new ArrayList<String>(keys);
        sourceList.remove("SMILEY");

        Collections.sort(sourceList);

        table.setModel(new ReplacementConfigurationTableModel(sourceList));

        table.getSelectionModel().addListSelectionListener(
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    if (e.getValueIsAdjusting())
                        return;
                    if (table.getSelectedRow() != -1)
                    {
                        boolean isEnabled =
                            (Boolean) table.getValueAt(table.getSelectedRow(),
                                0);

                        if (isEnabled)
                        {
                            enableReplacement.setSelected(true);
                        }
                    }
                }
            });

        TableColumnModel tableColumnModel = table.getColumnModel();
        TableColumn tableColumn = tableColumnModel.getColumn(0);
        tableColumn.setMaxWidth(tableColumn.getMinWidth());
        table.setDefaultRenderer(table.getColumnClass(1),
            new FixedTableCellRenderer());

        replacementPanel.add(Box.createVerticalStrut(10));
        replacementPanel.add(container);

        mainPanel.add(replacementPanel);

        return mainPanel;
    }

    /**
     * Init the values of the widgets
     */
    private void initValues()
    {
        ConfigurationService configService =
            ChatConfigActivator.getConfigurationService();

        this.enableSmiley.setSelected(
            configService.getBoolean(
                ReplacementProperty.getPropertyName(
                    ReplacementServiceSmileyImpl.SMILEY_SOURCE),
                true));

        this.enableReplacement.setSelected(
            configService.getBoolean(
                ReplacementProperty.REPLACEMENT_ENABLE, true));

        this.enableReplacementProposal.setSelected(
            configService.getBoolean(
                ReplacementProperty.REPLACEMENT_PROPOSAL, true));

        this.disableReplacement.setSelected(
            !this.enableReplacement.isSelected()
                && !this.enableReplacementProposal.isSelected());

        this.table.setEnabled(enableReplacement.isSelected());
    }

    /**
     * Save data in the configuration file
     */
    private void saveData()
    {
        ConfigurationService configService =
            ChatConfigActivator.getConfigurationService();

        configService.setProperty(ReplacementProperty
            .getPropertyName(ReplacementServiceSmileyImpl.SMILEY_SOURCE),
            Boolean.toString(enableSmiley.isSelected()));

        configService.setProperty(ReplacementProperty.REPLACEMENT_ENABLE,
            Boolean.toString(enableReplacement.isSelected()));

        configService.setProperty(
            "plugin.chatconfig.replacement.proposal.enable",
            Boolean.toString(enableReplacementProposal.isSelected()));

        table.getSelectionModel().clearSelection();
        table.setEnabled(enableReplacement.isSelected()
            || enableReplacementProposal.isSelected());
    }

    /**
     * Renderer for text column in the table.
     */
    private static class FixedTableCellRenderer
        extends DefaultTableCellRenderer
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean selected, boolean focused, int row, int column)
        {
            setEnabled(table == null || table.isEnabled());

            super.getTableCellRendererComponent(table, value, selected, focused,
                row, column);

            return this;
        }
    }
}
