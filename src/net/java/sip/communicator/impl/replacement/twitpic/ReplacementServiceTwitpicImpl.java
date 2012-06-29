/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
        "(?<=>)http:\\/\\/(?:www\\.)?twitpic\\.com\\/([^\\/<]*)(?=</A>)";

    /**
     * Configuration label shown in the config form. 
     */
    public static final String TWITPIC_CONFIG_LABEL = "TwitPic";

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
     * Replaces the twitpic image links with their corresponding thumbnails.
     *
     * @param sourceString the original twitpic link.
     * @return thumbnail image link for the source string; the original
     *         image link in case of no match.
     */
    public String getReplacement(String sourceString)
    {
        final Pattern p =
            Pattern.compile("\\.com\\/([^\\/<]*)", Pattern.CASE_INSENSITIVE
                | Pattern.DOTALL);

        Matcher m = p.matcher(sourceString);
        String thumbUrl = sourceString;

        while (m.find())
        {
            thumbUrl = "http://twitpic.com/show/thumb/" + m.group(1);
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
        return TWITPIC_PATTERN;
    }
}
