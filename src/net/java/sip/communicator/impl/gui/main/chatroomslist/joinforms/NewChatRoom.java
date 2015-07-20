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
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import net.java.sip.communicator.service.muc.*;

/**
 * The <tt>NewChatRoom</tt> is meant to be used from the
 * <tt>JoinChatRoomWizard</tt>, to collect information concerning the chat
 * room to join.
 *
 * @author Yana Stamcheva
 */
public class NewChatRoom
{
    private ChatRoomProviderWrapper chatRoomProvider;

    private String chatRoomName;

    /**
     * Returns the name of the chat room.
     *
     * @return the name of the chat room
     */
    public String getChatRoomName()
    {
        return chatRoomName;
    }

    /**
     * Sets the name of the chat room.
     *
     * @param chatRoomName the name of the chat room
     */
    public void setChatRoomName(String chatRoomName)
    {
        this.chatRoomName = chatRoomName;
    }

    /**
     * Returns the chat room provider corresponding to the chosen account.
     *
     * @return the chat room provider corresponding to the chosen account
     */
    public ChatRoomProviderWrapper getChatRoomProvider()
    {
        return chatRoomProvider;
    }

    /**
     * Sets the chat room provider corresponding to the chosen account.
     *
     * @param provider the chat room provider corresponding to
     * the chosen account
     */
    public void setChatRoomProvider(ChatRoomProviderWrapper provider)
    {
        this.chatRoomProvider = provider;
    }
}
