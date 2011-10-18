/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
 * @author Emil Ivov
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
     * Indicates whether string is <tt>null</tt> or empty.
     *
     * @param s the string to analyze.
     * @return true if string is <tt>null</tt> or empty.
     */
    public static boolean isNullOrEmpty(String s)
    {
        return isNullOrEmpty(s, true);
    }

    /**
     * Indicates whether string is <tt>null</tt> or empty.
     *
     * @param s    the string to analyze.
     * @param trim indicates whether to trim the string.
     * @return true if string is <tt>null</tt> or empty.
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

    /**
     * Returns the UTF8 bytes for <tt>string</tt> and handles the unlikely case
     * where UTF-8 is not supported.
     *
     * @param string the <tt>String</tt> whose bytes we'd like to obtain.
     *
     * @return <tt>string</tt>'s bytes.
     */
    public static byte[] getUTF8Bytes(String string)
    {
        try
        {
            return string.getBytes("UTF-8");
        }
        catch(UnsupportedEncodingException exc)
        {
            // shouldn't happen. UTF-8 is always supported, anyways ... if
            //this happens, we'll cheat
            return string.getBytes();
        }
    }

    /**
     * Converts <tt>string</tt> into an UTF8 <tt>String</tt> and handles the
     * unlikely case where UTF-8 is not supported.
     *
     * @param bytes the <tt>byte</tt> array that we'd like to convert into a
     * <tt>String</tt>.
     *
     * @return the UTF-8 <tt>String</tt>.
     */
    public static String getUTF8String(byte[] bytes)
    {
        try
        {
            return new String(bytes, "UTF-8");
        }
        catch(UnsupportedEncodingException exc)
        {
            // shouldn't happen. UTF-8 is always supported, anyways ... if
            //this happens, we'll cheat
            return new String(bytes);
        }
    }

    /**
     * Indicates if the given string is composed only of digits or not.
     *
     * @param string the string to check
     * @return <tt>true</tt> if the given string is composed only of digits,
     * <tt>false</tt> - otherwise
     */
    public static boolean isNumber(String string)
    {
        for (int i = 0; i < string.length(); i++)
        {
            //If we find a non-digit character we return false.
            if (!Character.isDigit(string.charAt(i)))
                return false;
        }

        return true;
    }

    /**
     * Indicates if the given string contains any letters.
     *
     * @param string the string to check for letters
     * @return <tt>true</tt> if the given string contains letters,
     * <tt>false</tt> - otherwise
     */
    public static boolean containsLetters(String string)
    {
        for (int i = 0; i < string.length(); i++)
        {
            if (Character.isLetter(string.charAt(i)))
                return true;
        }

        return false;
    }

    /**
     * Removes all spaces from the given string and returns a concatenated
     * result string.
     *
     * @param string the string to concatenate
     * @return the concatenated string
     */
    public static String concatenateWords(String string)
    {
        StringBuffer buff = new StringBuffer();
        char[] stringAsCharArray = string.toCharArray();

        for (char character : stringAsCharArray)
        {
            if (character != ' ')
            {
                buff.append(character);
            }
        }
        return buff.toString();
    }
}
