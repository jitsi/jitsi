/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;


/**
 * The <tt>ChatContactRightButtonMenu</tt> is the menu, opened when user clicks
 * with the right mouse button on a contact in the list of contacts in the chat
 * window.
 *
 * @author Yana Stamcheva
 */
public class ChatContactRightButtonMenu
    extends JPopupMenu
    implements  ActionListener
{
    private Logger logger = Logger.getLogger(ChatContactRightButtonMenu.class);
    
    private I18NString kickString
        = Messages.getI18NString("kick");
    
    private I18NString banString
        = Messages.getI18NString("ban");

    private JMenuItem kickItem = new JMenuItem(
        kickString.getText());
    
    private JMenuItem banItem = new JMenuItem(
        banString.getText());
    
    private static String KICK_OPERATION = "Kick";
    
    private static String BAN_OPERATION = "Ban";
    
    private ChatPanel chatPanel;
    
    private ChatContact chatContact;

    /**
     * Creates an instance of <tt>ChatRoomsListRightButtonMenu</tt>.
     */
    public ChatContactRightButtonMenu(ChatPanel chatPanel,
            ChatContact chatContact)
    {
        this.chatPanel = chatPanel;
        
        this.chatContact = chatContact;
        
        this.setLocation(getLocation());

        this.init();
    }

    /**
     * Initializes the menu, by adding all containing menu items.
     */
    private void init()
    {
        this.add(kickItem);
        this.add(banItem);
                
        this.kickItem.setName("kick");
        this.banItem.setName("ban");
                
        this.kickItem
            .setMnemonic(kickString.getMnemonic());
        
        this.banItem
            .setMnemonic(banString.getMnemonic());
    
        this.kickItem.addActionListener(this);
        this.banItem.addActionListener(this);                
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Determines which menu item was
     * selected and makes the appropriate operations.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        
        if (itemName.equals("kick"))
        {
            new ReasonDialog(KICK_OPERATION).setVisible(true);
        }
        else if (itemName.equals("ban"))
        {
            new ReasonDialog(BAN_OPERATION).setVisible(true);
        }
    }
    
    private class ReasonDialog extends SIPCommDialog
    {
        private JLabel iconLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.REASON_DIALOG_ICON)));
        
        private JLabel infoLabel = new JLabel(
            Messages.getI18NString("specifyReason").getText());
        
        private JLabel reasonLabel = new JLabel(
            Messages.getI18NString("reason").getText() + ":");
        
        private JTextField reasonField = new JTextField();
        
        private JButton okButton = new JButton(
            Messages.getI18NString("ok").getText());
        
        private JButton cancelButton = new JButton(
            Messages.getI18NString("cancel").getText());
        
        private JPanel buttonsPanel
            = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
        private JPanel titlePanel = new JPanel(new BorderLayout(10, 10));
        
        private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        
        private String operationType;
        
        ReasonDialog(String operation)
        {
            super(chatPanel.getChatWindow());
            
            this.setTitle(Messages.getI18NString("reason").getText());
            
            this.operationType = operation;
            
            this.buttonsPanel.add(okButton);
            this.buttonsPanel.add(cancelButton);
         
            okButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    //This menu is shown only for chat rooms, so we're sure
                    // here that we deal with a multi user chat.
                    ChatRoomWrapper chatRoomWrapper
                        = (ChatRoomWrapper) chatPanel.getChatIdentifier();
                    
                    ChatRoom chatRoom = chatRoomWrapper.getChatRoom();
                    
                    if(chatRoom == null)
                    {   
                        new ErrorDialog(chatPanel.getChatWindow(),
                            Messages.getI18NString("chatRoomNotJoined")
                                .getText(),
                            Messages.getI18NString("error").getText())
                                .setVisible(true);
                        
                        return;
                    }
                    
                    if (operationType.equals(KICK_OPERATION))
                    {
                        new KickParticipantThread(chatRoom,
                            reasonField.getText()).start();
                    }
                    else if (operationType.equals(BAN_OPERATION))
                    {
                        new BanParticipantThread(chatRoom,
                            reasonField.getText()).start();
                    }
                    
                    ReasonDialog.this.dispose();
                }
            });
        
            cancelButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    ReasonDialog.this.dispose();
                }
            });
            
            this.infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD));
            
            this.titlePanel.add(iconLabel, BorderLayout.WEST);
            this.titlePanel.add(infoLabel, BorderLayout.CENTER);
            
            this.mainPanel.add(titlePanel, BorderLayout.NORTH);            
            this.mainPanel.add(reasonLabel, BorderLayout.WEST);
            this.mainPanel.add(reasonField, BorderLayout.CENTER);
            this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
            
            this.mainPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            this.getContentPane().add(mainPanel);
            
            this.pack();
        }

        protected void close(boolean isEscaped)
        {   
        }
    }
    
    /**
     * Kicks the the selected chat room participant or shows a message to the
     * user that he/she has not enough permissions to do a ban.
     */
    private class KickParticipantThread extends Thread
    {
        private ChatRoom chatRoom;
        
        private String reason;
        
        KickParticipantThread(ChatRoom chatRoom, String reason)
        {
            this.chatRoom = chatRoom;
        }
        
        public void run()
        {
            try
            {
                chatRoom.kickParticipant(
                    (ChatRoomMember)chatContact
                        .getSourceContact(),
                    reason);
            }
            catch (OperationFailedException e)
            {
                logger.error("Failed to kick participant.", e);
                
                if (e.getErrorCode()                
                    == OperationFailedException.NOT_ENOUGH_PRIVILEGES)
                {
                    ErrorDialog errorDialog
                        = new ErrorDialog(chatPanel.getChatWindow(),
                        Messages.getI18NString(
                            "kickFailedNotEnoughPermissions",
                            new String[]{chatContact.getName()})
                            .getText(),
                        e,
                        Messages.getI18NString("kickFailed")
                            .getText());
                    
                    errorDialog.showDialog();
                }
                else if (e.getErrorCode()                
                    == OperationFailedException.FORBIDDEN)
                {   
                    new ErrorDialog(chatPanel.getChatWindow(),
                        Messages.getI18NString("kickFailedNotAllowed",
                            new String[]{chatContact.getName()})
                            .getText(),
                            e,
                        Messages.getI18NString("kickFailed").getText())
                            .showDialog();
                }
                else
                {
                    new ErrorDialog(chatPanel.getChatWindow(),
                        Messages.getI18NString("kickFailedGeneralError",
                            new String[]{chatContact.getName()})
                            .getText(),
                        e,
                        Messages.getI18NString("kickFailed").getText())
                            .showDialog();
                }
            }
        }
    }
    
    /**
     * Bans the the selected chat room participant or shows a message to the
     * user that he/she has not enough permissions to do a ban.
     */
    private class BanParticipantThread extends Thread
    {
        private ChatRoom chatRoom;
        
        private String reason;
        
        BanParticipantThread(ChatRoom chatRoom, String reason)
        {
            this.chatRoom = chatRoom;
        }
        
        public void run()
        {
            try
            {
                chatRoom.banParticipant(
                    (ChatRoomMember)chatContact
                        .getSourceContact(),
                    reason);
            }
            catch (OperationFailedException e)
            {
                logger.error("Failed to ban participant.", e);
                
                if (e.getErrorCode()                
                    == OperationFailedException.NOT_ENOUGH_PRIVILEGES)
                {
                    new ErrorDialog(chatPanel.getChatWindow(),
                        Messages.getI18NString("banFailedNotEnoughPermissions",
                            new String[]{chatContact.getName()})
                            .getText(),
                        e,
                        Messages.getI18NString("banFailed").getText())
                            .showDialog();
                }
                else if (e.getErrorCode()                
                    == OperationFailedException.FORBIDDEN)
                {
                    new ErrorDialog(chatPanel.getChatWindow(),
                        Messages.getI18NString("banFailedNotAllowed",
                            new String[]{chatContact.getName()})
                            .getText(),
                        e,
                        Messages.getI18NString("banFailed").getText())
                            .showDialog();
                }
                else
                {
                    new ErrorDialog(chatPanel.getChatWindow(),
                        Messages.getI18NString("banFailedGeneralError",
                            new String[]{chatContact.getName()})
                            .getText(),
                        e,
                        Messages.getI18NString("banFailed").getText())
                            .showDialog();
                }
            }
        }
    }
}
