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
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.chat.*;
import net.java.sip.communicator.service.muc.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChatContactRightButtonMenu</tt> is the menu, opened when user clicks
 * with the right mouse button on a contact in the list of contacts in the chat
 * window.
 *
 * @author Yana Stamcheva
 * @author Valentin Martinet
 * @author Adam Netocny
 * @author Ingo Bauersachs
 * @author Boris Grozev
 */
public class ChatContactRightButtonMenu
    extends SIPCommPopupMenu
    implements  ActionListener,
                Skinnable
{
    private static final long serialVersionUID = -4069653895234333083L;

    private Logger logger = Logger.getLogger(ChatContactRightButtonMenu.class);

    private final JMenuItem kickItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.KICK"));
    private final JMenuItem banItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.BAN"));

    private final JMenuItem changeRoomSubjectItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.CHANGE_ROOM_SUBJECT"));
    private final JMenuItem changeNicknameItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.CHANGE_NICKNAME"));

    private final JMenuItem grantOwnershipItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.GRANT_OWNERSHIP"));
    private final JMenuItem grantAdminItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.GRANT_ADMIN"));
    private final JMenuItem grantMembershipItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.GRANT_MEMBERSHIP"));
    private final JMenuItem grantModeratorItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.GRANT_MODERATOR"));
    private final JMenuItem grantVoiceItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.GRANT_VOICE"));

    private final JMenuItem revokeOwnershipItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.REVOKE_OWNERSHIP"));
    private final JMenuItem revokeAdminItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.REVOKE_ADMIN"));
    private final JMenuItem revokeMembershipItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.REVOKE_MEMBERSHIP"));
    private final JMenuItem revokeModeratorItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.REVOKE_MODERATOR"));
    private final JMenuItem revokeVoiceItem
        = new JMenuItem(GuiActivator.getResources().getI18NString(
            "service.gui.REVOKE_VOICE"));
    private final JMenuItem sendPrivateMessageItem
    = new JMenuItem(GuiActivator.getResources().getI18NString(
        "service.gui.SEND_PRIVATE_MESSAGE"));

    private final ChatPanel chatPanel;

    /**
     * The contact associated with this menu
     */
    private final ChatContact<?> chatContact;

    /**
     * The Chatroom in which the chatContact is currently participating.
     */
    private ChatRoom room;

    /**
     * Creates an instance of <tt>ChatRoomsListRightButtonMenu</tt>.
     * @param chatPanel the chat panel containing this menu
     * @param chatContact the contact
     */
    public ChatContactRightButtonMenu(  ChatPanel chatPanel,
                                        ChatContact<?> chatContact)
    {
        this.chatPanel = chatPanel;
        this.chatContact = chatContact;

        Object descriptor
            = chatPanel.getChatSession().getDescriptor();

        if (descriptor instanceof ChatRoomWrapper)
            this.room = ((ChatRoomWrapper) descriptor).getChatRoom();

        this.setLocation(getLocation());

        this.init();
    }

    /**
     * Initializes the menu, by adding all containing menu items.
     */
    private void init()
    {
        this.kickItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.KICK"));

        this.banItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.BAN"));

        this.grantAdminItem.setMnemonic(
           GuiActivator.getResources().getI18nMnemonic(
           "service.gui.GRANT_ADMIN"));
        this.grantMembershipItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
           "service.gui.GRANT_MEMBERSHIP"));
        this.grantModeratorItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.GRANT_MODERATOR"));
        this.grantOwnershipItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.GRANT_OWNERSHIP"));
        this.grantVoiceItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.GRANT_VOICE"));
        this.revokeAdminItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.REVOKE_ADMIN"));
        this.revokeMembershipItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.REVOKE_MEMBERSHIP"));
        this.revokeModeratorItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.REVOKE_MODERATOR"));
        this.revokeOwnershipItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.REVOKE_OWNERSHIP"));
        this.revokeVoiceItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.REVOKE_VOICE"));
        this.changeNicknameItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.CHANGE_NICKNAME"));
        this.changeRoomSubjectItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.CHANGE_ROOM_SUBJECT"));
        this.sendPrivateMessageItem.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic(
            "service.gui.SEND_PRIVATE_MESSAGE"));

        this.kickItem.addActionListener(this);
        this.banItem.addActionListener(this);
        this.changeNicknameItem.addActionListener(this);
        this.changeRoomSubjectItem.addActionListener(this);
        this.grantAdminItem.addActionListener(this);
        this.grantMembershipItem.addActionListener(this);
        this.grantModeratorItem.addActionListener(this);
        this.grantOwnershipItem.addActionListener(this);
        this.grantVoiceItem.addActionListener(this);
        this.revokeAdminItem.addActionListener(this);
        this.revokeMembershipItem.addActionListener(this);
        this.revokeModeratorItem.addActionListener(this);
        this.revokeOwnershipItem.addActionListener(this);
        this.revokeVoiceItem.addActionListener(this);
        this.sendPrivateMessageItem.addActionListener(this);

        this.kickItem.setName("kickItem");
        this.banItem.setName("banItem");
        this.changeNicknameItem.setName("changeNicknameItem");
        this.changeRoomSubjectItem.setName("changeRoomSubjectItem");
        this.grantAdminItem.setName("grantAdminItem");
        this.grantMembershipItem.setName("grantMembershipItem");
        this.grantModeratorItem.setName("grantModeratorItem");
        this.grantOwnershipItem.setName("grantOwnershipItem");
        this.grantVoiceItem.setName("grantVoiceItem");
        this.revokeAdminItem.setName("revokeAdminItem");
        this.revokeMembershipItem.setName("revokeMembershipItem");
        this.revokeModeratorItem.setName("revokeModeratorItem");
        this.revokeOwnershipItem.setName("revokeOwnershipItem");
        this.revokeVoiceItem.setName("revokeVoiceItem");
        this.sendPrivateMessageItem.setName("sendPrivateMessageItem");

        loadSkin();

        ChatRoomMemberRole role
            = chatContact.getName().equals(room.getUserNickname())
                ? room.getUserRole()
                : ((ChatRoomMember) chatContact.getDescriptor()).getRole();
        int roleIndex = role.getRoleIndex();
        String roleName = role.getLocalizedRoleName();

        JLabel jl_username =
            new JLabel(" " + chatContact.getName() + " ("
                + GuiActivator.getResources().getI18NString(roleName) + ") ");
        jl_username.setFont(jl_username.getFont().deriveFont(Font.BOLD));

        this.add(jl_username);
        this.addSeparator();

        // Here we build the menu when the local user cell renderer is clicked:
        if(chatContact.getName().equals(room.getUserNickname()))
        {
            if(roleIndex >= 50)
            {
                // It means we are at least a moderator, so we can change room's
                // subject:
                this.add(this.changeRoomSubjectItem);
            }

            this.add(this.changeNicknameItem);
        }
        else
        {
            if(!ConfigurationUtils.isPrivateMessagingInChatRoomDisabled())
                this.add(this.sendPrivateMessageItem);

            if(room.getUserRole().getRoleIndex() >= 50)
            {
                if(roleIndex <= 40)
                {
                    this.add(this.kickItem);

                    // Admins and owners can ban members:
                    if(room.getUserRole().getRoleIndex() >= 60 && roleIndex < 50)
                    {
                        this.add(this.banItem);
                    }
                    this.addSeparator();
                }

                // we must at least be a moderator for managing voice rights
                if(roleIndex <= 20)
                    this.add(this.grantVoiceItem);
                else if(roleIndex == 40 || roleIndex == 30)
                    this.add(this.revokeVoiceItem);
            }

            if(room.getUserRole().getRoleIndex() >= 60)
            {
                // we must at least be an admin to manage membership
                if(roleIndex < 40)
                    this.add(this.grantMembershipItem);
//                else if(roleIndex == 40)
//                    this.add(this.revokeMembershipItem);

                if(roleIndex < 50)    // room admins can edit moderators list
                    this.add(this.grantModeratorItem);
                else if(roleIndex == 50)
                    this.add(this.revokeModeratorItem);
            }

            // only room owners can edit admins list
//            if(room.getUserRole().getRoleIndex() == 70)
//            {
//                if(roleIndex != 60 && roleIndex >= 30)
//                // room owners can grant members or unaffiliated users as admins
//                    this.add(this.grantAdminItem);
//                else if(roleIndex == 60)
//                    this.add(this.revokeAdminItem);
//
//                // room owners can edit owners list
//                if(roleIndex != 70 && roleIndex >= 40)
//                    this.add(this.grantOwnershipItem);
//                else if(roleIndex == 70)
//                    this.add(this.revokeOwnershipItem);
//            }
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt>. Determines which menu item was
     * selected and makes the appropriate operations.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        String menuItemName = ((JMenuItem) e.getSource()).getName();

        if (menuItemName.equals("kickItem"))
        {
            ChatOperationReasonDialog reasonDialog
                = new ChatOperationReasonDialog();

            int result = new ChatOperationReasonDialog().showDialog();

            if (result == MessageDialog.OK_RETURN_CODE)
                new KickParticipantThread(  room,
                                            reasonDialog.getReason()).start();
        }
        else if (menuItemName.equals("banItem"))
        {
            ChatOperationReasonDialog reasonDialog
                = new ChatOperationReasonDialog();

            int result = reasonDialog.showDialog();

            if (result == MessageDialog.OK_RETURN_CODE)
                new BanParticipantThread(   room,
                                            reasonDialog.getReason()).start();
        }
        else if (menuItemName.equals("changeRoomSubjectItem"))
        {
            ChatOperationReasonDialog reasonDialog
                = new ChatOperationReasonDialog(
                    chatPanel.getChatContainer().getFrame(),
                    GuiActivator.getResources().getI18NString(
                    "service.gui.CHANGE_ROOM_SUBJECT"),
                    GuiActivator.getResources().getI18NString(
                    "service.gui.CHANGE_ROOM_SUBJECT_LABEL"),
                    "Ok",
                    false);

            //reasonDialog.setIconImage(
            //ImageLoader.getImage(ImageLoader.CHANGE_ROOM_SUBJECT_ICON_16x16));
            reasonDialog.setReasonFieldText(room.getSubject());

            int result = reasonDialog.showDialog();

            if (result == MessageDialog.OK_RETURN_CODE)
            {
                try
                {
                    room.setSubject(reasonDialog.getReason().trim());
                }
                catch (OperationFailedException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
        else if (menuItemName.equals("changeNicknameItem"))
        {
            ChatOperationReasonDialog reasonDialog
                = new ChatOperationReasonDialog(
                    chatPanel.getChatContainer().getFrame(),
                    GuiActivator.getResources().getI18NString(
                    "service.gui.CHANGE_NICKNAME"),
                    GuiActivator.getResources().getI18NString(
                    "service.gui.CHANGE_NICKNAME_LABEL"),
                    "Ok",
                    false, true);

            reasonDialog.setIconImage(ImageLoader.getImage(
              ImageLoader.CHANGE_NICKNAME_ICON));
            reasonDialog.setReasonFieldText(chatContact.getName());

            int result = reasonDialog.showDialog();

            if (result == MessageDialog.OK_RETURN_CODE)
            {
                String nickname = reasonDialog.getReason().trim();
                try
                {
                    room.setUserNickname(nickname);
                    ConfigurationUtils.updateChatRoomProperty(
                        room.getParentProvider(),
                        room.getIdentifier(),
                        "userNickName", nickname);
                }
                catch (OperationFailedException ex)
                {
                    String errorMessage = null;
                    if(ex.getErrorCode()
                        == OperationFailedException.IDENTIFICATION_CONFLICT)
                    {
                        errorMessage = GuiActivator.getResources()
                            .getI18NString(
                                "service.gui.CHANGE_NICKNAME_CONFLICT_ERROR");
                    }
                    else
                    {
                        errorMessage = ex.getLocalizedMessage();
                    }

                    chatPanel.addErrorMessage(
                        nickname,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CHANGE_NICKNAME_ERROR"),
                        errorMessage);
               }
           }
        }
        else if (menuItemName.equals("grantVoiceItem"))
        {
            room.grantVoice(chatContact.getName());
            ((ChatRoomMember)chatContact.getDescriptor()).setRole(
                ChatRoomMemberRole.MEMBER);
        }
        else if (menuItemName.equals("grantMembershipItem"))
        {
            room.grantMembership(((ChatRoomMember)
                chatContact.getDescriptor()).getContactAddress());
            ((ChatRoomMember)chatContact.getDescriptor()).setRole(
                ChatRoomMemberRole.MEMBER);
        }
        else if(menuItemName.equals("grantModeratorItem"))
        {
            room.grantModerator(chatContact.getName());
            ((ChatRoomMember)chatContact.getDescriptor()).setRole(
                ChatRoomMemberRole.MODERATOR);
        }
        else if(menuItemName.equals("grantAdminItem"))
        {
            room.grantAdmin(((ChatRoomMember) chatContact
                    .getDescriptor()).getContactAddress());
            ((ChatRoomMember)chatContact.getDescriptor()).setRole(
                               ChatRoomMemberRole.ADMINISTRATOR);
        }
        else if(menuItemName.equals("grantOwnershipItem"))
        {
            room.grantOwnership(((ChatRoomMember)
                chatContact.getDescriptor()).getContactAddress());
            ((ChatRoomMember)chatContact.getDescriptor()).setRole(
                ChatRoomMemberRole.OWNER);
        }
        else if(menuItemName.equals("revokeOwnershipItem"))
        {
            room.revokeOwnership(((ChatRoomMember)
                chatContact.getDescriptor()).getContactAddress());
            ((ChatRoomMember)chatContact.getDescriptor()).setRole(
                ChatRoomMemberRole.ADMINISTRATOR);
        }
        else if(menuItemName.equals("revokeAdminItem"))
        {
            room.revokeAdmin(((ChatRoomMember)
                chatContact.getDescriptor()).getContactAddress());
            ((ChatRoomMember)chatContact.getDescriptor()).setRole(
                ChatRoomMemberRole.MEMBER);
        }
        else if(menuItemName.equals("revokeModeratorItem"))
        {
            room.revokeModerator(chatContact.getName());
            ((ChatRoomMember)chatContact.getDescriptor()).setRole(
                ChatRoomMemberRole.MEMBER);
        }
        else if(menuItemName.equals("revokeMembershipItem"))
        {
            room.revokeMembership(((ChatRoomMember)
                chatContact.getDescriptor()).getContactAddress());
            ((ChatRoomMember)chatContact.getDescriptor()).setRole(
                ChatRoomMemberRole.GUEST);
        }
        else if(menuItemName.equals("revokeVoiceItem"))
        {
            room.revokeVoice(chatContact.getName());
            ((ChatRoomMember)chatContact.getDescriptor()).setRole(
                ChatRoomMemberRole.SILENT_MEMBER);
        }
        else if(menuItemName.equals("sendPrivateMessageItem"))
        {
            ChatWindowManager chatWindowManager
                = GuiActivator.getUIService()
                    .getChatWindowManager();
            chatWindowManager.openPrivateChatForChatRoomMember(room, 
                chatContact.getName());
        }
    }

    /**
     * Kicks the the selected chat room participant or shows a message to the
     * user that he/she has not enough permissions to do a ban.
     */
    private class KickParticipantThread extends Thread
    {
        private final ChatRoom chatRoom;

        private final String reason;

        KickParticipantThread(ChatRoom chatRoom, String reason)
        {
            this.chatRoom = chatRoom;
            this.reason = reason;
        }

        @Override
        public void run()
        {
            try
            {
                chatRoom.kickParticipant(
                    (ChatRoomMember) chatContact.getDescriptor(),
                    reason);
            }
            catch (OperationFailedException e)
            {
                logger.error("Failed to kick participant.", e);

                if (e.getErrorCode()
                    == OperationFailedException.NOT_ENOUGH_PRIVILEGES)
                {
                    ErrorDialog errorDialog
                        = new ErrorDialog(
                            chatPanel.getChatContainer().getFrame(),
                            GuiActivator.getResources()
                                .getI18NString("service.gui.KICK_FAILED"),
                            GuiActivator.getResources().getI18NString(
                            "service.gui.KICK_FAILED_NOT_ENOUGH_PERMISSIONS",
                            new String[]{chatContact.getName()}),
                            e);

                    errorDialog.showDialog();
                }
                else if (e.getErrorCode()
                    == OperationFailedException.FORBIDDEN)
                {
                    new ErrorDialog(
                        chatPanel.getChatContainer().getFrame(),
                        GuiActivator.getResources()
                            .getI18NString("service.gui.KICK_FAILED"),
                        GuiActivator.getResources()
                            .getI18NString("service.gui.KICK_FAILED_NOT_ALLOWED",
                            new String[]{chatContact.getName()}),
                            e).showDialog();
                }
                else
                {
                    new ErrorDialog(
                        chatPanel.getChatContainer().getFrame(),
                        GuiActivator.getResources()
                            .getI18NString("service.gui.KICK_FAILED"),
                        GuiActivator.getResources()
                            .getI18NString("service.gui.KICK_FAILED_GENERAL_ERROR",
                            new String[]{chatContact.getName()}),
                        e).showDialog();
                }
            }
        }
    }

    /**
     * Bans the the selected chat room participant or shows a message to the
     * user that he/she has not enough permissions to do a ban.
     */
    private class BanParticipantThread extends Thread
    {
        private final ChatRoom chatRoom;

        private final String reason;

        BanParticipantThread(ChatRoom chatRoom, String reason)
        {
            this.chatRoom = chatRoom;
            this.reason = reason;
        }

        @Override
        public void run()
        {
            try
            {
                chatRoom.banParticipant(
                    (ChatRoomMember) chatContact.getDescriptor(),
                    reason);
            }
            catch (OperationFailedException e)
            {
                logger.error("Failed to ban participant.", e);

                String errorTitle = GuiActivator.getResources()
                    .getI18NString("service.gui.BAN_FAILED");
                String errorMessageKey;

                switch (e.getErrorCode()) {
                case OperationFailedException.NOT_ENOUGH_PRIVILEGES:
                    errorMessageKey =
                        "service.gui.BAN_FAILED_NOT_ENOUGH_PERMISSIONS";
                    break;
                case OperationFailedException.FORBIDDEN:
                    errorMessageKey = "service.gui.BAN_FAILED_NOT_ALLOWED";
                    break;
                default:
                    errorMessageKey = "service.gui.BAN_FAILED_GENERAL_ERROR";
                    break;
                }
                new ErrorDialog(
                    chatPanel.getChatContainer().getFrame(),
                    errorTitle,
                    GuiActivator.getResources().getI18NString(
                        errorMessageKey,
                        new String[]{chatContact.getName()}),
                    e).showDialog();
            }
        }
    }

    /**
     * Reloads icons for all menu items.
     */
    public void loadSkin()
    {
        this.grantOwnershipItem.setIcon(ImageUtils.getScaledRoundedIcon(
            ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_OWNER), 16, 16));
        this.grantAdminItem.setIcon(ImageUtils.getScaledRoundedIcon(
            ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_ADMIN), 16, 16));
        this.grantMembershipItem.setIcon(ImageUtils.getScaledRoundedIcon(
            ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_STANDARD), 16, 16));
        this.grantModeratorItem.setIcon(ImageUtils.getScaledRoundedIcon(
            ImageLoader.getImage(
                ImageLoader.CHATROOM_MEMBER_MODERATOR), 16, 16));
        this.grantVoiceItem.setIcon(ImageUtils.getScaledRoundedIcon(
            ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_STANDARD), 16, 16));
        this.revokeAdminItem.setIcon(ImageUtils.getScaledRoundedIcon(
            ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_STANDARD), 16, 16));
        this.revokeMembershipItem.setIcon(ImageUtils.getScaledRoundedIcon(
            ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_GUEST), 16, 16));
        this.revokeModeratorItem.setIcon(ImageUtils.getScaledRoundedIcon(
            ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_STANDARD), 16, 16));
        this.revokeOwnershipItem.setIcon(ImageUtils.getScaledRoundedIcon(
            ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_ADMIN), 16, 16));
        this.revokeVoiceItem.setIcon(ImageUtils.getScaledRoundedIcon(
            ImageLoader.getImage(ImageLoader.CHAT_ROOM_REVOKE_VOICE), 16, 16));
        this.kickItem.setIcon(new ImageIcon(ImageLoader.getImage(
            ImageLoader.KICK_ICON_16x16)));
        this.banItem.setIcon(new ImageIcon(ImageLoader.getImage(
            ImageLoader.BAN_ICON_16x16)));
        this.changeNicknameItem.setIcon(new ImageIcon(ImageLoader.getImage(
            ImageLoader.CHANGE_NICKNAME_ICON_16x16)));
        this.changeRoomSubjectItem.setIcon(new ImageIcon(ImageLoader.getImage(
            ImageLoader.CHANGE_ROOM_SUBJECT_ICON_16x16)));
    }
}
