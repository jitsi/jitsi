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

import java.util.*;

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
     * Additional parameters.
     */
    private String[] params;

    /**
     * Constructor for initiating mode parser and parsing mode string.
     *
     * @param modestring mode string that should be parsed
     */
    protected ModeParser(final String modestring)
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
    private void parse(final String modestring)
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
                {
                    throw new IllegalStateException(
                        "expect modifier (+ or -) first");
                }
                try
                {
                    ModeEntry entry = process(addition, c);
                    modes.add(entry);
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    throw new IllegalArgumentException("invalid mode string "
                        + "provided: parameter missing", e);
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
    private ModeEntry process(final boolean add, final char mode)
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
            String[] limitparams;
            if (add)
            {
                limitparams = new String[]
                { this.params[this.index++] };
            }
            else
            {
                limitparams = new String[] {};
            }
            return new ModeEntry(add, Mode.LIMIT, limitparams);
        case 'p':
            return new ModeEntry(add, Mode.PRIVATE);
        case 's':
            return new ModeEntry(add, Mode.SECRET);
        case 'i':
            return new ModeEntry(add, Mode.INVITE);
        case 'b':
            return new ModeEntry(add, Mode.BAN, this.params[this.index++]);
        default:
            return new ModeEntry(add, Mode.UNKNOWN, "" + mode);
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
    public static final class ModeEntry
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
        private ModeEntry(final boolean add, final Mode mode,
            final String... params)
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
         * Get type of mode.
         *
         * @return returns enum instance of mode
         */
        public Mode getMode()
        {
            return this.mode;
        }

        /**
         * Get additional parameters.
         *
         * @return returns array of additional parameters if any
         */
        public String[] getParams()
        {
            return this.params;
        }
    }
}
