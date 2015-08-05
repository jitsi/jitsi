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
package net.java.sip.communicator.impl.replacement.vbox7;

import java.util.*;
import java.util.regex.*;

import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Vbox7
 * links.
 *
 * @author Purvesh Sahoo
 */
public class ReplacementServiceVbox7Impl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceVbox7Impl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String VBOX7_PATTERN =
        "(https?\\:\\/\\/(www\\.)*?vbox7\\.com"
        + "\\/play\\:([a-zA-Z0-9_\\-]+))([?&]\\w+=[\\w-]*)*";

    /**
     * Configuration label shown in the config form.
     */
    public static final String VBOX7_CONFIG_LABEL = "Vbox7";

    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "VBOX7";

    /**
     * Constructor for <tt>ReplacementServiceVbox7Impl</tt>.
     */
    public ReplacementServiceVbox7Impl()
    {
        logger.trace("Creating a Vbox7 Source.");
    }

    /**
     * Returns the thumbnail URL of the video link provided.
     *
     * @param sourceString the original video link.
     * @return the thumbnail image link; the original link in case of no match.
     */
    public String getReplacement(String sourceString)
    {
        final Pattern p =
            Pattern.compile("\\/play\\:([a-zA-Z0-9_\\-]+)([?&]\\w+=[\\w-]*)*",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sourceString);
        String thumbUrl = sourceString;
        String id = null;

        while (m.find())
        {
            id = m.group(1);
            thumbUrl = "https://i.vbox7.com/p/" + id + "3.jpg";
        }

        if(id != null)
        {
            try
            {
                HttpUtils.HTTPResponseResult res = HttpUtils.openURLConnection(
                    "http://vbox7.com/etc/ext.do?key=" + id);

                StringTokenizer toks = new StringTokenizer(
                    res.getContentString(), "&");
                while(toks.hasMoreTokens())
                {
                    String value = toks.nextToken();
                    String[] entries = value.split("=");
                    if(entries.length > 1
                        && entries[0].equals("jpg_addr"))
                    {
                        return "http://" + entries[1];
                    }
                }
            }
            catch(Throwable t)
            {}
        }

        return thumbUrl;
    }

    /**
     * Returns the source name
     *
     * @return the source name
     */
    public String getSourceName()
    {
        return SOURCE_NAME;
    }

    /**
     * Returns the pattern of the source
     *
     * @return the source pattern
     */
    public String getPattern()
    {
        return VBOX7_PATTERN;
    }
}
