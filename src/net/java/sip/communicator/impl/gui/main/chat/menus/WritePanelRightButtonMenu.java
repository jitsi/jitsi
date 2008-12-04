/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.swing.*;

/**
 * The <tt>WritePanelRightButtonMenu</tt> appears when the user makes a right
 * button click on the chat window write area (where user types messages).
 *  
 * @author Yana Stamcheva
 */
public class WritePanelRightButtonMenu extends JPopupMenu
    implements ActionListener {

    private ChatWindow parentWindow;
    
    private I18NString cutString = Messages.getI18NString("cut");
    
    private I18NString copyString = Messages.getI18NString("copy");
    
    private I18NString pasteString = Messages.getI18NString("paste");
    
    private I18NString closeString = Messages.getI18NString("close");
    
    private JMenuItem cutMenuItem = new JMenuItem(
        cutString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CUT_ICON)));

    private JMenuItem copyMenuItem = new JMenuItem(
        copyString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.COPY_ICON)));

    private JMenuItem pasteMenuItem = new JMenuItem(
        pasteString.getText(),
        new ImageIcon(ImageLoader.getImage(ImageLoader.PASTE_ICON)));
    
    private JMenuItem closeMenuItem = new JMenuItem(
        closeString.getText(), 
        new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSE_ICON)));
    /**
     * Creates an instance of <tt>WritePanelRightButtonMenu</tt>.
     *  
     * @param parentWindow The window owner of this popup menu.
     */
    public WritePanelRightButtonMenu(ChatWindow parentWindow) {
        super();

        this.parentWindow = parentWindow;
        
        this.init();
    }
    
    /**
     * Initializes this menu with menu items.
     */
    private void init() {
        
        this.add(copyMenuItem);
        this.add(cutMenuItem);
        this.add(pasteMenuItem);

        this.addSeparator();

        this.add(closeMenuItem);

        this.copyMenuItem.setName("copy");
        this.cutMenuItem.setName("cut");
        this.pasteMenuItem.setName("paste");
        this.closeMenuItem.setName("close");

        this.copyMenuItem.addActionListener(this);
        this.cutMenuItem.addActionListener(this);
        this.pasteMenuItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);
        
        this.copyMenuItem.setMnemonic(copyString.getMnemonic());
        this.cutMenuItem.setMnemonic(cutString.getMnemonic());
        this.pasteMenuItem.setMnemonic(pasteString.getMnemonic());
        this.closeMenuItem.setMnemonic(closeString.getMnemonic());
        
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
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("cut")) {

            this.parentWindow.getCurrentChatPanel().cut();            
        }
        else if (itemText.equalsIgnoreCase("copy")) {
            
            this.parentWindow.getCurrentChatPanel().copyWriteArea();
        }
        else if (itemText.equalsIgnoreCase("paste")) {

            this.parentWindow.getCurrentChatPanel().paste();
        }
        else if (itemText.equalsIgnoreCase("close")) {

            this.parentWindow.setVisible(false);
            this.parentWindow.dispose();

        }
    }
}
