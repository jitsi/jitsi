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
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;

/**
 * The <tt>ChatRoomsListPanel</tt> is the panel that contains the
 * <tt>ChatRoomsList</tt>.
 *
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class ChatRoomListUI
    extends SCScrollPane
    implements  MouseListener,
                ChatRoomListChangeListener,
                AdHocChatRoomListChangeListener
{
    private final JList chatRoomList = new JList();

    private final ChatRoomListModel chatRoomsListModel
        = new ChatRoomListModel();

    private final JPanel treePanel = new JPanel(new BorderLayout());

    /**
     * Creates the scroll panel containing the chat rooms list.
     * 
     * @param parentDialog Currently not used
     */
    public ChatRoomListUI(ChatRoomListDialog parentDialog)
    {
        ConferenceChatManager confChatManager
            = GuiActivator.getUIService().getConferenceChatManager();

        confChatManager.addChatRoomListChangeListener(this);
        confChatManager.addAdHocChatRoomListChangeListener(this);

        this.treePanel.add(chatRoomList, BorderLayout.NORTH);

        this.setViewportView(treePanel);

        this.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.getActionMap().put("runChat", new ChatRoomsListPanelEnterAction());

        InputMap imap = this.getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "runChat");

        this.setPreferredSize(new Dimension(200, 450));
        this.setMinimumSize(new Dimension(80, 200));

        this.initChatRoomList();
    }

    /**
     * Initializes the chat rooms list interface.
     */
    private void initChatRoomList()
    {
        this.chatRoomList.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.chatRoomList.setOpaque(false);

        this.chatRoomList.setModel(chatRoomsListModel);
        this.chatRoomList.setCellRenderer(new ChatRoomsListCellRenderer());

        this.chatRoomList.addMouseListener(this);

    }

    private void openChatForSelection()
    {
        Object selectedValue = chatRoomList.getSelectedValue();
        ChatRoomWrapper chatRoomWrapper;

        if (selectedValue instanceof ChatRoomProviderWrapper)
            chatRoomWrapper
                = ((ChatRoomProviderWrapper) selectedValue)
                        .getSystemRoomWrapper();
        else if (selectedValue instanceof ChatRoomWrapper)
            chatRoomWrapper = (ChatRoomWrapper) selectedValue;
        else
            return;

        ChatWindowManager chatWindowManager
            = GuiActivator.getUIService().getChatWindowManager();
        ChatPanel chatPanel
            = chatWindowManager.getMultiChat(chatRoomWrapper, true);

        chatWindowManager.openChat(chatPanel, true);
    }
    /**
     * Opens chat window when the selected value is a MetaContact and opens a
     * group when the selected value is a MetaContactGroup.
     */
    private class ChatRoomsListPanelEnterAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            openChatForSelection();
        }
    }

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
            this.chatRoomList.setSelectedIndex(
                chatRoomList.locationToIndex(e.getPoint()));
        }

        Object o = chatRoomList.getSelectedValue();

        Point selectedCellPoint
            = chatRoomList.indexToLocation(chatRoomList.getSelectedIndex());

        SwingUtilities.convertPointToScreen(selectedCellPoint, chatRoomList);

        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
        {
            JPopupMenu rightButtonMenu;

            if(o instanceof ChatRoomProviderWrapper)
                rightButtonMenu
                    = new ChatRoomServerRightButtonMenu(
                            (ChatRoomProviderWrapper) o);
            else if (o instanceof ChatRoomWrapper)
                rightButtonMenu
                    = new ChatRoomRightButtonMenu((ChatRoomWrapper) o);
            else
                return;

            rightButtonMenu.setInvoker(this);
            rightButtonMenu
                .setLocation(selectedCellPoint.x, selectedCellPoint.y + 20);
            rightButtonMenu.setVisible(true);
        }
    }

    /**
     * Refreshes the chat room's list when a modification in the model has
     * occurred.
     */
    public void contentChanged(ChatRoomListChangeEvent evt)
    {
        ChatRoomWrapper chatRoomWrapper = evt.getSourceChatRoom();

        if (evt.getEventID() == ChatRoomListChangeEvent.CHAT_ROOM_ADDED)
        {
            int index = chatRoomsListModel.indexOf(chatRoomWrapper);

            if (index != -1)
                chatRoomsListModel.contentAdded(index, index);
        }
        else if (evt.getEventID() == ChatRoomListChangeEvent.CHAT_ROOM_REMOVED)
        {
            int groupIndex = chatRoomsListModel.indexOf(
                chatRoomWrapper.getParentProvider());

            int listSize = chatRoomsListModel.getSize();

            if (groupIndex != -1 && listSize > 0)
            {
                chatRoomsListModel.contentChanged(groupIndex, listSize - 1);
                chatRoomsListModel.contentRemoved(listSize, listSize);
            }
        }
        else if (evt.getEventID() == ChatRoomListChangeEvent.CHAT_ROOM_CHANGED)
        {
            int index = chatRoomsListModel.indexOf(chatRoomWrapper);

            chatRoomsListModel.contentChanged(index, index);
        }
    }

    /**
     * Updates the chat room list model when notified of a change in the chat
     * room list.
     */
    public void contentChanged(AdHocChatRoomListChangeEvent evt)
    {
        AdHocChatRoomWrapper chatRoomWrapper = evt.getSourceAdHocChatRoom();

        if (evt.getEventID()
                == AdHocChatRoomListChangeEvent.AD_HOC_CHAT_ROOM_ADDED)
        {
            int index = chatRoomsListModel.indexOf(chatRoomWrapper);

            if (index != -1)
                chatRoomsListModel.contentAdded(index, index);
        }
        else if (evt.getEventID()
                == AdHocChatRoomListChangeEvent.AD_HOC_CHAT_ROOM_REMOVED)
        {
            int groupIndex = chatRoomsListModel.indexOf(
                chatRoomWrapper.getParentProvider());

            int listSize = chatRoomsListModel.getSize();

            if (groupIndex != -1 && listSize > 0)
            {
                chatRoomsListModel.contentChanged(groupIndex, listSize - 1);
                chatRoomsListModel.contentRemoved(listSize, listSize);
            }
        }
        else if (evt.getEventID()
                == AdHocChatRoomListChangeEvent.AD_HOC_CHAT_ROOM_CHANGED)
        {
            int index = chatRoomsListModel.indexOf(chatRoomWrapper);

            chatRoomsListModel.contentChanged(index, index);
        }
    }
}
