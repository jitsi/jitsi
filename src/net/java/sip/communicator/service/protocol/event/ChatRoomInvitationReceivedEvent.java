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
 * <tt>ChatRoomInvitationReceivedEvent</tt>s indicate reception of an
 * invitation to join a chat room.
 *
 * @author Emil Ivov
 * @author Stephane Remy
 * @author Yana Stamcheva
 */
public class ChatRoomInvitationReceivedEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The invitation corresponding to this event.
     */
    private final ChatRoomInvitation invitation;

    /**
     * A timestamp indicating the exact date when the event occurred.
     */
    private final Date timestamp;

    /**
     * Creates an <tt>InvitationReceivedEvent</tt> representing reception of
     * the <tt>source</tt> invitation received from the specified
     * <tt>from</tt> chat room member.
     *
     * @param multiUserChatOpSet the <tt>OperationSetMultiUserChat</tt>, which
     * dispatches this event
     * @param invitation the <tt>ChatRoomInvitation</tt> that this event is for
     * @param timestamp the exact date when the event ocurred.
     */
    public ChatRoomInvitationReceivedEvent(
        OperationSetMultiUserChat multiUserChatOpSet,
        ChatRoomInvitation invitation,
        Date timestamp)
    {
        super(multiUserChatOpSet);

        this.invitation = invitation;
        this.timestamp = timestamp;
    }

    /**
     * Returns the multi user chat operation set that dispatches this event.
     *
     * @return the multi user chat operation set that dispatches this event.
     */
    public OperationSetMultiUserChat getSourceOperationSet()
    {
        return (OperationSetMultiUserChat) getSource();
    }

    /**
     * Returns the <tt>ChatRoomInvitation</tt> that this event is for.
     *
     * @return the <tt>ChatRoomInvitation</tt> that this event is for.
     */
    public ChatRoomInvitation getInvitation()
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
