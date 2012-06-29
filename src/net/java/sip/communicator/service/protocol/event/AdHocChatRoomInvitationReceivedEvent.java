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
 * <tt>AdHocChatRoomInvitationReceivedEvent</tt>s indicate reception of an
 * invitation to join an ad-hoc chat room.
 * 
 * @author Valentin Martinet
 */
@SuppressWarnings("serial")
public class AdHocChatRoomInvitationReceivedEvent
    extends EventObject
{
    /**
     * The invitation corresponding to this event.
     */
    private final AdHocChatRoomInvitation invitation;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private final Date timestamp;

    /**
     * Creates an <tt>InvitationReceivedEvent</tt> representing reception of
     * the <tt>source</tt> invitation received from the specified
     * <tt>from</tt> ad-hoc chat room participant.
     *
     * @param adHocMultiUserChatOpSet the 
     * <tt>OperationSetAdHocMultiUserChat</tt>, which dispatches this event
     * @param invitation the <tt>AdHocChatRoomInvitation</tt> that this event is
     * for
     * @param timestamp the exact date when the event occurred.
     */
    public AdHocChatRoomInvitationReceivedEvent(
        OperationSetAdHocMultiUserChat adHocMultiUserChatOpSet,
        AdHocChatRoomInvitation invitation,
        Date timestamp)
    {
        super(adHocMultiUserChatOpSet);

        this.invitation = invitation;
        this.timestamp = timestamp;
    }

    /**
     * Returns the ad-hoc multi user chat operation set that dispatches this 
     * event.
     * 
     * @return the ad-hoc multi user chat operation set that dispatches this 
     * event.
     */
    public OperationSetAdHocMultiUserChat getSourceOperationSet()
    {
        return (OperationSetAdHocMultiUserChat) getSource();
    }

    /**
     * Returns the <tt>AdHocChatRoomInvitation</tt> that this event is for.
     *
     * @return the <tt>AdHocChatRoomInvitation</tt> that this event is for.
     */
    public AdHocChatRoomInvitation getInvitation()
    {
        return invitation;
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
