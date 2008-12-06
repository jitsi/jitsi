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

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.createforms.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms.*;
import net.java.sip.communicator.swing.*;

/**
 * The dialog containing a list of all chat rooms ever joined by the user and
 * also interface for create a new chat room, join a chat room, search all
 * chat rooms, etc.
 * 
 * @author Yana Stamcheva
 */
public class ChatRoomListDialog
    extends SIPCommDialog
    implements  ActionListener
{
    private static final String CREATE_CHAT_ROOM = "CreateChatRoom";

    private static final String JOIN_CHAT_ROOM = "JoinChatRoom";

    private static final String CANCEL = "Cancel";

    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>MyChatRoomsDialog</tt> by specifying the
     * parent window.
     * 
     * @param parentWindow the parent window of this dialog
     */
    public ChatRoomListDialog(MainFrame parentWindow)
    {
        super(parentWindow);

        this.mainFrame = parentWindow;

        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.init();
    }

    /**
     * Initializes this chat room dialog.
     */
    private void init()
    {
        ChatRoomListUI chatRoomsListUI = new ChatRoomListUI(this);

        I18NString createChatRoomString
            = Messages.getI18NString("createChatRoom");

        I18NString joinChatRoomString
            = Messages.getI18NString("joinChatRoom");

        I18NString cancelString
            = Messages.getI18NString("cancel");

        JButton createChatRoomButton = new JButton(
            createChatRoomString.getText());

        JButton joinChatRoomButton = new JButton(
            joinChatRoomString.getText());

        JButton cancelButton = new JButton(cancelString.getText());

        TransparentPanel buttonPanel = new TransparentPanel();

        this.setTitle(Messages.getI18NString("myChatRooms").getText());

        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        this.getContentPane().add(chatRoomsListUI, BorderLayout.CENTER);

        buttonPanel.add(joinChatRoomButton);
        buttonPanel.add(createChatRoomButton);
        buttonPanel.add(cancelButton);

        createChatRoomButton.addActionListener(this);
        joinChatRoomButton.addActionListener(this);
        cancelButton.addActionListener(this);

        createChatRoomButton.setName(CREATE_CHAT_ROOM);
        joinChatRoomButton.setName(JOIN_CHAT_ROOM);
        cancelButton.setName(CANCEL);

        createChatRoomButton
            .setMnemonic(createChatRoomString.getMnemonic());
        joinChatRoomButton
            .setMnemonic(joinChatRoomString.getMnemonic());
        cancelButton
            .setMnemonic(cancelString.getMnemonic());
    }

    /**
     * Handles <tt>ActionEvent</tt>s triggered by a button click.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton sourceButton = (JButton) e.getSource();
        String buttonName = sourceButton.getName();

        if (buttonName.equals(CREATE_CHAT_ROOM))
        {
            CreateChatRoomWizard createChatRoomWizard
                = new CreateChatRoomWizard(mainFrame);

            createChatRoomWizard.showDialog(false);
        }
        else if (buttonName.equals(JOIN_CHAT_ROOM))
        {
            JoinChatRoomWizard joinChatRoomWizard
                = new JoinChatRoomWizard(mainFrame);

            joinChatRoomWizard.showDialog(false);
        }
        else if (buttonName.equals(CANCEL))
        {
            this.dispose();
        }
    }

    @Override
    protected void close(boolean isEscaped)
    {
    }
}
