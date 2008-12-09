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
            Resources.getString("service.gui.SETTINGS"),
            Resources.getImage("service.gui.icons.QUICK_MENU_CONFIGURE_ICON"));
    
    private JMenuItem closeItem = new JMenuItem(
            Resources.getString("service.gui.CLOSE"),
            Resources.getImage("service.systray.CLOSE_MENU_ICON"));
    
    private JMenuItem addContactMenuItem = new JMenuItem(
            Resources.getString("service.gui.ADD_CONTACT"),
            Resources.getImage("service.gui.icons.ADD_CONTACT_16x16_ICON"));
    
    private StatusSubMenu statusMenu;
    
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
        this.closeItem.setName("service.gui.CLOSE");
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
            ExportedWindow configWindow
                = SystrayActivator.getUIService()
                    .getExportedWindow(ExportedWindow.CONFIGURATION_WINDOW);
            
            configWindow.setVisible(true);
        }
        else if(itemName.equals("service.gui.CLOSE"))
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
                    .showMessagePopupDialog(Resources.getString(
                        "impl.systray.FAILED_TO_OPEN_ADD_CONTACT_DIALOG"));
        }
    }
}