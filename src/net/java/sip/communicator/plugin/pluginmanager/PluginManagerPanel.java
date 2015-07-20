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
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final JTable pluginTable = new JTable();

    private final PluginTableModel tableModel = new PluginTableModel();

    private final ManageButtonsPanel buttonsPanel;

    private JCheckBox showSysBundlesCheckBox = new SIPCommCheckBox(
        Resources.getString("plugin.pluginmanager.SHOW_SYSTEM_BUNDLES"));

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

        this.initSystemBundleCheckBox();

        pluginListScrollPane.getViewport().add(pluginTable);

        pluginListScrollPane.getVerticalScrollBar().setUnitIncrement(30);

        PluginManagerActivator.bundleContext
            .addBundleListener(new PluginListBundleListener());
    }

    /**
     * Initializes the check box used to show or hide system bundles from the
     * list.
     */
    private void initSystemBundleCheckBox()
    {
        //Obtains previously saved value for the showSystemBundles check box.
        String showSystemBundlesProp = PluginManagerActivator
            .getConfigurationService().getString(
            "net.java.sip.communicator.plugin.pluginManager.showSystemBundles");

        if(showSystemBundlesProp != null)
        {
            boolean isShowSystemBundles
                = new Boolean(showSystemBundlesProp).booleanValue();

            this.showSysBundlesCheckBox.setSelected(isShowSystemBundles);

            ((PluginTableModel)pluginTable.getModel())
                .setShowSystemBundles(isShowSystemBundles);
        }

        this.showSysBundlesCheckBox
            .addChangeListener(new ShowSystemBundlesChangeListener());

        JPanel checkBoxPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
        checkBoxPanel.add(showSysBundlesCheckBox);

        this.add(checkBoxPanel, BorderLayout.SOUTH);
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

            Bundle selectedBundle =
                (Bundle) pluginTable.getValueAt(selectedRow, 0);


            if(PluginManagerActivator.isSystemBundle(selectedBundle))
            {
                buttonsPanel.enableUninstallButton(false);
                buttonsPanel.enableDeactivateButton(false);

                if (selectedBundle.getState() != Bundle.ACTIVE)
                {
                    buttonsPanel.enableActivateButton(true);
                }
                else
                {
                    buttonsPanel.enableActivateButton(false);
                }
            }
            else
            {
                buttonsPanel.enableUninstallButton(true);

                if (selectedBundle.getState() != Bundle.ACTIVE)
                {
                    buttonsPanel.enableActivateButton(true);
                    buttonsPanel.enableDeactivateButton(false);
                }
                else
                {
                    buttonsPanel.enableActivateButton(false);
                    buttonsPanel.enableDeactivateButton(true);
                }
            }

            // every bundle can be updated
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
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        bundleChanged(event);
                    }
                });
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


    /**
     * Adds all system bundles to the bundles list when the check box is
     * selected and removes them when user deselect it.
     */
    private class ShowSystemBundlesChangeListener implements ChangeListener
    {
        private boolean currentValue = false;

        public ShowSystemBundlesChangeListener()
        {
            currentValue = showSysBundlesCheckBox.isSelected();
        }

        public void stateChanged(ChangeEvent e)
        {
            if (currentValue == showSysBundlesCheckBox.isSelected())
            {
                return;
            }
            currentValue = showSysBundlesCheckBox.isSelected();
            //Save the current value of the showSystemBundles check box.
            PluginManagerActivator.getConfigurationService().setProperty(
                "net.java.sip.communicator.plugin.pluginManager.showSystemBundles",
                new Boolean(showSysBundlesCheckBox.isSelected()));

            PluginTableModel tableModel
                = (PluginTableModel)pluginTable.getModel();

            tableModel.setShowSystemBundles(showSysBundlesCheckBox.isSelected());

            tableModel.update();

            // as this changes the selection to none, make the buttons
            // at defautl state
            buttonsPanel.defaultButtonState();
        }
    }
}
