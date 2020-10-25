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

import java.util.*;

import javax.swing.table.*;

import org.osgi.framework.*;

/**
 * The <tt>TableModel</tt> of the table containing all plug-ins.
 *
 * @author Yana Stamcheva
 */
public class PluginTableModel
    extends AbstractTableModel
{
    private BundleContext bundleContext = PluginManagerActivator.bundleContext;

    private Bundle[] bundles = null;

    private final BundleComparator bundleComparator = new BundleComparator();

    /**
     * Create an instance of <tt>PluginTableModel</tt>
     */
    public PluginTableModel()
    {
        refreshSortedBundlesList();
    }

    /**
     * Returns the count of table rows.
     * @return int the count of table rows
     */
    public int getRowCount()
    {
        if(bundles == null)
            return 0;
        else
        {
            return bundles.length;
        }
    }

    /**
     * Returns the count of table columns.
     * @return int the count of table columns
     */
    public int getColumnCount()
    {
        return 1;
    }

    /**
     * Returns FALSE for all cells in this table.
     */
    @Override
    public boolean isCellEditable(int row, int column)
    {
        return false;
    }

    /**
     * Returns the value in the cell given by row and column.
     */
    public Object getValueAt(int row, int column)
    {
        return bundles[row];
    }

    /**
     * Updates the table content.
     */
    public void update()
    {
        refreshSortedBundlesList();
        fireTableDataChanged();
    }

    /**
     * Syncs the content of the bundle list with the bundles currently
     * available in the bundle context and sorts it again.
     */
    private void refreshSortedBundlesList()
    {
        this.bundles = this.bundleContext.getBundles();
        Arrays.sort(this.bundles, bundleComparator);
    }
}
