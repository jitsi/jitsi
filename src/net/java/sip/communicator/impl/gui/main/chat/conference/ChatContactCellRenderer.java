/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>ChatContactCellRenderer</tt> is the renderer for the chat room
 * contact list.
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 */
public class ChatContactCellRenderer
    extends ContactListCellRenderer
{
    /**
     * Implements the <tt>ListCellRenderer</tt> method. Returns this panel that
     * has been configured to display a chat contact.
     *
     * @param list the source list
     * @param value the value of the current cell
     * @param index the index of the current cell in the source list
     * @param isSelected indicates if this cell is selected
     * @param cellHasFocus indicates if this cell is focused
     * 
     * @return this panel
     */
    @Override
    public Component getListCellRendererComponent(  JList list,
                                                    Object value,
                                                    int index,
                                                    boolean isSelected,
                                                    boolean cellHasFocus)
    {
        this.index = index;

        this.rightLabel.setIcon(null);

        final ChatContact chatContact = (ChatContact) value;
        final ChatRoomMember member 
            = (ChatRoomMember) chatContact.getDescriptor();

        this.setPreferredSize(new Dimension(20, 30));

        String displayName = chatContact.getName();

        if (displayName == null || displayName.length() < 1)
        {
            displayName = GuiActivator.getResources()
                .getI18NString("service.gui.UNKNOWN");
        }

        this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));
        this.nameLabel.setText(displayName);

        if(member.getRole() != null)
            this.nameLabel.setIcon(
                ChatContactRoleIcon.getRoleIcon(member.getRole()));

        if (contactForegroundColor != null)
            this.nameLabel.setForeground(contactForegroundColor);

        this.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));

        ImageIcon avatar = chatContact.getAvatar();

        if (avatar != null)
            this.rightLabel.setIcon(avatar);
        else
        {
            if(chatContact.getName().equals(
                    member.getChatRoom().getUserNickname()))
            {
                // Try to retrieve local user avatar:
                OperationSetServerStoredAccountInfo opSet
                    = (OperationSetServerStoredAccountInfo)
                    member.getChatRoom().getParentProvider().getOperationSet(
                        OperationSetServerStoredAccountInfo.class);

                Iterator<GenericDetail> itr = opSet.getAllAvailableDetails();
                while(itr.hasNext())
                {
                    GenericDetail detail = itr.next();
                    if(detail instanceof BinaryDetail)
                    {
                        BinaryDetail bin = (BinaryDetail)detail;
                        if(bin.getBytes() != null)
                            this.rightLabel.setIcon(
                                ImageUtils.getScaledRoundedIcon(
                                    bin.getBytes(), 25, 25));
                        break;
                    }
                }
                ChatRoomMemberRole role = member.getChatRoom().getUserRole();
                if (role != null)
                    this.nameLabel.setIcon(
                        ChatContactRoleIcon.getRoleIcon(role));
            }
            else
            {
                // Try to retrieve participant avatar:
                OperationSetPersistentPresence opSet
                    = (OperationSetPersistentPresence)
                member.getChatRoom().getParentProvider().getOperationSet(
                    OperationSetPersistentPresence.class);

                Contact c = opSet.findContactByID(member.getContactAddress());

                if(opSet != null && c != null && c.getImage() != null)
                    this.rightLabel.setIcon(ImageUtils.getScaledRoundedIcon(
                            c.getImage(), 25, 25));
            }
        }

        // We should set the bounds of the cell explicitly in order to
        // make getComponentAt work properly.
        this.setBounds(0, 0, list.getWidth() - 2, 30);

        this.nameLabel.setBounds(
                    0, 0, list.getWidth() - 28, 17);

        this.rightLabel.setBounds(
            list.getWidth() - 28, 0, 25, 30);

        this.isLeaf = true;

        this.isSelected = isSelected;

        return this;
    }
}
