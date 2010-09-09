/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.twitpic;

import java.util.regex.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Twitpic
 * links.
 * 
 * @author Purvesh Sahoo
 */
public class ReplacementServiceTwitpicImpl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(ReplacementServiceTwitpicImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String TWITPIC_PATTERN =
        "http:\\/\\/(?:www\\.)?twitpic\\.com\\/([^\\/<]*)(?=<)";

    /**
     * Configuration label shown in the config form. 
     */
    public static final String TWITPIC_CONFIG_LABEL = "Twitpic";
    
    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "TWITPIC";

    /**
     * Constructor for <tt>ReplacementServiceTwitpicImpl</tt>.
     */
    public ReplacementServiceTwitpicImpl()
    {
        logger.trace("Creating a Twitpic Source.");
    }

    /**
     * Replaces the twitpic image links in the chat message with their
     * corresponding thumbnails.
     * 
     * @param chatString the original chat message.
     * @return replaced chat message with the thumbnail image; the original
     *         message in case of no match.
     */
    public String getReplacedMessage(String chatString)
    {
        final Pattern p =
            Pattern.compile(TWITPIC_PATTERN, Pattern.CASE_INSENSITIVE
                | Pattern.DOTALL);
        Matcher m = p.matcher(chatString);

        int count = 0, startPos = 0;
        StringBuffer msgBuff = new StringBuffer();

        while (m.find())
        {
            count++;
            msgBuff.append(chatString.substring(startPos, m.start()));
            startPos = m.end();

            msgBuff.append("<IMG HEIGHT=\"90\" WIDTH=\"120\" SRC=\"");
            msgBuff.append("http://twitpic.com/show/thumb/");
            msgBuff.append(m.group(1));
            msgBuff.append("\"></IMG>");

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