package net.java.sip.communicator.impl.contactlist;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the meta contact group that would only be used for the
 * root meta contact group.
 * @author Emil Ivov
 */
public class RootMetaContactGroupImpl
    implements MetaContactGroup
{
    private static final Logger logger =
        Logger.getLogger(RootMetaContactGroupImpl.class);
    /**
     * All the subgroups that this group contains.
     */
    private Vector subgroups = new Vector();

    /**
     * A list containing all child contacts.
     */
    private List childContacts = new LinkedList();

    /**
     * The name of the group (fixed for root groups since it won't show).
     */
    private static final String groupName = "RootMetaContactGroup";

    /**
     * The root groups for all protocol contact lists that we've detected so
     * far, mapped against their owner protocol providers.
     */
    private Hashtable protoGroups = new Hashtable();

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
     * @return the number of <tt>MetaContact</tt>s that this group contains.
     */
    public int countChildContacts()
    {
        return childContacts.size();
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

    /**
     * Returns a String representation of this group and the contacts it
     * contains (may turn out to be a relatively long string).
     * @return a String representing this group and its child contacts.
     */
     public String toString()
     {

        StringBuffer buff = new StringBuffer(getGroupName());
        buff.append(".subGroups=" + countSubgroups() + ":\n");

        Iterator subGroups = getSubgroups();
        while (subGroups.hasNext())
        {
            MetaContactGroupImpl group = (MetaContactGroupImpl)subGroups.next();
            buff.append(group.toString());
            if (subGroups.hasNext())
                buff.append("\n");
        }

        buff.append("\nRootChildContacts="+countChildContacts()+":[");

        Iterator contacts = getChildContacts();
        while (contacts.hasNext())
        {
            MetaContactImpl contact = (MetaContactImpl) contacts.next();
            buff.append(contact.toString());
            if(contacts.hasNext())
                buff.append(", ");
        }
        return buff.append("]").toString();
    }

    /**
     * Addes the specified group to the list of protocol specific roots
     * that we're encapsulating in this meta contact list.
     * @param protoRoot the root to add to the groups merged in this meta contact
     * group.
     * @param ownerProtocol the protocol that the specified group came from.
     */
    void addProtoGroup( ProtocolProviderService ownerProtocol,
                        ContactGroup protoRoot)
    {
        protoGroups.put(ownerProtocol, protoRoot);
    }



    /**
     * Adds the specified meta group to the subgroups of this one.
     * @param subgroup the MetaContactGroup to register as a subgroup to this
     * root meta contact group.
     */
    void addSubgroup(MetaContactGroup subgroup)
    {
        logger.trace("Adding subgroup " + subgroup.getGroupName()
                     + " to" + getGroupName());
        this.subgroups.add(subgroup);
    }

    /**
     * Removes the meta contact group with the specified index.
     * @param index the index of the group to remove.
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
