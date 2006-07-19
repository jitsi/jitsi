/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;

/**
 * The main menu.
 * 
 * @author Yana Stamcheva
 */
public class Menu extends JMenuBar {
    private JMenu userMenu = new JMenu();

    private JMenu toolsMenu = new JMenu();

    private JMenu viewMenu = new JMenu();

    private JMenu helpMenu = new JMenu();

    /**
     * Creates an instance of <tt>Menu</tt>.
     */
    public Menu() {
        this.init();
    }

    /**
     * Constructs the menu.
     */
    private void init() {
        userMenu.setText(Messages.getString("file"));
        userMenu.setMnemonic(Messages.getString("mnemonic.file").charAt(0));
        userMenu.setToolTipText(Messages.getString("file"));

        toolsMenu.setText(Messages.getString("tools"));
        toolsMenu.setMnemonic(Messages.getString("mnemonic.tools").charAt(0));
        toolsMenu.setToolTipText(Messages.getString("tools"));

        viewMenu.setText(Messages.getString("settings"));
        viewMenu.setMnemonic(Messages.getString("mnemonic.settings").charAt(0));
        viewMenu.setToolTipText(Messages.getString("settings"));

        helpMenu.setText(Messages.getString("help"));
        helpMenu.setMnemonic(Messages.getString("mnemonic.help").charAt(0));
        helpMenu.setToolTipText(Messages.getString("help"));

        this.add(userMenu);
        this.add(toolsMenu);
        this.add(viewMenu);
        this.add(helpMenu);

        // Disable all menus that are not yet implemented.
        this.userMenu.setEnabled(false);
        this.toolsMenu.setEnabled(false);
        this.viewMenu.setEnabled(false);
        this.helpMenu.setEnabled(false);
    }
}
