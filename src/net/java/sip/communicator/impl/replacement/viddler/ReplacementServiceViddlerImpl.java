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
package net.java.sip.communicator.impl.replacement.viddler;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Viddler
 * links.
 *
 * @author Purvesh Sahoo
 */
public class ReplacementServiceViddlerImpl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceViddlerImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String VIDDLER_PATTERN =
        "(http:\\/\\/(?:www\\.)?viddler\\.com"
        + "\\/explore\\/(\\w+)\\/videos\\/\\d+.*)";

    /**
     * API Key required to access the viddler api.
     */
    private static final String API_KEY = "1bi6ckuzmklyaqseiqtl";

    /**
     * Viddler API url.
     */
    private static final String sourceURL =
        "http://api.viddler.com/rest/v1/"
        + "?method=viddler.videos.getDetailsByUrl&api_key="
        + API_KEY;

    /**
     * Configuration label shown in the config form.
     */
    public static final String VIDDLER_CONFIG_LABEL = "Viddler";

    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "VIDDLER";

    /**
     * Constructor for <tt>ReplacementServiceViddlerImpl</tt>.
     */
    public ReplacementServiceViddlerImpl()
    {
        logger.trace("Creating a Viddler Source.");
    }

    /**
     * Returns the thumbnail URL of the video link provided.
     *
     * @param sourceString the original video link.
     * @return the thumbnail image link; the original link in case of no match.
     */
    public String getReplacement(String sourceString)
    {
        try
        {
            String url = sourceURL + "&url=" + sourceString + "/";

            URL sourceURL = new URL(url);
            URLConnection conn = sourceURL.openConnection();

            BufferedReader in =
                new BufferedReader(new InputStreamReader(conn
                    .getInputStream()));

            String inputLine;
            StringBuffer holder = new StringBuffer();

            while ((inputLine = in.readLine()) != null)
                holder.append(inputLine);
            in.close();

            String startTag = "<thumbnail_url>";
            String endTag = "</thumbnail_url>";

            String response = holder.toString();

            int start = response.indexOf(startTag) + startTag.length();
            int end = response.toString().indexOf(endTag);
            String thumbUrl = response.substring(start, end);

            if (thumbUrl != null)
            {
                return thumbUrl;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
        return VIDDLER_PATTERN;
    }
}
