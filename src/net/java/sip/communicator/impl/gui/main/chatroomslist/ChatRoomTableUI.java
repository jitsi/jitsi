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

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;
import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>ChatRoomTableUI</tt> is the panel that contains the
 * <tt>ChatRoomsList</tt>.
 *
 * @author Damian Minkov
 */
public class ChatRoomTableUI
    extends SCScrollPane
    implements MouseListener
{
    /**
     * The table with available rooms.
     */
    private JTable chatRoomList = new JTable();

    /**
     * The model of the table with the available rooms.
     */
    private ChatRoomTableModel chatRoomsTableModel = null;

    /**
     * Creates the scroll panel containing the chat rooms list.
     * 
     * @param parentDialog Currently not used
     */
    public ChatRoomTableUI(ChatRoomTableDialog parentDialog)
    {
        this.initChatRoomList();

        this.setViewportView(chatRoomList);
        this.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    /**
     * Initializes the chat rooms list interface.
     */
    private void initChatRoomList()
    {
        this.chatRoomsTableModel = new ChatRoomTableModel(chatRoomList);

        this.chatRoomList.addMouseListener(this);

        this.chatRoomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        this.chatRoomList.setDefaultRenderer(ProtocolProviderService.class,
            new ProtocolProviderTableCellRenderer());
        this.chatRoomList.setDefaultRenderer(ChatRoomWrapper.class,
            new ChatRoomTableCellRenderer());

        this.chatRoomList.setOpaque(false);
        this.chatRoomList.setModel(chatRoomsTableModel);

        ConferenceChatManager confChatManager
            = GuiActivator.getUIService().getConferenceChatManager();

        confChatManager.addChatRoomListChangeListener(chatRoomsTableModel);

//        this.chatRoomList.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
//        this.chatRoomList.getColumnModel().getColumn(0).setMinWidth(250);
//        this.chatRoomList.getColumnModel().getColumn(1).setMinWidth(250);
//        this.chatRoomList.getColumnModel().getColumn(2).setPreferredWidth(50);
    }

    /**
     * Removes the room that is currently selected.
     */
    void removeSelectedRoom()
    {
        if(chatRoomList.getSelectedRow() == -1)
            return;

        ChatRoomWrapper chatRoomWrapper = 
            chatRoomsTableModel.getValueAt(chatRoomList.getSelectedRow());

        ConferenceChatManager conferenceManager = GuiActivator.getUIService()
            .getConferenceChatManager();
        conferenceManager.removeChatRoom(chatRoomWrapper);
    }

    /**
     * Clears any selected room.
     */
    void clearSelection()
    {
        this.chatRoomList.clearSelection();
    }

    /**
     * Adds listener for selection changes.
     * @param l
     */
    void addSelectionListener(ListSelectionListener l)
    {
        this.chatRoomList.getSelectionModel().addListSelectionListener(l);
    }

    /**
     * Returns the currently selected room.
     * @return the currently selected room.
     */
    ChatRoomWrapper getSelectedRoom()
    {
        return chatRoomsTableModel.getValueAt(chatRoomList.getSelectedRow());
    }

    /**
     * Opens the currently selected chat.
     */
    void openChatForSelection()
    {
        Object selectedValue = this.chatRoomsTableModel.getValueAt(
            this.chatRoomList.getSelectedRow());

        ChatRoomWrapper chatRoomWrapper;
        if (selectedValue instanceof ChatRoomWrapper)
            chatRoomWrapper = (ChatRoomWrapper) selectedValue;
        else
            return;

        if(chatRoomWrapper.getChatRoom() == null)
        {
            chatRoomWrapper =
                GuiActivator.getUIService().getConferenceChatManager()
                    .createChatRoom(
                        chatRoomWrapper.getChatRoomName(),
                        chatRoomWrapper.getParentProvider()
                            .getProtocolProvider(),
                            new ArrayList<String>(),
                            "",
                            false,
                            true);

            this.chatRoomsTableModel.setValueAt(chatRoomWrapper,
                this.chatRoomList.getSelectedRow(),
                this.chatRoomList.getSelectedColumn());
        }

        String nickName = null;
        ChatOperationReasonDialog reasonDialog =
            new ChatOperationReasonDialog(GuiActivator
                .getResources().getI18NString(
                    "service.gui.CHANGE_NICKNAME"), GuiActivator
                .getResources().getI18NString(
                    "service.gui.CHANGE_NICKNAME_LABEL"));

        // reasonDialog.setIconImage(ImageLoader.getImage(
        // ImageLoader.CHANGE_NICKNAME_ICON_16x16));
        reasonDialog.setReasonFieldText("");

        int result = reasonDialog.showDialog();

        if (result == MessageDialog.OK_RETURN_CODE)
        {
            nickName = reasonDialog.getReason().trim();

            if (!chatRoomWrapper.getChatRoom().isJoined())
            {
                GuiActivator.getUIService().getConferenceChatManager()
                    .joinChatRoom(chatRoomWrapper, nickName, null);
            }
        }
        else
        {
            if(!chatRoomWrapper.getChatRoom().isJoined())
            {
                GuiActivator.getUIService().getConferenceChatManager()
                    .joinChatRoom(chatRoomWrapper);
            }
        }

        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();
        ChatPanel chatPanel
            = chatWindowManager.getMultiChat(chatRoomWrapper, true);

        chatWindowManager.openChat(chatPanel, true);
    }

    /**
     * Listens for double clicks to open the chat room.
     * @param e
     */
    public void mouseClicked(MouseEvent e)
    {
        if (((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0)
                && (e.getClickCount() > 1))
            openChatForSelection();
    }

    public void mouseEntered(MouseEvent e)
    {}

    public void mouseExited(MouseEvent e)
    {}

    public void mouseReleased(MouseEvent e)
    {}

    /**
     * A chat room was selected. Opens the chat room in the chat window.
     *
     * @param e the <tt>MouseEvent</tt> instance containing details of
     * the event that has just occurred.
     */
    public void mousePressed(MouseEvent e)
    {
        //Select the object under the right button click.
        if ((e.getModifiers() & InputEvent.BUTTON2_MASK) != 0
            || (e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
            || (e.isControlDown() && !e.isMetaDown()))
        {
            int ix = this.chatRoomList.rowAtPoint(e.getPoint());

            if(ix != -1)
            {
                this.chatRoomList.setRowSelectionInterval(ix, ix);
            }
        }

        Object o = this.chatRoomsTableModel.getValueAt(
            this.chatRoomList.getSelectedRow());
        
        Point selectedCellPoint = e.getPoint();

        SwingUtilities.convertPointToScreen(selectedCellPoint, chatRoomList);

        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
        {
            JPopupMenu rightButtonMenu;

            if (o instanceof ChatRoomWrapper)
                rightButtonMenu
                    = new ChatRoomRightButtonMenu((ChatRoomWrapper) o);
            else
                return;

            rightButtonMenu.setInvoker(this);
            rightButtonMenu.setLocation(selectedCellPoint);
            rightButtonMenu.setVisible(true);
        }
    }

    /**
     * Renders the chat room with an icon corresponding its status in the table.
     */
    private class ChatRoomTableCellRenderer
        extends JLabel implements TableCellRenderer
    {
        /**
         * Creates the renderer.
         */
        public ChatRoomTableCellRenderer()
        {
            this.setHorizontalAlignment(JLabel.LEFT);
            this.setOpaque(true);
            this.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        }

        public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column)
        {
            ChatRoomWrapper chatRoom = (ChatRoomWrapper)value;
            

            this.setText(chatRoom.getChatRoomName());

            Image chatRoomImage = ImageLoader
                .getImage(ImageLoader.CHAT_ROOM_16x16_ICON);

            if(chatRoom.getChatRoom() == null ||
                !chatRoom.getChatRoom().isJoined())
            {
                chatRoomImage
                    = LightGrayFilter.createDisabledImage(chatRoomImage);
            }

            this.setIcon(new ImageIcon(chatRoomImage));

            this.setFont(this.getFont().deriveFont(Font.PLAIN));

            if(isSelected)
                this.setBackground(table.getSelectionBackground());
            else
                this.setBackground(UIManager.getColor("Table.background"));

            return this;
        }
    }

    /**
     * Renders in the table the account with its protocol icon, which
     * is corresponding the current status of the protocol.
     */
    private class ProtocolProviderTableCellRenderer
        extends JLabel implements TableCellRenderer
    {
        /**
         * Creates the Renderer.
         */
        public ProtocolProviderTableCellRenderer()
        {
            this.setHorizontalAlignment(JLabel.LEFT);
            this.setOpaque(true);
            this.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        }

        public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column)
        {
            ProtocolProviderService pps = (ProtocolProviderService)value;
            OperationSetPresence presence
                = pps.getOperationSet(OperationSetPresence.class);

            if(presence != null)
            {
                byte[] protocolStatusImage =
                    presence.getPresenceStatus().getStatusIcon();

                if(protocolStatusImage != null)
                {
                    this.setIcon(new ImageIcon(protocolStatusImage));
                }
                else
                {
                    this.setIcon(null);
                }
            }

            this.setText(pps.getAccountID().getDisplayName());

            if(isSelected)
                this.setBackground(table.getSelectionBackground());
            else
                this.setBackground(UIManager.getColor("Table.background"));

            return this;
        }        
    }
}
