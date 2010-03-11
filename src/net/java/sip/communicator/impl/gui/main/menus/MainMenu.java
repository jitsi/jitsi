/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.menus;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.resources.*;

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
    /**
     * Creates an instance of <tt>Menu</tt>.
     */
    public MainMenu(MainFrame mainFrame)
    {
        this.setForeground(
            new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAIN_MENU_FOREGROUND")));

        addMenu(new FileMenu(mainFrame), "service.gui.FILE");
        addMenu(new ToolsMenu(mainFrame), "service.gui.TOOLS");
        addMenu(new HelpMenu(mainFrame), "service.gui.HELP");
    }

    private void addMenu(JMenu menu, String key)
    {
        ResourceManagementService resources = GuiActivator.getResources();

        menu.setText(resources.getI18NString(key));
        menu.setMnemonic(resources.getI18nMnemonic(key));
        menu.setOpaque(false);
        add(menu);
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
     * Paints the MENU_BACKGROUND image on the background of this container.
     * 
     * @param g the <tt>Graphics</tt> object that does the painting
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Image backgroundImage
            = ImageLoader.getImage(ImageLoader.MENU_BACKGROUND);

        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }
}
