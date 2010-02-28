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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.createforms.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The dialog containing a list of all chat rooms ever joined by the user and
 * also interface for create a new chat room, join a chat room, search all
 * chat rooms, etc.
 * 
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ChatRoomListDialog
    extends SIPCommDialog
    implements  ActionListener
{

    /**
     * An eclipse generated serial version uid.
     */
    private static final long serialVersionUID = -5117526914795659109L;

    /**
     * The key for the string containing the name of the Create button.
     */
    private static final String CREATE_CHAT_ROOM 
        = "service.gui.CREATE_CHAT_ROOM";

    /**
     * The key for the string containing the name of the Join button.
     */
    private static final String JOIN_CHAT_ROOM = "service.gui.JOIN_CHAT_ROOM";

    /**
     * The key for the string containing the name of the "My Chat Rooms" title.
     */
    private static final String MY_CHAT_ROOMS = "service.gui.MY_CHAT_ROOMS";
    
    /**
     * The global/shared <code>ChatRoomListDialog</code> currently showing.
     */
    private static ChatRoomListDialog chatRoomListDialog;

    /**
     * Shows a <code>ChatRoomListDialog</code> creating it first if necessary.
     * The shown instance is shared in order to prevent displaying multiple
     * instances of one and the same <code>ChatRoomListDialog</code>.
     */
    public static void showChatRoomListDialog()
    {
        if (chatRoomListDialog == null)
        {
            chatRoomListDialog
                = new ChatRoomListDialog(
                        GuiActivator.getUIService().getMainFrame());
            chatRoomListDialog.setPreferredSize(new Dimension(500, 400));

            /*
             * When the global/shared ChatRoomListDialog closes, don't keep a
             * reference to it and let it be garbage-collected.
             */
            chatRoomListDialog.addWindowListener(new WindowAdapter()
            {
                public void windowClosed(WindowEvent e)
                {
                    if (chatRoomListDialog == e.getWindow())
                        chatRoomListDialog = null;
                }
            });
        }
        chatRoomListDialog.setVisible(true);
    }

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

        JButton createChatRoomButton = new JButton(
            GuiActivator.getResources()
                .getI18NString(CREATE_CHAT_ROOM));

        JButton joinChatRoomButton = new JButton(
            GuiActivator.getResources()
                .getI18NString(JOIN_CHAT_ROOM));

        TransparentPanel buttonPanel = new TransparentPanel();

        this.setTitle(GuiActivator.getResources()
            .getI18NString(MY_CHAT_ROOMS));

        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        this.getContentPane().add(chatRoomsListUI, BorderLayout.CENTER);

        buttonPanel.add(joinChatRoomButton);
        buttonPanel.add(createChatRoomButton);

        createChatRoomButton.addActionListener(this);
        joinChatRoomButton.addActionListener(this);

        createChatRoomButton.setName(CREATE_CHAT_ROOM);
        joinChatRoomButton.setName(JOIN_CHAT_ROOM);

        createChatRoomButton.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic(CREATE_CHAT_ROOM));
        joinChatRoomButton.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic(JOIN_CHAT_ROOM));
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
            new JoinChatRoomWindow();
        }
    }

    @Override
    protected void close(boolean isEscaped)
    {
    }
}
