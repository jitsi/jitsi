package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;

/**
 * IRC Modes enum
 * 
 * @author danny
 */
public enum Mode
{
    OWNER('O', ChatRoomMemberRole.OWNER),
    OPERATOR('o', ChatRoomMemberRole.ADMINISTRATOR),
    VOICE('v', ChatRoomMemberRole.MEMBER),
    LIMIT('l', null);

    /**
     * Find Mode instance by mode char.
     * 
     * @param symbol mode char
     * @return returns instance
     */
    public static Mode bySymbol(char symbol)
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
     * mode char
     */
    final private char symbol;

    /**
     * ChatRoomMemberRole or null
     */
    final private ChatRoomMemberRole role;

    /**
     * Create Mode instance.
     * 
     * @param symbol mode char
     * @param role ChatRoomMemberRole or null
     */
    private Mode(char symbol, ChatRoomMemberRole role)
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
