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
package net.java.sip.communicator.impl.protocol.jabber;

import net.java.sip.communicator.service.protocol.*;

/**
 * The Jabber implementation of the <tt>ChatRoomInvitation</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomInvitationJabberImpl
    implements ChatRoomInvitation
{
    private ChatRoom chatRoom;

    private String inviter;

    private String reason;

    private byte[] password;

    /**
     * Creates an invitation for the given <tt>targetChatRoom</tt>, from the
     * given <tt>inviter</tt>.
     *
     * @param targetChatRoom the <tt>ChatRoom</tt> for which the invitation is
     * @param inviter the <tt>ChatRoomMember</tt>, which sent the invitation
     * @param reason the reason of the invitation
     * @param password the password
     */
    public ChatRoomInvitationJabberImpl(ChatRoom targetChatRoom,
                                        String inviter,
                                        String reason,
                                        byte[] password)
    {
        this.chatRoom = targetChatRoom;
        this.inviter = inviter;
        this.reason = reason;
        this.password = password;
    }

    public ChatRoom getTargetChatRoom()
    {
        return chatRoom;
    }

    public String getInviter()
    {
        return inviter;
    }

    public String getReason()
    {
        return reason;
    }

    public byte[] getChatRoomPassword()
    {
        return password;
    }
}
