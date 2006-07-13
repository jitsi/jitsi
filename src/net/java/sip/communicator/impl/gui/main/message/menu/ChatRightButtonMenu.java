/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.message.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * The <tt>ChatRightButtonMenu</tt> appears when the user makes a right button
 * click on the chat window conversation area (where sent and received messages
 * are displayed).
 *  
 * @author Yana Stamcheva
 */
public class ChatRightButtonMenu extends JPopupMenu
    implements ActionListener {

    private ChatWindow parentWindow;
    
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
    public ChatRightButtonMenu(ChatWindow parentWindow) {
        super();

        this.parentWindow = parentWindow;
        
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

        if (itemText.equalsIgnoreCase("copy")) {
            this.parentWindow.getCurrentChatPanel().copyConversation();
            
        } else if (itemText.equalsIgnoreCase("save")) {

        } else if (itemText.equalsIgnoreCase("print")) {

        } else if (itemText.equalsIgnoreCase("close")) {

            this.parentWindow.setVisible(false);
            this.parentWindow.dispose();

        }
    }
}
