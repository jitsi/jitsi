/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChatContactListPanel</tt> is the panel added on the right of the
 * chat conversation area, containing information for all contacts
 * participating the chat. It contains a list of <tt>ChatContactPanel</tt>s.
 * Each of these panels is containing the name, status, etc. of only one
 * <tt>MetaContact</tt> or simple <tt>Contact</tt>. There is also a button,
 * which allows to add new contact to the chat.
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 * @author Adam Netocny
 */
public class ChatRoomMemberListPanel
    extends JPanel
    implements Skinnable
{
    private static final long serialVersionUID = -8250816784228586068L;

    /**
     * The list of members.
     */
    private final DefaultContactList memberList = new DefaultContactList();

    /**
     * The model of the members list.
     */
    private final ChatContactListModel memberListModel;

    /**
     * Current chat panel.
     */
    private final ChatPanel chatPanel;

    /**
     * Initializes a new <tt>ChatRoomMemberListPanel</tt> instance which is to
     * depict the members of a chat specified by its <tt>ChatPanel</tt>.
     *
     * @param chatPanel the <tt>ChatPanel</tt> which specifies the chat the new
     * instance is to depict the members of
     */
    public ChatRoomMemberListPanel(ChatPanel chatPanel)
    {
        super(new BorderLayout());

        this.chatPanel = chatPanel;
        this.memberListModel
            = new ChatContactListModel(chatPanel.getChatSession());

        this.memberList.setModel(memberListModel);
        this.memberList.addKeyListener(new CListKeySearchListener(memberList));
        this.memberList.setCellRenderer(new ChatContactCellRenderer());

        // It's pertinent to add the ChatContactRightButtonMenu only we aren't
        // in an ad-hoc multi user chat (which support roles)
        if(this.chatPanel.getChatSession().getCurrentChatTransport()
                .getProtocolProvider().getSupportedOperationSets().containsKey(
                    OperationSetMultiUserChat.class.getName()))
        {
            this.memberList.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if(e.getButton() == MouseEvent.BUTTON3)
                    {
                        memberList.setSelectedIndex(
                            memberList.locationToIndex(e.getPoint()));

                        ChatContact<?> chatContact
                            = (ChatContact<?>) memberList.getSelectedValue();

                        if (chatContact != null)
                            new ChatContactRightButtonMenu(
                                    ChatRoomMemberListPanel.this.chatPanel,
                                    chatContact)
                                .show(memberList, e.getX(), e.getY());
                    }
                    else if(e.getButton() == MouseEvent.BUTTON1 
                        && e.getClickCount() == 2)
                    {
                        if(ConfigurationUtils
                                .isPrivateMessagingInChatRoomDisabled())
                            return;

                        memberList.setSelectedIndex(
                            memberList.locationToIndex(e.getPoint()));

                        ChatContact<?> chatContact
                            = (ChatContact<?>) memberList.getSelectedValue();
                        
                        ChatRoom room 
                            = ((ChatRoomWrapper) ChatRoomMemberListPanel.this
                                .chatPanel.getChatSession().getDescriptor())
                                    .getChatRoom();
                        if(room.getUserNickname().equals(chatContact.getName()))
                            return;
                        ChatWindowManager chatWindowManager
                            = GuiActivator.getUIService()
                                .getChatWindowManager();
                        chatWindowManager.openPrivateChatForChatRoomMember(room, 
                            chatContact.getName());

                    }
                }
            });
        }


        JScrollPane contactsScrollPane = new SIPCommScrollPane();
        contactsScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contactsScrollPane.setOpaque(false);
        contactsScrollPane.setBorder(null);

        JViewport viewport = contactsScrollPane.getViewport();
        viewport.setOpaque(false);
        viewport.add(memberList);

        this.add(contactsScrollPane);
    }

    /**
     * Adds a <tt>ChatContact</tt> to the list of contacts contained in the
     * chat.
     *
     * @param chatContact the <tt>ChatContact</tt> to add
     */
    public void addContact(ChatContact<?> chatContact)
    {
        memberListModel.addElement(chatContact);
    }

    /**
     * Removes the given <tt>ChatContact</tt> from the list of chat contacts.
     *
     * @param chatContact the <tt>ChatContact</tt> to remove
     */
    public void removeContact(ChatContact<?> chatContact)
    {
        memberListModel.removeElement(chatContact);
    }

    /**
     * Removes all chat contacts from the contact list of the chat.
     */
    public void removeAllChatContacts()
    {
        memberListModel.removeAllElements();
    }

    /**
     * In the corresponding <tt>ChatContactPanel</tt> changes the name of the
     * given <tt>Contact</tt>.
     *
     * @param chatContact the <tt>ChatContact</tt> to be renamed
     */
    public void renameContact(ChatContact<?> chatContact)
    {
    }

    /**
     * Reloads renderer.
     */
    public void loadSkin()
    {
        ((ChatContactCellRenderer)memberList.getCellRenderer()).loadSkin();
    }

    /**
     * Runs clean-up.
     */
    public void dispose()
    {
        if(memberListModel != null)
            memberListModel.dispose();
    }

    /**
     * Opens a web page containing information of the currently selected user.
     *
     * @param evt the action event that has just occurred.
     */
//    public void actionPerformed(ActionEvent evt)
//    {
//        JButton button = (JButton) evt.getSource();
//
//        // first, see if the contact with which we chat supports telephony
//        // and call that one. If he don't, we look for the default
//        // telephony contact in its enclosing metacontact
//        if(button.getName().equals("call"))
//        {
//            ChatTransport telephonyTransport
//                = chatPanel.getChatSession().getTelephonyTransport();
//
//            if (telephonyTransport != null)
//            {
//                // hope an appropriate telephony will be used.
//                CallManager.createCall( telephonyTransport.getProtocolProvider(),
//                                        telephonyTransport.getName());
//            }
//
//            chatPanel.getChatWindow().getMainFrame().toFront();
//        }
//        else if(button.getName().equals("info"))
//        {
//            ChatTransport contactDetailsTransport
//                = chatPanel.getChatSession().getContactDetailsTransport();
//
//            if(contactDetailsTransport != null)
//            {
//                // TODO: Open the contact details dialog.
//            }
//        }
//    }
}
