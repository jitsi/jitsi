/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.menus;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.message.*;
import net.java.sip.communicator.impl.gui.utils.*;
/**
 * The <tt>FileMenu</tt> is the menu in the chat window menu bar that contains
 * save, print and close.
 * 
 * @author Yana Stamcheva
 */
public class FileMenu extends JMenu 
    implements ActionListener {

    private JMenuItem saveMenuItem = new JMenuItem(Messages.getString("save"),
            new ImageIcon(ImageLoader.getImage(ImageLoader.SAVE_ICON)));

    private JMenuItem printMenuItem = new JMenuItem(
            Messages.getString("print"), new ImageIcon(ImageLoader
                    .getImage(ImageLoader.PRINT_ICON)));

    private JMenuItem closeMenuItem = new JMenuItem(
            Messages.getString("close"), new ImageIcon(ImageLoader
                    .getImage(ImageLoader.CLOSE_ICON)));

    private ChatWindow parentWindow;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(ChatWindow parentWindow) {

        super(Messages.getString("file"));
        
        this.parentWindow = parentWindow;

        this.add(saveMenuItem);
        this.add(printMenuItem);

        this.addSeparator();

        this.add(closeMenuItem);

        this.saveMenuItem.setName("save");
        this.printMenuItem.setName("print");
        this.closeMenuItem.setName("close");

        this.saveMenuItem.addActionListener(this);
        this.printMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);

        this.setMnemonic(Messages.getString("file").charAt(0));
        this.saveMenuItem.setMnemonic(
                Messages.getString("mnemonic.save").charAt(0));
        this.printMenuItem.setMnemonic(
                Messages.getString("mnemonic.print").charAt(0));
        this.closeMenuItem.setMnemonic(
                Messages.getString("mnemonic.close").charAt(0));
        
        this.saveMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_S,
                KeyEvent.CTRL_MASK));
        
        this.printMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_R,
                KeyEvent.CTRL_MASK));
        
        // Disable all menu items that do nothing.
        this.saveMenuItem.setEnabled(false);
        this.printMenuItem.setEnabled(false);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("save")) {

        } else if (itemText.equalsIgnoreCase("print")) {

        } else if (itemText.equalsIgnoreCase("close")) {

            this.parentWindow.setVisible(false);
            this.parentWindow.dispose();

        }
    }
}
