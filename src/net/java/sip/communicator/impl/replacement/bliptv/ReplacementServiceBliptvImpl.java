/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.bliptv;

import java.io.*;
import java.net.*;
import java.util.regex.*;

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
        "(?:[\\>])(http:\\/\\/(?:www\\.)?blip\\.tv\\/file\\/(\\d+).*(?=<))";

    /**
     * Configuration label property name. The label is saved in the languages
     * file under this property.
     */
    public static final String BLIPTV_CONFIG_LABEL = "BLIPTV";

    /**
     * Constructor for <tt>ReplacementServiceBliptvImpl</tt>. The source needs
     * to add itself to {@link ReplacementService} sourceList to be displayed in
     * the configuration panel.
     */
    public ReplacementServiceBliptvImpl()
    {
        sourceList.add(BLIPTV_CONFIG_LABEL);
        logger.trace("Creating a Blip.TV Source.");
    }

    /**
     * Replaces the Blip.tv video links in the chat message with their
     * corresponding thumbnails.
     * 
     * @param chatString the original chat message.
     * @return replaced chat message with the thumbnail image; the original
     *         message in case of no match.
     */
    public String getReplacedMessage(String chatString)
    {
        final Pattern p =
            Pattern.compile(BLIPTV_PATTERN, Pattern.CASE_INSENSITIVE
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
                String url = "http://oohembed.com/oohembed/?url=" + m.group(1);

                URL sourceURL = new URL(url);
                URLConnection conn = sourceURL.openConnection();

                BufferedReader in =
                    new BufferedReader(new InputStreamReader(conn
                        .getInputStream()));

                String inputLine, holder = "";

                while ((inputLine = in.readLine()) != null)
                    holder += inputLine;
                in.close();

                JSONObject wrapper = new JSONObject(holder);

                String thumbUrl = wrapper.getString("thumbnail_url");

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
}