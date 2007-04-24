/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

public class JoinChannelDialog
    extends SIPCommDialog
    implements ActionListener
{
    private static final Logger logger
        = Logger.getLogger(JoinChannelDialog.class);

    private JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

    private JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private JButton channelListButton
        = new JButton(Messages.getI18NString("showChannelsList").getText());

    private JLabel channelLabel
        = new JLabel(Messages.getI18NString("joinChatRoom").getText() + ":");

    private JTextField channelTextField = new JTextField();

    private JButton okButton
        = new JButton(Messages.getI18NString("ok").getText());

    private JButton cancelButton
        = new JButton(Messages.getI18NString("cancel").getText());

    private MainFrame mainFrame;

    private ProtocolProviderService protocolProvider;

    public JoinChannelDialog (MainFrame mainFrame,
            ProtocolProviderService protocolProvider)
    {
        super(mainFrame);

        this.setSize(400, 300);

        this.mainFrame = mainFrame;

        this.protocolProvider = protocolProvider;

        this.getContentPane().add(mainPanel, BorderLayout.CENTER);

        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

        buttonsPanel.add(okButton);

        buttonsPanel.add(cancelButton);

        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        mainPanel.add(centerPanel, BorderLayout.NORTH);

        mainPanel.add(southPanel, BorderLayout.CENTER);

        centerPanel.add(channelLabel, BorderLayout.WEST);

        centerPanel.add(channelTextField, BorderLayout.CENTER);

        southPanel.add(channelListButton);

        channelTextField.setText("#");

        okButton.setName("ok");
        cancelButton.setName("cancel");
        channelListButton.setName("channelList");

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);
        channelListButton.addActionListener(this);
    }

    protected void close(boolean isEscaped)
    {
    }

    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();

        if(buttonName.equals("ok"))
        {
            OperationSetMultiUserChat multiChatOpSet
                = (OperationSetMultiUserChat) protocolProvider.getOperationSet(
                        OperationSetMultiUserChat.class);

            String chatRoomName = channelTextField.getText();

            ChatRoom chatRoom = null;
            try
            {
                chatRoom = multiChatOpSet.findRoom(chatRoomName);
            }
            catch (Exception ex)
            {
                logger.error("Failed to find chat room.", ex);
                
                new ErrorDialog(mainFrame,
                    Messages.getI18NString(
                        "findChatRoomError",
                        new String[]{chatRoomName}).getText(),
                        ex,
                    Messages.getI18NString(
                        "error").getText())
                        .showDialog();
            }

            if(chatRoom != null)
            {
                try
                {
                    chatRoom.join();

                    ChatRoomsList chatRoomsList
                        = mainFrame.getChatRoomsListPanel().getChatRoomsList();
                    
                    if(!chatRoomsList.containsChatRoom(chatRoom))
                        mainFrame.getChatRoomsListPanel().getChatRoomsList()
                            .addChatRoom(chatRoom);
                }
                catch (OperationFailedException ex)
                {
                    new ErrorDialog(mainFrame,
                            Messages.getI18NString("failedToJoinChannel",
                                    new String[]{chatRoomName}).getText(),
                            ex,
                            Messages.getI18NString("error").getText())
                            .showDialog();
                }
            }
            else
            {
                new ErrorDialog(mainFrame,
                        Messages.getI18NString("chatRoomNotExist",
                                new String[]{chatRoomName,
                                protocolProvider.getAccountID().getService()})
                            .getText(),
                        Messages.getI18NString("error").getText()).showDialog();
            }
        }
        else if(buttonName.equals("cancel"))
        {
            this.dispose();
        }
        else if(buttonName.equals("channelList"))
        {

        }
    }
}

