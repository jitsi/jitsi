/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
 
package net.java.sip.communicator.impl.systray.jdic;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>TrayMenu</tt> is the menu that appears when the user right-click
 * on the Systray icon.
 *
 * @author Nicolas Chamouard
 */
public class TrayMenu
    extends JPopupMenu
    implements  ActionListener
{
    /**
     * The logger for this class.
     */
    private Logger logger = Logger.getLogger(TrayMenu.class.getName());
    
    /**
     * A reference of <tt>Systray</tt>
     */
    private SystrayServiceJdicImpl parentSystray;
        
    private JMenuItem settingsItem = new JMenuItem(
            Resources.getString("settings"),
            new ImageIcon(Resources.getImage("settingsMenuIcon")));
    
    private JMenuItem closeItem = new JMenuItem(
            Resources.getString("close"),
            new ImageIcon(Resources.getImage("closeMenuIcon")));
    
    private JMenuItem addContactMenuItem = new JMenuItem(
            Resources.getString("addContact"),
            new ImageIcon(Resources.getImage("addContactIcon")));
        
    private StatusSubMenu statusMenu;
    
    /**
     * The configuration window called by the menu item "settings"
     */
    private ConfigurationWindow configDialog;
    
    
    /**
     * Creates an instance of <tt>TrayMenu</tt>.
     * @param tray a reference of the parent <tt>Systray</tt>
     */
    public TrayMenu(SystrayServiceJdicImpl tray)
    {
        parentSystray = tray;

        statusMenu = new StatusSubMenu(tray);

        this.add(settingsItem);
        this.add(addContactMenuItem);
        this.addSeparator();
        this.add(statusMenu);
        this.addSeparator();
        this.add(closeItem);

        this.settingsItem.setName("settings");
        this.closeItem.setName("close");
        this.addContactMenuItem.setName("addContact");

        this.settingsItem.addActionListener(this);
        this.closeItem.addActionListener(this);
        this.addContactMenuItem.addActionListener(this);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     * @param evt the event containing the menu item name
     */
    public void actionPerformed(ActionEvent evt)
    {
  
        JMenuItem menuItem = (JMenuItem) evt.getSource();
        String itemName = menuItem.getName();
        
        if(itemName.equals("settings"))
        {  
            configDialog
                = SystrayActivator.getUIService().getConfigurationWindow();
            
            configDialog.setVisible(true);
        }
        else if(itemName.equals("close"))
        {   
            try
            {
                SystrayActivator.bundleContext.getBundle(0).stop();
            } catch (BundleException ex) 
            {
                logger.error("Failed to gently shutdown Felix", ex);
                System.exit(0);
            }
            
        }
        else if(itemName.equals("addContact"))
        {                   
            ExportedWindow dialog
                = SystrayActivator.getUIService().getExportedWindow(
                    ExportedWindow.ADD_CONTACT_WINDOW);
            
            if(dialog != null)
                dialog.setVisible(true);
            else
                SystrayActivator.getUIService().getPopupDialog()
                    .showMessagePopupDialog(
                        Resources.getString("failedToLoadAddContactDialog"));
        }
    }
}