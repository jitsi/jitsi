/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Dispatched to notify interested parties that a change in our role in the
 * source chat room has occurred. Changes may include us being granted admin
 * permissions, or other permissions.
 *
 * @see ChatRoomMemberRole
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public class ChatRoomLocalUserRoleChangeEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The previous role that local participant had.
     */
    private ChatRoomMemberRole previousRole = null;

    /**
     * The new role that local participant get.
     */
    private ChatRoomMemberRole newRole = null;

    /**
     * Creates a <tt>ChatRoomLocalUserRoleChangeEvent</tt> representing that
     * a change in local participant role in the source chat room has
     * occured.
     *
     * @param sourceRoom the <tt>ChatRoom</tt> that produced the event
     * @param previousRole the previous role that local participant had
     * @param newRole the new role that local participant get
     */
    public ChatRoomLocalUserRoleChangeEvent(ChatRoom sourceRoom,
                                        ChatRoomMemberRole previousRole,
                                        ChatRoomMemberRole newRole)
    {
        super(sourceRoom);
        this.previousRole = previousRole;
        this.newRole = newRole;
    }

    /**
     * Returns the new role the local participant get.
     *
     * @return newRole the new role the local participant get
     */
    public ChatRoomMemberRole getNewRole()
    {
        return newRole;
    }

    /**
     * Returns the previous role that local participant had.
     *
     * @return previousRole the previous role that local participant had
     */
    public ChatRoomMemberRole getPreviousRole()
    {
        return previousRole;
    }

    /**
     * Returns the <tt>ChatRoom</tt>, where this event occured.
     *
     * @return the <tt>ChatRoom</tt>, where this event occured
     */
    public ChatRoom getSourceChatRoom()
    {
        return (ChatRoom)getSource();
    }
}
