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

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
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
    
    private I18NString copyString = Messages.getI18NString("copy");
    
    private I18NString saveString = Messages.getI18NString("save");
    
    private I18NString printString = Messages.getI18NString("print");
    
    private I18NString closeString = Messages.getI18NString("close");
    
    private JMenuItem copyMenuItem = new JMenuItem(
        copyString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));
    
    private JMenuItem saveMenuItem = new JMenuItem(
        saveString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.SAVE_ICON)));

    private JMenuItem printMenuItem = new JMenuItem(
        printString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.PRINT_ICON)));

    private JMenuItem closeMenuItem = new JMenuItem(
        closeString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSE_ICON)));
    /**
     * Creates an instance of <tt>ChatRightButtonMenu</tt>.
     *  
     * @param chatConvPanel The conversation panel, where this menu will apear.
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
        
        this.copyMenuItem.setMnemonic(copyString.getMnemonic());
        this.saveMenuItem.setMnemonic(saveString.getMnemonic());
        this.printMenuItem.setMnemonic(printString.getMnemonic());
        this.closeMenuItem.setMnemonic(closeString.getMnemonic());
        
        this.copyMenuItem.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyEvent.CTRL_MASK));
        
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
            
            Window window = this.chatConvPanel
                .getChatContainer().getConversationContainerWindow();
            
            window.setVisible(false);
            window.dispose();
        }
    }
}
