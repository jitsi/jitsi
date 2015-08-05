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
package net.java.sip.communicator.service.protocol;

/**
 * This interface represents an invitation, which is send from an ad-hoc chat
 * room participant to another user in order to invite this user to join the
 * ad-hoc chat room.
 *
 * @author Valentin Martinet
 */
public interface AdHocChatRoomInvitation
{
    /**
     * Returns the <tt>AdHocChatRoom</tt>, which is the  target of this
     * invitation.
     * The ad-hoc chat room returned by this method will be the room to which
     * the user
     * is invited to join to.
     *
     * @return the <tt>AdHocChatRoom</tt>, which is the  target of this
     * invitation
     */
    public AdHocChatRoom getTargetAdHocChatRoom();

    /**
     * Returns the <tt>Contact</tt> that sent this invitation.
     *
     * @return the <tt>Contact</tt> that sent this invitation.
     */
    public String getInviter();

    /**
     * Returns the reason of this invitation, or null if there is no reason.
     *
     * @return the reason of this invitation, or null if there is no reason
     */
    public String getReason();
}
