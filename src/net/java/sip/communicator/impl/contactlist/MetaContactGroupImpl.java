package net.java.sip.communicator.impl.contactlist;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * A Default implementation of a MetaContactGroup. Note that this implementation
 * is only meant to be used for non-root contact groups and can only contain
 * contacts. All subgroup retrieving methods would returns null/0 values.
 * Root contact groups are to be represented by the RootMetaContactGroupImpl.
 * <p>
 * @author Emil Ivov
 */
public class MetaContactGroupImpl
    implements MetaContactGroup
{
    /**
     * All child contacts for this group.
     */
    private Vector childContacts = new Vector();

    /**
     * An empty list that we'll be using in order to return an empty iterator
     * of the (non-existing) sub groups.
     */
    private final List dummySubgroupsList = new LinkedList();

    private String groupName = null;

    protected MetaContactGroupImpl(String groupName)
    {
        this.groupName = groupName;
    }

    /**
     * Determines whether or not this group can contain subgroups.
     *
     * @return Always false since only the root contact group may contain sub
     * groups in our implementation.
     */
    public boolean canContainSubgroups()
    {
        return false;
    }

    /**
     * Returns the number of <tt>MetaContact</tt>s that this group contains
     * <p>
     * @return an int indicating the number of MetaContact-s that this group
     *   contains.
     */
    public int countChildContacts()
    {
        return childContacts.size();
    }

    /**
     * Returns a <tt>java.util.Iterator</tt> over the <tt>MetaContact</tt>s
     * contained in this <tt>MetaContactGroup</tt>.
     *
     * @return a <tt>java.util.Iterator</tt> over the <tt>MetaContacts</tt>
     *   in this group.
     */
    public Iterator getChildContacts()
    {
        return childContacts.iterator();
    }

    /**
     * Returns the contact with the specified identifier
     *
     * @param metaContactID a String identifier obtained through the
     *   <tt>MetaContact.getMetaContactID()</tt> method. <p>
     * @return the <tt>MetaContact</tt> with the specified idnetifier.
     */
    public MetaContact getMetaContact(String metaContactID)
    {
        Iterator contactsIter = getChildContacts();
        while(contactsIter.hasNext())
        {
            MetaContact contact = (MetaContact)contactsIter.next();

            if (contact.getMetaContactID().equals(metaContactID))
                return contact;
        }

        return null;
    }

    /**
     * Returns the meta contact on the specified index.
     *
     * @param index the index of the meta contact to return.
     * @return the MetaContact with the specified index, <p>
     * @throws IndexOutOfBoundsException in case <tt>index</tt> is not a
     *   valid index for this group.
     */
    public MetaContact getMetaContact(int index) throws
        IndexOutOfBoundsException
    {
        return (MetaContact)childContacts.get(index);
    }

    /**
     * Returns the <tt>MetaContactGroup</tt> with the specified index.
     *
     * @param index the index of the group to return.
     * @return always null since only the root contact group may contain sub
     * gorups in our implementation.
     * @throws IndexOutOfBoundsException if <tt>index</tt> is not a valid
     *   index.
     */
    public MetaContactGroup getMetaContactSubgroup(int index) throws
        IndexOutOfBoundsException
    {
        return null;
    }

    /**
     * Returns the name of this group.
     * @return a String containing the name of this group.
     */
    public String getGroupName()
    {
        return groupName;
    }

    /**
     * Returns the <tt>MetaContactGroup</tt> with the specified name.
     *
     * @param groupName the name of the group to return.
     * @return always null since only the root contact group may contain
     * subgroups in our implementation.
     */
    public MetaContactGroup getMetaContactSubgroup(String groupName)
    {
        return null;
    }

    /**
     * Returns the number of subgroups that this <tt>MetaContactGroup</tt>
     * contains.
     *
     * @return always 0 since only the root contact group may contain subgroups
     * in our implementation.
     */
    public int countSubgroups()
    {
        return 0;
    }

    /**
     * Returns an <tt>java.util.Iterator</tt> over the sub groups that this
     * <tt>MetaContactGroup</tt> contains.
     *
     * @return an Iterator over the empty subgroups list.
     */
    public Iterator getSubgroups()
    {
        return dummySubgroupsList.iterator();
    }

    /**
     * Adds the specified <tt>metaContact</tt> to ths local list of child
     * contacts.
     * @param metaContact the <tt>MetaContact</tt> to add in the local vector.
     */
    void addMetaContact(MetaContact metaContact)
    {
        this.childContacts.add(metaContact);
    }

    /**
     * Removes the specified <tt>metaContact</tt> from the local list of
     * contacts.
     * @param metaContact the <tt>MetaContact</tt>
     */
    void removeMetaContact(MetaContact metaContact)
    {
        this.childContacts.remove( metaContact );
    }


}
