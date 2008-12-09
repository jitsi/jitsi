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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
    
    private JMenuItem kickItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.KICK"));
    
    private JMenuItem banItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.BAN"));
    
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

        this.kickItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.KICK"));

        this.banItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.BAN"));

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
            GuiActivator.getResources()
                .getI18NString("service.gui.SPECIFY_REASON"));
        
        private JLabel reasonLabel = new JLabel(
            GuiActivator.getResources()
                .getI18NString("service.gui.REASON") + ":");
        
        private JTextField reasonField = new JTextField();
        
        private JButton okButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.OK"));
        
        private JButton cancelButton = new JButton(
            GuiActivator.getResources().getI18NString("service.gui.CANCEL"));
        
        private JPanel buttonsPanel
            = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        private JPanel titlePanel = new JPanel(new BorderLayout(10, 10));
        
        private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        
        private String operationType;
        
        ReasonDialog(String operation)
        {
            super(chatPanel.getChatWindow());
            
            this.setTitle(
                GuiActivator.getResources().getI18NString("service.gui.REASON"));
            
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
                        = (ChatRoomWrapper) chatPanel
                            .getChatSession().getDescriptor();

                    ChatRoom chatRoom = chatRoomWrapper.getChatRoom();

                    if(chatRoom == null)
                    {
                        new ErrorDialog(chatPanel.getChatWindow(),
                            GuiActivator.getResources()
                                .getI18NString("service.gui.ERROR"),
                            GuiActivator.getResources()
                                .getI18NString("service.gui.CHAT_ROOM_NOT_JOINED"))
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
                    
                    ReasonDialog.this.setVisible(false);
                }
            });
        
            cancelButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    ReasonDialog.this.setVisible(false);
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
                    (ChatRoomMember) chatContact
                        .getDescriptor(),
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
                            GuiActivator.getResources()
                                .getI18NString("service.gui.KICK_FAILED"),
                            GuiActivator.getResources().getI18NString(
                            "service.gui.KICK_FAILED_NOT_ENOUGH_PERMISSIONS",
                            new String[]{chatContact.getName()}),
                            e);

                    errorDialog.showDialog();
                }
                else if (e.getErrorCode()
                    == OperationFailedException.FORBIDDEN)
                {   
                    new ErrorDialog(chatPanel.getChatWindow(),
                        GuiActivator.getResources()
                            .getI18NString("service.gui.KICK_FAILED"),
                        GuiActivator.getResources()
                            .getI18NString("service.gui.KICK_FAILED_NOT_ALLOWED",
                            new String[]{chatContact.getName()}),
                            e).showDialog();
                }
                else
                {
                    new ErrorDialog(chatPanel.getChatWindow(),
                        GuiActivator.getResources()
                            .getI18NString("service.gui.KICK_FAILED"),
                        GuiActivator.getResources()
                            .getI18NString("service.gui.KICK_FAILED_GENERAL_ERROR",
                            new String[]{chatContact.getName()}),
                        e).showDialog();
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
                        .getDescriptor(),
                    reason);
            }
            catch (OperationFailedException e)
            {
                logger.error("Failed to ban participant.", e);

                String errorTitle = GuiActivator.getResources()
                    .getI18NString("service.gui.BAN_FAILED");

                if (e.getErrorCode()
                    == OperationFailedException.NOT_ENOUGH_PRIVILEGES)
                {
                    new ErrorDialog(chatPanel.getChatWindow(),
                        errorTitle,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.BAN_FAILED_NOT_ENOUGH_PERMISSIONS",
                            new String[]{chatContact.getName()}),
                        e).showDialog();
                }
                else if (e.getErrorCode()
                    == OperationFailedException.FORBIDDEN)
                {
                    new ErrorDialog(chatPanel.getChatWindow(),
                        errorTitle,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.BAN_FAILED_NOT_ALLOWED",
                            new String[]{chatContact.getName()}),
                        e).showDialog();
                }
                else
                {
                    new ErrorDialog(chatPanel.getChatWindow(),
                        errorTitle,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.BAN_FAILED_GENERAL_ERROR",
                            new String[]{chatContact.getName()}),
                        e).showDialog();
                }
            }
        }
    }
}
