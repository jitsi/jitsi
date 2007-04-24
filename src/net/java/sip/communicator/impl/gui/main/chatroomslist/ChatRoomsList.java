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
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.service.protocol.*;
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

        try
        {
            List existingChatRooms
                = multiUserChatOperationSet.getExistingChatRooms();

            if(existingChatRooms == null)
                return;

            Iterator i = existingChatRooms.iterator();

            while(i.hasNext())
            {
                ChatRoom chatRoom = (ChatRoom) i.next();

                listModel.addElement(chatRoom);
            }

        }
        catch (OperationNotSupportedException ex)
        {
            logger.error(
                "Failed to obtain existing chat rooms for the following server: "
                + pps.getAccountID().getService(), ex);
        }
        catch (OperationFailedException ex)
        {
            logger.error(
                "Failed to obtain existing chat rooms for the following server: "
                + pps.getAccountID().getService(), ex);
        }

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
     *
     * @param pps
     * @return
     */
    public boolean isChatServerClosed(ProtocolProviderService pps)
    {
        return false;
    }

    /**
     * A chat room was selected. Opens the chat room in the chat window.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        Object o = this.getSelectedValue();

        if(o instanceof ChatRoom)
        {
            ChatRoom chatRoom = (ChatRoom) o;
            ChatWindowManager chatWindowManager = mainFrame.getChatWindowManager();

            ChatPanel chatPanel = chatWindowManager.getChatRoom(chatRoom);

            chatWindowManager.openChat(chatPanel, true);
        }
    }

    public void mouseClicked(MouseEvent e)
    {}

    public void mouseEntered(MouseEvent e)
    {}

    public void mouseExited(MouseEvent e)
    {}

    public void mousePressed(MouseEvent e)
    {
        // Select the contact under the right button click.
        if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0
            || (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
            || (e.isControlDown() && !e.isMetaDown()))
        {
            this.setSelectedIndex(locationToIndex(e.getPoint()));
        }

        Object selectedValue = this.getSelectedValue();

        if(selectedValue instanceof ProtocolProviderService)
        {
            ProtocolProviderService pps
                = (ProtocolProviderService) selectedValue;

            if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
            {
                ChatRoomServerRightButtonMenu rightButtonMenu
                    = new ChatRoomServerRightButtonMenu(mainFrame, pps);

                rightButtonMenu.setInvoker(this);

                rightButtonMenu.setLocation(e.getX()
                        + mainFrame.getX() + 5, e.getY() + mainFrame.getY()
                        + 105);

                rightButtonMenu.setVisible(true);
            }
        }
    }

    public void mouseReleased(MouseEvent e)
    {}
    
    public ChatRoom getChatRoomFromList(String chatRoomName)
    {
        for(int i=0; i<listModel.getSize(); i++)
        {
            Object o = listModel.getElementAt(i);
           
            if(o instanceof ChatRoom)
            {
                if((((ChatRoom) o).getName()).equalsIgnoreCase(chatRoomName))
                    return ((ChatRoom)(o));
            }
        }
        return null;
    }
}
