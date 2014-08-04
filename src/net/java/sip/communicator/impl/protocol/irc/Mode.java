/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

/**
 * IRC Modes enum.
 *
 * @author Danny van Heumen
 */
public enum Mode
{
    UNKNOWN('?', null),
    OWNER('O', ChatRoomMemberRole.OWNER),
    OPERATOR('o', ChatRoomMemberRole.ADMINISTRATOR),
    HALFOP('h', ChatRoomMemberRole.MODERATOR),
    VOICE('v', ChatRoomMemberRole.MEMBER),
    LIMIT('l', null),
    PRIVATE('p', null),
    SECRET('s', null),
    INVITE('i', null),
    BAN('b', null);

    /**
     * Find Mode instance by mode char.
     *
     * @param symbol mode char
     * @return returns instance
     */
    public static Mode bySymbol(final char symbol)
    {
        for (Mode mode : Mode.values())
        {
            if (mode.getSymbol() == symbol)
            {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown mode symbol provided. ('"
            + symbol + "')");
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