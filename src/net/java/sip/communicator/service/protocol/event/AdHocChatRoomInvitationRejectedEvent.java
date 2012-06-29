/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>AdHocChatRoomInvitationRejectedEvent</tt>s indicates the reception of a
 * rejection of an invitation.
 *
 * @author Valentin Martinet
 */
@SuppressWarnings("serial")
public class AdHocChatRoomInvitationRejectedEvent
    extends EventObject
{
    /**
     * The <tt>AdHocChatRoom</tt> for which the initial invitation was.
     */
    private AdHocChatRoom adHocChatRoom;

    /**
     * The invitee that rejected the invitation.
     */
    private String invitee;

    /**
     * The reason why this invitation is rejected or null if there is no reason
     * specified.
     */
    private String reason;

    /**
     * The exact date at which this event occured.
     */
    private Date timestamp;

    /**
     * Creates a <tt>AdHocChatRoomInvitationRejectedEvent</tt> representing the
     * rejection of an invitation, rejected by the given <tt>invitee</tt>.
     *
     * @param source the <tt>OperationSetAdHocMultiUserChat</tt> that dispatches
     * this
     * event
     * @param adHocChatRoom the <tt>AdHocChatRoom</tt> for which the initial 
     * invitation was
     * @param invitee the name of the invitee that rejected the invitation
     * @param reason the reason of the rejection
     * @param timestamp the exact date when the event ocurred
     */
    public AdHocChatRoomInvitationRejectedEvent( 
                                        OperationSetAdHocMultiUserChat source,
                                        AdHocChatRoom adHocChatRoom,
                                        String invitee,
                                        String reason,
                                        Date timestamp)
    {
        super(source);

        this.adHocChatRoom = adHocChatRoom;
        this.invitee = invitee;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    /**
     * Returns the ad-hoc multi user chat operation set that dispatches this 
     * event.
     * 
     * @return the ad-hoc multi user chat operation set that dispatches this 
     * event
     */
    public OperationSetAdHocMultiUserChat getSourceOperationSet()
    {
        return (OperationSetAdHocMultiUserChat)getSource();
    }

    /**
     * Returns the <tt>AdHocChatRoom</tt> for which the initial invitation was.
     * 
     * @return the <tt>AdHocChatRoom</tt> for which the initial invitation was
     */
    public AdHocChatRoom getChatRoom()
    {
        return adHocChatRoom;
    }

    /**
     * Returns the name of the invitee that rejected the invitation.
     *
     * @return the name of the invitee that rejected the invitation
     */
    public String getInvitee()
    {
        return invitee;
    }

    /**
     * Returns the reason for which the <tt>AdHocChatRoomInvitation</tt> is 
     * rejected.
     *
     * @return the reason for which the <tt>AdHocChatRoomInvitation</tt> is 
     * rejected.
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * A timestamp indicating the exact date when the event occurred.
     * @return a Date indicating when the event occurred.
     */
    public Date getTimestamp()
    {
        return timestamp;
    }
}
