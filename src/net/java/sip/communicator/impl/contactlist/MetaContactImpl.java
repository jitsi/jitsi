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
    implements MetaContact, Comparable
{
    /**
     * A vector containing all protocol specific contacts merged in this
     * MetaContact.
     *
     */
    private Vector protoContacts = new Vector();

    /**
     * The accumulated status index of all proto contacts merged in this
     * meta contact.
     */
    private int totalStatus = 0;

    /**
     * An id uniquely identifying the meta contact in this contact list.
     */
    private String uid = null;

    /**
     * Returns a human readable string used by the UI to display the contact.
     */
    private String displayName = "";

    /**
     * A callback to the meta contact group that is currently our parent. If
     * this is an orphan meta contact that has not yet been added or has been
     * removed from a group this callback is going to be null.
     */
    private MetaContactGroupImpl parentGroup = null;

    /**
     * A sync lock for use when modifying the parentGroup field.
     */
    private Object parentGroupModLock = new Object();

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
     * Compares this meta contact with the specified object for order.  Returns
     * a negative integer, zero, or a positive integer as this meta contact is
     * less than, equal to, or greater than the specified object.
     * <p>
     * The result of this method is calculated the following way:
     * <p>
     * (totalStatus - o.totalStatus) * 1 000 000  <br>
     * + getDisplayName().compareTo(o.getDisplayName()) * 100 000
     * + getMetaUID().compareTo(o.getMetaUID())<br>
     * <p>
     * Or in other words ordering of meta accounts would be first done by
     * presence status, then display name, and finally (in order to avoid
     * equalities) be the farely random meta contact metaUID.
     * <p>
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object is not
     *          a MetaContactListImpl
     */
    public int compareTo(Object o)
    {
        MetaContactImpl target = (MetaContactImpl)o;

        return ( (PresenceStatus.MAX_STATUS_VALUE - totalStatus)
                    - (PresenceStatus.MAX_STATUS_VALUE - target.totalStatus))
               * 1000000
                + getDisplayName().compareTo(target.getDisplayName()) * 100000
                + getMetaUID().compareToIgnoreCase(target.getMetaUID());
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
        synchronized (parentGroupModLock)
        {
            if (parentGroup != null)
            {
                parentGroup.lightRemoveMetaContact(this);
            }

            this.displayName = new String(displayName);

            if (parentGroup != null)
            {
                parentGroup.lightAddMetaContact(this);
            }
        }
    }

    /**
     * Adds the specified protocol specific contact to the list of contacts
     * merged in this meta contact. The method also keeps up to date the
     * totalStatus field which is used in the compareTo() method.
     *
     * @param contact the protocol specific Contact to add.
     */
    void addProtoContact(Contact contact)
    {
        synchronized (parentGroupModLock)
        {
            if (parentGroup != null)
            {
                parentGroup.lightRemoveMetaContact(this);
            }
            this.totalStatus += contact.getPresenceStatus().getStatus();

            this.protoContacts.add(contact);

            //if this is our firt contact - set the display name too.
            if(this.protoContacts.size() == 1){
                //be careful not to use setDisplayName() here cause this will
                //bring us into a deadlock.
                this.displayName = new String(contact.getDisplayName().getBytes());
            }

            if (parentGroup != null)
            {
                parentGroup.lightAddMetaContact(this);
            }
        }
    }

    /**
     * Called by MetaContactListServiceImpl after a contact has changed its
     * status, so that ordering in the parent group is updated.
     */
    void reevalContact()
    {
        synchronized (parentGroupModLock)
        {
            this.totalStatus = 0;

            //first lightremove or otherwise we won't be able to get hold of the
            //contact
            if (parentGroup != null)
            {
                parentGroup.lightRemoveMetaContact(this);
            }

            Iterator protoContacts = this.protoContacts.iterator();

            while (protoContacts.hasNext())
                totalStatus += ( (Contact) protoContacts.next()).
                    getPresenceStatus()
                    .getStatus();

            //now readd it and the contact would be automatically placed
            //properly by the containing group
            if (parentGroup != null)
            {
                parentGroup.lightAddMetaContact(this);
            }

        }
    }



    /**
     * Removes the specified protocol specific contact from the contacts
     * encapsulated in this <code>MetaContact</code>. The method also updates
     * the total status field accordingly. And updates its ordered position
     * in its parent group.
     *
     * @param contact the contact to remove
     */
    void removeProtoContact(Contact contact)
    {
        synchronized (parentGroupModLock)
        {
            if (parentGroup != null)
            {
                parentGroup.lightRemoveMetaContact(this);
            }
            totalStatus -= contact.getPresenceStatus().getStatus();
            this.protoContacts.remove(contact);

            if (parentGroup != null)
            {
                parentGroup.lightAddMetaContact(this);
            }
        }
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

    /**
     * Sets <tt>parentGroup</tt> as a parent of this meta contact. Do not
     * call this method with a null argument even if a group is removing
     * this contact from itself as this could lead to race conditions (imagine
     * another group setting itself as the new parent and you removing it).
     * Use unsetParentGroup instead.
     *
     * @param parentGroup the <tt>MetaContactGroupImpl</tt> that is currently a
     * parent of this meta contact.
     * @throws NullPointerException if <tt>parentGroup</tt> is null.
     */
    void setParentGroup(MetaContactGroupImpl parentGroup)
    {
        synchronized(parentGroupModLock)
        {
            if (parentGroup == null)
                throw new NullPointerException(
                    "Do not call this method with a "
                    + "null argument even if a group is removing this contact "
                    + "from itself as this could lead to race conditions "
                    + "(imagine another group setting itself as the new "
                    +"parent and you  removing it). Use unsetParentGroup "
                    +"instead.");

            this.parentGroup = parentGroup;
        }
    }

    /**
     * If <tt>parentGroup</tt> was the parent of this meta contact then it
     * sets it to null. Call this method when removing this contact from a
     * meta contact group.
     * @param parentGroup the <tt>MetaContactGroupImpl</tt> that we don't want
     * considered as a parent of this contact any more.
     */
    void unsetParentGroup(MetaContactGroupImpl parentGroup)
    {
        synchronized(parentGroupModLock)
        {
            if (this.parentGroup == parentGroup)
                this.parentGroup = null;
        }
    }

    /**
     * Returns the group that is currently holding this meta contact.
     *
     * @return the gorup that is currently holding this meta contact.
     */
    MetaContactGroupImpl getParentGroup()
    {
        return parentGroup;
    }
}
