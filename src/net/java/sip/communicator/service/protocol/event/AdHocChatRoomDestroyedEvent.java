/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * The event that occurs when an ad-hoc chat room has been created.
 * 
 * @author Valentin Martinet
 */
public class AdHocChatRoomDestroyedEvent
{
    /**
     * The ad-hoc room that has been created.
     */
    private AdHocChatRoom adHocChatRoom;

    /**
     * The <tt>Contact</tt> who created the ad-hoc room.
     */
    private Contact by;

    /**
     * Initializes an <tt>AdHocChatRoomDestroyedEvent</tt> with the creator
     * (<tt> by</tt>) and the ad-hoc room <tt>adHocChatRoom</tt>.
     * 
     * @param adHocChatRoom the <tt>AdHocChatRoom</tt>
     * @param by the <tt>Contact</tt> who created this ad-hoc room
     */
    public AdHocChatRoomDestroyedEvent(AdHocChatRoom adHocChatRoom, Contact by)
    {
        this.adHocChatRoom = adHocChatRoom;
        this.by = by;
    }

    /**
     * Returns the <tt>Contact</tt> who created the room.
     * 
     * @return <tt>Contact</tt>
     */
    public Contact getBy()
    {
        return this.by;
    }

    /**
     * Returns the ad-hoc room concerned by this event.
     * 
     * @return <tt>AdHocChatRoom</tt>
     */
    public AdHocChatRoom getAdHocDestroyedRoom()
    {
        return this.adHocChatRoom;
    }
}
