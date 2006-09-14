/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.message.menu;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.message.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>ChatRightButtonMenu</tt> appears when the user makes a right button
 * click on the chat window conversation area (where sent and received messages
 * are displayed).
 *  
 * @author Yana Stamcheva
 */
public class ChatRightButtonMenu extends JPopupMenu
    implements ActionListener {

    private ChatConversationPanel chatConvPanel;
    
    private JMenuItem copyMenuItem = new JMenuItem(Messages.getString("copy"),
            new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));
    
    private JMenuItem saveMenuItem = new JMenuItem(Messages.getString("save"),
            new ImageIcon(ImageLoader.getImage(ImageLoader.SAVE_ICON)));

    private JMenuItem printMenuItem = new JMenuItem(
            Messages.getString("print"), new ImageIcon(ImageLoader
                    .getImage(ImageLoader.PRINT_ICON)));

    private JMenuItem closeMenuItem = new JMenuItem(
            Messages.getString("close"), new ImageIcon(ImageLoader
                    .getImage(ImageLoader.CLOSE_ICON)));
    /**
     * Creates an instance of <tt>ChatRightButtonMenu</tt>.
     *  
     * @param parentWindow The window owner of this popup menu.
     */
    public ChatRightButtonMenu(ChatConversationPanel chatConvPanel) {
        super();

        this.chatConvPanel = chatConvPanel;
        
        this.init();
    }
    
    /**
     * Initializes the menu with all menu items.
     */
    private void init() {
        
        this.add(copyMenuItem);
        
        this.addSeparator();
        
        this.add(saveMenuItem);
        this.add(printMenuItem);

        this.addSeparator();

        this.add(closeMenuItem);

        this.copyMenuItem.setName("copy");
        this.saveMenuItem.setName("save");
        this.printMenuItem.setName("print");
        this.closeMenuItem.setName("close");

        this.copyMenuItem.addActionListener(this);
        this.saveMenuItem.addActionListener(this);
        this.printMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);
        
        this.copyMenuItem.setMnemonic(
                Messages.getString("mnemonic.copy").charAt(0));
        this.saveMenuItem.setMnemonic(
                Messages.getString("mnemonic.save").charAt(0));
        this.printMenuItem.setMnemonic(
                Messages.getString("mnemonic.print").charAt(0));
        this.closeMenuItem.setMnemonic(
                Messages.getString("mnemonic.close").charAt(0));
        
        // Disable all menu items that do nothing.
        this.saveMenuItem.setEnabled(false);
        this.printMenuItem.setEnabled(false);
    }
    
    /**
     * Disables the copy item.
     */
    public void disableCopy() {
        this.copyMenuItem.setEnabled(false);
    }
    
    /**
     * Enables the copy item.
     */
    public void enableCopy() {
        this.copyMenuItem.setEnabled(true);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("copy")) {
            this.chatConvPanel.copyConversation();
            
        } else if (itemText.equalsIgnoreCase("save")) {

        } else if (itemText.equalsIgnoreCase("print")) {

        } else if (itemText.equalsIgnoreCase("close")) {
            
            Window window = this.chatConvPanel.getChatContainer().getWindow();
            window.setVisible(false);
            window.dispose();
        }
    }
}
