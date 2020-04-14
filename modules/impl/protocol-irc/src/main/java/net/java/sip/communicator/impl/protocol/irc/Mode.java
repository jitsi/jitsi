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
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.impl.protocol.irc.exception.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * IRC Modes enum.
 *
 * @author Danny van Heumen
 */
public enum Mode
{
    /**
     * Instance for unknown mode type.
     */
    UNKNOWN('?', null),

    /**
     * Mode 'Owner'. (Not recognized by all IRC servers.)
     */
    OWNER('O', ChatRoomMemberRole.OWNER),

    /**
     * Mode 'Operator'.
     */
    OPERATOR('o', ChatRoomMemberRole.ADMINISTRATOR),

    /**
     * Mode 'Half-Operator'. (Not recognized by all IRC servers.)
     */
    HALFOP('h', ChatRoomMemberRole.MODERATOR),

    /**
     * Mode 'Voice'. For giving voice to an IRC member which comes into effect
     * in a moderated channel.
     */
    VOICE('v', ChatRoomMemberRole.MEMBER),

    /**
     * Mode 'Limit'.
     */
    LIMIT('l', null),

    /**
     * Mode 'Private'.
     */
    PRIVATE('p', null),

    /**
     * Mode 'Secret'.
     */
    SECRET('s', null),

    /**
     * Mode 'Invite'.
     */
    INVITE('i', null),

    /**
     * Mode 'Ban'.
     */
    BAN('b', null);

    /**
     * Find Mode instance by mode char.
     *
     * @param symbol mode char
     * @return returns instance
     * @throws UnknownModeException throws exception in case of unknown mode
     *             symbol
     */
    public static Mode bySymbol(final char symbol) throws UnknownModeException
    {
        for (Mode mode : Mode.values())
        {
            if (mode.getSymbol() == symbol)
            {
                return mode;
            }
        }
        throw new UnknownModeException(symbol);
    }

    /**
     * mode char.
     */
    private final char symbol;

    /**
     * ChatRoomMemberRole or null.
     */
    private final ChatRoomMemberRole role;

    /**
     * Create Mode instance.
     *
     * @param symbol mode char
     * @param role ChatRoomMemberRole or null
     */
    private Mode(final char symbol, final ChatRoomMemberRole role)
    {
        this.symbol = symbol;
        this.role = role;
    }

    /**
     * Get character symbol for mode.
     *
     * @return returns char symbol
     */
    public char getSymbol()
    {
        return this.symbol;
    }

    /**
     * Get corresponding ChatRoomMemberRole instance if available or null
     * otherwise.
     *
     * @return returns ChatRoomMemberRole instance or null
     */
    public ChatRoomMemberRole getRole()
    {
        return this.role;
    }
}
