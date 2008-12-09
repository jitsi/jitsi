/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;

/**
 * The <tt>ChatContactCellRenderer</tt> is the renderer for the chat contact
 * list.
 *
 * @author Yana Stamcheva
 */
public class ChatContactCellRenderer
    extends ContactListCellRenderer
{
    public Component getListCellRendererComponent(  JList list,
                                                    Object value,
                                                    int index,
                                                    boolean isSelected,
                                                    boolean cellHasFocus)
    {
        this.index = index;

        this.photoLabel.setIcon(null);

        ContactList contactList = (ContactList) list;

        ChatContact chatContact = (ChatContact) value;

        this.setPreferredSize(new Dimension(20, 30));

        String displayName = chatContact.getName();

        if (displayName == null || displayName.length() < 1)
        {
            displayName = GuiActivator.getResources()
                .getI18NString("service.gui.UNKNOWN");
        }

        this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));
        this.nameLabel.setText(displayName);

//        statusIcon.setImage(Constants.getStatusIcon();
//        this.nameLabel.setIcon(statusIcon);

        if (contactForegroundColor != null)
            this.nameLabel.setForeground(contactForegroundColor);

        this.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));

        ImageIcon avatar = chatContact.getAvatar();

        if (avatar != null)
            this.photoLabel.setIcon(avatar);

        // We should set the bounds of the cell explicitly in order to
        // make getComponentAt work properly.
        this.setBounds(0, 0, list.getWidth() - 2, 30);

        this.nameLabel.setBounds(
                    0, 0, list.getWidth() - 28, 17);

        this.photoLabel.setBounds(
            list.getWidth() - 28, 0, 25, 30);

        this.isLeaf = true;

        this.isSelected = isSelected;

        return this;
    }
}
