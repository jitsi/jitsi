package net.java.sip.communicator.impl.protocol.irc;

import java.util.ArrayList;
import java.util.List;

import com.ircclouds.irc.api.domain.messages.ChannelModeMessage;

public class ModeParser
{
    private final List<Mode> modes = new ArrayList<Mode>();
    private int index = 0;
    private String[] params;
    
    public ModeParser(ChannelModeMessage message)
    {
        this(message.getModeStr());
    }
    
    protected ModeParser(String modestring)
    {
        String[] parts = modestring.split(" ");
        String mode = parts[0];
        this.params = new String[parts.length-1];
        System.arraycopy(parts, 1, this.params, 0, parts.length-1);
        parse(mode);
    }

    private void parse(String modestring)
    {
        boolean adding = true;
        for(char c : modestring.toCharArray())
        {
            switch(c)
            {
            case '+':
                adding = true;
                break;
            case '-':
                adding = false;
                break;
            default:
                try
                {
                    Mode entry = process(adding, c);
                    modes.add(entry);
                }
                catch(IllegalArgumentException e)
                {
                    System.out.println("Unknown mode encountered: '"+c+"' (mode string '"+modestring+"')");
                }
                break;
            }
        }
    }
    
    private Mode process(boolean add, char mode)
    {
        switch(mode)
        {
        case 'O':
            return new Mode(add, mode, this.params[this.index++]);
        case 'o':
            return new Mode(add, mode, this.params[this.index++]);
        case 'v':
            return new Mode(add, mode, this.params[this.index++]);
        default:
            throw new IllegalArgumentException(""+mode);
        }
    }
    
    public List<Mode> getModes()
    {
        return modes;
    }
    
    public static class Mode
    {
        private final boolean added;
        //TODO Danny: Use enums for modes instead of characters.
        private final char mode;
        private final String[] params;
        
        protected Mode(boolean add, char mode, String... params)
        {
            this.added = add;
            this.mode = mode;
            this.params = params;
        }
        
        public boolean isAdded()
        {
            return this.added;
        }
        
        public char getMode()
        {
            return this.mode;
        }
        
        public String[] getParams()
        {
            return this.params;
        }
    }
}
