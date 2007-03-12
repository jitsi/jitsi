/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.menus;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
/**
 * The <tt>MessageWindowMenuBar</tt> is the menu bar in the chat window where 
 * all menus are added.
 * 
 * @author Yana Stamcheva
 */
public class MessageWindowMenuBar extends JMenuBar {

    private FileMenu fileMenu;

    private EditMenu editMenu;

    private SettingsMenu settingsMenu;

    private HelpMenu helpMenu;

    private ChatWindow parentWindow;

    /**
     * Creates an instance of <tt>MessageWindowMenuBar</tt>.
     * @param parentWindow The parent ChatWindow.
     */
    public MessageWindowMenuBar(ChatWindow parentWindow) {

        this.parentWindow = parentWindow;

        fileMenu = new FileMenu(this.parentWindow);

        editMenu = new EditMenu(this.parentWindow);

        settingsMenu = new SettingsMenu(this.parentWindow);
        
        helpMenu = new HelpMenu(this.parentWindow);
        
        this.init();
    }

    /**
     * Initializes the menu bar, by adding all contained menus.
     */
    private void init() {

        this.add(fileMenu);

        this.add(editMenu);

        this.add(settingsMenu);

        this.add(helpMenu);
    }
    
    /**
     * Returns the currently selected menu. 
     */
    public SIPCommMenu getSelectedMenu()
    {
        int menuCount = this.getMenuCount();
        
        for(int i = 0; i < menuCount; i ++) {
            SIPCommMenu menu = (SIPCommMenu) this.getMenu(i);
            
            if(menu.isSelected()) {
                return menu;
            }
        }
        return null;
    }
}
