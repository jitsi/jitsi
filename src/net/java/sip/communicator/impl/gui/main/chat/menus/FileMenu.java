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
import net.java.sip.communicator.util.swing.*;

/**
 * The <tt>FileMenu</tt> is the menu in the chat window menu bar that contains
 * save, print and close.
 * 
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class FileMenu
    extends SIPCommMenu
    implements  ActionListener,
                Skinnable
{
    private JMenuItem myChatRoomsItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.MY_CHAT_ROOMS"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CHAT_ROOM_16x16_ICON)));

    private JMenuItem historyItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.HISTORY"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.HISTORY_ICON)));

    private JMenuItem closeMenuItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.CLOSE"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.CLOSE_ICON)));

    private ChatWindow parentWindow;

    /**
     * Creates an instance of <tt>FileMenu</tt>.
     * @param parentWindow The parent <tt>ChatWindow</tt>.
     */
    public FileMenu(ChatWindow parentWindow)
    {
        super(GuiActivator.getResources().getI18NString("service.gui.FILE"));

        this.parentWindow = parentWindow;

        this.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.FILE"));

        this.add(myChatRoomsItem);
        this.add(historyItem);

        this.addSeparator();

        this.add(closeMenuItem);

        this.myChatRoomsItem.setName("myChatRooms");
        this.historyItem.setName("history");
        this.closeMenuItem.setName("close");

        this.myChatRoomsItem.addActionListener(this);
        this.historyItem.addActionListener(this);
        this.closeMenuItem.addActionListener(this);

        this.myChatRoomsItem.setMnemonic(
            GuiActivator.getResources()
                .getI18nMnemonic("service.gui.MY_CHAT_ROOMS"));
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
    public void actionPerformed(ActionEvent e)
    {
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

            ChatPanel chatPanel = this.parentWindow.getCurrentChat();
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
            this.parentWindow.setVisible(false);
            this.parentWindow.dispose();
        }
    }

    /**
     * Reloads menu icons.
     */
    public void loadSkin()
    {
        myChatRoomsItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CHAT_ROOM_16x16_ICON)));

        historyItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.HISTORY_ICON)));

        closeMenuItem.setIcon(new ImageIcon(
                ImageLoader.getImage(ImageLoader.CLOSE_ICON)));
    }
}
