/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.*;
/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 * 
 * @author Yana Stamcheva
 */
public class ToolsMenu
    extends SIPCommMenu
    implements  ActionListener,
                PluginComponentListener
{

    private Logger logger = Logger.getLogger(ToolsMenu.class.getName());
    
    private I18NString settingsString = Messages.getI18NString("settings");
    
    private JMenuItem configMenuItem = new JMenuItem(settingsString.getText());
    
    private MainFrame parentWindow;
    
    private ConfigurationWindow configDialog;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public ToolsMenu(MainFrame parentWindow) {

        super(Messages.getI18NString("tools").getText());

        this.setForeground(
            new Color(ColorProperties.getColor("toolsMenuForeground")));

        this.parentWindow = parentWindow;

        this.add(configMenuItem);
        
        this.configMenuItem.setName("config");
        
        this.configMenuItem.addActionListener(this);
        
        this.setMnemonic(Messages.getI18NString("tools").getMnemonic());
        this.configMenuItem.setMnemonic(settingsString.getMnemonic());
        
        this.initPluginComponents();
    }

    /**
     * Initialize plugin components already registered for this container.
     */
    private void initPluginComponents()
    {   
        Iterator pluginComponents = GuiActivator.getUIService()
            .getComponentsForContainer(
                UIService.CONTAINER_TOOLS_MENU);
        
        if(pluginComponents.hasNext())
            this.addSeparator();
        
        while (pluginComponents.hasNext())
        {
            Component o = (Component)pluginComponents.next();
            
            this.add(o);
        }
        
        GuiActivator.getUIService().addPluginComponentListener(this);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("config")) {
            configDialog = GuiActivator.getUIService().getConfigurationWindow();

            configDialog.setVisible(true);
        }
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        Component c = (Component) event.getSource();
        
        if(event.getContainerID().equals(UIService.CONTAINER_TOOLS_MENU))
        {
            this.add(c);
            
            this.revalidate();
            this.repaint();
        }
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        Component c = (Component) event.getSource();
        
        if(event.getContainerID().equals(UIService.CONTAINER_TOOLS_MENU))
        {
            this.remove(c);
        }
    }
}
