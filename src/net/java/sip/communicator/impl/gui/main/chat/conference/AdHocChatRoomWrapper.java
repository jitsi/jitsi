/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
