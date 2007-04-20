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

    private ChatRoomServerRightButtonMenu rightButtonMenu;
        
    /**
     * Creates the scroll panel containing the chat rooms list.
     * 
     * @param mainFrame the main application frame
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
}
