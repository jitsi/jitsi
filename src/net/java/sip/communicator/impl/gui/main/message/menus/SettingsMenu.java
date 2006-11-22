package net.java.sip.communicator.impl.gui.main.message.menus;

import java.awt.event.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.message.*;
import net.java.sip.communicator.service.configuration.*;

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
                Messages.getString("enableTypingNotifications"));
        
    private JCheckBoxMenuItem sendingMessageCommandItem 
        = new JCheckBoxMenuItem(
                Messages.getString("useCtrlEnterToSend"));
    
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
        
        typingNotificationsItem.setName("typingNotifications");
        sendingMessageCommandItem.setName("sendingMessageCommand");
        
        this.setMnemonic(Messages.getString("mnemonic.chatSettings").charAt(0));
        this.typingNotificationsItem.setMnemonic(
                Messages.getString("mnemonic.typingNotifications").charAt(0));
        
        
        ConfigurationService configService
            = GuiActivator.getConfigurationService();
        
        String messageCommand = configService.getString(
                "net.java.sip.communicator.impl.ui.sendMessageCommand");
        
        if(messageCommand == null || messageCommand.equalsIgnoreCase("enter"))
            this.sendingMessageCommandItem.setSelected(false);
        else
            this.sendingMessageCommandItem.setSelected(true);
    
        this.init();
    }
    
    /**
     * Initializes this menu by adding all menu items.
     */
    private void init(){
        this.add(typingNotificationsItem);
        this.add(sendingMessageCommandItem);
        
        this.typingNotificationsItem.addActionListener(this);
        this.sendingMessageCommandItem.addActionListener(this);
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e) {
        JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
        
        if(item.getName().equals("typingNotifications")) {
            
            if (item.isSelected()) {
                chatWindow.enableTypingNotification(true);
            }
            else {
                chatWindow.enableTypingNotification(false);
            }
        }
        else if(item.getName().equals("sendingMessageCommand")) {
            
            chatWindow.changeSendCommand(!item.isSelected());
        }
    }
}
