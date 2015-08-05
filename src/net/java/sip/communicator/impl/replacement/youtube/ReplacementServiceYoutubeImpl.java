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
package net.java.sip.communicator.impl.replacement.youtube;

import java.util.regex.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Youtube
 * links.
 *
 * @author Purvesh Sahoo
 */
public class ReplacementServiceYoutubeImpl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceYoutubeImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String YOUTUBE_PATTERN =
        "(https?\\:\\/\\/(www\\.)*?youtube\\.com"
        + "\\/watch\\?v=([a-zA-Z0-9_\\-]+))([?&]\\w+=[\\w-]+)*";

    /**
     * Configuration label shown in the config form.
     */
    public static final String YOUTUBE_CONFIG_LABEL = "YouTube";

    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "YOUTUBE";

    /**
     * Constructor for <tt>ReplacementServiceYoutubeImpl</tt>.
     */
    public ReplacementServiceYoutubeImpl()
    {
        logger.trace("Creating a Youtube Source.");
    }

    /**
     * Returns the thumbnail URL of the video link provided.
     *
     * @param sourceString the original video link.
     * @return the thumbnail image link; the original link in case of no match.
     */
    public String getReplacement(String sourceString)
    {
        final String pattern = "https?:\\/\\/(?:[0-9A-Z-]+\\.)?(?:youtu\\"
            + ".be\\/|youtube\\.com\\S*[^\\w\\-\\s])([\\w\\-]{11})(?=[^\\"
            + "w\\-]|$)(?![?=&+%\\w]*(?:['\"][^<>]*>|<\\/a>))[?=&+%\\w]*";
        final Pattern compiledPattern
            = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = compiledPattern.matcher(sourceString);
        String thumbUrl = sourceString;
        
        while (matcher.find())
        {
            String videoID = "";
            try
            {
                videoID = matcher.group(1);
            }
            catch (Exception e)
            {
                logger.debug("Replacement failed for " + getSourceName(), e);
                return thumbUrl;
            }
                thumbUrl
                    = "https://img.youtube.com/vi/" + videoID + "/3.jpg";
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
        return YOUTUBE_PATTERN;
    }
}
