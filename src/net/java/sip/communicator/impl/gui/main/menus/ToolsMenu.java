/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
/**
 * The <tt>FileMenu</tt> is a menu in the main application menu bar that
 * contains "New account".
 * 
 * @author Yana Stamcheva
 */
public class ToolsMenu
    extends SIPCommMenu 
    implements ActionListener
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
        
        this.parentWindow = parentWindow;

        this.add(configMenuItem);
        
        this.configMenuItem.setName("config");
        
        this.configMenuItem.addActionListener(this);
        
        this.setMnemonic(Messages.getI18NString("tools").getMnemonic());
        this.configMenuItem.setMnemonic(settingsString.getMnemonic());
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("config")) {
            configDialog = GuiActivator.getUIService().getConfigurationWindow();

            configDialog.showWindow();
        }
    }
}
