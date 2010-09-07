/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
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
        "(http.*?(www\\.)*?metacafe\\.com\\/watch\\/([a-zA-Z0-9_\\-]+))(\\/[a-zA-Z0-9_\\-\\/]+)*";

    /**
     * Configuration label property name. The label is saved in the languages
     * file under this property.
     */
    public static final String METACAFE_CONFIG_LABEL = "METACAFE";

    /**
     * Constructor for <tt>ReplacementServiceMetacafeImpl</tt>. The source needs
     * to register itself with {@link ReplacementService} sourceList to be
     * displayed in the configuration panel.
     */
    public ReplacementServiceMetacafeImpl()
    {
        logger.trace("Creating a Metacafe Source.");
        sourceList.add(METACAFE_CONFIG_LABEL);
    }

    /**
     * Replaces the metacafe video links in the chat message with their
     * corresponding thumbnails.
     *
     * @param chatString the original chat message.
     * @return replaced chat message with the thumbnail image; the original
     *         message in case of no match.
     */
    public String getReplacedMessage(String chatString)
    {
        final Pattern p =
            Pattern.compile(METACAFE_PATTERN, Pattern.CASE_INSENSITIVE
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
                msgBuff.append("<IMG HEIGHT=\"81\" WIDTH=\"136\" SRC=\"");
                msgBuff.append("http://www.metacafe.com/thumb/");
                msgBuff.append(m.group(3));
                msgBuff.append(".jpg\"></IMG>");
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