/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;
/**
 * The <tt>FileMenu</tt> is the menu in the chat window menu bar that contains
 * save, print and close.
 * 
 * @author Yana Stamcheva
 */
public class FileMenu extends SIPCommMenu 
    implements ActionListener {

    private I18NString saveString = Messages.getI18NString("save");
    
    private I18NString printString = Messages.getI18NString("print");
    
    private I18NString closeString = Messages.getI18NString("close");
    
    private JMenuItem saveMenuItem = new JMenuItem(
        saveString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.SAVE_ICON)));

    private JMenuItem printMenuItem = new JMenuItem(
        printString.getText(), 
        new ImageIcon(ImageLoader.getImage(ImageLoader.PRINT_ICON)));

    private JMenuItem closeMenuItem = new JMenuItem(
        closeString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSE_ICON)));

    private ChatWindow parentWindow;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(ChatWindow parentWindow) {

        super(Messages.getI18NString("file").getText());

        this.parentWindow = parentWindow;

        this.setForeground(new Color(
            ColorProperties.getColor("chatMenuForeground")));

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

        this.setMnemonic(Messages.getI18NString("file").getMnemonic());
        this.saveMenuItem.setMnemonic(saveString.getMnemonic());
        this.printMenuItem.setMnemonic(printString.getMnemonic());
        this.closeMenuItem.setMnemonic(closeString.getMnemonic());
        
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
