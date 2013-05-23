/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A listener that will be notified of changes in the presence of a member in a
 * particular chat room. Changes may include member being kicked, join, left.
 *
 * @author Emil Ivov
 */
public interface ChatRoomMemberPresenceListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in the presence of a
     * member in a particular chat room has occurred. Changes may include member
     * being kicked, join, left.
     *
     * @param evt the <tt>ChatRoomMemberPresenceChangeEvent</tt> instance
     * containing the source chat room and type, and reason of the presence
     * change
     */
    public void memberPresenceChanged(ChatRoomMemberPresenceChangeEvent evt );

}
