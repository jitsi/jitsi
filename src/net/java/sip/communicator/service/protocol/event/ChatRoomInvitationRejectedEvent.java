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
 * <tt>ChatRoomInvitationRejectedEvent</tt>s indicates the reception of a
 * rejection of an invitation.
 *
 * @author Emil Ivov
 * @author Stephane Remy
 */
public class ChatRoomInvitationRejectedEvent
    extends EventObject
{
    /**
     * The chat room member that has sent this rejection.
     */
    private ChatRoomMember from = null;
    
    /**
     * The chat room member that is the target of this rejection.
     */
    private ChatRoomMember to = null;
    
    /**
     * The reason why this invitation is rejected or null if there is no reason
     * specified.
     */
    private String reason = null;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private Date timestamp = null;
    
    /**
     * Creates a <tt>ChatRoomInvitationRejectedEvent</tt> representing the
     * rejection of the <tt>source</tt> invitation, rejected from the specified
     * <tt>from</tt> chat room member.
     *
     * @param source the <tt>ChatRoomInvitation</tt> whose rejection this event
     * represents
     * @param from the <tt>ChatRoomMember</tt> that has sent this invitation
     * rejection
     * @param to the <tt>ChatRoomMember</tt> that is the target of this
     * invitation rejection
     * @param timestamp the exact date when the event ocurred.
     */
    public ChatRoomInvitationRejectedEvent(ChatRoomInvitation source,
        ChatRoomMember from, ChatRoomMember to, String reason, Date timestamp)
    {
        super(source);
        
        this.from = from;
        this.to = to;
        this.reason = reason;
        this.timestamp = timestamp;
    }
    
    /**
     * Returns a reference to the <tt>ChatRoomMember</tt> that has sent the
     * rejection.
     *
     * @return a reference to the <tt>ChatRoomMember</tt> that has sent the
     * rejection
     */
    public ChatRoomMember getFrom()
    {
        return from;
    }
    
    /**
     * Returns a reference to the <tt>ChatRoomMember</tt> that is the target of
     * this rejection.
     *
     * @return a reference to the <tt>ChatRoomMember</tt> that is the target of
     * this rejection
     */
    public ChatRoomMember getTo()
    {
        return to;
    }
    
    /**
     * Returns the reason for which the <tt>ChatRoomInvitation</tt> is rejected.
     *
     * @return the reason for which the <tt>ChatRoomInvitation</tt> is rejected.
     */
    public String getReason()
    {
        return reason;
    }
    
    /**
     * Returns the <tt>ChatRoomInvitation</tt> that was rejected.
     * 
     * @return the <tt>ChatRoomInvitation</tt> that was rejected.
     */
    public ChatRoomInvitation getSourceInvitation()
    {
        return (ChatRoomInvitation)getSource();
    }
    
    /**
     * A timestamp indicating the exact date when the event ocurred.
     * @return a Date indicating when the event ocurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }

}
