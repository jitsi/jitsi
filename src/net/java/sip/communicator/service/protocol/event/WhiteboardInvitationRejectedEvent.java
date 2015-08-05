/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>WhiteboardInvitationRejectedEvent</tt>s indicates the reception of a
 * rejection of an invitation.
 *
 * @author Yana Stamcheva
 */
public class WhiteboardInvitationRejectedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>WhiteboardSession</tt> for which the initial invitation was.
     */
    private WhiteboardSession whiteboardSession;

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
     * Creates a <tt>WhiteboardInvitationRejectedEvent</tt> representing the
     * rejection of an invitation, rejected by the given <tt>invitee</tt>.
     *
     * @param source the <tt>OperationSetWhiteboarding</tt> that dispatches this
     * event
     * @param session the <tt>WhiteboardSession</tt> for which the initial
     * invitation was
     * @param invitee the name of the invitee that rejected the invitation
     * @param reason the reason of the rejection
     * @param timestamp the exact date when the event ocurred
     */
    public WhiteboardInvitationRejectedEvent(
            OperationSetWhiteboarding source,
            WhiteboardSession session,
            String invitee,
            String reason,
            Date timestamp)
    {
        super(source);

        this.whiteboardSession = session;
        this.invitee = invitee;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    /**
     * Returns the whiteboarding operation set that dispatches this event.
     *
     * @return the whiteboarding operation set that dispatches this event
     */
    public OperationSetWhiteboarding getSourceOperationSet()
    {
        return (OperationSetWhiteboarding) getSource();
    }

    /**
     * Returns the <tt>WhiteboardSession</tt> for which the initial invitation
     * was.
     *
     * @return the <tt>WhiteboardSession</tt> for which the initial invitation
     * was
     */
    public WhiteboardSession getWhiteboardSession()
    {
        return whiteboardSession;
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
     * Returns the reason for which the <tt>ChatRoomInvitation</tt> is rejected.
     *
     * @return the reason for which the <tt>ChatRoomInvitation</tt> is rejected.
     */
    public String getReason()
    {
        return reason;
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
