/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Represents an OSGi service which aids the parsing, formatting and validating
 * of international phone numbers.
 *
 * @author Lyubomir Marinov
 */
public interface PhoneNumberI18nService
{
    /**
     * Normalizes a <tt>String</tt> phone number by converting alpha characters
     * to their respective digits on a keypad and then stripping non-digit
     * characters.
     * 
     * @param phoneNumber a <tt>String</tt> which represents a phone number to
     * normalize
     * @return a <tt>String</tt> which is a normalized form of the specified
     * <tt>phoneNumber</tt>
     */
    public String normalize(String phoneNumber);

    /**
     * Determines whether two <tt>String</tt> phone numbers match.
     *
     * @param aPhoneNumber a <tt>String</tt> which represents a phone number to
     * match to <tt>bPhoneNumber</tt>
     * @param bPhoneNumber a <tt>String</tt> which represents a phone number to
     * match to <tt>aPhoneNumber</tt>
     * @return <tt>true</tt> if the specified <tt>String</tt>s match as phone
     * numbers; otherwise, <tt>false</tt>
     */
    public boolean phoneNumbersMatch(String aPhoneNumber, String bPhoneNumber);

    /**
     * Indicates if the given string is possibly a phone number.
     *
     * @param possibleNumber the string to be verified
     * @return 
     */
    public boolean isPhoneNumber(String possibleNumber);
}
