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

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;

/**
 * The <tt>ChatRoomsListPanel</tt> is the panel that contains the
 * <tt>ChatRoomsList</tt>. It is situated in the second tab in the main
 * application window.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomsListPanel
    extends JScrollPane
{
    private MainFrame mainFrame;

    private ChatRoomsList chatRoomsList;

    private JPanel treePanel = new JPanel(new BorderLayout());

    /**
     * Creates the scroll panel containing the chat rooms list.
     *
     * @param frame the main application frame
     */
    public ChatRoomsListPanel(MainFrame frame)
    {
        this.mainFrame = frame;

        this.chatRoomsList = new ChatRoomsList(mainFrame);

        this.treePanel.add(chatRoomsList, BorderLayout.NORTH);

        this.treePanel.setBackground(Color.WHITE);

        this.getViewport().add(treePanel);

        this.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.getVerticalScrollBar().setUnitIncrement(30);

        this.getActionMap().put("runChat", new ChatRoomsListPanelEnterAction());

        InputMap imap = this.getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "runChat");
    }

    /**
     * Returns the <tt>ChatRoomsList</tt> component contained in this panel.
     *
     * @return the <tt>ChatRoomsList</tt> component contained in this panel
     */
    public ChatRoomsList getChatRoomsList()
    {
        return chatRoomsList;
    }

    /**
     * Opens chat window when the selected value is a MetaContact and opens a
     * group when the selected value is a MetaContactGroup.
     */
    private class ChatRoomsListPanelEnterAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            Object selectedValue = chatRoomsList.getSelectedValue();

            if(selectedValue instanceof MultiUserChatServerWrapper)
            {
                MultiUserChatServerWrapper serverWrapper
                    = (MultiUserChatServerWrapper) selectedValue;

                ChatWindowManager chatWindowManager
                    = mainFrame.getChatWindowManager();

                ConferenceChatPanel chatPanel
                    = chatWindowManager.getMultiChat(
                        serverWrapper.getSystemRoomWrapper());

                chatWindowManager.openChat(chatPanel, true);
            }
            else if(selectedValue instanceof ChatRoomWrapper)
            {
                ChatRoomWrapper chatRoomWrapper
                    = (ChatRoomWrapper) selectedValue;

                ChatWindowManager chatWindowManager
                    = mainFrame.getChatWindowManager();

                ConferenceChatPanel chatPanel
                    = chatWindowManager.getMultiChat(chatRoomWrapper);

                chatWindowManager.openChat(chatPanel, true);
            }
        }
    }

}
