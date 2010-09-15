/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.skinmanager;

import java.util.*;

import javax.swing.table.*;

import org.osgi.framework.*;

/**
 * The <tt>TableModel</tt> of the table containing all plug-ins.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SkinTableModel
    extends AbstractTableModel
{
    /**
     * The bundle context.
     */
    private BundleContext bundleContext = SkinManagerActivator.bundleContext;

    /**
     * The array of bundles.
     */
    private Bundle[] bundles = null;

    /**
     * A bundle comparator.
     */
    private final BundleComparator bundleComparator = new BundleComparator();

    /**
     * Create an instance of <tt>SkinTableModel</tt>
     */
    public SkinTableModel()
    {
        refreshSortedBundlesList();
    }

    /**
     * Returns the count of table rows.
     * @return int the count of table rows
     */
    public int getRowCount()
    {
        if (bundles == null)
        {
            return 0;
        }
        else
        {
            return bundles.length;
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
        for (int i = 0; i < bundles.length; i++)
        {
            Bundle b = bundles[i];

            if (b.equals(bundle))
            {
                return true;
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
     * @param row the row number to check
     * @param column the column number to check
     * @return false
     */
    public boolean isCellEditable(int row, int column)
    {
        return false;
    }

    /**
     * Returns the value in the cell given by row and column.
     * @param row the row number of the cell, which value we're looking for
     * @param column the column of the cell, which value we're looking for
     * @return the value of the cell given by <tt>row</tt> and <tt>column</tt>
     */
    public Object getValueAt(int row, int column)
    {
        int bundleCounter = 0;

        for (int i = 0; i < bundles.length; i++)
        {
            if (bundleCounter == row)
            {
                return bundles[i];
            }

            bundleCounter++;
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
     * Synchronizes the content of the bundle list with the bundles currently
     * available in the bundle context and sorts it again.
     */
    private void refreshSortedBundlesList()
    {
        Bundle[] list = this.bundleContext.getBundles();
        ArrayList<Bundle> show = new ArrayList<Bundle>();
        if (list != null)
        {
            for (Bundle b : list)
            {
                Dictionary<?, ?> headers = b.getHeaders();
                if (headers.get(Constants.BUNDLE_ACTIVATOR) != null)
                {
                    if (headers.get(Constants.BUNDLE_ACTIVATOR).toString()
                        .equals("net.java.sip.communicator.plugin." +
                                "skinresourcepack.SkinResourcePack"))
                    {
                        show.add(b);
                    }
                }
            }
        }

        this.bundles = new Bundle[show.size()];
        int i = 0;
        for (Bundle b : show)
        {
            this.bundles[i] = b;
            i++;
        }

        Arrays.sort(this.bundles, bundleComparator);
    }
}
