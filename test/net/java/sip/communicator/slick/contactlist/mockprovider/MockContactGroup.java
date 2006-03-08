/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.contactlist.mockprovider;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * A simple, straightforward mock implementation of the ContactGroup interface
 * that  can be manually created and filled and used in testing a
 * MetaContactList service
 * @author Emil Ivov
 */
public class MockContactGroup
    implements ContactGroup
{
    private String groupName = null;

    private Vector contacts = new Vector();
    private Vector subGroups = new Vector();

    private MockProvider parentProvider = null;

    /**
     * Creates a MockGroup with the specified name.
     * @param groupName the name of the group.
     * @param parentProvider the protocol provider that created this group.
     */
    public MockContactGroup(String groupName, MockProvider parentProvider)
    {
        this.groupName = groupName;
        this.parentProvider = parentProvider;
    }

    /**
     * Determines whether the group may contain subgroups or not.
     *
     * @return always true in this implementation.
     */
    public boolean canContainSubgroups()
    {
        return true;
    }

    /**
     * Returns the protocol provider that this group belongs to.
     * @return a regerence to the ProtocolProviderService instance that this
     * ContactGroup belongs to.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return parentProvider;
    }

    /**
     * Returns an Iterator over all contacts, member of this
     * <tt>ContactGroup</tt>.
     *
     * @return a java.util.Iterator over all contacts inside this
     *   <tt>ContactGroup</tt>
     */
    public Iterator contacts()
    {
        return contacts.iterator();
    }

    /**
     * Adds the specified contact to this group.
     * @param contactToAdd the MockContact to add to this group.
     */
    public void addContact(MockContact contactToAdd)
    {
        this.contacts.add(contactToAdd);
    }

    /**
     * Returns the number of <tt>Contact</tt> members of this
     * <tt>ContactGroup</tt>
     *
     * @return an int indicating the number of <tt>Contact</tt>s, members of
     *   this <tt>ContactGroup</tt>.
     */
    public int countContacts()
    {
        return contacts.size();
    }

    /**
     * Returns the number of subgroups contained by this
     * <tt>ContactGroup</tt>.
     *
     * @return the number of subGroups currently added to this group.
     */
    public int countSubgroups()
    {
        return subGroups.size();
    }

    /**
     * Adds the specified contact group to the contained by this group.
     * @param subGroup the MockContactGroup to add as a subgroup to this group.
     */
    public void addSubGroup(MockContactGroup subGroup)
    {
        this.subGroups.add(subGroup);
    }

    /**
     * Removes the specified contact group from the this group's subgroups.
     * @param subGroup the MockContactGroup subgroup to remove.
     */
    public void removeSubGroup(MockContactGroup subGroup)
    {
        this.subGroups.remove(subGroup);
    }


    /**
     * Returns the <tt>Contact</tt> with the specified index.
     *
     * @param index the index of the <tt>Contact</tt> to return.
     * @return the <tt>Contact</tt> with the specified index.
     */
    public Contact getContact(int index)
    {
        return (MockContact)contacts.get(index);
    }

    /**
     * Returns the group that is parent of the specified mockGroup or null
     * if no parent was found.
     * @param mockGroup the group whose parent we're looking for.
     * @return the MockContactGroup instance that mockGroup belongs to or null
     * if no parent was found.
     */
    public MockContactGroup findGroupParent(MockContactGroup mockGroup)
    {
        if ( subGroups.contains(mockGroup) )
            return this;

        Iterator subGroupsIter = subGroups();
        while (subGroupsIter.hasNext())
        {
            MockContactGroup subgroup = (MockContactGroup) subGroupsIter.next();

            MockContactGroup parent = subgroup.findGroupParent(mockGroup);
            if(parent != null)
                return parent;
        }
        return null;
    }


    /**
     * Returns the <tt>Contact</tt> with the specified address or identifier.
     *
     * @param id the addres or identifier of the <tt>Contact</tt> we are
     *   looking for.
     * @return the <tt>Contact</tt> with the specified id or address.
     */
    public Contact getContact(String id)
    {
        Iterator contactsIter = contacts();
        while (contactsIter.hasNext())
        {
            MockContact contact = (MockContact) contactsIter.next();
            if (contact.getAddress().equals(id))
                return contact;

        }
        return null;
    }

    /**
     * Returns the subgroup with the specified index.
     *
     * @param index the index of the <tt>ContactGroup</tt> to retrieve.
     * @return the <tt>ContactGroup</tt> with the specified index.
     */
    public ContactGroup getGroup(int index)
    {
        return (ContactGroup)subGroups.get(index);
    }

    /**
     * Returns the subgroup with the specified name.
     *
     * @param groupName the name of the <tt>ContactGroup</tt> to retrieve.
     * @return the <tt>ContactGroup</tt> with the specified index.
     */
    public ContactGroup getGroup(String groupName)
    {
        Iterator groupsIter = subGroups();
        while (groupsIter.hasNext())
        {
            MockContactGroup contactGroup
                = (MockContactGroup) groupsIter.next();
            if (contactGroup.getGroupName().equals(groupName))
                return contactGroup;

        }
        return null;

    }

    /**
     * Returns the name of this group.
     *
     * @return a String containing the name of this group.
     */
    public String getGroupName()
    {
        return this.groupName;
    }

    /**
     * Sets this group a new name.
     * @param newGrpName a String containing the new name of this group.
     */
    public void setGroupName(String newGrpName)
    {
        this.groupName = newGrpName;
    }

    /**
     * Returns an iterator over the sub groups that this
     * <tt>ContactGroup</tt> contains.
     *
     * @return a java.util.Iterator over the <tt>ContactGroup</tt> children
     *   of this group (i.e. subgroups).
     */
    public Iterator subGroups()
    {
        return subGroups.iterator();
    }

    /**
     * Removes the specified contact from this group.
     * @param contact the MockContact to remove from this group
     */
    public void removeContact(MockContact contact)
    {
        this.contacts.remove(contact);
    }

    /**
     * Returns the contact with the specified id or null if no such contact
     * exists.
     * @param id the id of the contact we're looking for.
     * @return MockContact
     */
    public MockContact findContactByID(String id)
    {
        //first go through the contacts that are direct children.
        Iterator contactsIter = contacts();

        while(contactsIter.hasNext())
        {
            MockContact mContact = (MockContact)contactsIter.next();

            if( mContact.getAddress().equals(id) )
                return mContact;
        }

        //if we didn't find it here, let's try in the subougroups
        Iterator groupsIter = subGroups();

        while( groupsIter.hasNext() )
        {
            MockContactGroup mGroup = (MockContactGroup)groupsIter.next();

            MockContact mContact = mGroup.findContactByID(id);

            if (mContact != null)
                return mContact;
        }

        return null;
    }


    /**
     * Returns a String representation of this group and the contacts it
     * contains (may turn out to be a relatively long string).
     * @return a String representing this group and its child contacts.
     */
     public String toString()
     {

        StringBuffer buff = new StringBuffer(getGroupName());
        buff.append(".subGroups=" + countSubgroups() + ":\n");

        Iterator subGroups = subGroups();
        while (subGroups.hasNext())
        {
            MockContactGroup group = (MockContactGroup)subGroups.next();
            buff.append(group.toString());
            if (subGroups.hasNext())
                buff.append("\n");
        }

        buff.append("\nChildContacts="+countContacts()+":[");

        Iterator contacts = contacts();
        while (contacts.hasNext())
        {
            MockContact contact = (MockContact) contacts.next();
            buff.append(contact.toString());
            if(contacts.hasNext())
                buff.append(", ");
        }
        return buff.append("]").toString();
    }

}

