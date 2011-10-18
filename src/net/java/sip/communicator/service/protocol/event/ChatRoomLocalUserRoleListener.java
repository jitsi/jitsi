/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A listener that will be notified of changes in the role of the local
 * user participant in a particular chat room. Changes could be us being granted
 * any of the roles defined in <tt>ChatRoomMemberRole</tt>.
 * 
 * @see net.java.sip.communicator.service.protocol.ChatRoomMemberRole
 * 
 * @author Stephane Remy
 */
public interface ChatRoomLocalUserRoleListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in the role of the
     * local user participant in a particular chat room has occurred.
     * @param evt the <tt>ChatRoomLocalUserRoleChangeEvent</tt> instance
     * containing the source chat room and role old and new state.
     */
    public void localUserRoleChanged(ChatRoomLocalUserRoleChangeEvent evt);
}
