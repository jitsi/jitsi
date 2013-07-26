/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
}
