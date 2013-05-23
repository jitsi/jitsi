/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.regex.*;

import org.jitsi.service.configuration.*;

/**
 * Implements <tt>PhoneNumberI18nService</tt> which aids the parsing, formatting
 * and validating of international phone numbers.
 *
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 */
public abstract class PhoneNumberI18nService
{
    /**
     * The configuration service.
     */
    private static ConfigurationService configService
        = ProtocolProviderActivator.getConfigurationService();

    /**
     * Characters which have to be removed from a phone number in order to
     * normalized it.
     */
    private static final Pattern removedCharactersToNormalizedPhoneNumber
        = Pattern.compile("[-\\(\\)\\.\\\\\\/ ]");

    /**
     * Characters which have to be removed from a number (which is not a phone
     * number, such as a sip id, a jabber id, etc.) in order to normalized it.
     */
    private static final Pattern removedCharactersToNormalizedIdentifier
        = Pattern.compile("[\\(\\) ]");

    /**
     * The list of characters corresponding to the number 2 in a phone dial pad.
     */
    private static final Pattern charactersFordialPadNumber2
        = Pattern.compile("[abc]", Pattern.CASE_INSENSITIVE);
    /**
     * The list of characters corresponding to the number 3 in a phone dial pad.
     */
    private static final Pattern charactersFordialPadNumber3
        = Pattern.compile("[def]", Pattern.CASE_INSENSITIVE);

    /**
     * The list of characters corresponding to the number 4 in a phone dial pad.
     */
    private static final Pattern charactersFordialPadNumber4
        = Pattern.compile("[ghi]", Pattern.CASE_INSENSITIVE);

    /**
     * The list of characters corresponding to the number 5 in a phone dial pad.
     */
    private static final Pattern charactersFordialPadNumber5
        = Pattern.compile("[jkl]", Pattern.CASE_INSENSITIVE);

    /**
     * The list of characters corresponding to the number 6 in a phone dial pad.
     */
    private static final Pattern charactersFordialPadNumber6
        = Pattern.compile("[mno]", Pattern.CASE_INSENSITIVE);

    /**
     * The list of characters corresponding to the number 7 in a phone dial pad.
     */
    private static final Pattern charactersFordialPadNumber7
        = Pattern.compile("[pqrs]", Pattern.CASE_INSENSITIVE);

    /**
     * The list of characters corresponding to the number 8 in a phone dial pad.
     */
    private static final Pattern charactersFordialPadNumber8
        = Pattern.compile("[tuv]", Pattern.CASE_INSENSITIVE);

    /**
     * The list of characters corresponding to the number 9 in a phone dial pad.
     */
    private static final Pattern charactersFordialPadNumber9
        = Pattern.compile("[wxyz]", Pattern.CASE_INSENSITIVE);

    /**
     * Normalizes a <tt>String</tt> which may be a phone number or a identifier
     * by removing useless characters and, if necessary, replacing the alpahe
     * characters in corresponding dial pad numbers.
     *
     * @param possibleNumber a <tt>String</tt> which may represents a phone
     * number or an identifier to normalize.
     *
     * @return a <tt>String</tt> which is a normalized form of the specified
     * <tt>possibleNumber</tt>.
     */
    public static String normalize(String possibleNumber)
    {
        String normalizedNumber;
        if(isPhoneNumber(possibleNumber))
        {
            normalizedNumber = normalizePhoneNumber(possibleNumber);
        }
        else
        {
            normalizedNumber = normalizeIdentifier(possibleNumber);
        }

        return normalizedNumber;
    }


    /**
     * Normalizes a <tt>String</tt> phone number by converting alpha characters
     * to their respective digits on a keypad and then stripping non-digit
     * characters.
     *
     * @param phoneNumber a <tt>String</tt> which represents a phone number to
     * normalize
     *
     * @return a <tt>String</tt> which is a normalized form of the specified
     * <tt>phoneNumber</tt>
     *
     * @see PhoneNumberI18nService#normalize(String)
     */
    private static String normalizePhoneNumber(String phoneNumber)
    {
        phoneNumber = convertAlphaCharactersInNumber(phoneNumber);
        return removedCharactersToNormalizedPhoneNumber
            .matcher(phoneNumber).replaceAll("");
    }

    /**
     * Removes useless characters from a identifier (which is not a phone
     * number) in order to normalized it.
     *
     * @param id The identifier string with some useless characters like: " ",
     * "(", ")".
     *
     * @return The normalized identifier.
     */
    private static String normalizeIdentifier(String id)
    {
        return removedCharactersToNormalizedIdentifier
            .matcher(id).replaceAll("");
    }

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
    public static boolean phoneNumbersMatch(
            String aPhoneNumber,
            String bPhoneNumber)
    {
        String aPhoneNumberNormalized = normalize(aPhoneNumber);
        String bPhoneNumberNormalized = normalize(bPhoneNumber);

        return aPhoneNumberNormalized.equals(bPhoneNumberNormalized);
    }

    /**
     * Indicates if the given string is possibly a phone number.
     *
     * @param possibleNumber the string to be verified
     * @return <tt>true</tt> if the possibleNumber is a phone number,
     * <tt>false</tt> - otherwise
     */
    public static boolean isPhoneNumber(String possibleNumber)
    {
        // If the string does not contains an "@", this may be a phone number.
        if(possibleNumber.indexOf('@') == -1)
        {
            // If the string does not contain any alphabetical characters, then
            // this is a phone number.
            if(!possibleNumber.matches(".*[a-zA-Z].*"))
            {
                return true;
            }
            else
            {
                // Removes the " ", "(" and ")" in order to search the "+"
                // character at the beginning at the string.
                String tmpPossibleNumber
                    = possibleNumber.replaceAll(" \\(\\)", "");
                // If the property is enabled and the string starts with a "+",
                // then we consider that this is a phone number.
                if(configService.getBoolean(
                        "impl.gui.ACCEPT_PHONE_NUMBER_WITH_ALPHA_CHARS",
                        true)
                        && tmpPossibleNumber.startsWith("+"))
                {
                    return true;
                }
            }
        }
        // Else the string is not a phone number.
        return false;
    }

    /**
     * Changes all alphabetical characters into numbers, following phone dial
     * pad disposition.
     *
     * @param phoneNumber The phone number string with some alphabetical
     * characters.
     *
     * @return The phone number with all alphabetical caracters replaced with
     * the corresponding dial pad number.
     */
    private static String convertAlphaCharactersInNumber(String phoneNumber)
    {
        phoneNumber
            = charactersFordialPadNumber2.matcher(phoneNumber).replaceAll("2");
        phoneNumber
            = charactersFordialPadNumber3.matcher(phoneNumber).replaceAll("3");
        phoneNumber
            = charactersFordialPadNumber4.matcher(phoneNumber).replaceAll("4");
        phoneNumber
            = charactersFordialPadNumber5.matcher(phoneNumber).replaceAll("5");
        phoneNumber
            = charactersFordialPadNumber6.matcher(phoneNumber).replaceAll("6");
        phoneNumber
            = charactersFordialPadNumber7.matcher(phoneNumber).replaceAll("7");
        phoneNumber
            = charactersFordialPadNumber8.matcher(phoneNumber).replaceAll("8");
        return charactersFordialPadNumber9.matcher(phoneNumber).replaceAll("9");
    }
}
