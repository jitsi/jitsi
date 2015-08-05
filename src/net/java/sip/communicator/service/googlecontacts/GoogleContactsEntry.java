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
package net.java.sip.communicator.service.googlecontacts;

import java.util.*;

/**
 * Entry of Google Contacts directory.
 *
 * @author Sebastien Vincent
 */
public interface GoogleContactsEntry
{
    /**
     * The supported IM protocol
     */
    public enum IMProtocol
    {
        /**
         * Google Talk protocol.
         */
        GOOGLETALK,

        /**
         * Yahoo protocol.
         */
        YAHOO,

        /**
         * AIM protocol.
         */
        AIM,

        /**
         * MSN protocol.
         */
        MSN,

        /**
         * ICQ protocol.
         */
        ICQ,

        /**
         * Jabber protocol.
         */
        JABBER,

        /**
         * Other protocol (i.e. not supported).
         */
        OTHER,
    }

    /**
     * Get the full name.
     *
     * @return full name
     */
    public String getFullName();

    /**
     * Get the family name.
     *
     * @return family name
     */
    public String getFamilyName();

    /**
     * Get the given name.
     *
     * @return given name
     */
    public String getGivenName();

    /**
     * Returns mails.
     *
     * @return mails
     */
    public List<String> getAllMails();

    /**
     * Adds a home mail address.
     *
     * @param mail the mail address
     */
    public void addHomeMail(String mail);

    /**
     * Returns home mail addresses.
     *
     * @return home mail addresses
     */
    public List<String> getHomeMails();

    /**
     * Adds a work mail address.
     *
     * @param mail the mail address
     */
    public void addWorkMails(String mail);

    /**
     * Returns work mail addresses.
     *
     * @return work mail addresses
     */
    public List<String> getWorkMails();

    /**
     * Returns telephone numbers.
     *
     * @return telephone numbers
     */
    public List<String> getAllPhones();

    /**
     * Adds a work telephone number.
     *
     * @param telephoneNumber the work telephone number
     */
    public void addWorkPhone(String telephoneNumber);

    /**
     * Returns work telephone numbers.
     *
     * @return work telephone numbers
     */
    public List<String> getWorkPhones();

    /**
     * Adds a mobile telephone numbers.
     *
     * @param telephoneNumber the mobile telephone number
     */
    public void addMobilePhone(String telephoneNumber);

    /**
     * Returns mobile telephone numbers.
     *
     * @return mobile telephone numbers
     */
    public List<String> getMobilePhones();

    /**
     * Adds a home telephone numbers.
     *
     * @param telephoneNumber the home telephone number
     */
    public void addHomePhone(String telephoneNumber);

    /**
     * Returns home telephone numbers.
     *
     * @return home telephone numbers
     */
    public List<String> getHomePhones();

    /**
     * Get the photo full URI.
     *
     * @return the photo URI or null if there isn't
     */
    public String getPhoto();

    /**
     * Returns IM addresses.
     *
     * @return Map where key is IM address and value is IM protocol (MSN, ...)
     */
    public Map<String, IMProtocol> getIMAddresses();

    /**
     * Adds an IM address.
     *
     * @param imAddress IM address
     * @param protocol IM protocol
     */
    public void addIMAddress(String imAddress, IMProtocol protocol);
}
