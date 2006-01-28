package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * A dummy ContactGroup implementation representing the ContactList root for
 * ICQ contact lists.
 * @author Emil Ivov
 */
public class RootContactGroupIcqImpl
    extends AbstractContactGroupIcqImpl
{
    private String ROOT_CONTACT_GROUP_NAME = "ContactListRoot";
    private List subGroups = new LinkedList();

    /**
     * An empty list that we use when returning an iterator.
     */
    private List dummyContacts = new LinkedList();


    /**
     * The ContactListRoot in ICQ is the only group that can contain subgroups.
     *
     * @return true (always)
     */
    public boolean canContainSubgroups()
    {
        return true;
    }

    /**
     * Returns the name of this group which is always
     * <code>ROOT_CONTACT_GROUP_NAME</code>.
     *
     * @return a String containing the name of this group.
     */
    public String getGroupName()
    {
        return ROOT_CONTACT_GROUP_NAME;
    }

    /**
     * Adds the specified group at the specified position in the list of sub
     * groups.
     *
     * @param index the position at which the specified group should be added.
     * @param group the ContactGroup to add
     */
    void addSubGroup(int index, ContactGroupIcqImpl group)
    {
        subGroups.add(index, group);
    }

    /**
     * Adds the specified group to the end of the list of sub groups.
     * @param group the group to add.
     */
    void addSubGroup(ContactGroupIcqImpl group)
    {
        addSubGroup(countContacts(), group);
    }

    /**
     * Removes the specified from the list of sub groups
     * @param group the group to remove.
     */
    void removeSubGroup(ContactGroupIcqImpl group)
    {
        removeSubGroup(subGroups.indexOf(group));
    }

    /**
     * Removes the sub group with the specified index.
     * @param index the index of the group to remove
     */
    void removeSubGroup(int index)
    {
        subGroups.remove(index);
    }

    /**
     * Removes all contact sub groups and reinsterts them as specified
     * by the <code>newOrder</code> param. Contact groups not contained in the
     * newOrder list are left at the end of this group.
     *
     * @param newOrder a list containing all contact groups in the order that is
     * to be applied.
     *
     */
    void reorderSubGroups(List newOrder)
    {
        subGroups.removeAll(newOrder);
        subGroups.addAll(0, newOrder);
    }

    /**
     * Returns the number of subgroups contained by this
     * <code>RootContactGroupIcqImpl</code>.
     *
     * @return an int indicating the number of subgroups that this
     *   ContactGroup contains.
     */
    public int countSubGroups()
    {
        return subGroups.size();
    }

    /**
     * Returns the subgroup with the specified index.
     *
     * @param index the index of the <code>ContactGroup</code> to retrieve.
     * @return the <code>ContactGroup</code> with the specified index.
     */
    public ContactGroup getGroup(int index)
    {
        return (ContactGroupIcqImpl)subGroups.get(index);
    }

    /**
     * Returns the subgroup with the specified name.
     * @param groupName the name of the <code>ContactGroup</code> to retrieve.
     * @return the <code>ContactGroup</code> with the specified index.
     */
    public ContactGroup getGroup(String groupName)
    {
        Iterator subgroups = subGroups();
        while (subgroups.hasNext())
        {
            ContactGroupIcqImpl grp = (ContactGroupIcqImpl)subgroups.next();

            if (grp.getGroupName().equals(groupName))
                return grp;
        }

        return null;
    }

    /**
     * Returns the <code>Contact</code> with the specified address or
     * identifier.
     * @param id the addres or identifier of the <code>Contact</code> we are
     * looking for.
     * @return the <code>Contact</code> with the specified id or address.
     */
    public Contact getContact(String id)
    {
        //no contacts in the root group for this icq impl.
        return null;
    }

    /**
     * Returns an iterator over the sub groups that this
     * <code>ContactGroup</code> contains.
     *
     * @return a java.util.Iterator over the <code>ContactGroup</code>
     *   children of this group (i.e. subgroups).
     */
    public Iterator subGroups()
    {
        return subGroups.iterator();
    }

    /**
     * Returns the number, which is always 0, of <code>Contact</code> members
     * of this <code>ContactGroup</code>
     * @return an int indicating the number of <code>Contact</code>s, members
     * of this <code>ContactGroup</code>.
     */
    public int countContacts()
    {
        return 0;
    }

    /**
     * Returns an Iterator over all contacts, member of this
     * <code>ContactGroup</code>.
     * @return a java.util.Iterator over all contacts inside this
     * <code>ContactGroup</code>
     */
    public Iterator contacts()
    {
        return dummyContacts.iterator();
    }

    /**
     * A dummy impl of the corresponding interface method - always returns null.
     *
     * @param index the index of the <code>Contact</code> to return.
     * @return the <code>Contact</code> with the specified index, i.e. always
     * null.
     */
    public Contact getContact(int index)
    {
        return null;
    }

    /**
     * Returns a string representation of the root contact group that contains
     * all subgroups and subcontacts of this group.
     *
     * @return  a string representation of this root contact group.
     */
    public String toString()
    {
        StringBuffer buff = new StringBuffer(getGroupName());
        buff.append(".subGroups="+countSubGroups()+":\n");

        Iterator subGroups = subGroups();
        while (subGroups.hasNext())
        {
            ContactGroup group = (ContactGroup) subGroups.next();
            buff.append(group.toString());
            if(subGroups.hasNext())
                buff.append("\n");
        }
        return buff.toString();
    }

}
