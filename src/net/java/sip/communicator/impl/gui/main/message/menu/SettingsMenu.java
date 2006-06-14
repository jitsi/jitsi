package net.java.sip.communicator.impl.gui.main.message.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;

public class SettingsMenu extends JMenu
    implements ActionListener {
    
    private JCheckBoxMenuItem typingNotificationsItem 
        = new JCheckBoxMenuItem(Messages.getString("typingNotifications"), true);
    
    private ChatWindow chatWindow;
    
    public SettingsMenu(ChatWindow chatWindow){
        super(Messages.getString("settings"));
        
        this.chatWindow = chatWindow;
        
        this.init();
    }
    
    private void init(){
        this.add(typingNotificationsItem);
        
        this.typingNotificationsItem.addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
        
        if (item.isSelected()) {
            chatWindow.getCurrentChatPanel().enableTypingNotification(true);
        }
        else {
            chatWindow.getCurrentChatPanel().enableTypingNotification(false);
        }   
    }
}
