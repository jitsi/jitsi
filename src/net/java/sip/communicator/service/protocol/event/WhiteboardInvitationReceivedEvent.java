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
 * <tt>WhiteboardInvitationReceivedEvent</tt>s indicate reception of an
 * invitation to join a whiteboard.
 *
 * @author Yana Stamcheva
 */
public class WhiteboardInvitationReceivedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The invitation corresponding to this event.
     */
    private WhiteboardInvitation invitation;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private Date timestamp;

    /**
     * Creates an <tt>WhiteboardInvitationReceivedEvent</tt> representing
     * reception of the <tt>source</tt> invitation received from the specified
     * <tt>from</tt> white-board participant.
     *
     * @param whiteboardOpSet the <tt>OperationSetWhiteboarding</tt>, which
     * dispatches this event
     * @param invitation the <tt>WhiteboardInvitation</tt> that this event is
     * for.
     * @param timestamp the exact date when the event ocurred.
     */
    public WhiteboardInvitationReceivedEvent(
        OperationSetWhiteboarding whiteboardOpSet,
        WhiteboardInvitation invitation,
        Date timestamp)
    {
        super(whiteboardOpSet);

        this.invitation = invitation;
        this.timestamp = timestamp;
    }

    /**
     * Returns the whiteboarding operation set that dispatches this event.
     *
     * @return the whiteboarding operation set that dispatches this event.
     */
    public OperationSetWhiteboarding getSourceOperationSet()
    {
        return (OperationSetWhiteboarding) getSource();
    }

    /**
     * Returns the <tt>WhiteboardInvitation</tt> that this event is for.
     *
     * @return the <tt>WhiteboardInvitation</tt> that this event is for.
     */
    public WhiteboardInvitation getInvitation()
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
