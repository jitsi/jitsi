/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.bliptv;

import java.io.*;
import java.net.*;

import org.json.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Blip.tv
 * links.
 * 
 * @author Purvesh Sahoo
 */
public class ReplacementServiceBliptvImpl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceBliptvImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String BLIPTV_PATTERN =
        "(?<=>)(http:\\/\\/(?:www\\.)?blip\\.tv"
        + "\\/file\\/(\\d+)([?&\\?]\\w+=[\\w-]+)*)(?=</A>)";

    /**
     * Configuration label shown in the config form. 
     */
    public static final String BLIPTV_CONFIG_LABEL = "Blip.tv";

    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "BLIPTV";

    /**
     * Constructor for <tt>ReplacementServiceBliptvImpl</tt>.
     */
    public ReplacementServiceBliptvImpl()
    {
        logger.trace("Creating a Blip.TV Source.");
    }

    /**
     * Replaces the Blip.tv video links with their corresponding thumbnails.
     *
     * @param sourceString the original chat message.
     * @return replaced thumbnail image link; the original video link in case of
     *         no match.
     */
    public String getReplacement(String sourceString)
    {
        try
        {
            String url = "http://oohembed.com/oohembed/?url=" + sourceString;

            URL sourceURL = new URL(url);
            URLConnection conn = sourceURL.openConnection();

            BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String inputLine, holder = "";

            while ((inputLine = in.readLine()) != null)
                holder += inputLine;
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
        return BLIPTV_PATTERN;
    }
}