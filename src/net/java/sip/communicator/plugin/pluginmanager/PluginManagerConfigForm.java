/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.plugin.pluginmanager;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

/**
 * The <tt>ConfigurationForm</tt> that would be added in the user interface
 * configuration window. It contains a list of all installed plug-ins. Allows
 * installing new plugins and manage the existing once.
 * 
 * @author Yana Stamcheva
 */
public class PluginManagerConfigForm
    extends JPanel
    implements ConfigurationForm
{ 
    private JScrollPane pluginListScrollPane = new JScrollPane();
    
    private JTable pluginTable = new JTable();
    
    private PluginTableModel tableModel = new PluginTableModel();
    
    private ManageButtonsPanel buttonsPanel;
    
    public PluginManagerConfigForm()
    {
        super(new BorderLayout());
        
        pluginTable.setModel(tableModel);
        
        TableColumn col = pluginTable.getColumnModel().getColumn(0);
        col.setCellRenderer(new PluginListCellRenderer());
        
        PluginListSelectionListener selectionListener
            = new PluginListSelectionListener();
        
        pluginTable.getSelectionModel()
            .addListSelectionListener(selectionListener);
        pluginTable.getColumnModel().getSelectionModel()
            .addListSelectionListener(selectionListener);
        
        pluginTable.setRowHeight(48);
        
        pluginTable.setBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8));
        
        pluginTable.setTableHeader(null);
        
        buttonsPanel = new ManageButtonsPanel(pluginTable);
        
        this.add(pluginListScrollPane, BorderLayout.CENTER);
        
        this.add(buttonsPanel, BorderLayout.EAST);
        
        
        pluginListScrollPane.getViewport().add(pluginTable);
        
        pluginListScrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        pluginListScrollPane.getVerticalScrollBar().setUnitIncrement(30);
                
        PluginManagerActivator.bundleContext
            .addBundleListener(new PluginListBundleListener());
    }
    
    /**
     * Listens for events triggered when a selection is made in the plugin list.
     */
    private class PluginListSelectionListener implements ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e)
        {
            int selectedRow = pluginTable.getSelectedRow();
            
            if(selectedRow == -1)
                return;
            
            Bundle selectedBundle
                = (Bundle) pluginTable.getValueAt(selectedRow, 0);
            
            Object sysBundleProp
                = selectedBundle.getHeaders().get("System-Bundle");
            
            if(sysBundleProp != null && sysBundleProp.equals("yes"))
                buttonsPanel.enableUninstallButton(false);
            else
                buttonsPanel.enableUninstallButton(true);
            
            if(selectedBundle.getState() == Bundle.ACTIVE)
            {   
                if(sysBundleProp != null && sysBundleProp.equals("yes"))
                    buttonsPanel.enableDeactivateButton(false);
                else
                    buttonsPanel.enableDeactivateButton(true);
                
                buttonsPanel.enableActivateButton(false);
            }
            else
            {  
                buttonsPanel.enableActivateButton(true);
                buttonsPanel.enableDeactivateButton(false);
            }
        }
    }
    
    /**
     * Listens for <tt>BundleEvents</tt> triggered by the bundle context.
     */
    private class PluginListBundleListener implements BundleListener
    {
        public void bundleChanged(BundleEvent event)
        {   
            tableModel.update();
     
            if(event.getType() == BundleEvent.INSTALLED)
            {
                pluginTable.scrollRectToVisible(new Rectangle(
                            0, pluginTable.getHeight(),
                            1, pluginTable.getHeight()));
            }
        }
    }
    
    /**
     * Implements the <tt>ConfigurationForm.getTitle()</tt> method. Returns the
     * title of this configuration form.
     */
    public String getTitle()
    {
        return Resources.getString("plugins");
    }

    /**
     * Implements the <tt>ConfigurationForm.getIcon()</tt> method. Returns the
     * icon of this configuration form.
     */
    public byte[] getIcon()
    {
        return Resources.getResources()
                                .getImageInBytes("pluginIconPluginManager");
    }

    /**
     * Implements the <tt>ConfigurationForm.getForm()</tt> method. Returns the
     * component corresponding to this configuration form.
     */
    public Object getForm()
    {
        return this;
    }

    public int getIndex()
    {
        return -1;
    }
}
