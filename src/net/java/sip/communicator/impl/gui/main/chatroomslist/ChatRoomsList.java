/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.authorization.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ChatRoomsList</tt> is the list containing all chat rooms.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomsList
    extends JList
    implements  ListSelectionListener,
                MouseListener
{
    private Logger logger = Logger.getLogger(ChatRoomsList.class);

    private MainFrame mainFrame;

    private DefaultListModel listModel = new DefaultListModel();

    /**
     * Creates an instance of the <tt>ChatRoomsList</tt>.
     *
     * @param mainFrame The main application window.
     */
    public ChatRoomsList(MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.setModel(listModel);
        this.setCellRenderer(new ChatRoomsListCellRenderer());
        this.addListSelectionListener(this);

        this.addMouseListener(this);
    }

    /**
     * Adds a chat server and all its existing chat rooms.
     *
     * @param pps the <tt>ProtocolProviderService</tt> corresponding to the chat
     * server
     * @param multiUserChatOperationSet the <tt>OperationSetMultiUserChat</tt>
     * from which we manage chat rooms
     */
    public void addChatServer(ProtocolProviderService pps,
        OperationSetMultiUserChat multiUserChatOperationSet)
    {
        listModel.addElement(pps);
    }


    /**
     * Adds a chat room to this list.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to add
     */
    public void addChatRoom(ChatRoom chatRoom)
    {
        listModel.addElement(chatRoom);
    }

    /**
     * Adds a chat room to this list.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to add
     * @param parentProvider the <tt>ProtocolProviderService</tt>, which is the
     * parent of the given <tt>ChatRoom</tt>.
     */
    public void addChatRoom(ChatRoom chatRoom,
            ProtocolProviderService parentProvider)
    {
        int parentIndex = listModel.indexOf(parentProvider);

        if(parentIndex != -1)
            listModel.add(parentIndex + 1, chatRoom);
    }

    /**
     * Verifies if the given <tt>ChatRoom</tt> is contained in the list.
     *
     * @param chatRoom the <tt>ChatRoom</tt> to search.
     * @return TRUE if the given <tt>ChatRoom</tt> is contained in the list,
     * FALSE - otherwise.
     */
    public boolean containsChatRoom(ChatRoom chatRoom)
    {
        return listModel.contains(chatRoom);
    }

    /**
     * Determines if the chat server is closed.
     *
     * @param pps the protocol provider service that we'll be checking
     * @return true if the chat server is closed and false otherwise.
     */
    public boolean isChatServerClosed(ProtocolProviderService pps)
    {
        return false;
    }

    /**
     * A chat room was selected. Opens the chat room in the chat window.
     *
     * @param evt a <tt>ListSelectionEvent</tt> instance containing details of
     * the event that has just occurred.
     */
    public void valueChanged(ListSelectionEvent evt)
    {
        Object obj = this.getSelectedValue();

        if(obj instanceof ChatRoom)
        {
            ChatRoom chatRoom = (ChatRoom) obj;
            ChatWindowManager chatWindowManager
                = mainFrame.getChatWindowManager();

            ChatPanel chatPanel = chatWindowManager.getChatRoom(chatRoom);

            chatWindowManager.openChat(chatPanel, true);
        }
    }

    public void mouseClicked(MouseEvent evt)
    {}

    public void mouseEntered(MouseEvent evt)
    {}

    public void mouseExited(MouseEvent evt)
    {}

    public void mousePressed(MouseEvent evt)
    {
        // Select the contact under the right button click.
        if ((evt.getModifiers() & InputEvent.BUTTON2_MASK) != 0
            || (evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0
            || (evt.isControlDown() && !evt.isMetaDown()))
        {
            this.setSelectedIndex(locationToIndex(evt.getPoint()));
        }

        Object selectedValue = this.getSelectedValue();

        if(selectedValue instanceof ProtocolProviderService)
        {
            ProtocolProviderService pps
                = (ProtocolProviderService) selectedValue;

            if ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
            {
                ChatRoomServerRightButtonMenu rightButtonMenu
                    = new ChatRoomServerRightButtonMenu(mainFrame, pps);

                rightButtonMenu.setInvoker(this);

                rightButtonMenu.setLocation(evt.getX()
                        + mainFrame.getX() + 5, evt.getY() + mainFrame.getY()
                        + 105);

                rightButtonMenu.setVisible(true);
            }
        }
    }

    public void mouseReleased(MouseEvent evt)
    {}

    public ChatRoom getChatRoomFromList(String chatRoomName)
    {
        for(int i=0; i<listModel.getSize(); i++)
        {
            Object o = listModel.getElementAt(i);

            if(o instanceof ChatRoom)
            {
                if((((ChatRoom) o).getName()).equalsIgnoreCase(chatRoomName))
                    return (ChatRoom)o;
            }
        }
        return null;
    }
}
