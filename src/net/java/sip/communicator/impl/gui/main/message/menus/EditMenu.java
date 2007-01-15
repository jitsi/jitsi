/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message.menus;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.message.*;
import net.java.sip.communicator.impl.gui.utils.*;
/**
 * The <tt>EditMenu</tt> is the menu in the chat window menu bar, which contains
 * cut, copy and paste.
 * 
 * @author Yana Stamcheva
 */
public class EditMenu extends SIPCommMenu 
    implements ActionListener {

    private I18NString cutString = Messages.getI18NString("cut");
    
    private I18NString copyString = Messages.getI18NString("copy");
    
    private I18NString pasteString = Messages.getI18NString("paste");
    
    private JMenuItem cutMenuItem = new JMenuItem(
        cutString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CUT_ICON)));

    private JMenuItem copyMenuItem = new JMenuItem(
        copyString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

    private JMenuItem pasteMenuItem = new JMenuItem(
        pasteString.getText(), 
        new ImageIcon(ImageLoader.getImage(ImageLoader.PASTE_ICON)));

    private ChatWindow chatWindow;

    /**
     * Creates an instance of <tt>EditMenu</tt>.
     * 
     * @param chatWindow The parent <tt>ChatWindow</tt>.
     */
    public EditMenu(ChatWindow chatWindow) {

        super(Messages.getI18NString("edit").getText());

        this.chatWindow = chatWindow;

        this.cutMenuItem.setName("cut");
        this.copyMenuItem.setName("copy");
        this.pasteMenuItem.setName("paste");

        this.cutMenuItem.addActionListener(this);
        this.copyMenuItem.addActionListener(this);
        this.pasteMenuItem.addActionListener(this);

        this.add(cutMenuItem);
        this.add(copyMenuItem);
        this.add(pasteMenuItem);
        
        this.setMnemonic(Messages.getI18NString("edit").getMnemonic());
        this.cutMenuItem.setMnemonic(cutString.getMnemonic());
        this.copyMenuItem.setMnemonic(copyString.getMnemonic());
        this.pasteMenuItem.setMnemonic(pasteString.getMnemonic());
        
        this.cutMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_X,
                KeyEvent.CTRL_MASK));
        
        this.copyMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyEvent.CTRL_MASK));
        
        this.pasteMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_P,
                KeyEvent.CTRL_MASK));
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String menuItemName = menuItem.getName();

        if (menuItemName.equalsIgnoreCase("cut")) {

            this.chatWindow.getCurrentChatPanel().cut();            
        }
        else if (menuItemName.equalsIgnoreCase("copy")) {
            
            this.chatWindow.getCurrentChatPanel().copy();
        }
        else if (menuItemName.equalsIgnoreCase("paste")) {

            this.chatWindow.getCurrentChatPanel().paste();
        }
    }
}
