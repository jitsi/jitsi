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
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private BundleContext bundleContext = PluginManagerActivator.bundleContext;

    private boolean showSystemBundles;

    private final Object showSystemBundlesSync = new Object();

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
        boolean showSystem;
        synchronized (showSystemBundlesSync)
        {
            showSystem = showSystemBundles;
        }

        if(bundles == null)
            return 0;
        else
        {
            if(showSystem)
                return bundles.length;
            else
            {
                int bundlesSize = 0;

                for (int i = 0; i < bundles.length; i ++)
                {
                    Bundle bundle = bundles[i];

                    if(!PluginManagerActivator.isSystemBundle(bundle))
                        bundlesSize++;
                }
                return bundlesSize;
            }
        }
    }

    /**
     * Returns TRUE if the given <tt>Bundle</tt> is contained in this table,
     * FALSE - otherwise.
     * @param bundle the <tt>Bundle</tt> to search for
     * @return TRUE if the given <tt>Bundle</tt> is contained in this table,
     * FALSE - otherwise.
     */
    public boolean contains(Bundle bundle)
    {
        boolean showSystem;
        synchronized (showSystemBundlesSync)
        {
            showSystem = showSystemBundles;
        }

        for (int i = 0; i < bundles.length; i ++)
        {
            Bundle b = bundles[i];

            if(b.equals(bundle))
            {
                return showSystem
                    || !PluginManagerActivator.isSystemBundle(bundle);
            }
        }

        return false;
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
        boolean showSystem;
        synchronized (showSystemBundlesSync)
        {
            showSystem = showSystemBundles;
        }

        if(showSystem)
            return bundles[row];
        else
        {
            int bundleCounter = 0;

            for(int i = 0; i < bundles.length; i++)
            {
                //ignore if this is a system bundle
                if(PluginManagerActivator.isSystemBundle(bundles[i]))
                    continue;

                if(bundleCounter == row)
                    return bundles[i];

                bundleCounter++;
            }
        }

        return null;
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
     * Returns TRUE if system bundles are show, FALSE - otherwise.
     * @return TRUE if system bundles are show, FALSE - otherwise
     */
    public boolean isShowSystemBundles()
    {
        boolean showSystem;

        synchronized (showSystemBundlesSync)
        {
            showSystem = showSystemBundles;
        }

        return showSystem;
    }

    /**
     * Sets the <tt>showSystemBundles</tt> property.
     * @param showSystemBundles indicates if system bundles will be shown or not
     */
    public void setShowSystemBundles(boolean showSystemBundles)
    {
        synchronized (showSystemBundlesSync)
        {
            this.showSystemBundles = showSystemBundles;
        }
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
