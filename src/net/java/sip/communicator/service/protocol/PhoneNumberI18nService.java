/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Implements <tt>PhoneNumberI18nService</tt> which aids the parsing, formatting
 * and validating of international phone numbers.
 *
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 * @author Damian Minkov
 */
public interface PhoneNumberI18nService
{
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
    public String normalize(String possibleNumber);

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
     * @return <tt>true</tt> if the possibleNumber is a phone number,
     * <tt>false</tt> - otherwise
     */
    public boolean isPhoneNumber(String possibleNumber);
}
