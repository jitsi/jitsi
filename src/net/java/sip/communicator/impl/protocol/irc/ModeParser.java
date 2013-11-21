package net.java.sip.communicator.impl.protocol.irc;

import java.util.ArrayList;
import java.util.List;

import com.ircclouds.irc.api.domain.messages.ChannelModeMessage;

public class ModeParser
{
    private final List<ModeEntry> modes = new ArrayList<ModeEntry>();
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
                    ModeEntry entry = process(adding, c);
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
    
    private ModeEntry process(boolean add, char mode)
    {
        switch(mode)
        {
        case 'O':
            return new ModeEntry(add, Mode.bySymbol(mode), this.params[this.index++]);
        case 'o':
            return new ModeEntry(add, Mode.bySymbol(mode), this.params[this.index++]);
        case 'v':
            return new ModeEntry(add, Mode.bySymbol(mode), this.params[this.index++]);
        default:
            throw new IllegalArgumentException(""+mode);
        }
    }
    
    public List<ModeEntry> getModes()
    {
        return modes;
    }
    
    public static class ModeEntry
    {
        private final boolean added;
        private final Mode mode;
        private final String[] params;
        
        protected ModeEntry(boolean add, Mode mode, String... params)
        {
            this.added = add;
            this.mode = mode;
            this.params = params;
        }
        
        public boolean isAdded()
        {
            return this.added;
        }
        
        public Mode getMode()
        {
            return this.mode;
        }
        
        public String[] getParams()
        {
            return this.params;
        }
    }
}
