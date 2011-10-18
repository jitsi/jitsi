/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * A listener that will be notified of changes in the role of a chat
 * participant in a particular chat room. Changes may include participant being
 * granted any of the roles defined in <tt>ChatRoomMemberRole</tt>.
 * 
 * @see net.java.sip.communicator.service.protocol.ChatRoomMemberRole
 *  
 * @author Emil Ivov
 * @author Stephane Remy
 */
public interface ChatRoomMemberRoleListener
    extends EventListener
{
    /**
     * Called to notify interested parties that a change in the role of a
     * chat room member has occurred.
     * 
     * @param evt the <tt>ChatRoomMemberRoleChangeEvent</tt> instance
     * containing the source chat room and role old and new state.
     */
    public void memberRoleChanged(ChatRoomMemberRoleChangeEvent evt);

}
