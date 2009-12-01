/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;

/**
 * The <tt>ChatContactListPanel</tt> is the panel added on the right of the
 * chat conversation area, containing information for all contacts
 * participating the chat. It contains a list of <tt>ChatContactPanel</tt>s.
 * Each of these panels is containing the name, status, etc. of only one
 * <tt>MetaContact</tt> or simple <tt>Contact</tt>. There is also a button,
 * which allows to add new contact to the chat.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomMemberListPanel
    extends JPanel
{
    /**
     * The list of members.
     */
    private final DefaultContactList memberList = new DefaultContactList();

    /**
     * The model of the members list.
     */
    private final ChatContactListModel memberListModel
        = new ChatContactListModel();

     private final ChatPanel chatPanel;

    /**
     * Creates an instance of <tt>ChatContactListPanel</tt>.
     * @param chat Currently not used
     */
    public ChatRoomMemberListPanel(final ChatPanel chat)
    {
        super(new BorderLayout());

         this.chatPanel = chat;

        this.memberList.setModel(memberListModel);
        this.memberList.addKeyListener(new CListKeySearchListener(memberList));
        this.memberList.setCellRenderer(new ChatContactCellRenderer());
        this.memberList.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if(e.getButton() == MouseEvent.BUTTON3)
                {
                    new ChatContactRightButtonMenu(
                        chat, (ChatContact)memberList.getSelectedValue()).show(
                            memberList, e.getX(), e.getY());
                }
            }
        });

        JScrollPane contactsScrollPane = new SCScrollPane();
        contactsScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contactsScrollPane.setOpaque(false);

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
    public void addContact(ChatContact chatContact)
    {
        memberListModel.addElement(chatContact);
    }

    /**
     * Removes the given <tt>ChatContact</tt> from the list of chat contacts.
     *
     * @param chatContact the <tt>ChatContact</tt> to remove
     */
    public void removeContact(ChatContact chatContact)
    {
        memberListModel.removeElement(chatContact);
    }

    /**
     * In the corresponding <tt>ChatContactPanel</tt> changes the name of the
     * given <tt>Contact</tt>.
     *
     * @param chatContact the <tt>ChatContact</tt> to be renamed
     */
    public void renameContact(ChatContact chatContact)
    {
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
