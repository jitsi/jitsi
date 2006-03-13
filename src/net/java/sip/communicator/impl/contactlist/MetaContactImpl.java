/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.contactlist;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A default implementation of a meta contact.
 * @author Emil Ivov
 */
public class MetaContactImpl
    implements MetaContact
{
    /**
     * A vector containing all protocol specific contacts merged in this
     * MetaContact.
     *
     */
    private Vector protoContacts = new Vector();

    /**
     * An id uniquely identifying the meta contact in this contact list.
     */
    private String uid = null;

    /**
     * Returns a human readable string used by the UI to display the contact.
     */
    private String displayName = "";

    MetaContactImpl()
    {
        //create the uid
        this.uid = String.valueOf( System.currentTimeMillis())
                   + String.valueOf(hashCode());
    }

    /**
     * Returns the number of protocol speciic <tt>Contact</tt>s that this
     * <tt>MetaContact</tt> contains.
     *
     * @return an int indicating the number of protocol specific contacts
     *   merged in this <tt>MetaContact</tt>
     */
    public int getContactCount()
    {
        return protoContacts.size();
    }

    /**
     * Returns a Contact, encapsulated by this MetaContact and coming from
     * the specified ProtocolProviderService.
     * <p>
     * In order to prevent problems with concurrency, the <tt>Iterator</tt>
     * returned by this method is not be over the actual list of contacts but
     * over a copy of that list.
     *
     * @param provider a reference to the <tt>ProtocolProviderService</tt>
     *   that we'd like to get a <tt>Contact</tt> for.
     * @return a <tt>Contact</tt> encapsulated in this <tt>MetaContact</tt>
     *   and originating from the specified provider.
     */
    public Iterator getContactsForProvider(ProtocolProviderService provider)
    {
        Iterator contactsIter = protoContacts.iterator();
        LinkedList providerContacts = new LinkedList();

        while (contactsIter.hasNext())
        {
            Contact contact = (Contact)contactsIter.next();

            if(contact.getProtocolProvider() == provider)
                providerContacts.add( contact );
        }

        return providerContacts.iterator();
    }

    /**
     * Returns a contact encapsulated by this meta contact, having the specified
     * contactAddress and coming from the indicated ownerProvider.
     * <p>
     * @param contactAddress the address of the contact who we're looking for.
     * @param ownerProvider a reference to the ProtocolProviderService that
     * the contact we're looking for belongs to.
     * @return a reference to a <tt>Contact</tt>, encapsulated by this
     * MetaContact, carrying the specified address and originating from the
     * specified ownerProvider or null if no such contact exists..
     */
    public Contact getContact(String contactAddress,
                              ProtocolProviderService ownerProvider)
    {
        Iterator contactsIter = protoContacts.iterator();

        while (contactsIter.hasNext())
        {
            Contact contact = (Contact)contactsIter.next();

            if(   contact.getProtocolProvider() == ownerProvider
               && contact.getAddress().equals(contactAddress))
                return contact;
        }

        return null;

    }

    /**
     * Returns a <tt>java.util.Iterator</tt> over all protocol specific
     * <tt>Contacts</tt> encapsulated by this <tt>MetaContact</tt>.
     * <p>
     * In order to prevent problems with concurrency, the <tt>Iterator</tt>
     * returned by this method is not over the actual list of contacts but over
     * a copy of that list.
     * <p>
     * @return a <tt>java.util.Ierator</tt> over all protocol specific
     * <tt>Contact</tt>s that were registered as subcontacts for this
     * <tt>MetaContact</tt>
     */
    public Iterator getContacts()
    {
        List contactsCopy = new LinkedList(protoContacts);
        return contactsCopy.iterator();
    }

    /**
     * Currently simply returns the first contact in the list of proto spec.
     * contacts. We should do this more inteligently though and have it
     * chose according to preconfigured preferences.
     *
     * @return the default <tt>Contact</tt> to use when communicating with
     *   this <tt>MetaContact</tt>
     */
    public Contact getDefaultContact()
    {
        return (Contact)this.protoContacts.get(0);
    }

    /**
     * Returns a String identifier (the actual contents is left to
     * implementations) that uniquely represents this <tt>MetaContact</tt> in
     * the containing <tt>MetaContactList</tt>
     *
     * @return a String uniquely identifying this meta contact.
     */
    public String getMetaUID()
    {
        return uid;
    }

    /**
     * Returns a string representation of this contact, containing most of its
     * representative details.
     *
     * @return  a string representation of this contact.
     */
    public String toString()
    {
        StringBuffer buff = new StringBuffer("MetaContact[ DisplayName=")
            .append(getDisplayName()).append("]");

        return buff.toString();
    }


    /**
     * Returns a characteristic display name that can be used when including
     * this <tt>MetaContact</tt> in user interface.
     * @return a human readable String that represents this meta contact.
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Sets a name that can be used when displaying this contact in user
     * interface components.
     * @param displayName a human readable String representing this
     * <tt>MetaContact</tt>
     */
    void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * Adds the specified protocol specific contact to the list of contacts
     * merged in this meta contact.
     * @param contact the protocol specific Contact to add.
     */
    void addProtoContact(Contact contact)
    {
        this.protoContacts.add(contact);

        //if this is our firt contact - set the display name too.
        if(this.protoContacts.size() == 1)
            setDisplayName(contact.getDisplayName());
    }

    /**
     * Removes the specified protocol specific contact from the contacts
     * encapsulated in this <code>MetaContact</code>
     *
     * @param contact the contact to remove
     *
     * @return true if this <tt>MetaContact</tt> contained the specified
     * contact and false otherwise.
     */
    boolean removeProtoContact(Contact contact)
    {
        return this.protoContacts.remove(contact);
    }


    /**
     * Removes all proto contacts that belong to the specified provider.
     *
     * @param provider the provider whose contacts we want removed.
     *
     * @return true if this <tt>MetaContact</tt> was modified and false
     * otherwise.
     */
    boolean removeContactsForProvider(ProtocolProviderService provider)
    {
        boolean modified = false;
        Iterator contactsIter = protoContacts.iterator();

        while(contactsIter.hasNext())
        {
            Contact contact = (Contact)contactsIter.next();

            if (contact.getProtocolProvider() == provider)
            {
                contactsIter.remove();
                modified = true;
            }
        }

        return modified;
    }



}
