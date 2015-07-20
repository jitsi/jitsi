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

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Allows to determine which icon should be chosen for the chat contact
 * currently participating in a chatroom, regarding to his associated role.
 *
 * @author Valentin Martinet
 */
public class ChatContactRoleIcon
{
    /**
     * Returns the role icon which corresponds to the given role.
     *
     * @param role the role to analyse
     * @return Icon the associated Icon with this role
     */
    public static Icon getRoleIcon(ChatRoomMemberRole role)
    {
        Icon i = null;

        switch(role)
        {
            case OWNER: i = new ImageIcon(
                    ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_OWNER));
            break;
            case ADMINISTRATOR: i = new ImageIcon(
                    ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_ADMIN));
            break;
            case MODERATOR: i = new ImageIcon(
                    ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_MODERATOR));
            break;
            case MEMBER: i = new ImageIcon(
                    ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_STANDARD));
            break;
            case GUEST: i = new ImageIcon(
                    ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_GUEST));
            break;
            default: i = new ImageIcon(
                    ImageLoader.getImage(ImageLoader.CHATROOM_MEMBER_SILENT));
        }
        return i;
    }
}
