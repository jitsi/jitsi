/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.chatconfig.replacement;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.impl.replacement.smiley.*;
import net.java.sip.communicator.plugin.chatconfig.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.swing.*;

import java.util.*;

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
     * Checkbox to enable/disable smiley replacement.
     */
    private JCheckBox enableSmiley;

    /**
     * Checkbox to enable/disable replacements other than smileys.
     */
    private JCheckBox enableReplacement;

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
        JPanel mainPanel = new TransparentPanel(new BorderLayout());

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        enableSmiley =
            new SIPCommCheckBox(ChatConfigActivator.getResources()
                .getI18NString(
                    "plugin.chatconfig.replacement.ENABLE_SMILEY_STATUS"));

        mainPanel.add(enableSmiley, BorderLayout.WEST);

        enableSmiley.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                saveData();
            }
        });

        mainPanel.add(Box.createVerticalStrut(10));

        enableReplacement =
            new SIPCommCheckBox(ChatConfigActivator.getResources()
                .getI18NString(
                    "plugin.chatconfig.replacement.ENABLE_REPLACEMENT_STATUS"));

        mainPanel.add(enableReplacement, BorderLayout.WEST);

        enableReplacement.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                saveData();
                table.revalidate();
                table.repaint();
            }
        });

        // the Jtable to list all the available sources
        table = new JTable();
        table.setShowGrid(false);
        table.setTableHeader(null);

        table.setOpaque(true);
        table.setBackground(Color.white);
  
        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setOpaque(false);
        tablePane.setPreferredSize(new Dimension(mainPanel.getWidth(), 150));
        tablePane.setAlignmentX(LEFT_ALIGNMENT);

        JPanel container = new TransparentPanel(new BorderLayout());
        container.setPreferredSize(new Dimension(mainPanel.getWidth(), 200));
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JLabel label =
            new JLabel(ChatConfigActivator.getResources().getI18NString(
                "plugin.chatconfig.replacement.REPLACEMENT_SOURCES"));
        label.setDisplayedMnemonic(ChatConfigActivator.getResources()
            .getI18nMnemonic(
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

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(container, BorderLayout.WEST);

        return mainPanel;
    }

    /**
     * Init the values of the widgets
     */
    private void initValues()
    {
        ConfigurationService configService =
            ChatConfigActivator.getConfigurationService();

        boolean e =
            configService.getBoolean(ReplacementProperty
                .getPropertyName(ReplacementServiceSmileyImpl.SMILEY_SOURCE),
                true);
        this.enableSmiley.setSelected(e);

        e =
            configService.getBoolean(ReplacementProperty.REPLACEMENT_ENABLE,
                true);
        this.enableReplacement.setSelected(e);

        this.table.setEnabled(e);
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

        boolean e = enableReplacement.isSelected();
        table.getSelectionModel().clearSelection();
        table.setEnabled(e);
    }

    /**
     * Renderer for text column in the table.
     */
    private static class FixedTableCellRenderer
        extends DefaultTableCellRenderer
    {
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