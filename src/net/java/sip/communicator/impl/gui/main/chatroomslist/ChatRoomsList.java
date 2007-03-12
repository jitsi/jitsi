/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.awt.*;
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
    implements  ListSelectionListener
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
}
