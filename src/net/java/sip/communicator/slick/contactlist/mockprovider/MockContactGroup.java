
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
    public int countSubGroups()
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
        Iterator groupsIter = contacts();
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
}
