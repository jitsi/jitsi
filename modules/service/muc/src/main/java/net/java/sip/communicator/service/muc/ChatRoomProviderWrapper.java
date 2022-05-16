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
package net.java.sip.communicator.service.muc;

import net.java.sip.communicator.service.protocol.*;

/**
 * @author Yana Stamcheva
 * @author Damian Minkov
 * @author Hristo Terezov
 */
public interface ChatRoomProviderWrapper
{

    /**
     * Returns the name of this chat room provider.
     * @return the name of this chat room provider.
     */
    public String getName();

    public byte[] getIcon();
   

    public byte[] getImage();

    /**
     * Returns the system room wrapper corresponding to this server.
     *
     * @return the system room wrapper corresponding to this server.
     */
    public ChatRoomWrapper getSystemRoomWrapper();

    /**
     * Sets the system room corresponding to this server.
     *
     * @param systemRoom the system room to set
     */
    public void setSystemRoom(ChatRoom systemRoom);

    /**
     * Returns the protocol provider service corresponding to this server
     * wrapper.
     *
     * @return the protocol provider service corresponding to this server
     * wrapper.
     */
    public ProtocolProviderService getProtocolProvider();

    /**
     * Adds the given chat room to this chat room provider.
     *
     * @param chatRoom the chat room to add.
     */
    public void addChatRoom(ChatRoomWrapper chatRoom);

    /**
     * Removes the given chat room from this provider.
     *
     * @param chatRoom the chat room to remove.
     */
    public void removeChatRoom(ChatRoomWrapper chatRoom);

    /**
     * Returns <code>true</code> if the given chat room is contained in this
     * provider, otherwise - returns <code>false</code>.
     *
     * @param chatRoom the chat room to search for.
     * @return <code>true</code> if the given chat room is contained in this
     * provider, otherwise - returns <code>false</code>.
     */
    public boolean containsChatRoom(ChatRoomWrapper chatRoom);

    /**
     * Returns the chat room wrapper contained in this provider that corresponds
     * to the given chat room.
     *
     * @param chatRoom the chat room we're looking for.
     * @return the chat room wrapper contained in this provider that corresponds
     * to the given chat room.
     */
    public ChatRoomWrapper findChatRoomWrapperForChatRoom(ChatRoom chatRoom);
    
    /**
     * Returns the chat room wrapper contained in this provider that corresponds
     * to the chat room with the given id.
     *
     * @param chatRoomID the id of the chat room we're looking for.
     * @return the chat room wrapper contained in this provider that corresponds
     * to the given chat room id.
     */
    public ChatRoomWrapper findChatRoomWrapperForChatRoomID(String chatRoomID);

    /**
     * Returns the number of chat rooms contained in this provider.
     *
     * @return the number of chat rooms contained in this provider.
     */
    public int countChatRooms();

    public ChatRoomWrapper getChatRoom(int index);

    /**
     * Returns the index of the given chat room in this provider.
     *
     * @param chatRoomWrapper the chat room to search for.
     *
     * @return the index of the given chat room in this provider.
     */
    public int indexOf(ChatRoomWrapper chatRoomWrapper);

    /**
     * Goes through the locally stored chat rooms list and for each
     * {@link ChatRoomWrapper} tries to find the corresponding server stored
     * {@link ChatRoom} in the specified operation set. Joins automatically all
     * found chat rooms.
     */
    public void synchronizeProvider();
}
