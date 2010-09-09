/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
        "(http.*?(www\\.)*?flickr\\.com\\/photos\\/[0-9a-zA-Z_\\-\\@]+\\/([0-9]+)(\\/[^\"\\<]*)*)";

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
     * Replaces the Flickr image links in the chat message with their
     * corresponding thumbnails.
     * 
     * @param chatString the original chat message.
     * @return replaced chat message with the thumbnail image; the original
     *         message in case of no match.
     */
    public String getReplacedMessage(String chatString)
    {
        final Pattern p =
            Pattern.compile(FLICKR_PATTERN, Pattern.CASE_INSENSITIVE
                | Pattern.DOTALL);
        Matcher m = p.matcher(chatString);

        int count = 0, startPos = 0;
        StringBuffer msgBuff = new StringBuffer();

        while (m.find())
        {

            count++;
            msgBuff.append(chatString.substring(startPos, m.start()));
            startPos = m.end();

            if (count % 2 == 0)
            {
                try
                {
                    // API URL
                    String url =
                        "http://api.flickr.com/services/rest/?method=flickr.photos.getInfo&api_key="
                            + API_KEY + "&photo_id=" + m.group(3)
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

                        String farmID = result.getString("farm");
                        String serverID = result.getString("server");
                        String secret = result.getString("secret");

                        String thumbURL =
                            "http://farm" + farmID + ".static.flickr.com/"
                                + serverID + "/" + m.group(3) + "_" + secret
                                + "_t.jpg";

                        if (!(result.length() == 0))
                        {
                            msgBuff
                                .append("<IMG HEIGHT=\"90\" WIDTH=\"120\" SRC=\"");
                            msgBuff.append(thumbURL);
                            msgBuff.append("\"></IMG>");
                        }
                    }
                    else
                    {
                        startPos = 0;
                        msgBuff = new StringBuffer();
                    }
                }
                catch (Exception e)
                {
                    startPos = 0;
                    msgBuff = new StringBuffer();
                    e.printStackTrace();
                }
            }
            else
            {
                msgBuff.append(chatString.substring(m.start(), m.end()));
            }
        }

        msgBuff.append(chatString.substring(startPos));

        if (!msgBuff.toString().equals(chatString))
            return msgBuff.toString();

        return chatString;
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
}