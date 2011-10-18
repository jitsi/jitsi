/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.flickr;

import java.io.*;
import java.net.*;
import java.util.regex.*;

import org.json.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Flickr
 * links.
 * 
 * @author Purvesh Sahoo
 */
public class ReplacementServiceFlickrImpl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceFlickrImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String FLICKR_PATTERN =
        "(?<=>)(https?\\:\\/\\/(www\\.)*?flickr\\.com"
        + "\\/photos\\/[0-9a-zA-Z_\\-\\@]+\\/([0-9]+)(\\/[^\"\\<]*)*)(?=</A>)";

    /**
     * API Key required to access the Flickr api.
     */
    public static final String API_KEY = "8b5d9cee22f0f5154bf4e9846c025484";

    /**
     * Configuration label shown in the config form. 
     */
    public static final String FLICKR_CONFIG_LABEL = "Flickr Images";
    
    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "FLICKR";

    /**
     * Constructor for <tt>ReplacementServiceFlickrImpl</tt>. 
     */
    public ReplacementServiceFlickrImpl()
    {
        logger.trace("Creating a Flickr Source.");
    }

    /**
     * Replaces the Flickr image links with their corresponding thumbnails.
     *
     * @param sourceString the original flickr image link.
     * @return replaced thumbnail image link; the original image link in case of
     *         no match.
     */
    public String getReplacement(String sourceString)
    {
        final Pattern p =
            Pattern.compile(
                "\\/photos\\/[0-9a-zA-Z_\\-\\@]+\\/([0-9]+)(\\/[^\"\\<]*)*",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sourceString);
        String thumbUrl = sourceString;

        while (m.find())
        {
            try
            {
                // API URL
                String url =
                    "http://api.flickr.com/services/rest/"
                    + "?method=flickr.photos.getInfo&api_key="
                        + API_KEY + "&photo_id=" + m.group(1)
                        + "&format=json&nojsoncallback=1";

                URL flickrURL = new URL(url);
                URLConnection conn = flickrURL.openConnection();

                BufferedReader in =
                    new BufferedReader(new InputStreamReader(conn
                        .getInputStream()));

                String inputLine, holder = "";

                while ((inputLine = in.readLine()) != null)
                    holder = inputLine;
                in.close();

                JSONObject wrapper = new JSONObject(holder);

                if (wrapper.getString("stat").equals("ok"))
                {
                    JSONObject result = wrapper.getJSONObject("photo");
                    if (!(result.length() == 0))
                    {
                        String farmID = result.getString("farm");
                        String serverID = result.getString("server");
                        String secret = result.getString("secret");

                        thumbUrl =
                            "http://farm" + farmID + ".static.flickr.com/"
                                + serverID + "/" + m.group(1) + "_" + secret
                                + "_t.jpg";
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
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
        return FLICKR_PATTERN;
    }
}