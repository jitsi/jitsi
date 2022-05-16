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
