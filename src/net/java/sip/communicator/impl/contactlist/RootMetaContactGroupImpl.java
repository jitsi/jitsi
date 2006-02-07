package net.java.sip.communicator.impl.contactlist;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * An implementation of the meta contact group that would only be used for the
 * root meta contact group.
 * @author Emil Ivov
 */
public class RootMetaContactGroupImpl
    implements MetaContactGroup
{
    /**
     * All the subgroups that this group contains.
     */
    private Vector subgroups = new Vector();

    /**
     * An empty list that we'll use to return an iterator over the
     * (non-exising) contats in this group.
     */
    private List dummyContacts = new LinkedList();

    private static final String groupName = "RootMetaContactGroup";

    /**
     * Creates an instance of the root meta contact group.
     */
    RootMetaContactGroupImpl()
    {

    }

    /**
     * Determines whether or not this group can contain subgroups.
     *
     * @return always <tt>true</tt> since this is the root contact group
     * and in our imple it can only contain groups.
     */
    public boolean canContainSubgroups()
    {
        return false;
    }

    /**
     * Returns the number of <tt>MetaContact</tt>s that this group contains.
     * <p>
     * @return always 0 since this is the root contact group and in our impl it
     * can only contain groups.
     */
    public int countChildContacts()
    {
        return 0;
    }

    /**
     * Returns the number of subgroups that this <tt>MetaContactGroup</tt>
     * contains.
     *
     * @return an int indicating the number of subgroups in this group.
     */
    public int countSubgroups()
    {
        return subgroups.size();
    }

    /**
     * Returns a <tt>java.util.Iterator</tt> over the <tt>MetaContact</tt>s
     * contained in this <tt>MetaContactGroup</tt>.
     *
     * @return a <tt>java.util.Iterator</tt> over an empty contacts list.
     */
    public Iterator getChildContacts()
    {
        return dummyContacts.iterator();
    }

    /**
     * Returns the contact with the specified identifier
     *
     * @param metaContactID a String identifier obtained through the
     *   <tt>MetaContact.getMetaContactID()</tt> method. <p>
     * @return always null since this is the root contact group and in our impl
     * it can only contain groups.
     */
    public MetaContact getMetaContact(String metaContactID)
    {
        return null;
    }

    /**
     * Returns the meta contact on the specified index.
     *
     * @param index the index of the meta contact to return.
     * @return always null since this is the root contact group and in our impl
     * it can only contain groups. <p>
     */
    public MetaContact getMetaContact(int index)
    {
        return null;
    }

    /**
     * Returns the <tt>MetaContactGroup</tt> with the specified index.
     * <p>
     * @param index the index of the group to return.
     * @return the <tt>MetaContactGroup</tt> with the specified index. <p>
     * @throws IndexOutOfBoundsException if <tt>index</tt> is not a valid
     *   index.
     */
    public MetaContactGroup getMetaContactSubgroup(int index) throws
        IndexOutOfBoundsException
    {
        return (MetaContactGroup)subgroups.get(index);
    }

    /**
     * Returns the <tt>MetaContactGroup</tt> with the specified name.
     *
     * @param groupName the name of the group to return.
     * @return the <tt>MetaContactGroup</tt> with the specified name or null
     *   if no such group exists.
     */
    public MetaContactGroup getMetaContactSubgroup(String groupName)
    {
        Iterator groupsIter = getSubgroups();

        while(groupsIter.hasNext())
        {
            MetaContactGroup mcGroup = (MetaContactGroup)groupsIter.next();

            if(mcGroup.getGroupName().equals(groupName))
                return mcGroup;
        }

        return null;
    }

    /**
     * Returns an <tt>java.util.Iterator</tt> over the sub groups that this
     * <tt>MetaContactGroup</tt> contains.
     * <p>
     * @return a <tt>java.util.Iterator</tt> containing all subgroups.
     */
    public Iterator getSubgroups()
    {
        return subgroups.iterator();
    }

    /**
     * Returns the name of this group.
     * @return a String containing the name of this group.
     */
    public String getGroupName()
    {
        return groupName;
    }



    void addProtocolSpecificGroup()
    {

    }

    /**
     * Adds the specified meta group to the subgroups of this one.
     * @param subgroup the MetaContactGroup to register as a subgroup to this
     * root meta contact group.
     */
    void addSubgroup(MetaContactGroup subgroup)
    {
        this.subgroups.add(subgroup);
    }

    /**
     * Removes the meta contact group with the specified index.
     * @param the <tt>index</tt> index of the group to remove.
     * @return the <tt>MetaContactGroup</tt> that has just been removed.
     */
    MetaContactGroupImpl removeSubgroup(int index)
    {
        return (MetaContactGroupImpl)subgroups.remove(index);
    }

    /**
     * Removes the specified group from the list of groups in this list.
     * @param group the <tt>MetaContactGroup</tt> to remove.
     * @return true if the group has been successfully removed and false
     * otherwise.
     */
    boolean removeSubgroup(MetaContactGroup group)
    {
        return subgroups.remove(group);
    }
}
