/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import com.google.i18n.phonenumbers.*;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Implements <tt>PhoneNumberI18nService</tt> which aids the parsing, formatting
 * and validating of international phone numbers using the libphonenumber
 * library.
 *
 * @author Lyubomir Marinov
 */
public class PhoneNumberI18nServiceImpl
    implements PhoneNumberI18nService
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
     * @see PhoneNumberI18nService#normalize(String)
     */
    public String normalize(String phoneNumber)
    {
        String plusSign = "+";
        boolean plusSignFound = false;
        if (phoneNumber.startsWith(plusSign))
            plusSignFound = true;

        String normalizedNumber
            = PhoneNumberUtil.normalizeDigitsOnly(
                    PhoneNumberUtil.convertAlphaCharactersInNumber(
                            phoneNumber));

        if (plusSignFound && !normalizedNumber.startsWith(plusSign))
            return plusSign + normalizedNumber;

        return normalizedNumber;
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
    public boolean phoneNumbersMatch(String aPhoneNumber, String bPhoneNumber)
    {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

        try
        {
            String defaultCountry = Locale.getDefault().getCountry();

            if ((defaultCountry != null) && (defaultCountry.length() != 0))
            {
                PhoneNumber a
                    = phoneNumberUtil.parse(aPhoneNumber, defaultCountry);
                PhoneNumber b
                    = phoneNumberUtil.parse(bPhoneNumber, defaultCountry);

                if (PhoneNumberUtil.MatchType.NO_MATCH
                        != phoneNumberUtil.isNumberMatch(a, b))
                    return true;
            }
        }
        catch (NumberParseException npex)
        {
            /* Ignore it, we'll try without the defaultCountry. */
        }
        try
        {
            return
                PhoneNumberUtil.MatchType.NO_MATCH
                    != phoneNumberUtil.isNumberMatch(aPhoneNumber, bPhoneNumber);
        }
        catch (NumberParseException npex)
        {
            throw new IllegalArgumentException(npex);
        }
    }

    /**
     * Indicates if the given string is possibly a phone number.
     *
     * @param possibleNumber the string to be verified
     * @return <tt>true</tt> if the possibleNumber is a phone number,
     * <tt>false</tt> - otherwise
     */
    public boolean isPhoneNumber(String possibleNumber)
    {
        PhoneNumberUtil numberUtil = PhoneNumberUtil.getInstance();
        try
        {
            if (numberUtil.isPossibleNumber(possibleNumber, null))
                return true;
            else
                return numberUtil.isPossibleNumber(
                    numberUtil.parse(   possibleNumber,
                                        Locale.getDefault().getCountry()));
        }
        catch (NumberParseException e)
        {
            return false;
        }
    }
}
