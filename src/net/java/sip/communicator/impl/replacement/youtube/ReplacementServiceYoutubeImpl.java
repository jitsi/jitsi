/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.youtube;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import org.json.*;

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
        "(?<=>)(https?\\:\\/\\/(www\\.)*?youtube\\.com"
        + "\\/watch\\?v=([a-zA-Z0-9_\\-]+))([?&]\\w+=[\\w-]+)*(?=</A>)";

    /**
     * Configuration label shown in the config form. 
     */
    public static final String YOUTUBE_CONFIG_LABEL = "Youtube";

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
        try
        {
            String url = "http://youtube.com/oembed/?url=" + sourceString;
            URL sourceURL = new URL(url);
            URLConnection conn = sourceURL.openConnection();

            BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String inputLine, holder = "";

            while ((inputLine = in.readLine()) != null)
                holder = inputLine;
            in.close();

            JSONObject wrapper = new JSONObject(holder);

            String thumbUrl = wrapper.getString("thumbnail_url");

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
        return YOUTUBE_PATTERN;
    }
}