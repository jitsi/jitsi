package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>SettingsMenu</tt> is the menu in the chat window menu bar where
 * user could configure its chat window. At this moment there is only one
 * thing that could be configured from here - the typing notifications. The
 * user could turn on/of the typing notifications of the current
 * <tt>ChatPanel</tt> in the <tt>ChatWindow</tt>.
 * 
 * @author Yana Stamcheva
 */
public class SettingsMenu
    extends SIPCommMenu
    implements  ActionListener
{
    
    private I18NString typingNotifString
        = Messages.getI18NString("enableTypingNotifications");
    
    private I18NString useCtrlEnterString
        = Messages.getI18NString("useCtrlEnterToSend");
    
    private I18NString autoPopupString
        = Messages.getI18NString("autoPopup");

    private JCheckBoxMenuItem typingNotificationsItem 
        = new JCheckBoxMenuItem(typingNotifString.getText());
        
    private JCheckBoxMenuItem sendingMessageCommandItem 
        = new JCheckBoxMenuItem(useCtrlEnterString.getText());
    
    private JCheckBoxMenuItem autoPopupItem 
        = new JCheckBoxMenuItem(autoPopupString.getText());
    
    private ChatWindow chatWindow;
    
    /**
     * Creates an instance of <tt>SettingsMenu</tt> by specifying the
     * <tt>ChatWindow</tt>.
     * 
     * @param chatWindow The <tt>ChatWindow</tt>.
     */
    public SettingsMenu(ChatWindow chatWindow)
    {
        super(Messages.getI18NString("settings").getText());
        
        this.chatWindow = chatWindow;
        
        typingNotificationsItem.setName("typingNotifications");
        sendingMessageCommandItem.setName("sendingMessageCommand");
        autoPopupItem.setName("autopopup");
        
        this.setMnemonic(Messages.getI18NString("settings").getMnemonic());
        
        this.typingNotificationsItem.setMnemonic(
            typingNotifString.getMnemonic());
        
        this.sendingMessageCommandItem.setMnemonic(
            useCtrlEnterString.getMnemonic());
        
        this.autoPopupItem.setMnemonic(
            autoPopupString.getMnemonic());
                
        this.add(typingNotificationsItem);
        this.add(sendingMessageCommandItem);
        this.add(autoPopupItem);
        
        this.typingNotificationsItem.addActionListener(this);
        this.sendingMessageCommandItem.addActionListener(this);
        this.autoPopupItem.addActionListener(this);
        
        this.autoPopupItem.setSelected(
                ConfigurationManager.isAutoPopupNewMessage());
        
        this.typingNotificationsItem.setSelected(
                ConfigurationManager.isSendTypingNotifications());
        
        if(ConfigurationManager.getSendMessageCommand()
                == ConfigurationManager.ENTER_COMMAND)   
            this.sendingMessageCommandItem.setSelected(true);
        else
            this.sendingMessageCommandItem.setSelected(false);
        
    }
    
    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     */
    public void actionPerformed(ActionEvent e)
    {
        JCheckBoxMenuItem item = (JCheckBoxMenuItem)e.getSource();
        
        if (item.getName().equals("typingNotifications"))
        {       
            ConfigurationManager.setSendTypingNotifications(item.isSelected());
        }
        else if (item.getName().equals("sendingMessageCommand"))
        {   
            chatWindow.getCurrentChatPanel().getChatWritePanel()
                .changeSendCommand(!item.isSelected());
            
            if(!item.isSelected())
                ConfigurationManager.setSendMessageCommand(
                        ConfigurationManager.ENTER_COMMAND);
            else
                ConfigurationManager.setSendMessageCommand(
                        ConfigurationManager.ENTER_COMMAND);
        }
        else if (item.getName().equals("autopopup"))
        {
            ConfigurationManager.setAutoPopupNewMessage(item.isSelected());
        }
    }
}
