/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
 
package net.java.sip.communicator.impl.systray.jdic;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.systray.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>Traymenu</tt> is the menu that appears when the user right-click
 * on the systray icon
 *
 * @author Nicolas Chamouard
 */
public class TrayMenu
    extends JPopupMenu
    implements  ActionListener,
                PopupMenuListener
{
    /**
     * The logger for this class.
     */
    private Logger logger = Logger.getLogger(TrayMenu.class.getName());
    
    /**
     * A reference of the <tt>Uiservice</tt>
     */
    private UIService uiService;
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
        
    private StatusSubMenu setStatusMenu;
    
    /**
     * The configuration window called by the menu item "settings"
     */
    private ConfigurationWindow configDialog;
    
    
    /**
     * Creates an instance of <tt>TrayMenu</tt>.
     * @param service a reference of the current <tt>UIservice</tt>
     * @param tray a reference of the parent <tt>Systray</tt>
     */
    public TrayMenu(UIService service, SystrayServiceJdicImpl tray)
    {   
        uiService = service;
        parentSystray = tray;
        
        setStatusMenu = new StatusSubMenu(tray);
        
        this.add(settingsItem);
        this.add(addContactMenuItem);
        this.addSeparator();
        this.add(setStatusMenu);
        this.addSeparator();
        this.add(closeItem);
        
        this.settingsItem.setName("settings");
        this.closeItem.setName("close");
        this.addContactMenuItem.setName("addContact");
        
        this.settingsItem.addActionListener(this);
        this.closeItem.addActionListener(this);
        this.addContactMenuItem.addActionListener(this);
        
        this.addPopupMenuListener(this);
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
            configDialog = uiService.getConfigurationWindow();
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
            ExportedWindow dialog = uiService.getExportedWindow(
                ExportedWindow.ADD_CONTACT_WINDOW);
            
            if(dialog != null)
                dialog.setVisible(true);
            else
                uiService.getPopupDialog().showMessagePopupDialog(
                    Resources.getString("failedToLoadAddContactDialog"));
        }
    }
    
    /**
     * Currently unused
     * @param evt ignored
     */
    public void popupMenuCanceled(PopupMenuEvent evt)
    {
    }
    
    /**
     * Currently unused
     * @param evt ignored
     */
    public void popupMenuWillBecomeInvisible(PopupMenuEvent evt)
    {
    }
    
    /**
     * Fill the menu with items when it is displayed
     * @param evt ignored
     */
    public void popupMenuWillBecomeVisible(PopupMenuEvent evt)
    {
        setStatusMenu.update();
    }
    
}