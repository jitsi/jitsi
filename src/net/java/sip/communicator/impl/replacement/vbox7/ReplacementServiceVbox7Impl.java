/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.vbox7;

import java.util.regex.*;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for Vbox7
 * links.
 * 
 * @author Purvesh Sahoo
 */
public class ReplacementServiceVbox7Impl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceVbox7Impl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String VBOX7_PATTERN =
        "(?<=>)(https?\\:\\/\\/(www\\.)*?vbox7\\.com"
        + "\\/play\\:([a-zA-Z0-9_\\-]+))([?&]\\w+=[\\w-]*)*(?=</A>)";

    /**
     * Configuration label shown in the config form. 
     */
    public static final String VBOX7_CONFIG_LABEL = "Vbox7";

    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "VBOX7";
    
    /**
     * Constructor for <tt>ReplacementServiceVbox7Impl</tt>.
     */
    public ReplacementServiceVbox7Impl()
    {
        logger.trace("Creating a Vbox7 Source.");
    }

    /**
     * Returns the thumbnail URL of the video link provided.
     *
     * @param sourceString the original video link.
     * @return the thumbnail image link; the original link in case of no match.
     */
    public String getReplacement(String sourceString)
    {
        final Pattern p =
            Pattern.compile("\\/play\\:([a-zA-Z0-9_\\-]+)([?&]\\w+=[\\w-]*)*",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(sourceString);
        String thumbUrl = sourceString;

        while (m.find())
        {
            thumbUrl = "http://i.vbox7.com/p/" + m.group(1) + "3.jpg";
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
        return VBOX7_PATTERN;
    }
}