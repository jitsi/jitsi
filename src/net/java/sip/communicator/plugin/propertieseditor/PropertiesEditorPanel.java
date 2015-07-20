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
package net.java.sip.communicator.plugin.propertieseditor;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 * @author Marin Dzhigarov
 * @author Pawel Domas
 */
public class PropertiesEditorPanel
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The buttons panel.
     */
    private final ButtonsPanel buttonsPanel;

    /**
     * The panel containing the props table and the buttons panel
     */
    private final JPanel centerPanel;

    /**
     * The props table.
     */
    private final JTable propsTable;

    /**
     * Creates an instance <tt>PropertiesEditorPanel</tt>.
     */
    public PropertiesEditorPanel()
    {
        super(new BorderLayout());

        /**
         * Instantiates the properties table and adds selection model and
         * listener and adds a row sorter to the table model
         */
        ResourceManagementService r
            = PropertiesEditorActivator.getResourceManagementService();
        String[] columnNames
            = new String[]
                    {
                        r.getI18NString("service.gui.NAME"),
                        r.getI18NString("service.gui.VALUE")
                    };

        propsTable
            = new JTable(new PropsTableModel(initTableModel(), columnNames));
        propsTable.setRowSorter(
                new TableRowSorter<TableModel>(propsTable.getModel()));
        propsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        PropsListSelectionListener selectionListener
            = new PropsListSelectionListener();

        propsTable.getSelectionModel().addListSelectionListener(
                selectionListener);
        propsTable
            .getColumnModel()
                .getSelectionModel()
                    .addListSelectionListener(selectionListener);

        JScrollPane scrollPane = new JScrollPane(propsTable);
        SearchField searchField = new SearchField("", propsTable);

        buttonsPanel = new ButtonsPanel(propsTable, searchField);

        centerPanel = new TransparentPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(buttonsPanel, BorderLayout.EAST);

        JLabel needRestart
            = new JLabel(
                    r.getI18NString("plugin.propertieseditor.NEED_RESTART"));

        needRestart.setForeground(Color.RED);

        TransparentPanel searchPanel
            = new TransparentPanel(new BorderLayout(5, 0));

        searchPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        searchPanel.add(searchField, BorderLayout.CENTER);

        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(searchPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(needRestart, BorderLayout.SOUTH);
    }

    /**
     * Listens for events triggered when a selection is made in the props list.
     */
    private class PropsListSelectionListener
        implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            int selectedRow = propsTable.getSelectedRow();

            if (selectedRow == -1)
                buttonsPanel.defaultButtonState();
            else
                buttonsPanel.enableDeleteButton(true);
        }
    }

    /**
     * Gets the data from the <tt>ConfigurationService</tt> that will construct
     * the <tt>PropsTableModel</tt> for the properties table.
     *
     * @return The data necessary to initialize the <tt>PropsTableModel</tt>
     */
    private Object[][] initTableModel()
    {
        ConfigurationService confService
            = PropertiesEditorActivator.getConfigurationService();
        java.util.List<String> properties = confService.getAllPropertyNames();
        Object[][] data = new Object[properties.size()][];
        int i = 0;

        for (String property : properties)
        {
            data[i++]
                = new Object[] { property, confService.getProperty(property) };
        }

        return data;
    }
}
