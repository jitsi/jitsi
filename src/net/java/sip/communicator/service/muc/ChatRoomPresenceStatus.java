/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.muc;

import net.java.sip.communicator.service.protocol.*;

/**
 * The chat room statuses.
 * 
 * @author Hristo Terezov
 */
public class ChatRoomPresenceStatus extends PresenceStatus
{
    /**
     * An integer above which all values of the status coefficient indicate
     * eagerness to communicate
     */
    public static final int CHAT_ROOM_ONLINE_THRESHOLD = 86;
    
    /**
     * An integer above which all values of the status coefficient indicate
     * eagerness to communicate
     */
    public static final int CHAT_ROOM_OFFLINE_THRESHOLD = 87;
    
    /**
     * Indicates that the user is connected and ready to communicate.
     */
    public static final String ONLINE_STATUS = "Online";

    /**
     * Indicates that the user is disconnected.
     */
    public static final String OFFLINE_STATUS = "Offline";
    
    /**
     * The Online status. Indicate that the user is able and willing to
     * communicate in the chat room.
     */
    public static final ChatRoomPresenceStatus CHAT_ROOM_ONLINE
        = new ChatRoomPresenceStatus(
                86,
                ONLINE_STATUS);

    /**
     * The Offline  status. Indicates the user does not seem to be connected
     * to the chat room.
     */
    public static final ChatRoomPresenceStatus CHAT_ROOM_OFFLINE
        = new ChatRoomPresenceStatus(
                87,
                OFFLINE_STATUS);
    
    /**
     * Creates a status with the specified connectivity coeff and name for the 
     * chat rooms.
     * @param status the connectivity coefficient for the specified status
     * @param statusName String
     */
    protected ChatRoomPresenceStatus(int status, String statusName)
    {
        super(status, statusName);
    }
    
    @Override
    public boolean isOnline()
    {
        return getStatus() == CHAT_ROOM_ONLINE_THRESHOLD;
    }
    
}
