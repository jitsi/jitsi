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
