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
package net.java.sip.communicator.impl.replacement.hulu;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.json.simple.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Hulu links.
 *
 * @author Purvesh Sahoo
 */
public class ReplacementServiceHuluImpl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceHuluImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String HULU_PATTERN =
        "(https?\\:\\/\\/(www\\.)*?hulu\\.com"
        + "\\/watch\\/([a-zA-Z0-9_\\-]+))(\\/([^\\\"\\<]*)*)";

    /**
     * Configuration label shown in the config form.
     */
    public static final String HULU_CONFIG_LABEL = "Hulu";

    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "HULU";

    /**
     * Constructor for <tt>ReplacementServiceHuluImpl</tt>.
     */
    public ReplacementServiceHuluImpl()
    {
        logger.trace("Creating a Hulu Source.");
    }

    /**
     * Replaces the Hulu video links with their corresponding thumbnails.
     *
     * @param sourceString the original video link.
     * @return thumbnail image link; the original video link in case of no
     *         match.
     */
    public String getReplacement(String sourceString)
    {
        try
        {
            String url = "http://api.embed.ly/1/oembed?url=" + sourceString
                + "&key=cff57b37766440a6a8aa45df88097efe";
            URL sourceURL = new URL(url);
            URLConnection conn = sourceURL.openConnection();

            BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String inputLine, holder = "";

            while ((inputLine = in.readLine()) != null)
                holder = inputLine;
            in.close();

            JSONObject wrapper = (JSONObject)JSONValue
                .parseWithException(holder);

            String thumbUrl = (String)wrapper.get("thumbnail_url");

            if (thumbUrl != null)
            {
                return thumbUrl;
            }
        }
        catch (Throwable e)
        {
            logger.error("Error parsing", e);
        }

        return sourceString;
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
        return HULU_PATTERN;
    }
}
