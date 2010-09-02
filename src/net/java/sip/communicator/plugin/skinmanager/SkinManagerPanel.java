/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.skinmanager;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * @author Yana Stamcheva
 * @author Adam Netocny, CircleTech, s.r.o.
 */
public class SkinManagerPanel
        extends TransparentPanel
{
    /**
     * The table containing all skins.
     */
    private final JTable skinTable = new JTable();

    /**
     * The table model.
     */
    private final SkinTableModel tableModel = new SkinTableModel();

    /**
     * The panel containing manage buttons.
     */
    private final ManageButtonsPanel buttonsPanel;

    /**
     * Creates an instance of <tt>SkinManagerPanel</tt>.
     */
    public SkinManagerPanel()
    {
        super(new BorderLayout());
        JScrollPane pluginListScrollPane = new JScrollPane();

        skinTable.setModel(tableModel);

        TableColumn col = skinTable.getColumnModel().getColumn(0);
        col.setCellRenderer(new SkinListCellRenderer());

        SkinListSelectionListener selectionListener =
                new SkinListSelectionListener();

        skinTable.getSelectionModel().addListSelectionListener(
                selectionListener);
        skinTable.getColumnModel().getSelectionModel()
            .addListSelectionListener(selectionListener);

        skinTable.setRowHeight(48);

        skinTable.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        skinTable.setTableHeader(null);

        buttonsPanel = new ManageButtonsPanel(skinTable);

        this.add(pluginListScrollPane, BorderLayout.CENTER);

        this.add(buttonsPanel, BorderLayout.EAST);

        pluginListScrollPane.getViewport().add(skinTable);

        pluginListScrollPane.getVerticalScrollBar().setUnitIncrement(30);

        SkinManagerActivator.bundleContext
            .addBundleListener(new SkinListBundleListener());
    }

    /**
     * Listens for events triggered when a selection is made in the plugin list.
     */
    private class SkinListSelectionListener
        implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            int selectedRow = skinTable.getSelectedRow();

            if (selectedRow == -1)
                return;

            Bundle selectedBundle
                = (Bundle) skinTable.getValueAt(selectedRow, 0);

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
    }

    /**
     * Listens for <tt>BundleEvents</tt> triggered by the bundle context.
     */
    private class SkinListBundleListener
            implements BundleListener
    {
        public void bundleChanged(BundleEvent event)
        {
            tableModel.update();

            if (event.getType() == BundleEvent.INSTALLED)
            {
                skinTable.scrollRectToVisible(
                    new Rectangle(  0, skinTable.getHeight(),
                                    1, skinTable.getHeight()));
            }
        }
    }
}
