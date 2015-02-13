/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;
import java.util.regex.*;

/**
 * ISUPPORT parameters by IRC server.
 *
 * @author Danny van Heumen
 */
public enum ISupport
{
    /**
     * Maximum nick length allowed by IRC server.
     */
    NICKLEN,
    /**
     * Maximum channel name length allowed by IRC server.
     */
    CHANNELLEN,
    /**
     * Maximum topic length allowed by IRC server.
     */
    TOPICLEN,
    /**
     * Maximum kick message length allowed by IRC server.
     */
    KICKLEN,
    /**
     * Maximum away message length allowed by IRC server.
     */
    AWAYLEN,
    /**
     * Maximum number of joined channels allowed by IRC server.
     */
    CHANLIMIT,
    /**
     * Maximum number of entries in the MONITOR list supported by this server.
     */
    MONITOR,
    /**
     * Maximum number of entries in the WATCH list supported by this server.
     */
    WATCH;

    /**
     * Pattern for parsing ChanLimit ISUPPORT parameter.
     */
    private static final Pattern PATTERN_CHANLIMIT = Pattern
        .compile("([#&!+]++):(\\d*+)(?:,|$)");

    /**
     * Parse channel limit ISUPPORT parameter.
     *
     * @param destination the destination map for storing parsed values
     * @param chanLimitValue the raw ISUPPORT server parameter value
     */
    public static void parseChanLimit(final Map<Character, Integer> destination,
            final String chanLimitValue)
    {
        if (destination == null)
        {
            throw new IllegalArgumentException("destination cannot be null");
        }
        if (chanLimitValue == null)
        {
            return;
        }
        final Matcher matcher = PATTERN_CHANLIMIT.matcher(chanLimitValue);
        while (matcher.find())
        {
            String chanTypes = matcher.group(1);
            String limitValue = matcher.group(2);
            Integer limit = new Integer(limitValue);
            for (char c : chanTypes.toCharArray())
            {
                destination.put(c, limit);
            }
        }
    }
}
