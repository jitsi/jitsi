/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.ArrayList;
import java.util.List;

import com.ircclouds.irc.api.domain.messages.ChannelModeMessage;

/**
 * IRC mode parser.
 * 
 * Parses a mode string and returns individual mode entries complete with
 * parameters, if any.
 * 
 * @author Danny van Heumen
 */
public class ModeParser
{
    /**
     * List of parsed, processed modes.
     */
    private final List<ModeEntry> modes = new ArrayList<ModeEntry>();

    /**
     * Index of current parameter.
     */
    private int index = 0;

    /**
     * Additional parameters
     */
    private String[] params;

    /**
     * Constructor for initiating mode parser and parsing mode string.
     * 
     * @param modestring mode string that should be parsed
     */
    protected ModeParser(String modestring)
    {
        String[] parts = modestring.split(" ");
        String mode = parts[0];
        this.params = new String[parts.length - 1];
        System.arraycopy(parts, 1, this.params, 0, parts.length - 1);
        parse(mode);
    }

    /**
     * Parse a complete mode string and extract individual mode entries.
     * 
     * @param modestring full mode string
     */
    private void parse(String modestring)
    {
        Boolean addition = null;
        for (char c : modestring.toCharArray())
        {
            switch (c)
            {
            case '+':
                addition = true;
                break;
            case '-':
                addition = false;
                break;
            default:
                if (addition == null)
                    throw new IllegalStateException(
                        "expect modifier (+ or -) first");
                try
                {
                    ModeEntry entry = process(addition, c);
                    modes.add(entry);
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    throw new IllegalArgumentException("invalid mode string provided: parameter missing", e);
                }
                break;
            }
        }
    }

    /**
     * Process mode character given state of addition/removal.
     * 
     * @param add indicates whether mode change is addition or removal
     * @param mode mode character
     * @return returns an entry that contains all parts of an individual mode
     *         change
     */
    private ModeEntry process(boolean add, char mode)
    {
        switch (mode)
        {
        case 'O':
            return new ModeEntry(add, Mode.OWNER, this.params[this.index++]);
        case 'o':
            return new ModeEntry(add, Mode.OPERATOR, this.params[this.index++]);
        case 'v':
            return new ModeEntry(add, Mode.VOICE, this.params[this.index++]);
        case 'l':
            String[] params = (add ? new String[]
            { this.params[this.index++] } : new String[] {});
            return new ModeEntry(add, Mode.LIMIT, params);
        case 'p':
            return new ModeEntry(add, Mode.PRIVATE);
        case 's':
            return new ModeEntry(add, Mode.SECRET);
        case 'i':
            return new ModeEntry(add, Mode.INVITE);
        default:
            return new ModeEntry(add, Mode.UNKNOWN, ""+mode);
        }
    }

    /**
     * Get list of modes.
     * 
     * @return returns list of parsed modes
     */
    public List<ModeEntry> getModes()
    {
        return modes;
    }

    /**
     * Class for single mode entry, optionally with corresponding parameter(s).
     */
    public static class ModeEntry
    {
        /**
         * Flag to indicate addition or removal of mode.
         */
        private final boolean added;

        /**
         * Type of mode.
         */
        private final Mode mode;

        /**
         * Optional additional parameter(s).
         */
        private final String[] params;

        /**
         * Constructor.
         * 
         * @param add true if mode is added, false if it is removed
         * @param mode type of mode
         * @param params optional, additional parameters
         */
        private ModeEntry(boolean add, Mode mode, String... params)
        {
            this.added = add;
            this.mode = mode;
            this.params = params;
        }

        /**
         * Added or removed.
         * 
         * @return returns true if added, removed otherwise
         */
        public boolean isAdded()
        {
            return this.added;
        }

        /**
         * Get type of mode
         * 
         * @return returns enum instance of mode
         */
        public Mode getMode()
        {
            return this.mode;
        }

        /**
         * Get additional parameters
         * 
         * @return returns array of additional parameters if any
         */
        public String[] getParams()
        {
            return this.params;
        }
    }
}
