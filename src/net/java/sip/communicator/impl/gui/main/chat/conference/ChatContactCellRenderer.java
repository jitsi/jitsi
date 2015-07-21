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
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.BinaryDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;

/**
 * The <tt>ChatContactCellRenderer</tt> is the renderer for the chat room
 * contact list.
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 * @author Lubomir Marinov
 */
public class ChatContactCellRenderer
    extends ContactListCellRenderer
{
    /**
     * Color constant for contacts that are at least available.
     */
    private static final Color COLOR_AVAILABILITY_THRESHOLD = Color.BLACK;

    /**
     * Color constant for contacts that are at least away.
     */
    private static final Color COLOR_AWAY_THRESHOLD = Color.GRAY;

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

        final ChatContact<?> chatContact = (ChatContact<?>) value;

        if(chatContact == null)
            return this;

        ChatRoomMember member = null;

        if (chatContact.getDescriptor() instanceof ChatRoomMember)
            member = (ChatRoomMember) chatContact.getDescriptor();

        this.setPreferredSize(new Dimension(20, 30));

        String displayName;

//        if(member != null && member.getContact() != null)
//        {
//            displayName = member.getContact().getDisplayName();
//        }
//        else
        displayName = chatContact.getName();

        if (displayName == null || displayName.length() < 1)
        {
            displayName = GuiActivator.getResources()
                .getI18NString("service.gui.UNKNOWN");
        }

        this.nameLabel.setFont(this.getFont().deriveFont(Font.PLAIN));
        this.nameLabel.setText(displayName);

        if(member != null)
        {
            ChatRoomMemberRole memberRole = member.getRole();

            if(memberRole != null)
                this.nameLabel.setIcon(
                    ChatContactRoleIcon.getRoleIcon(memberRole));

            final int presenceStatus = member.getPresenceStatus().getStatus();
            if (presenceStatus >= PresenceStatus.AVAILABLE_THRESHOLD)
            {
                this.nameLabel.setForeground(COLOR_AVAILABILITY_THRESHOLD);
            }
            else if (presenceStatus >= PresenceStatus.AWAY_THRESHOLD)
            {
                this.nameLabel.setForeground(COLOR_AWAY_THRESHOLD);
            }
        }
        else if (contactForegroundColor != null)
            this.nameLabel.setForeground(contactForegroundColor);

        this.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 1));

        ImageIcon avatar = chatContact.getAvatar();

        if (avatar != null)
            this.rightLabel.setIcon(avatar);
        else if (member != null)
        {
            ChatRoom memberChatRoom = member.getChatRoom();
            ProtocolProviderService protocolProvider
                = memberChatRoom.getParentProvider();

            if(chatContact.getName().equals(
                    memberChatRoom.getUserNickname()))
            {
                // Try to retrieve local user avatar:
                OperationSetServerStoredAccountInfo opSet
                    = protocolProvider.getOperationSet(
                        OperationSetServerStoredAccountInfo.class);

                if (opSet != null)
                {
                    Iterator<GenericDetail> itr;

                    try
                    {
                        itr = opSet.getAllAvailableDetails();
                    }
                    catch (IllegalStateException isex)
                    {
                        /*
                         * It may be wrong to try to utilize the OperationSet
                         * when the account is logged out but this is painting
                         * we're doing here i.e. we'll screw the whole window
                         * up.
                         */
                        itr = null;
                    }

                    if (itr != null)
                        while(itr.hasNext())
                        {
                            GenericDetail detail = itr.next();

                            if(detail instanceof BinaryDetail)
                            {
                                BinaryDetail bin = (BinaryDetail)detail;
                                byte[] binBytes = bin.getBytes();

                                if(binBytes != null)
                                    this.rightLabel.setIcon(
                                        ImageUtils.getScaledRoundedIcon(
                                            binBytes, 25, 25));
                                break;
                            }
                        }
                }

                ChatRoomMemberRole role;

                /*
                 * XXX I don't know why ChatRoom#getUserRole() would not be
                 * implemented when ChatRoomMember#getRole() is or why the
                 * former would exist at all as anything else but as a
                 * convenience delegating to the latter, but IRC seems to be the
                 * case and the whole IRC channel painting fails because of it.
                 */
                try
                {
                    role = memberChatRoom.getUserRole();
                }
                catch (UnsupportedOperationException uoex)
                {
                    role = member.getRole();
                }

                if (role != null)
                    this.nameLabel.setIcon(
                        ChatContactRoleIcon.getRoleIcon(role));
            }
            else
            {
                // Try to retrieve participant's avatar.
                OperationSetPersistentPresence opSet
                    = protocolProvider.getOperationSet(
                    OperationSetPersistentPresence.class);

                if (opSet != null)
                {
                    Contact c
                        = opSet.findContactByID(member.getContactAddress());

                    if (c != null)
                    {
                        byte[] cImage = c.getImage();

                        if (cImage != null)
                            this.rightLabel.setIcon(
                                    ImageUtils.getScaledRoundedIcon(
                                            cImage, 25, 25));
                    }
                }
            }
        }

        // We should set the bounds of the cell explicitly in order to make
        // getComponentAt work properly.
        int listWidth = list.getWidth();

        this.setBounds(0, 0, listWidth - 2, 30);
        this.nameLabel.setBounds(0, 0, listWidth - 28, 17);
        this.rightLabel.setBounds(listWidth - 28, 0, 25, 30);

        this.isLeaf = true;
        this.isSelected = isSelected;

        return this;
    }
}
