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
 * Google Contacts service.
 *
 * @author Sebastien Vincent
 */
public interface GoogleContactsService
{
    /**
     * Perform a search for a contact using regular expression.
     *
     * @param cnx <tt>GoogleContactsConnection</tt> to perform the query
     * @param query Google query
     * @param count maximum number of matched contacts
     * @param callback object that will be notified for each new
     * <tt>GoogleContactsEntry</tt> found
     * @return list of <tt>GoogleContactsEntry</tt>
     */
    public List<GoogleContactsEntry> searchContact(GoogleContactsConnection cnx,
            GoogleQuery query, int count, GoogleEntryCallback callback);

    /**
     * Get a <tt>GoogleContactsConnection</tt>.
     *
     * Get a connection to Google Contacts. Only the login name may be provided.
     * Passwords are not supported anymore. Authorization is acquired by
     * requesting the user to go to Google and acquire an OAuth 2 approval for
     * Jitsi.
     *
     * @param login login to connect to the service
     * @return <tt>GoogleContactsConnection</tt>.
     */
    public GoogleContactsConnection getConnection(String login);

    /**
     * Get the full contacts list.
     *
     * @return list of <tt>GoogleContactsEntry</tt>
     */
    public List<GoogleContactsEntry> getContacts();

    /**
     * Add a contact source service with the specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param login login
     */
    public void addContactSource(String login);

    /**
     * Add a contact source service with the specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param cnx <tt>GoogleContactsConnection</tt>
     * @param googleTalk if the contact source has been created as GoogleTalk
     * account or via external Google Contacts
     */
    public void addContactSource(GoogleContactsConnection cnx,
        boolean googleTalk);

    /**
     * Remove a contact source service with the specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param cnx <tt>GoogleContactsConnection</tt>.
     */
    public void removeContactSource(GoogleContactsConnection cnx);

    /**
     * Remove a contact source service with the specified
     * <tt>GoogleContactsConnection</tt>.
     *
     * @param login login
     */
    public void removeContactSource(String login);
}
