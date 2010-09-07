/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.directimage;

import java.util.regex.*;

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
     * Configuration label property name. The label is saved in the languages
     * file under this property.
     */
    public static final String DIRECT_IMAGE_CONFIG_LABEL = "DIRECTIMAGE";

    /**
     * Constructor for <tt>ReplacementServiceDirectImageImpl</tt>. The source
     * needs to add itself to {@link ReplacementService} sourceList to be
     * displayed in the configuration panel.
     */
    public ReplacementServiceDirectImageImpl()
    {
        sourceList.add(DIRECT_IMAGE_CONFIG_LABEL);
        logger.trace("Creating a Direct Image Link Source.");
    }

    /**
     * Replaces the direct image links in the chat message with their
     * corresponding thumbnails.
     * 
     * @param chatString the original chat message.
     * @return replaced chat message with the thumbnail image; the original
     *         message in case of exception.
     */
    public String getReplacedMessage(String chatString)
    {
        final Pattern p =
            Pattern.compile(URL_PATTERN, Pattern.CASE_INSENSITIVE
                | Pattern.DOTALL);
        Matcher m = p.matcher(chatString);

        int count = 0, startPos = 0;
        StringBuffer msgBuff = new StringBuffer();

        while (m.find())
        {

            count++;
            msgBuff.append(chatString.substring(startPos, m.start()));
            startPos = m.end();

            String url =
                "<IMG HEIGHT=\"90\" WIDTH=\"120\" SRC=\"" + m.group(0)
                    + "\"></IMG>";
            msgBuff.append(url);

        }

        msgBuff.append(chatString.substring(startPos));

        if (!msgBuff.toString().equals(chatString))
            return msgBuff.toString();

        return chatString;
    }
}