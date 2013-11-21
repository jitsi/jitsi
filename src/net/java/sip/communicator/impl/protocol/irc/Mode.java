package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.ChatRoomMemberRole;

public enum Mode
{
    OWNER('O', ChatRoomMemberRole.OWNER),
    OPERATOR('o', ChatRoomMemberRole.ADMINISTRATOR),
    VOICE('v', ChatRoomMemberRole.MEMBER);
    
    public static ChatRoomMemberRole convertSymbolToRole(char symbol)
    {
        for(Mode mode : Mode.values())
        {
            if (mode.getSymbol() == symbol)
            {
                return mode.getRole();
            }
        }
        throw new IllegalArgumentException("Invalid mode symbol provided. ('"+symbol+"')");
    }
    
    public static Mode bySymbol(char symbol)
    {
        for(Mode mode : Mode.values())
        {
            if (mode.getSymbol() == symbol)
            {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown mode symbol provided. ('"+symbol+"')");
    }
    
    final private char symbol;
    final private ChatRoomMemberRole role;
    
    private Mode(char symbol, ChatRoomMemberRole role)
    {
        this.symbol = symbol;
        this.role = role;
    }
    
    public char getSymbol()
    {
        return this.symbol;
    }
    
    public ChatRoomMemberRole getRole()
    {
        return this.role;
    }
}
