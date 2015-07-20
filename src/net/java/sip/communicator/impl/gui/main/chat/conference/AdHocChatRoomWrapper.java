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

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>AdHocChatRoomWrapper</tt> is the representation of the
 * <tt>AdHocChatRoom</tt> in the GUI. It stores the information for the ad-hoc
 * chat room even when the corresponding protocol provider is not connected.
 *
 * @author Valentin Martinet
 */
public class AdHocChatRoomWrapper
{
    private final AdHocChatRoomProviderWrapper parentProvider;

    private AdHocChatRoom adHocChatRoom;

    private final String adHocChatRoomName;

    private final String adHocChatRoomID;

    /**
     * Creates a <tt>AdHocChatRoomWrapper</tt> by specifying the protocol
     * provider, the identifier and the name of the ad-hoc chat room.
     *
     * @param parentProvider the protocol provider to which the corresponding
     * ad-hoc chat room belongs
     * @param adHocChatRoomID the identifier of the corresponding ad-hoc chat
     * room
     * @param adHocChatRoomName the name of the corresponding  ad-hoc chat room
     */
    public AdHocChatRoomWrapper(AdHocChatRoomProviderWrapper parentProvider,
                                String adHocChatRoomID,
                                String adHocChatRoomName)
    {
        this.parentProvider = parentProvider;
        this.adHocChatRoomID = adHocChatRoomID;
        this.adHocChatRoomName = adHocChatRoomName;
    }

    /**
     * Creates a <tt>ChatRoomWrapper</tt> by specifying the corresponding chat
     * room.
     *
     * @param adHocChatRoom the chat room to which this wrapper corresponds.
     */
    public AdHocChatRoomWrapper( AdHocChatRoomProviderWrapper parentProvider,
                            AdHocChatRoom adHocChatRoom)
    {
        this(   parentProvider,
                adHocChatRoom.getIdentifier(),
                adHocChatRoom.getName());

        this.adHocChatRoom = adHocChatRoom;
    }

    /**
     * Returns the <tt>AdHocChatRoom</tt> that this wrapper represents.
     *
     * @return the <tt>AdHocChatRoom</tt> that this wrapper represents.
     */
    public AdHocChatRoom getAdHocChatRoom()
    {
        return adHocChatRoom;
    }

    /**
     * Sets the <tt>AdHocChatRoom</tt> that this wrapper represents.
     *
     * @param adHocChatRoom the ad-hoc chat room
     */
    public void setAdHocChatRoom(AdHocChatRoom adHocChatRoom)
    {
        this.adHocChatRoom = adHocChatRoom;
    }

    /**
     * Returns the ad-hoc chat room name.
     *
     * @return the ad-hoc chat room name
     */
    public String getAdHocChatRoomName()
    {
        return adHocChatRoomName;
    }

    /**
     * Returns the identifier of the ad-hoc chat room.
     *
     * @return the identifier of the ad-hoc chat room
     */
    public String getAdHocChatRoomID()
    {
        return adHocChatRoomID;
    }

    /**
     * Returns the parent protocol provider.
     *
     * @return the parent protocol provider
     */
    public AdHocChatRoomProviderWrapper getParentProvider()
    {
        return this.parentProvider;
    }
}
