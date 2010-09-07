/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.vimeo;

import java.net.*;
import java.util.regex.*;

import org.json.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

import java.io.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Vimeo
 * links.
 * 
 * @author Purvesh Sahoo
 */
public class ReplacementServiceVimeoImpl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceVimeoImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String VIMEO_PATTERN =
        "(http.*?(www\\.)*?vimeo\\.com\\/([a-zA-Z0-9_\\-]+))";

    /**
     * Configuration label property name. The label is saved in the languages
     * file under this property.
     */
    public static final String VIMEO_CONFIG_LABEL = "VIMEO";

    /**
     * Constructor for <tt>ReplacementServiceVimeoImpl</tt>. The source needs
     * to register itself with {@link ReplacementService} sourceList to be
     * displayed in the configuration panel.
     */
    public ReplacementServiceVimeoImpl()
    {
        sourceList.add(VIMEO_CONFIG_LABEL);
        logger.trace("Creating a Vimeo Source.");
    }

    /**
     * Replaces the vimeo video links in the chat message with their
     * corresponding thumbnails.
     *
     * @param chatString the original chat message.
     * @return replaced chat message with the thumbnail image; the original
     *         message in case of no match.
     */
    public String getReplacedMessage(String chatString)
    {
        final Pattern p =
            Pattern.compile(VIMEO_PATTERN, Pattern.CASE_INSENSITIVE
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
                    String url =
                        "http://vimeo.com/api/v2/video/" + m.group(3) + ".json";
                    URL vimeoURL = new URL(url);
                    URLConnection conn = vimeoURL.openConnection();

                    BufferedReader in =
                        new BufferedReader(new InputStreamReader(conn
                            .getInputStream()));

                    String inputLine, holder = "";

                    while ((inputLine = in.readLine()) != null)
                        holder = inputLine;
                    in.close();

                    JSONArray result = new JSONArray(holder);

                    if (!(result.length() == 0))
                    {
                        msgBuff
                            .append("<IMG HEIGHT=\"150\" WIDTH=\"200\" SRC=\"");
                        msgBuff.append(result.getJSONObject(0).getString(
                            "thumbnail_medium"));
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
}