/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.propertieseditor;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.resources.*;

/**
 * @author Marin Dzhigarov
 * @author Pawel Domas
 * 
 */
public class PropertiesEditorPanel
    extends TransparentPanel

{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    private ResourceManagementService resourceManagementService =
        PropertiesEditorActivator.getResourceManagementService();

    /**
     * The panel containing the props table and the buttons panel
     */
    private JPanel centerPanel;

    /**
     * The props table.
     */
    private JTable propsTable;

    /**
     * The buttons panel.
     */
    private ButtonsPanel buttonsPanel;

    /**
     * Creates an instance <tt>PropertiesEditorPanel</tt>.
     */
    public PropertiesEditorPanel()
    {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        /**
         * Instantiates the properties table and adds selection model and
         * listener and adds a row sorter to the table model
         */
        propsTable = new JTable(getTableModel());
        propsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        PropsListSelectionListener selectionListener =
            new PropsListSelectionListener();
        propsTable.getSelectionModel().addListSelectionListener(
            selectionListener);
        propsTable.getColumnModel().getSelectionModel()
            .addListSelectionListener(selectionListener);
        TableRowSorter<TableModel> sorter =
            new TableRowSorter<TableModel>(propsTable.getModel());
        propsTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(propsTable);

        SearchField searchField = new SearchField("", sorter);
        buttonsPanel = new ButtonsPanel(propsTable, searchField);

        centerPanel = new TransparentPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(buttonsPanel, BorderLayout.EAST);

        JLabel needRestart =
            new JLabel(
                resourceManagementService
                    .getI18NString("plugin.propertieseditor.NEED_RESTART"));
        needRestart.setForeground(Color.RED);

        TransparentPanel searchPanel =
            new TransparentPanel(new BorderLayout(5, 0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        searchPanel.add(searchField, BorderLayout.CENTER);

        add(searchPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(needRestart, BorderLayout.SOUTH);
    }

    /**
     * The table model of the props table.
     */
    private PropsTableModel tableModel;

    /**
     * Returns the table model of the props table.
     * 
     * @return The <tt>PropsTableModel</tt> of the props table.
     */
    private PropsTableModel getTableModel()
    {
        if (tableModel == null)
            tableModel = new PropsTableModel();
        return tableModel;
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
            {
                buttonsPanel.defaultButtonState();
                return;
            }

            buttonsPanel.enableDeleteButton(true);
        }
    }
}
