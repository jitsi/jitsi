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
 * <tt>ChatRoomInvitationReceivedEvent</tt>s indicate reception of an
 * invitation to join a chat room.
 * 
 * @author Emil Ivov
 * @author Stephane Remy
 */
public class ChatRoomInvitationReceivedEvent
    extends EventObject
{
    /**
     * The chat room member that has sent this invitation.
     */
    private ChatRoomMember from = null;

    /**
     * The chat room member that is the target of this invitation.
     */
    private ChatRoomMember to = null;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private Date timestamp = null;

    /**
     * Creates an <tt>InvitationReceivedEvent</tt> representing reception of
     * the <tt>source</tt> invitation received from the specified
     * <tt>from</tt> chat room member.
     *
     * @param source the <tt>ChatRoomInvitation</tt> whose reception this event
     * represents.
     * @param from the <tt>ChatRoomMember</tt> that has sent this invitation.
     * @param to the <tt>ChatRoomMember</tt> that is the target of this
     * invitation.
     * @param timestamp the exact date when the event ocurred.
     */
    public ChatRoomInvitationReceivedEvent(ChatRoomInvitation source,
        ChatRoomMember from, ChatRoomMember to, Date timestamp)
    {
        super(source);

        this.from = from;
        this.to = to;
        this.timestamp = timestamp;
    }

    /**
     * Returns a reference to the <tt>ChatRoomMember</tt> that has sent the
     * <tt>ChatRoomInvitation</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>ChatRoomMember</tt> that has sent the
     * <tt>ChatRoomInvitation</tt> whose reception this event represents.
     */
    public ChatRoomMember getFrom()
    {
        return from;
    }

    /**
     * Returns a reference to the <tt>ChatRoomMember</tt> that is the target
     * of the <tt>ChatRoomInvitation</tt> whose reception this event represents.
     *
     * @return a reference to the <tt>ChatRoomMember</tt> that is the target
     * of the <tt>ChatRoomInvitation</tt> whose reception this event represents.
     */
    public ChatRoomMember getTo()
    {
        return to;
    }

    /**
     * Returns the <tt>ChatRoomInvitation</tt> that triggered this event
     *
     * @return the <tt>ChatRoomInvitation</tt> that triggered this event.
     */
    public ChatRoomInvitation getSourceInvitation()
    {
        return (ChatRoomInvitation) getSource();
    }

    /**
     * A timestamp indicating the exact date when the event ocurred.
     *
     * @return a Date indicating when the event ocurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }
}
