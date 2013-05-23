/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.menus;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.history.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>FileMenu</tt> is the menu in the chat window menu bar that contains
 * my chat rooms, history and close.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ChatFileMenu
    extends net.java.sip.communicator.impl.gui.main.menus.FileMenu
    implements  ActionListener,
                Skinnable
{
    private JMenuItem historyItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.HISTORY"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.HISTORY_16x16_ICON)));

    private JMenuItem closeMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.CLOSE"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSE_ICON)));

    private ChatWindow chatWindow;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     *
     * @param chatWindow The parent <tt>ChatWindow</tt>.
     */
    public ChatFileMenu(ChatWindow chatWindow)
    {
        super(chatWindow, true);

        this.chatWindow = chatWindow;

        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.FILE"));

        this.add(historyItem);

        this.addSeparator();

        this.add(closeMenuItem);

        this.historyItem.setName("history");
        this.closeMenuItem.setName("close");

        this.historyItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);

        this.historyItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.HISTORY"));
        this.closeMenuItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CLOSE"));
    }

    /**
     * Handles the <tt>ActionEvent</tt> when one of the menu items is selected.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        super.actionPerformed(e);

        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemText = menuItem.getName();

        if (itemText.equalsIgnoreCase("myChatRooms"))
        {
            ChatRoomTableDialog.showChatRoomTableDialog();
        }
        else if (itemText.equals("history"))
        {
            HistoryWindow history;

            HistoryWindowManager historyWindowManager
                = GuiActivator.getUIService().getHistoryWindowManager();

            ChatPanel chatPanel = this.chatWindow.getCurrentChat();
            ChatSession chatSession = chatPanel.getChatSession();

            if(historyWindowManager
                .containsHistoryWindowForContact(chatSession.getDescriptor()))
            {
                history = historyWindowManager.getHistoryWindowForContact(
                    chatSession.getDescriptor());

                if(history.getState() == JFrame.ICONIFIED)
                    history.setState(JFrame.NORMAL);

                history.toFront();
            }
            else
            {
                history = new HistoryWindow(
                    chatPanel.getChatSession().getDescriptor());

                history.setVisible(true);

                historyWindowManager.addHistoryWindowForContact(
                    chatSession.getDescriptor(), history);
            }
        }
        else if (itemText.equalsIgnoreCase("close"))
        {
            this.chatWindow.setVisible(false);
            this.chatWindow.dispose();
        }
    }

    /**
     * Reloads menu icons.
     */
    @Override
    public void loadSkin()
    {
        super.loadSkin();

        if (historyItem != null)
            historyItem.setIcon(new ImageIcon(
                    ImageLoader.getImage(ImageLoader.HISTORY_ICON)));

        if (closeMenuItem != null)
            closeMenuItem.setIcon(new ImageIcon(
                    ImageLoader.getImage(ImageLoader.CLOSE_ICON)));
    }
}
