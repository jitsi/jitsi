/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.menu;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
/**
 * The MessageWindowMenuBar is the menu bar in the chat window where 
 * all menus are added.
 * @author Yana Stamcheva
 */
public class MessageWindowMenuBar extends JMenuBar {

    private FileMenu fileMenu;

    private EditMenu editMenu;

    private SettingsMenu settingsMenu;

    private JMenu helpMenu = new JMenu(Messages.getString("help"));

    private ChatWindow parentWindow;

    /**
     * Creates an instance of MessageWindowMenuBar.
     * @param parentWindow The parent ChatWindow.
     */
    public MessageWindowMenuBar(ChatWindow parentWindow) {

        this.parentWindow = parentWindow;

        fileMenu = new FileMenu(this.parentWindow);

        editMenu = new EditMenu(this.parentWindow);

        settingsMenu = new SettingsMenu(this.parentWindow);
        
        this.init();
    }

    /**
     * Initialize the menu bar, by adding all contained menus.
     */
    private void init() {

        this.add(fileMenu);

        this.add(editMenu);

        this.add(settingsMenu);

        this.add(helpMenu);

        // Disable all menus that are not yet implemented.
        this.helpMenu.setEnabled(false);
    }
}
