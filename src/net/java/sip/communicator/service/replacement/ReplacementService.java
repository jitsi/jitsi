/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.replacement;

import java.util.*;

/**
 * A service used to provide substitution for any text in chat messages, like
 * smileys, video and image previews, etc.
 * 
 * @author Purvesh Sahoo
 */
public interface ReplacementService
{
    /**
     * The source name property name.
     */
    public final String SOURCE_NAME = "SOURCE";

    /**
     * List of all the available replacement sources. All sources need to
     * register itself.
     */
    public static ArrayList<String> sourceList = new ArrayList<String>();

    /**
     * Returns the chat message with the text replacements if any or returns the
     * original chat message.
     * 
     * @param chatString the original chat message.
     * @return the replaced chat message in case of match; the original message
     *         in case of no match.
     */
    public String getReplacedMessage(String chatString);
}