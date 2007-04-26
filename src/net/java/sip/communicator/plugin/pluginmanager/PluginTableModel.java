/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.pluginmanager;

import javax.swing.table.*;

import org.osgi.framework.*;

/**
 * The <tt>TableModel</tt> of the table containing all plug-ins.
 * 
 * @author Yana Stamcheva
 */
public class PluginTableModel extends AbstractTableModel
{
    private BundleContext bundleContext = PluginManagerActivator.bundleContext;
    
    private boolean showSystemBundles;
    
    private Object showSystemBundlesSync = new Object();
    
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
        
        if(bundleContext.getBundles() == null)
            return 0;
        else
        {
            if(showSystem)
                return bundleContext.getBundles().length;
            else
            {
                int bundlesSize = 0;
                
                Bundle[] bundles
                    = PluginManagerActivator.bundleContext.getBundles();
             
                for (int i = 0; i < bundles.length; i ++)
                {
                    Bundle bundle = bundles[i];
                    
                    Object sysBundleProp
                        = bundle.getHeaders().get("System-Bundle");
                
                    if(sysBundleProp == null || !sysBundleProp.equals("yes"))
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
        
        Bundle[] bundles
            = PluginManagerActivator.bundleContext.getBundles();
     
        for (int i = 0; i < bundles.length; i ++)
        {
            Bundle b = bundles[i];
            
            if(b.equals(bundle))
            {
                if(showSystem)
                    return true;
                else
                {
                    Object sysBundleProp
                        = bundle.getHeaders().get("System-Bundle");
            
                    if(sysBundleProp == null || !sysBundleProp.equals("yes"))
                        return true;
                    else
                        return false;
                }
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
        
        Bundle[] bundles
            = PluginManagerActivator.bundleContext.getBundles();
        
        if(showSystem)
            return bundles[row];
        else
        {
            int bundleCounter = 0;
            
            for(int i = 0; i < bundles.length; i++)   
            {   
                if(bundleCounter == row)   
                    return bundles[i];   
                    
                Object sysBundleProp
                    = bundles[i+1].getHeaders().get("System-Bundle");
        
                if(sysBundleProp == null || !sysBundleProp.equals("yes"))
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
}