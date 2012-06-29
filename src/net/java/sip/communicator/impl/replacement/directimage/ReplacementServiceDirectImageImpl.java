/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.directimage;

import net.java.sip.communicator.service.replacement.*;
import net.java.sip.communicator.util.*;

/**
 * Implements the {@link ReplacementService} to provide previews for direct
 * image links.
 * 
 * @author Purvesh Sahoo
 */
public class ReplacementServiceDirectImageImpl
    implements ReplacementService
{
    /**
     * The logger for this class.
     */
    private static final Logger logger =
        Logger.getLogger(ReplacementServiceDirectImageImpl.class);

    /**
     * The regex used to match the link in the message.
     */
    public static final String URL_PATTERN =
        "[^<>]+\\.(?:jpg|png|gif)[^<>]*(?=</a>)";

    /**
     * Configuration label shown in the config form. 
     */
    public static final String DIRECT_IMAGE_CONFIG_LABEL = "Direct Image Link";

    /**
     * Source name; also used as property label.
     */
    public static final String SOURCE_NAME = "DIRECTIMAGE";

    /**
     * Constructor for <tt>ReplacementServiceDirectImageImpl</tt>. 
     */
    public ReplacementServiceDirectImageImpl()
    {
        logger.trace("Creating a Direct Image Link Source.");
    }

    /**
     * Returns the thumbnail URL of the image link provided.
     *
     * @param sourceString the original image link.
     * @return the thumbnail image link; the original link in case of no match.
     */
    public String getReplacement(String sourceString)
    {
        return sourceString;
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
        return URL_PATTERN;
    }
}
