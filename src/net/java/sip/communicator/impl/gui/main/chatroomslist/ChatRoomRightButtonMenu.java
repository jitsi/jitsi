/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>ChatRoomsListRightButtonMenu</tt> is the menu, opened when user clicks
 * with the right mouse button on the chat rooms list panel. It's the one that
 * contains the create chat room item.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomRightButtonMenu
    extends JPopupMenu
    implements  ActionListener
{
    private JMenuItem leaveChatRoomItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.LEAVE"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.LEAVE_ICON)));

    private JMenuItem joinChatRoomItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.JOIN"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.JOIN_ICON)));

    private JMenuItem joinAsChatRoomItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.JOIN_AS"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.JOIN_AS_ICON)));

    private JMenuItem removeChatRoomItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.REMOVE"),
        new ImageIcon(ImageLoader.getImage(ImageLoader.DELETE_16x16_ICON)));

    private ChatRoomWrapper chatRoomWrapper = null;

    /**
     * Creates an instance of <tt>ChatRoomsListRightButtonMenu</tt>.
     * @param chatRoomWrapper the chat room wrapper, corresponding to the
     * selected chat room
     */
    public ChatRoomRightButtonMenu(ChatRoomWrapper chatRoomWrapper)
    {
        super();

        this.chatRoomWrapper = chatRoomWrapper;

        this.setLocation(getLocation());

        this.init();
    }

    /**
     * Initializes the menu, by adding all containing menu items.
     */
    private void init()
    {
        this.add(joinChatRoomItem);
        this.add(joinAsChatRoomItem);
        this.add(leaveChatRoomItem);
        this.add(removeChatRoomItem);

        this.joinChatRoomItem.setName("joinChatRoom");
        this.joinAsChatRoomItem.setName("joinAsChatRoom");
        this.leaveChatRoomItem.setName("leaveChatRoom");
        this.removeChatRoomItem.setName("removeChatRoom");

        this.joinChatRoomItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.JOIN"));

        this.joinAsChatRoomItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.JOIN_AS"));

        this.leaveChatRoomItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.LEAVE"));

        this.removeChatRoomItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.REMOVE"));

        this.joinChatRoomItem.addActionListener(this);
        this.joinAsChatRoomItem.addActionListener(this);
        this.leaveChatRoomItem.addActionListener(this);
        this.removeChatRoomItem.addActionListener(this);

        if (chatRoomWrapper.getChatRoom() != null
            && chatRoomWrapper.getChatRoom().isJoined())
        {
            this.joinAsChatRoomItem.setEnabled(false);
            this.joinChatRoomItem.setEnabled(false);
        }
        else
            this.leaveChatRoomItem.setEnabled(false);
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Determines which menu item was
     * selected and makes the appropriate operations.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        ConferenceChatManager conferenceManager
            = GuiActivator.getUIService().getConferenceChatManager();

        if (itemName.equals("removeChatRoom"))
        {
            conferenceManager.removeChatRoom(chatRoomWrapper);

        }
        else if (itemName.equals("leaveChatRoom"))
        {
            conferenceManager.leaveChatRoom(chatRoomWrapper);
        }
        else if (itemName.equals("joinChatRoom"))
        {
            conferenceManager.joinChatRoom(chatRoomWrapper);
        }
        else if(itemName.equals("joinAsChatRoom"))
        {
            ChatRoomAuthenticationWindow authWindow
                = new ChatRoomAuthenticationWindow(chatRoomWrapper);

            authWindow.setVisible(true);
        }
    }
}
