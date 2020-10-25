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
package net.java.sip.communicator.plugin.pluginmanager;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.osgi.framework.*;

/**
 * @author Yana Stamcheva
 */
public class PluginManagerPanel
    extends TransparentPanel
{
    private final JTable pluginTable = new JTable();

    private final PluginTableModel tableModel = new PluginTableModel();

    private final ManageButtonsPanel buttonsPanel;

    /**
     * Creates an instance of <tt>PluginManagerPanel</tt>.
     */
    public PluginManagerPanel()
    {
        super(new BorderLayout());
        JScrollPane pluginListScrollPane = new JScrollPane();

        pluginTable.setModel(tableModel);

        TableColumn col = pluginTable.getColumnModel().getColumn(0);
        col.setCellRenderer(new PluginListCellRenderer());

        PluginListSelectionListener selectionListener =
            new PluginListSelectionListener();

        pluginTable.getSelectionModel().addListSelectionListener(
            selectionListener);
        pluginTable.getColumnModel().getSelectionModel()
            .addListSelectionListener(selectionListener);

        pluginTable.setRowHeight(48);

        pluginTable.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        pluginTable.setTableHeader(null);

        buttonsPanel = new ManageButtonsPanel(pluginTable);

        this.add(pluginListScrollPane, BorderLayout.CENTER);

        this.add(buttonsPanel, BorderLayout.EAST);

        pluginListScrollPane.getViewport().add(pluginTable);

        pluginListScrollPane.getVerticalScrollBar().setUnitIncrement(30);

        PluginManagerActivator.bundleContext
            .addBundleListener(new PluginListBundleListener());
    }

    /**
     * Listens for events triggered when a selection is made in the plugin list.
     */
    private class PluginListSelectionListener
        implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            int selectedRow = pluginTable.getSelectedRow();

            if (selectedRow == -1)
                return;

            buttonsPanel.enableUninstallButton(false);
            buttonsPanel.enableDeactivateButton(false);
            buttonsPanel.enableActivateButton(false);
            buttonsPanel.enableUpdateButton(true);
        }
    }

    /**
     * Listens for <tt>BundleEvents</tt> triggered by the bundle context.
     */
    private class PluginListBundleListener
        implements BundleListener
    {
        public void bundleChanged(final BundleEvent event)
        {
            if(!SwingUtilities.isEventDispatchThread())
            {
                SwingUtilities.invokeLater(() -> bundleChanged(event));
                return;
            }

            tableModel.update();

            if (event.getType() == BundleEvent.INSTALLED)
            {
                pluginTable.scrollRectToVisible(new Rectangle(0, pluginTable
                    .getHeight(), 1, pluginTable.getHeight()));
            }
        }
    }
}
