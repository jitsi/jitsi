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
package net.java.sip.communicator.impl.replacement.dailymotion;

import java.util.regex.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Dailymotion
 * links.
 *
 * @author Purvesh Sahoo
 */
public class ReplacementServiceDailymotionImpl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceDailymotionImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String DAILYMOTION_PATTERN =
        "(https?\\:\\/\\/(www\\.)*?dailymotion\\.com"
        + "\\/video\\/([a-zA-Z0-9_\\-]+))([?#]([a-zA-Z0-9_\\-]+))*";

    /**
     * Configuration label shown in the config form.
     */
    public static final String DAILYMOTION_CONFIG_LABEL = "Dailymotion";

    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "DAILYMOTION";

    /**
     * Constructor for <tt>ReplacementServiceDailymotionImpl</tt>.
     */
    public ReplacementServiceDailymotionImpl()
    {
        logger.trace("Creating a DailyMotion Source.");
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
            Pattern.compile(
                "(.+\\/video\\/([a-zA-Z0-9_\\-]+))([?#]([a-zA-Z0-9_\\-]+))*",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sourceString);

        String thumbUrl = sourceString;

        while (m.find())
            thumbUrl =
                "http://www.dailymotion.com/thumbnail/160x120/video/"
                    + m.group(2);

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
        return DAILYMOTION_PATTERN;
    }
}
