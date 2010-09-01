/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.io.*;

/**
 * Utility class that helps to work with <tt>String</tt> class.
 *
 * @author Grigorii Balutsel
 */
public final class StringUtils
{
    /**
     * This class cannot be instantiated.
     */
    private StringUtils()
    {
    }

    /**
     * Gets a <tt>String</tt> which represents the conversion of a specific
     * camel-case formatted <tt>String</tt> to a more human-readable form (i.e.
     * with spaces between the words).
     *
     * @param camelCase a camel-case (or Pascal-case) formatted <tt>String</tt>
     * from which a human-readable <tt>String</tt> is to be constructed 
     * @return a <tt>String</tt> which represents the conversion of the specified
     * camel-case formatted <tt>String</tt> to a more human-readable form
     */
    public static String convertCamelCaseToDisplayString(String camelCase)
    {
        if (camelCase == null)
            return null;

        int camelCaseLength = camelCase.length();

        if (camelCaseLength == 0)
            return camelCase;

        int wordEndIndex = 0;
        int wordBeginIndex = 0;
        StringBuilder display = new StringBuilder();

        for (; wordEndIndex < camelCaseLength; wordEndIndex++)
        {
            char ch = camelCase.charAt(wordEndIndex);

            if (Character.isUpperCase(ch) && (wordBeginIndex != wordEndIndex))
            {
                display.append(
                        camelCase.substring(wordBeginIndex, wordEndIndex));
                display.append(' ');
                wordBeginIndex = wordEndIndex;
            }
        }
        if (wordEndIndex >= camelCaseLength)
            display.append(camelCase.substring(wordBeginIndex));
        return display.toString();
    }

    /**
     * Indicates whether string is null or empty.
     *
     * @param s the string to analyze.
     * @return true if string is null or empty.
     */
    public static boolean isNullOrEmpty(String s)
    {
        return isNullOrEmpty(s, true);
    }

    /**
     * Indicates whether string is null or empty.
     *
     * @param s    the string to analyze.
     * @param trim indicates whether to trim the string.
     * @return true if string is null or empty.
     */
    public static boolean isNullOrEmpty(String s, boolean trim)
    {
        if (s == null)
            return true;
        if (trim)
            s = s.trim();
        return s.length() == 0;
    }

    /**
     * Indicates whether strings are equal.
     *
     * @param s1 the string to analyze.
     * @param s2 the string to analyze.
     * @return true if string are equal.
     */
    public static boolean isEquals(String s1, String s2)
    {
        return (s1 == null && s2 == null) || (s1 != null && s1.equals(s2));
    }

    /**
     * Creates <tt>InputStream</tt> from the string in UTF8 encoding.
     *
     * @param string the string to convert.
     * @return the <tt>InputStream</tt>.
     * @throws UnsupportedEncodingException if UTF8 is unsupported.
     */
    public static InputStream fromString(String string)
            throws UnsupportedEncodingException
    {
        return fromString(string, "UTF-8");
    }

    /**
     * Creates <tt>InputStream</tt> from the string in the specified encoding.
     *
     * @param string   the string to convert.
     * @param encoding the encoding
     * @return the <tt>InputStream</tt>.
     * @throws UnsupportedEncodingException if the encoding is unsupported.
     */
    public static InputStream fromString(String string, String encoding)
            throws UnsupportedEncodingException
    {
        byte[] bytes = string.getBytes(encoding);
        return new ByteArrayInputStream(bytes);
    }
}
