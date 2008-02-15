/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The main menu bar. This is the menu bar that appears on top of the main
 * window. It contains a file menu, tools menu, view menu and help menu.
 * <p>
 * Note that this container allows also specifying a custom background by
 * modifying the menuBackgroundImage.png in the resources/images/impl/gui/common
 * folder.
 * 
 * @author Yana Stamcheva
 */
public class MainMenu
    extends JMenuBar
{
    private FileMenu fileMenu; 

    private ToolsMenu toolsMenu;

    private ViewMenu viewMenu;

    private HelpMenu helpMenu;

    private I18NString fileString = Messages.getI18NString("file");
    
    private I18NString toolsString = Messages.getI18NString("tools");
    
    private I18NString viewString = Messages.getI18NString("view");
    
    private I18NString helpString = Messages.getI18NString("help");
    
    /**
     * Creates an instance of <tt>Menu</tt>.
     */
    public MainMenu(MainFrame mainFrame)
    {
        this.fileMenu = new FileMenu(mainFrame);
        this.toolsMenu = new ToolsMenu(mainFrame);
        this.viewMenu = new ViewMenu(mainFrame);
        this.helpMenu = new HelpMenu(mainFrame);

        this.init();
    }

    /**
     * Constructs the menu.
     */
    private void init()
    {
        fileMenu.setText(fileString.getText());
        fileMenu.setMnemonic(fileString.getMnemonic());
        fileMenu.setToolTipText(fileString.getText());

        toolsMenu.setText(toolsString.getText());
        toolsMenu.setMnemonic(toolsString.getMnemonic());
        toolsMenu.setToolTipText(toolsString.getText());

        viewMenu.setText(viewString.getText());
        viewMenu.setMnemonic(viewString.getMnemonic());
        viewMenu.setToolTipText(viewString.getText());

        helpMenu.setText(helpString.getText());
        helpMenu.setMnemonic(helpString.getMnemonic());
        helpMenu.setToolTipText(helpString.getText());

        this.add(fileMenu);
        this.add(toolsMenu);
        this.add(viewMenu);
        this.add(helpMenu);
    }
    
    /**
     * Returns <code>true</code> if there are selected menus, otherwise - 
     * returns <code>false</code>.
     * 
     * @return <code>true</code> if there are selected menus, otherwise - 
     * returns <code>false</code>.
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

    /**
     * Returns the <tt>ViewMenu</tt>, contained in this menu bar.
     * 
     * @return the <tt>ViewMenu</tt>, contained in this menu bar.
     */
    public ViewMenu getViewMenu()
    {
        return viewMenu;
    }

    /**
     * Paints the MENU_BACKGROUND image on the background of this container.
     * 
     * @param g the Graphics object that does the painting
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Image backgroundImage
            = ImageLoader.getImage(ImageLoader.MENU_BACKGROUND);

        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
    }
}
