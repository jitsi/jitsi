/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.contactlist;

import java.util.*;

import java.util.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
/**
 * A default implementation of the MetaContact interface.
 * @author Emil Ivov
 */
public class MetaContactImpl
    implements MetaContact, Comparable
{
    /**
     * Logger for <tt>MetaContactImpl</tt>.
     */
    private static final Logger logger
        = Logger.getLogger(MetaContactImpl.class);

    /**
     * A vector containing all protocol specific contacts merged in this
     * MetaContact.
     *
     */
    private Vector protoContacts = new Vector();

    /**
     * The number of contacts online in this meta contact.
     */
    private int contactsOnline = 0;

    /**
     * An id uniquely identifying the meta contact in this contact list.
     */
    private String uid = null;

    /**
     * Returns a human readable string used by the UI to display the contact.
     */
    private String displayName = "";

    /**
     * The contact that should be chosen by default when communicating with this
     * meta contact.
     */
    private Contact defaultContact = null;

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
    
    /**
     * Hashtable containing the contact details.
     * Name -> Value or Name -> (List of values).
     */
    private Hashtable details = new Hashtable();
    
    /**
     * The service that is creating the contact.
     */
    private MetaContactListServiceImpl mclServiceImpl  = null;

    /**
     * Creates new meta contact with a newly generated meta contact UID.
     * @param mclServiceImpl the service that creates the contact.
     */
    MetaContactImpl(MetaContactListServiceImpl mclServiceImpl)
    {
        //create the uid
        this.uid = String.valueOf( System.currentTimeMillis())
                   + String.valueOf(hashCode());
        this.mclServiceImpl = mclServiceImpl;
    }

    /**
     * Creates a new meta contact with the specified UID. This constructor
     * MUST ONLY be used when restoring contacts stored in the contactlist.xml.
     * @param metaUID the meta uid that this meta contact should have.
     * @param mclServiceImpl the service that creates the contact.
     * @param details the already stored details for the contact.
     */
    MetaContactImpl(MetaContactListServiceImpl mclServiceImpl, 
            String metaUID, Hashtable details)
    {
        this.uid = metaUID;
        this.mclServiceImpl = mclServiceImpl;
        this.details = details;
    }

    /**
     * Returns the number of protocol specific <tt>Contact</tt>s that this
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
     * Returns contacts, encapsulated by this MetaContact and belonging to
     * the specified protocol ContactGroup.
     * <p>
     * In order to prevent problems with concurrency, the <tt>Iterator</tt>
     * returned by this method is not be over the actual list of contacts but
     * over a copy of that list.
     *
     * @param parentProtoGroup a reference to the <tt>ContactGroup</tt>
     *   whose children we'd like removed..
     * @return an Iterator over all <tt>Contact</tt>s encapsulated in this
     * <tt>MetaContact</tt> and belonging to the specified proto ContactGroup.
     */
    public Iterator getContactsForContactGroup(ContactGroup parentProtoGroup)
    {
        Iterator contactsIter = protoContacts.iterator();
        LinkedList providerContacts = new LinkedList();

        while (contactsIter.hasNext())
        {
            Contact contact = (Contact)contactsIter.next();

            if(contact.getParentContactGroup() == parentProtoGroup)
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
     * Returns a contact encapsulated by this meta contact, having the specified
     * contactAddress and coming from a provider with a mathing
     * <tt>accountID</tt>. The method returns null if no such contact exists.
     * <p>
     * @param contactAddress the address of the contact who we're looking for.
     * @param accountID the identifier of the provider that the contact we're
     * looking for must belong to.
     * @return a reference to a <tt>Contact</tt>, encapsulated by this
     * MetaContact, carrying the specified address and originating from the
     * ownerProvider carryign <tt>accountID</tt>.
     */
    public Contact getContact(String contactAddress,
                              String accountID)
    {
        Iterator contactsIter = protoContacts.iterator();

        while (contactsIter.hasNext())
        {
            Contact contact = (Contact)contactsIter.next();

            if(  contact.getProtocolProvider().getAccountID()
                    .getAccountUniqueID().equals(accountID)
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
     * Currently simply returns the most connected protocol contact. We should
     * add the possibility to choose it also according to preconfigured
     * preferences.
     *
     * @return the default <tt>Contact</tt> to use when communicating with
     *   this <tt>MetaContact</tt>
     */
    public Contact getDefaultContact()
    {
        if(defaultContact == null)
        {

            PresenceStatus currentStatus = null;
            for (int i = 0; i < this.protoContacts.size(); i++)
            {
                Contact protoContact = (Contact)this.protoContacts.get(i);

                PresenceStatus contactStatus = protoContact.getPresenceStatus();

                if (currentStatus != null)
                {
                    if (currentStatus.getStatus() < contactStatus.getStatus())
                    {
                        currentStatus = contactStatus;
                        defaultContact = protoContact;
                    }
                }
                else
                {
                    currentStatus = contactStatus;
                    defaultContact = protoContact;
                }
            }
        }
        return defaultContact;
    }

    /**
     * Returns a default contact for a specific operation (call,
     * file transfert, IM ...)
     *
     * @param operationSet the operation for which the default contact is needed
     * @return the default contact for the specified operation.
     */
    public Contact getDefaultContact(Class operationSet)
    {
        Contact defaultOpSetContact = null;

        // if the current default contact supports the requested operationSet
        // we use it
        if (getDefaultContact().getProtocolProvider()
                .getOperationSet(operationSet) != null)
        {
            defaultOpSetContact = getDefaultContact();
        }
        else
        {
            for (int i = 0; i < protoContacts.size(); i++)
            {
                PresenceStatus currentStatus = null;
                Contact protoContact = (Contact)this.protoContacts.get(i);

                // we filter to care only about contact which support
                // the needed opset.
                if (protoContact.getProtocolProvider()
                        .getOperationSet(operationSet) != null)
                {
                    PresenceStatus contactStatus
                            = protoContact.getPresenceStatus();

                    if (currentStatus != null)
                    {
                        if (currentStatus.getStatus()
                                < contactStatus.getStatus())
                        {
                            currentStatus = contactStatus;
                            defaultOpSetContact = protoContact;
                        }
                    }
                    else
                    {
                        currentStatus = contactStatus;
                        defaultOpSetContact = protoContact;
                    }
                }
            }
        }
        return defaultOpSetContact;
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
     * (contactsOnline - o.contactsOnline) * 1 000 000  <br>
     * + getDisplayName().compareTo(o.getDisplayName()) * 100 000
     * + getMetaUID().compareTo(o.getMetaUID())<br>
     * <p>
     * Or in other words ordering of meta accounts would be first done by
     * presence status, then display name, and finally (in order to avoid
     * equalities) be the fairly random meta contact metaUID.
     * <p>
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *      is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object is not
     *          a MetaContactListImpl
     */
    public int compareTo(Object o)
    {
        MetaContactImpl target = (MetaContactImpl) o;

        int isOnline
            = (contactsOnline > 0)
            ? 1
            : 0;
        int targetIsOnline
            = (target.contactsOnline > 0)
            ? 1
            : 0;

        return ( (10 - isOnline) - (10 - targetIsOnline)) * 100000000
            + getDisplayName().compareToIgnoreCase(target.getDisplayName())
            * 10000
            + getMetaUID().compareTo(target.getMetaUID());
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

            this.displayName = new String((displayName==null)?"":displayName);

            if (parentGroup != null)
            {
                parentGroup.lightAddMetaContact(this);
            }
        }
    }

    /**
     * Adds the specified protocol specific contact to the list of contacts
     * merged in this meta contact. The method also keeps up to date the
     * contactsOnline field which is used in the compareTo() method.
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
            contactsOnline += contact.getPresenceStatus().isOnline() ? 1 : 0;

            this.protoContacts.add(contact);

            //if this is our firt contact and we don't already have a display
            //name, use theirs.
            if(this.protoContacts.size() == 1
                &&( this.displayName == null
                    || this.displayName.trim().length() == 0)){
                //be careful not to use setDisplayName() here cause this will
                //bring us into a deadlock.
                this.displayName
                    = new String(contact.getDisplayName().getBytes());
            }

            if (parentGroup != null)
            {
                parentGroup.lightAddMetaContact(this);
            }
        }
    }

    /**
     * Called by MetaContactListServiceImpl after a contact has changed its
     * status, so that ordering in the parent group is updated. The method also
     * elects the most connected contact as default contact.
     *
     * @return the new index at which the contact was added.
     */
    int reevalContact()
    {
        synchronized (parentGroupModLock)
        {
            //first lightremove or otherwise we won't be able to get hold of the
            //contact
            if (parentGroup != null)
            {
                parentGroup.lightRemoveMetaContact(this);
            }

            this.contactsOnline = 0;
            int maxContactStatus = 0;

            Iterator protoContacts = this.protoContacts.iterator();

            while (protoContacts.hasNext())
            {
                Contact contact = ( (Contact) protoContacts.next());
                int contactStatus = contact.getPresenceStatus()
                        .getStatus();

                if(maxContactStatus < contactStatus)
                {
                    maxContactStatus = contactStatus;
                    this.defaultContact = contact;
                }
                contact.getPresenceStatus();
                contactsOnline += contact.getPresenceStatus().isOnline() ? 1 : 0;
            }
            //now readd it and the contact would be automatically placed
            //properly by the containing group
            if (parentGroup != null)
            {
                return parentGroup.lightAddMetaContact(this);
            }
        }

        return -1;
    }



    /**
     * Removes the specified protocol specific contact from the contacts
     * encapsulated in this <code>MetaContact</code>. The method also updates
     * the total status field accordingly. And updates its ordered position
     * in its parent group. If the display name of this <code>MetaContact</code>
     * was the one of the removed contact, we update it.
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
            contactsOnline -= contact.getPresenceStatus().isOnline() ? 1 : 0;
            this.protoContacts.remove(contact);

            if (defaultContact == contact)
            {
                defaultContact = null;
            }

            if ((protoContacts.size() > 0)
                    && displayName.equals(contact.getDisplayName()))
            {
                displayName = getDefaultContact().getDisplayName();
            }

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

        // if the default contact has been modified, set it to null
        if (modified && !protoContacts.contains(defaultContact))
        {
            defaultContact = null;
        }

        return modified;
    }

    /**
     * Removes all proto contacts that belong to the specified protocol group.
     *
     * @param protoGroup the group whose children we want removed.
     *
     * @return true if this <tt>MetaContact</tt> was modified and false
     * otherwise.
     */
    boolean removeContactsForGroup(ContactGroup protoGroup)
    {
        boolean modified = false;
        Iterator contactsIter = protoContacts.iterator();

        while(contactsIter.hasNext())
        {
            Contact contact = (Contact)contactsIter.next();

            if (contact.getParentContactGroup() == protoGroup)
            {
                contactsIter.remove();
                modified = true;
            }
        }

        // if the default contact has been modified, set it to null
        if (modified && !protoContacts.contains(defaultContact))
        {
            defaultContact = null;
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
     * @return the group that is currently holding this meta contact.
     */
    MetaContactGroupImpl getParentGroup()
    {
        return parentGroup;
    }

    /**
     * Returns the MetaContactGroup currently containing this meta contact
     * @return a reference to the MetaContactGroup currently containing this
     * meta contact.
     */
    public MetaContactGroup getParentMetaContactGroup()
    {
        return getParentGroup();
    }

    /**
     * Adds a custom detail to this contact. 
     * @param name name of the detail.
     * @param value the value of the detail.
     */
    public void addDetail(String name, String value)
    {
        ArrayList values = (ArrayList)details.get(name);
        
        if(values == null)
            values = new ArrayList();
        
        values.add(value);
        
        details.put(name, values);
        
        mclServiceImpl.fireMetaContactEvent(
            new MetaContactModifiedEvent(
                this,
                name,
                null,
                value));
    }
    
    /**
     * Remove the given detail.
     * @param name of the detail to be removed.
     * @param value value of the detail to be removed.
     */
    public void removeDetail(String name, String value)
    {
        ArrayList values = (ArrayList)details.get(name);
        
        if(values == null)
            return;
        
        values.remove(value);
        
        mclServiceImpl.fireMetaContactEvent(
            new MetaContactModifiedEvent(
                this,
                name,
                value,
                null));
    }
    
    /**
     * Remove all details with given name.
     * @param name of the details to be removed.
     */
    public void removeDetails(String name)
    {
        Object removed = details.remove(name);
        
        mclServiceImpl.fireMetaContactEvent(
            new MetaContactModifiedEvent(
                this,
                name,
                removed,
                null));
    }
    
    /**
     * Change the detail.
     * @param name of the detail to be changed.
     * @param oldValue the old value of the detail.
     * @param newValue the new value of the detail.
     */
    public void changeDetail(String name, String oldValue, String newValue)
    {
        ArrayList values = (ArrayList)details.get(name);
        
        if(values == null)
            return;
        
        int changedIx = -1;
        
        for (int i = 0; i < values.size(); i++) 
        {
            if(values.get(i).equals(oldValue))
            {
                changedIx = i;
                break;
            }
        }
        
        if(changedIx == -1)
            return;
        
        values.set(changedIx, newValue);
        
        mclServiceImpl.fireMetaContactEvent(
            new MetaContactModifiedEvent(
                this,
                name,
                oldValue,
                newValue));
    }
    
    /**
     * Get all details with given name.
     * @param name the name of the details we are searching.
     */
    public List getDetails(String name)
    {
        ArrayList values = (ArrayList)details.get(name);
        
        if(values == null)
            values = new ArrayList();
        else
            values = (ArrayList)values.clone();
        
        return values;
    }
}
