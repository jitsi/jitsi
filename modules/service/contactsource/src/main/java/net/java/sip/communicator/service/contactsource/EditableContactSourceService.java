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
package net.java.sip.communicator.service.contactsource;

/**
 * Provides an interface to create or delete contact for a given contact source
 * service.
 *
 * @author Vincent Lucas
 */
public interface EditableContactSourceService
    extends ContactSourceService
{
    /**
     * Creates a new contact from the database (i.e "contacts" or
     * "msoutlook", etc.).
     *
     * @return The ID of the contact to remove. NULL if failed to create a new
     * contact.
     */
    public String createContact();

    /**
     * Adds a new empty contact, which will be filled in later.
     *
     * @param id The ID of the contact to add.
     */
    public void addEmptyContact(String id);

    /**
     * Removes the given contact from the database (i.e "contacts" or
     * "msoutlook", etc.).
     *
     * @param id The ID of the contact to remove.
     */
    public void deleteContact(String id);

    /**
     * Returns the bitness of this contact source service.
     *
     * @return The bitness of this contact source service.
     */
    public int getBitness();

    /**
     * Returns the version of this contact source service.
     *
     * @return The version of this contact source service.
     */
    public int getVersion();

    /**
     * Returns the number of contact notifications to deal with.
     *
     * @return The number of contact notifications to deal with.
     */
    public int getNbRemainingNotifications();
}
