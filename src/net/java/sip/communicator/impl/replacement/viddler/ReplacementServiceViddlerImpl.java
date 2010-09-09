/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.viddler;

import java.io.*;
import java.net.*;
import java.util.regex.*;

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
        "(?:[\\>])(http:\\/\\/(?:www\\.)?viddler\\.com\\/explore\\/(\\w+)\\/videos\\/\\d+.*(?=<\\/A>))";

    /**
     * API Key required to access the viddler api.
     */
    private static final String API_KEY = "1bi6ckuzmklyaqseiqtl";

    /**
     * Viddler API url.
     */
    private static final String sourceURL =
        "http://api.viddler.com/rest/v1/?method=viddler.videos.getDetailsByUrl&api_key="
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
     * Replaces the viddler video links in the chat message with their
     * corresponding thumbnails.
     * 
     * @param chatString the original chat message.
     * @return replaced chat message with the thumbnail image; the original
     *         message in case of no match.
     */
    public String getReplacedMessage(String chatString)
    {
        final Pattern p =
            Pattern.compile(VIDDLER_PATTERN, Pattern.CASE_INSENSITIVE
                | Pattern.DOTALL);
        Matcher m = p.matcher(chatString);

        int count = 0, startPos = 0;
        StringBuffer msgBuff = new StringBuffer();

        while (m.find())
        {
            count++;
            msgBuff.append(chatString.substring(startPos, m.start()));
            startPos = m.end();

            try
            {
                String url = sourceURL + "&url=" + m.group(1) + "/";

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
                    msgBuff.append("<IMG HEIGHT=\"90\" WIDTH=\"120\" SRC=\"");
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