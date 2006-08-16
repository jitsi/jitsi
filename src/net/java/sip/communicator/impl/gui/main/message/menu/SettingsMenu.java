package net.java.sip.communicator.impl.gui.main.message.menu;

import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.message.*;

/**
 * The <tt>SettingsMenu</tt> is the menu in the chat window menu bar where
 * user could configure its chat window. At this moment there is only one
 * thing that could be configured from here - the typing notifications. The
 * user could turn on/of the typing notifications of the current
 * <tt>ChatPanel</tt> in the <tt>ChatWindow</tt>.
 * 
 * @author Yana Stamcheva
 */
public class SettingsMenu extends JMenu
    implements ActionListener {
    
    private JCheckBoxMenuItem typingNotificationsItem 
        = new JCheckBoxMenuItem(
                Messages.getString("typingNotifications"), true);
    
    private ChatWindow chatWindow;
    
    /**
     * Creates an instance of <tt>SettingsMenu</tt> by specifying the
     * <tt>ChatWindow</tt>.
     * 
     * @param chatWindow The <tt>ChatWindow</tt>.
     */
    public SettingsMenu(ChatWindow chatWindow){
        super(Messages.getString("settings"));
        
        this.chatWindow = chatWindow;
        
        this.init();
    }
    
    /**
     * Initializes this menu by adding all menu items.
     */
    private void init(){
        this.add(typingNotificationsItem);
        
        this.typingNotificationsItem.addActionListener(this);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {
        JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
        
        if (item.isSelected()) {
            chatWindow.enableTypingNotification(true);
        }
        else {
            chatWindow.enableTypingNotification(false);
        }   
    }
}
