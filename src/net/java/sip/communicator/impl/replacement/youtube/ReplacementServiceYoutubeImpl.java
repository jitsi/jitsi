/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.youtube;

import java.io.*;
import java.net.*;
import java.util.regex.*;

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
    public static String re1 =
        "(http.*?(www\\.)*?youtube\\.com\\/watch\\?v=([a-zA-Z0-9_\\-]+))([?&]\\w+=[\\w-]+)*";

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
     * Replaces the youtube video links in the chat message with their
     * corresponding thumbnails.
     * 
     * @param chatString the original chat message.
     * @return replaced chat message with the thumbnail image; the original
     *         message in case of no match.
     */
    public String getReplacedMessage(final String chatString)
    {
        final Pattern p =
            Pattern.compile(re1, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(chatString);

        int count = 0, startPos = 0;
        StringBuffer msgBuff = new StringBuffer();

        while (m.find())
        {
            count++;
            msgBuff.append(chatString.substring(startPos, m.start()));
            startPos = m.end();

            // We only want to replace the inner link text and not the link src.
            // All even matches are the text we want to replace.
            if (count % 2 == 0)
            {
                try
                {
                    String url =
                        "http://youtube.com/oembed/?url=" + m.group(0);

                    URL sourceURL = new URL(url);
                    URLConnection conn = sourceURL.openConnection();

                    BufferedReader in =
                        new BufferedReader(new InputStreamReader(conn
                            .getInputStream()));

                    String inputLine, holder = "";

                    while ((inputLine = in.readLine()) != null)
                        holder = inputLine;
                    in.close();

                    JSONObject wrapper = new JSONObject(holder);

                    String thumbUrl = wrapper.getString("thumbnail_url");

                    if (thumbUrl != null)
                    {
                        msgBuff
                            .append("<IMG HEIGHT=\"90\" WIDTH=\"120\" SRC=\"");
                        msgBuff.append(thumbUrl);
                        msgBuff.append("\"></IMG>");

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