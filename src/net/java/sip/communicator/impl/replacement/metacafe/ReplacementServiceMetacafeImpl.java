/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.metacafe;

import java.util.regex.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Metacafe
 * links.
 *
 * @author Purvesh Sahoo
 */
public class ReplacementServiceMetacafeImpl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceMetacafeImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String METACAFE_PATTERN =
        "(?<=>)(https?\\:\\/\\/(www\\.)*?metacafe\\.com"
        + "\\/watch\\/([a-zA-Z0-9_\\-]+))(\\/[a-zA-Z0-9_\\-\\/]+)*(?=</A>)";

    /**
     * Configuration label shown in the config form. 
     */
    public static final String METACAFE_CONFIG_LABEL = "Metacafe";

    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "METACAFE";

    /**
     * Constructor for <tt>ReplacementServiceMetacafeImpl</tt>. 
     */
    public ReplacementServiceMetacafeImpl()
    {
        logger.trace("Creating a Metacafe Source.");
    }

    /**
     * Replaces the metacafe video links with their corresponding thumbnails.
     *
     * @param sourceString the original video link.
     * @return replaced thumbnail image; the original video link in case of no
     *         match.
     */
    public String getReplacement(String sourceString)
    {
        final Pattern p =
            Pattern.compile(
                "\\/watch\\/([a-zA-Z0-9_\\-]+)(\\/[a-zA-Z0-9_\\-\\/]+)*",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sourceString);

        String thumbUrl = sourceString;

        while (m.find())
            thumbUrl = "http://www.metacafe.com/thumb/" + m.group(1) + ".jpg";

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
        return METACAFE_PATTERN;
    }
}