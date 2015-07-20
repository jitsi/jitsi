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
import net.java.sip.communicator.service.protocol.whiteboardobjects.*;

/**
 * The Jabber implementation of the <tt>WhiteboardInvitation</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class WhiteboardInvitationJabberImpl
    implements WhiteboardInvitation
{
    private WhiteboardSession whiteboardSession;

    private WhiteboardObject firstWhiteboardObject;

    private String inviter;

    private String reason;

    private byte[] password;

    /**
     * Creates an invitation for the given <tt>targetWhiteboard</tt>, from the
     * given <tt>inviter</tt>.
     *
     * @param targetWhiteboard the <tt>WhiteboardSession</tt> for which the
     * invitation is
     * @param firstWhiteboardObject the white-board object that inviter send
     * with this invitation and which will be shown on the white-board if the
     * user accepts the invitation
     * @param inviter the <tt>WhiteboardParticipant</tt>, which sent the
     * invitation
     * @param reason the reason of the invitation
     * @param password the password to use to join the given white-board
     */
    public WhiteboardInvitationJabberImpl(
        WhiteboardSession targetWhiteboard,
        WhiteboardObject firstWhiteboardObject,
        String inviter,
        String reason,
        byte[] password)
    {
        this.whiteboardSession = targetWhiteboard;
        this.firstWhiteboardObject = firstWhiteboardObject;
        this.inviter = inviter;
        this.reason = reason;
        this.password = password;
    }

    /**
     * Returns the <tt>WhiteboardSession</tt>, that this invitation is about.
     *
     * @return the <tt>WhiteboardSession</tt>, that this invitation is about
     */
    public WhiteboardSession getTargetWhiteboard()
    {
        return whiteboardSession;
    }

    /**
     * Returns the inviter, who sent this invitation.
     *
     * @return the inviter, who sent this invitation
     */
    public String getInviter()
    {
        return inviter;
    }

    /**
     * Returns the reason of the invitation.
     *
     * @return the reason of the invitation
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Returns the password to use in order to join the white-board, that this
     * invitation is about.
     *
     * @return the password to use in order to join the white-board, that this
     * invitation is about.
     */
    public byte[] getWhiteboardPassword()
    {
        return password;
    }

    /**
     * Returns the first white-board object that the inviter would like to
     * exchange with the user. If the user accepts this invitation he/she
     * should see this object on his white-board.
     *
     * @return the first white-board object
     */
    public WhiteboardObject getWhiteboardInitialObject()
    {
        return firstWhiteboardObject;
    }
}
