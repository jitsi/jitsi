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
        "(http.*?(www\\.)*?vbox7\\.com\\/play\\:([a-zA-Z0-9_\\-]+))([?&]\\w+=[\\w-]*)*";

    /**
     * Configuration label property name. The label is saved in the languages
     * file under this property.
     */
    public static final String VBOX7_CONFIG_LABEL = "VBOX7";

    /**
     * Constructor for <tt>ReplacementServiceVbox7Impl</tt>. The source needs
     * to register itself with {@link ReplacementService} sourceList to be
     * displayed in the configuration panel.
     */
    public ReplacementServiceVbox7Impl()
    {
        sourceList.add(VBOX7_CONFIG_LABEL);
        logger.trace("Creating a Vbox7 Source.");
    }

    /**
     * Replaces the vbox7 video links in the chat message with their
     * corresponding thumbnails.
     *
     * @param chatString the original chat message.
     * @return replaced chat message with the thumbnail image; the original
     *         message in case of no match.
     */
    public String getReplacedMessage(String chatString)
    {
        final Pattern p =
            Pattern.compile(VBOX7_PATTERN, Pattern.CASE_INSENSITIVE
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
                msgBuff.append("<IMG HEIGHT=\"90\" WIDTH=\"120\" SRC=\"");
                msgBuff
                    .append("http://i.vbox7.com/p/");
                msgBuff.append(m.group(3));
                msgBuff.append("3.jpg\"></IMG>");
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