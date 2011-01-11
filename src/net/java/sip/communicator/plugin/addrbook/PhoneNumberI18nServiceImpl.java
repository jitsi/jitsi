/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addrbook;

import net.java.sip.communicator.service.protocol.*;

import com.google.i18n.phonenumbers.*;

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
        return
            PhoneNumberUtil.normalizeDigitsOnly(
                    PhoneNumberUtil.convertAlphaCharactersInNumber(
                            phoneNumber));
    }
}
