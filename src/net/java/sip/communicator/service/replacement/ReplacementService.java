/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.replacement;

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
     * Returns the text replacements if any or returns the original source
     * string.
     *
     * @param sourceString the original source string.
     * @return the replacement string for the source string provided; the
     *         original string in case of no match.
     */
    public String getReplacement(String sourceString);

    /**
     * Returns the name of the replacement source.
     * 
     * @return the replacement source name
     */
    public String getSourceName();

    /**
     * Returns the pattern used to match the source URL.
     * 
     * @return the pattern of the source
     */
    public String getPattern();
}