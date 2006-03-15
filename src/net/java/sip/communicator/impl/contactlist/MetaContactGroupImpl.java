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
import net.java.sip.communicator.util.*;

/**
 * A straightforward implementatiokn of the meta contact group. The group
 * implements a simple algorithme of sorting its children according to their
 * status.
 *
 * @author Emil Ivov
 */
public class MetaContactGroupImpl
    implements MetaContactGroup
{
    private static final Logger logger =
        Logger.getLogger(MetaContactGroupImpl.class);
    /**
     * All the subgroups that this group contains.
     */
    private Vector subgroups = new Vector();

    /**
     * A list containing all child contacts.
     */
    private TreeSet childContacts = new TreeSet();

    /**
     * A list of the contact groups encapsulated by this MetaContactGroup
     */
    private Vector protoGroups = new Vector();

    /**
     * The name of the group (fixed for root groups since it won't show).
     */
    private String groupName = null;

    /**
     * Creates an instance of the root meta contact group.
     *
     * @param groupName the name of the group to create
     */
    MetaContactGroupImpl(String groupName)
    {
        this.groupName = groupName;
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
     * <p>
     * In order to prevent problems with concurrency, the <tt>Iterator</tt>
     * returned by this method is not over the actual list of groups but over a
     * copy of that list.
     * <p>
     *
     * @return a <tt>java.util.Iterator</tt> over an empty contacts list.
     */
    public Iterator getChildContacts()
    {
        return new LinkedList(childContacts).iterator();
    }

    /**
     * Returns the contact with the specified identifier
     *
     * @param metaContactID a String identifier obtained through the
     *   <tt>MetaContact.getMetaUID()</tt> method. <p>
     * @return the <tt>MetaContact</tt> with the specified idnetifier.
     */
    public MetaContact getMetaContact(String metaContactID)
    {
        Iterator contactsIter = getChildContacts();
        while(contactsIter.hasNext())
        {
            MetaContact contact = (MetaContact)contactsIter.next();

            if (contact.getMetaUID().equals(metaContactID))
                return contact;
        }

        return null;
    }

    /**
     * Returns the index of metaContact according to other contacts in this or
     * -1 if metaContact does not belong to this group. The returned index is
     * only valid until another contact has been added / removed or a contact
     * has changed its status and hence - position. In such a case a REORDERED
     * event is fired.
     *
     * @param metaContact the <tt>MetaContact</tt> whose index we're looking
     * for.
     * @return the index of <tt>metaContact</tt> in the list of child contacts
     * or -1 if <tt>metaContact</tt>.
     */
    public int indexOf(MetaContact metaContact)
    {
        int i = 0;

        synchronized (childContacts)
        {
            Iterator childrenIter = childContacts.iterator();

            while (childrenIter.hasNext())
            {
                MetaContact current = (MetaContact) childrenIter.next();

                if (current == metaContact)
                {
                    return i;
                }
                i++;
            }
        }

        //if we got here then metaContact is not in this list
        return -1;
    }

    /**
     * Returns the index of metaContactGroup in relation to other subgroups in
     * this group or -1 if metaContact does not belong to this group. The
     * returned index is only valid until another group has been added /
     * removed or renamed In such a case a REORDERED event is fired.
     *
     * @param metaContactGroup the <tt>MetaContactGroup</tt> whose index we're
     * looking for.
     * @return the index of <tt>metaContactGroup</tt> in the list of child
     * contacts or -1 if <tt>metaContact</tt>.
     */
    public int indexOf(MetaContactGroup metaContactGroup)
    {
        return subgroups.indexOf(metaContactGroup);
    }

    /**
     * Returns the meta contact encapsulating a contact belonging to the
     * specified <tt>provider</tt> with the specified identifier.
     *
     * @param provider the ProtocolProviderService that the specified
     * <tt>contactID</tt> is pertaining to.
     * @param contactID a String identifier of the protocol specific contact
     * whose container meta contact we're looking for.
     * @return the <tt>MetaContact</tt> with the specified idnetifier.
     */
    public MetaContact getMetaContact(ProtocolProviderService provider,
                                      String contactID)
    {
        Iterator contactsIter = getChildContacts();
        while(contactsIter.hasNext())
        {
            MetaContact contact = (MetaContact)contactsIter.next();

            if (contact.getContact(contactID, provider) != null)
                return contact;
        }

        return null;

    }

    /**
     * Returns a meta contact, a child of this group or its subgroups, that
     * has the specified metaUID. If no such meta contact exists, the method
     * would return null.
     *
     * @param metaUID the Meta UID of the contact we're looking for.
     * @return the MetaContact with the specified UID or null if no such
     * contact exists.
     */
    public MetaContactImpl findMetaContactByMetaUID(String metaUID)
    {
        //first go through the contacts that are direct children of this method.
        Iterator contactsIter = getChildContacts();

        while(contactsIter.hasNext())
        {
            MetaContactImpl mContact = (MetaContactImpl)contactsIter.next();

            if( mContact.getMetaUID().equals(metaUID) )
                return mContact;
        }

        //if we didn't find it here, let's try in the subougroups
        Iterator groupsIter = getSubgroups();

        while( groupsIter.hasNext() )
        {
            MetaContactGroupImpl mGroup = (MetaContactGroupImpl)groupsIter.next();

            MetaContactImpl mContact = mGroup.findMetaContactByMetaUID(metaUID);

            if (mContact != null)
                return mContact;
        }

        return null;
    }

    /**
     * Returns an iterator over all the protocol specific groups that this
     * contact group represents.
     * <p>
     * In order to prevent problems with concurrency, the <tt>Iterator</tt>
     * returned by this method is not over the actual list of groups but over a
     * copy of that list.
     * <p>
     * @return an Iterator over the protocol specific groups that this group
     * represents.
     */
    public Iterator getContactGroups()
    {
        return new LinkedList( this.protoGroups ).iterator();
    }

    /**
     * Returns a contact group encapsulated by this meta contact group, having
     * the specified groupName and coming from the indicated ownerProvider.
     *
     * @param groupName the name of the contact group who we're looking for.
     * @param ownerProvider a reference to the ProtocolProviderService that
     * the contact we're looking for belongs to.
     * @return a reference to a <tt>ContactGroup</tt>, encapsulated by this
     * MetaContactGroup, carrying the specified name and originating from the
     * specified ownerProvider or null if no such contact group was found.
     */
    public ContactGroup getContactGroup(String groupName,
                                        ProtocolProviderService ownerProvider)
    {
        Iterator encapsulatedGroups = getContactGroups();

        while (encapsulatedGroups.hasNext())
        {
            ContactGroup group = (ContactGroup)encapsulatedGroups.next();

            if (group.getGroupName().equals(groupName)
                && group.getProtocolProvider() == ownerProvider)
            {
                return group;
            }
        }
        return null;
    }

    /**
     * Returns all protocol specific ContactGroups, encapsulated by this
     * MetaContactGroup and coming from the indicated ProtocolProviderService.
     * If none of the contacts encapsulated by this MetaContact is originating
     * from the specified provider then an empty iterator is returned.
     * <p>
     * @param provider a reference to the <tt>ProtocolProviderService</tt>
     * whose ContactGroups we'd like to get.
     * @return an <tt>Iterator</tt> over all contacts encapsulated in this
     * <tt>MetaContact</tt> and originating from the specified provider.
     */
    public Iterator getContactGroupsForProvider(
                                            ProtocolProviderService provider)
    {
        Iterator encapsulatedGroups = getContactGroups();
        LinkedList protoGroups = new LinkedList();

        while(encapsulatedGroups.hasNext())
        {
            ContactGroup group = (ContactGroup)encapsulatedGroups.next();

            if(group.getProtocolProvider() == provider)
                protoGroups.add(group);
        }
        return protoGroups.iterator();
    }

    /**
     * Returns a meta contact, a child of this group or its subgroups, that
     * has the specified protocol specific contact. If no such meta contact
     * exists, the method would return null.
     *
     * @param protoContact the protocol specific contact whos meta contact we're
     * looking for.
     * @return the MetaContactImpl that contains the specified protocol specific
     * contact.
     */
    public MetaContactImpl findMetaContactByContact(Contact protoContact)
    {
        //first go through the contacts that are direct children of this method.
        Iterator contactsIter = getChildContacts();

        while(contactsIter.hasNext())
        {
            MetaContactImpl mContact = (MetaContactImpl)contactsIter.next();

            Contact storedProtoContact = mContact.getContact(
                protoContact.getAddress(), protoContact.getProtocolProvider());

            if( storedProtoContact != null)
                return mContact;
        }

        //if we didn't find it here, let's try in the subougroups
        Iterator groupsIter = getSubgroups();

        while( groupsIter.hasNext() )
        {
            MetaContactGroupImpl mGroup = (MetaContactGroupImpl)groupsIter.next();

            MetaContactImpl mContact = mGroup.findMetaContactByContact(
                                                                protoContact);

            if (mContact != null)
                return mContact;
        }

        return null;
    }

    /**
     * Returns a meta contact group, encapsulated by this group or its
     * subgroups, that has the specified protocol specific contact. If no such
     * meta contact group exists, the method would return null.
     *
     * @param protoContactGroup the protocol specific contact group whose meta
     * contact group we're looking for.
     * @return the MetaContactImpl that contains the specified protocol specific
     * contact.
     */
    public MetaContactGroupImpl findMetaContactGroupByContactGroup(
                                                ContactGroup protoContactGroup)
    {
        //first check here, in this meta group
        if(protoGroups.contains(protoContactGroup))
            return this;


        //if we didn't find it here, let's try in the subougroups
        Iterator groupsIter = getSubgroups();

        while( groupsIter.hasNext() )
        {
            MetaContactGroupImpl mGroup = (MetaContactGroupImpl)groupsIter.next();

            MetaContactGroupImpl foundMetaContactGroup = mGroup
                    .findMetaContactGroupByContactGroup( protoContactGroup );

            if (foundMetaContactGroup != null)
                return foundMetaContactGroup;
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
        int i = 0;

        synchronized (childContacts)
        {
            Iterator childrenIter = childContacts.iterator();

            while (childrenIter.hasNext())
            {
                MetaContact result = (MetaContact) childrenIter.next();
                if (i++ == index)
                    return result;
            }
        }
        //if we got here then index was out of the bounds
        throw new IndexOutOfBoundsException(i
            + " is larger than size()="
            + childContacts.size());
    }

    /**
     * Adds the specified <tt>metaContact</tt> to ths local list of child
     * contacts.
     * @param metaContact the <tt>MetaContact</tt> to add in the local vector.
     */
    void addMetaContact(MetaContactImpl metaContact)
    {
        synchronized (childContacts)
        {
            //set this group as a callback in the meta contact
            metaContact.setParentGroup(this);

            this.childContacts.add(metaContact);
        }
    }

    /**
     * Adds the <tt>metaContact</tt> to the local list of child
     * contacts without setting its parrent contact and without any
     * synchronization. This method is meant for use _PRIMARILY_ by the
     * <tt>MetaContact</tt> itself upon chenge in its encapsulated protocol
     * specific contacts.
     *
     * @param metaContact the <tt>MetaContact</tt> to add in the local vector.
     */
    void lightAddMetaContact(MetaContactImpl metaContact)
    {
        this.childContacts.add(metaContact);
    }

    /**
      * Removes the <tt>metaContact</tt> from the local list of child
      * contacts without unsetting its parrent contact and without any
      * synchronization. This method is meant for use _PRIMARILY_ by the
      * <tt>MetaContact</tt> itself upon chenge in its encapsulated protocol
      * specific contacts.
      *
      * @param metaContact the <tt>MetaContact</tt> to remove from the local
      * vector.
      */
    void lightRemoveMetaContact(MetaContactImpl metaContact)
    {
        this.childContacts.remove(metaContact);
    }


    /**
     * Removes the specified <tt>metaContact</tt> from the local list of
     * contacts.
     * @param metaContact the <tt>MetaContact</tt>
     */
    void removeMetaContact(MetaContactImpl metaContact)
    {
        synchronized (childContacts)
        {
            metaContact.unsetParentGroup(this);
            this.childContacts.remove(metaContact);
        }
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
     * Returns true if and only if <tt>contact</tt> is a direct child of this
     * group.
     * @param contact the <tt>MetaContact</tt> whose relation to this group
     * we'd like to determine.
     * @return <tt>true</tt> if <tt>contact</tt> is a direct child of this group
     * and <tt>false</tt> otherwise.
     */
    public boolean contains(MetaContact contact)
    {
        synchronized (childContacts)
        {
            return this.childContacts.contains(contact);
        }
    }

    /**
     * Returns true if and only if <tt>group</tt> is a direct subgroup of this
     * <tt>MetaContactGroup</tt>.
     * @param group the <tt>MetaContactGroup</tt> whose relation to this group
     * we'd like to determine.
     * @return <tt>true</tt> if <tt>group</tt> is a direct child of this
     * <tt>MetaContactGroup</tt> and <tt>false</tt> otherwise.
     */
    public boolean contains(MetaContactGroup group)
    {
        return this.subgroups.contains(group);
    }


    /**
     * Returns an <tt>java.util.Iterator</tt> over the sub groups that this
     * <tt>MetaContactGroup</tt> contains.
     * <p>
     * In order to prevent problems with concurrency, the <tt>Iterator</tt>
     * returned by this method is not over the actual list of groups but over a
     * copy of that list.
     * <p>
     * @return a <tt>java.util.Iterator</tt> containing all subgroups.
     */
    public Iterator getSubgroups()
    {
        return new LinkedList( subgroups ).iterator();
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
     * Sets the name of this group.
     * @param newGroupName a String containing the new name of this group.
     */
    void setGroupName(String newGroupName)
    {
        this.groupName = newGroupName;
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
     * Addes the specified group to the list of protocol specific groups
     * that we're encapsulating in this meta contact group.
     * @param protoGroup the root to add to the groups merged in this meta contact
     * group.
     */
    void addProtoGroup( ContactGroup protoGroup)
    {
        protoGroups.add(protoGroup);
    }

    /**
     * Removes the specified group from the list of protocol specific groups
     * that we're encapsulating in this meta contact group.
     * @param protoGroup the group to remove from the groups merged in this meta
     * contact group.
     */
    void removeProtoGroup( ContactGroup protoGroup)
    {
        protoGroups.remove(protoGroup);
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
