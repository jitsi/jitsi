/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.presence.*;

/**
 * The main menu.
 * 
 * @author Yana Stamcheva
 */
public class MainMenu
    extends JMenuBar
{
    private FileMenu fileMenu; 

    private ToolsMenu toolsMenu;

    private ViewMenu viewMenu;

    private JMenu helpMenu = new JMenu();

    /**
     * Creates an instance of <tt>Menu</tt>.
     */
    public MainMenu(MainFrame mainFrame)
    {
        this.fileMenu = new FileMenu(mainFrame);
        this.toolsMenu = new ToolsMenu(mainFrame);
        this.viewMenu = new ViewMenu(mainFrame);
        
        this.init();
    }

    /**
     * Constructs the menu.
     */
    private void init()
    {
        fileMenu.setText(Messages.getString("file"));
        fileMenu.setMnemonic(Messages.getString("mnemonic.file").charAt(0));
        fileMenu.setToolTipText(Messages.getString("file"));

        toolsMenu.setText(Messages.getString("tools"));
        toolsMenu.setMnemonic(Messages.getString("mnemonic.tools").charAt(0));
        toolsMenu.setToolTipText(Messages.getString("tools"));

        viewMenu.setText(Messages.getString("view"));
        viewMenu.setMnemonic(Messages.getString("mnemonic.view").charAt(0));
        viewMenu.setToolTipText(Messages.getString("settings"));

        helpMenu.setText(Messages.getString("help"));
        helpMenu.setMnemonic(Messages.getString("mnemonic.help").charAt(0));
        helpMenu.setToolTipText(Messages.getString("help"));

        this.add(fileMenu);
        this.add(toolsMenu);
        this.add(viewMenu);
        this.add(helpMenu);

        // Disable all menus that are not yet implemented.
        this.helpMenu.setEnabled(false);
    }
    
    /**
     * Returns TRUE if there are selected menus, otherwise returns false.
     */
    public boolean hasSelectedMenus()
    {
        int menuCount = this.getMenuCount();
        
        for(int i = 0; i < menuCount; i ++) {
            JMenu menu = this.getMenu(i);
            
            if(menu.isSelected()) {
                return true;
            }
        }
        return false;
    }
}
