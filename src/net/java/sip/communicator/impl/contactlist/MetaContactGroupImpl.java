/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.contactlist;

import java.util.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A straightforward implementation of the meta contact group. The group
 * implements a simple algorithm of sorting its children according to their
 * status.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public class MetaContactGroupImpl
    implements MetaContactGroup
{
    /**
     * The <tt>Logger</tt> used by the class <tt>MetaContactGroupImpl</tt> and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(MetaContactGroupImpl.class);

    /**
     * All the subgroups that this group contains.
     */
    private Set<MetaContactGroupImpl> subgroups
                                        = new TreeSet<MetaContactGroupImpl>();

    /**
     * A list containing all child contacts.
     */
    private final Set<MetaContactImpl> childContacts
                                            = new TreeSet<MetaContactImpl>();

    /**
     * A list of the contact groups encapsulated by this MetaContactGroup
     */
    private Vector<ContactGroup> protoGroups = new Vector<ContactGroup>();

    /**
     * An id uniquely identifying the meta contact group in this contact list.
     */
    private String groupUID = null;

    /**
     * The name of the group (fixed for root groups since it won't show).
     */
    private String groupName = null;

    /**
     * We use this copy for returning iterators and searching over the list
     * in order to avoid creating it upon each query. The copy is updated upon
     * each modification
     */
    private List<MetaContact> childContactsOrderedCopy
                                            = new LinkedList<MetaContact>();

    /**
     * We use this copy for returning iterators and searching over the list
     * in order to avoid creating it upon each query. The copy is updated upon
     * each modification
     */
    private List<MetaContactGroup> subgroupsOrderedCopy
                                        = new LinkedList<MetaContactGroup>();

    /**
     * The meta contact group that is currently containing us.
     */
    private MetaContactGroupImpl parentMetaContactGroup = null;

    /**
     * The <tt>MetaContactListService</tt> implementation which manages this
     * <tt>MetaContactGroup</tt> and its associated hierarchy.
     */
    private final MetaContactListServiceImpl mclServiceImpl;

    /**
     * The user-specific key-value associations stored in this instance.
     * <p>
     * Like the Widget implementation of Eclipse SWT, the storage type takes
     * into account that there are likely to be many
     * <code>MetaContactGroupImpl</code> instances and <code>Map</code>s are
     * thus likely to impose increased memory use. While an array may very well
     * perform worse than a <code>Map</code> with respect to search, the
     * mechanism of user-defined key-value associations explicitly states that
     * it is not guaranteed to be optimized for any particular use and only
     * covers the most basic cases and performance-savvy code will likely
     * implement a more optimized solution anyway.
     * </p>
     */
    private Object[] data;

    /**
     * Creates an instance of the root meta contact group.
     *
     * @param mclServiceImpl the <tt>MetaContactListService</tt> implementation
     * which is to use the new <tt>MetaContactGroup</tt> instance as its root
     * @param groupName the name of the group to create
     */
    MetaContactGroupImpl(
            MetaContactListServiceImpl mclServiceImpl,
            String groupName)
    {
        this(mclServiceImpl, groupName, null);
    }

    /**
     * Creates an instance of the root meta contact group assigning it the
     * specified meta contact uid. This constructor MUST NOT be used for nothing
     * purposes else but restoring contacts extracted from the contactlist.xml
     *
     * @param mclServiceImpl the implementation of the
     * <tt>MetaContactListService</tt>, to which this group belongs
     * @param groupName the name of the group to create
     * @param metaUID a UID that has been stored earlier or null when a new
     * UID needs to be created.
     */
    MetaContactGroupImpl(
            MetaContactListServiceImpl mclServiceImpl,
            String groupName,
            String metaUID)
    {
        this.mclServiceImpl = mclServiceImpl;
        this.groupName = groupName;
        this.groupUID
            = (metaUID == null)
                ? String.valueOf( System.currentTimeMillis())
                        + String.valueOf(hashCode())
                : metaUID;
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
        return groupUID;
    }

    /**
     * Returns the MetaContactGroup currently containing this group or null if
     * this is the root group
     * @return a reference to the MetaContactGroup currently containing this
     * meta contact group or null if this is the root group.
     */
    public MetaContactGroup getParentMetaContactGroup()
    {
        return parentMetaContactGroup;
    }

    /**
     * Determines whether or not this group can contain subgroups.
     *
     * @return always <tt>true</tt> since this is the root contact group
     * and in our impl it can only contain groups.
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
     * Returns the number of online <tt>MetaContact</tt>s that this group
     * contains.
     * <p>
     * @return the number of online <tt>MetaContact</tt>s that this group
     * contains.
     */
    public int countOnlineChildContacts()
    {
        int onlineContactsNumber = 0;
        try
        {
            Iterator<MetaContact> itr = getChildContacts();
            while(itr.hasNext())
            {
                Contact contact = itr.next().getDefaultContact();

                if(contact == null)
                    continue;

                if(contact.getPresenceStatus().isOnline())
                {
                    onlineContactsNumber++;
                }
            }
        }
        catch(Exception e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to count online contacts.", e);
        }

        return onlineContactsNumber;
    }

    /**
     * Returns the number of <tt>ContactGroups</tt>s that this group
     * encapsulates
     * <p>
     * @return an int indicating the number of ContactGroups-s that this group
     * encapsulates.
     */
    public int countContactGroups()
    {
        return protoGroups.size();
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
    public Iterator<MetaContact> getChildContacts()
    {
        return childContactsOrderedCopy.iterator();
    }

    /**
     * Returns the contact with the specified identifier
     *
     * @param metaContactID a String identifier obtained through the
     *   <tt>MetaContact.getMetaUID()</tt> method. <p>
     * @return the <tt>MetaContact</tt> with the specified identifier.
     */
    public MetaContact getMetaContact(String metaContactID)
    {
        Iterator<MetaContact> contactsIter = getChildContacts();
        while(contactsIter.hasNext())
        {
            MetaContact contact = contactsIter.next();

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

        Iterator<MetaContact> childrenIter = getChildContacts();

        while (childrenIter.hasNext())
        {
            MetaContact current = childrenIter.next();

            if (current == metaContact)
            {
                return i;
            }
            i++;
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
        int i = 0;

        Iterator<MetaContactGroup> childrenIter = getSubgroups();

        while (childrenIter.hasNext())
        {
            MetaContactGroup current = childrenIter.next();

            if (current == metaContactGroup)
            {
                return i;
            }
            i++;
        }

        //if we got here then metaContactGroup is not in this list
        return -1;
    }

    /**
     * Returns the meta contact encapsulating a contact belonging to the
     * specified <tt>provider</tt> with the specified identifier.
     *
     * @param provider the ProtocolProviderService that the specified
     * <tt>contactID</tt> is pertaining to.
     * @param contactID a String identifier of the protocol specific contact
     * whose container meta contact we're looking for.
     * @return the <tt>MetaContact</tt> with the specified identifier.
     */
    public MetaContact getMetaContact(ProtocolProviderService provider,
                                      String contactID)
    {
        Iterator<MetaContact> contactsIter = getChildContacts();
        while(contactsIter.hasNext())
        {
            MetaContact contact = contactsIter.next();

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
    public MetaContact findMetaContactByMetaUID(String metaUID)
    {
        //first go through the contacts that are direct children of this method.
        Iterator<MetaContact> contactsIter = getChildContacts();

        while(contactsIter.hasNext())
        {
            MetaContact mContact = contactsIter.next();

            if( mContact.getMetaUID().equals(metaUID) )
                return mContact;
        }

        //if we didn't find it here, let's try in the subgroups
        Iterator<MetaContactGroup> groupsIter = getSubgroups();

        while( groupsIter.hasNext() )
        {
            MetaContactGroupImpl mGroup = (MetaContactGroupImpl) groupsIter.next();

            MetaContact mContact = mGroup.findMetaContactByMetaUID(metaUID);

            if (mContact != null)
                return mContact;
        }

        return null;
    }

    /**
     * Returns a meta contact group this group or some of its subgroups,
     * that has the specified metaUID. If no such meta contact group exists,
     * the method would return null.
     *
     * @param metaUID the Meta UID of the contact group we're looking for.
     * @return the MetaContactGroup with the specified UID or null if no such
     * contact exists.
     */
    public MetaContactGroup findMetaContactGroupByMetaUID(String metaUID)
    {
        if (metaUID.equals(groupUID))
            return this;

        //if we didn't find it here, let's try in the subgroups
        Iterator<MetaContactGroup> groupsIter = getSubgroups();

        while( groupsIter.hasNext() )
        {
            MetaContactGroupImpl mGroup
                = (MetaContactGroupImpl) groupsIter.next();

            if (metaUID.equals(mGroup.getMetaUID()))
                return mGroup;
            else
                mGroup.findMetaContactByMetaUID(metaUID);
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
    public Iterator<ContactGroup> getContactGroups()
    {
        return new LinkedList<ContactGroup>( this.protoGroups ).iterator();
    }

    /**
     * Returns a contact group encapsulated by this meta contact group, having
     * the specified groupName and coming from the indicated ownerProvider.
     *
     * @param grpName the name of the contact group who we're looking for.
     * @param ownerProvider a reference to the ProtocolProviderService that
     * the contact we're looking for belongs to.
     * @return a reference to a <tt>ContactGroup</tt>, encapsulated by this
     * MetaContactGroup, carrying the specified name and originating from the
     * specified ownerProvider or null if no such contact group was found.
     */
    public ContactGroup getContactGroup(String grpName,
                                        ProtocolProviderService ownerProvider)
    {
        Iterator<ContactGroup> encapsulatedGroups = getContactGroups();

        while (encapsulatedGroups.hasNext())
        {
            ContactGroup group = encapsulatedGroups.next();

            if (group.getGroupName().equals(grpName)
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
    public Iterator<ContactGroup> getContactGroupsForProvider(
                                            ProtocolProviderService provider)
    {
        Iterator<ContactGroup> encapsulatedGroups = getContactGroups();
        LinkedList<ContactGroup> protGroups = new LinkedList<ContactGroup>();

        while(encapsulatedGroups.hasNext())
        {
            ContactGroup group = encapsulatedGroups.next();

            if(group.getProtocolProvider() == provider)
            {
                protGroups.add(group);
            }
        }
        return protGroups.iterator();
    }

    /**
     * Returns all protocol specific ContactGroups, encapsulated by this
     * MetaContactGroup and coming from the provider matching the
     * <tt>accountID</tt> param. If none of the contacts encapsulated by this
     * MetaContact is originating from the specified account then an empty
     * iterator is returned.
     * <p>
     * Note to implementers:  In order to prevent problems with concurrency, the
     * <tt>Iterator</tt> returned by this method should not be over the actual
     * list of groups but rather over a copy of that list.
     * <p>
     * @param accountID the id of the account whose contact groups we'd like to
     * retrieve.
     * @return an <tt>Iterator</tt> over all contacts encapsulated in this
     * <tt>MetaContact</tt> and originating from the provider with the specified
     * account id.
     */
    public Iterator<ContactGroup> getContactGroupsForAccountID(String accountID)
    {
        Iterator<ContactGroup> encapsulatedGroups = getContactGroups();
        LinkedList<ContactGroup> protGroups = new LinkedList<ContactGroup>();

        while(encapsulatedGroups.hasNext())
        {
            ContactGroup group = encapsulatedGroups.next();

            if(group.getProtocolProvider().getAccountID()
               .getAccountUniqueID().equals(accountID))
            {
                protGroups.add(group);
            }
        }
        return protGroups.iterator();
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
    public MetaContact findMetaContactByContact(Contact protoContact)
    {
        //first go through the contacts that are direct children of this method.
        Iterator<MetaContact> contactsIter = getChildContacts();

        while(contactsIter.hasNext())
        {
            MetaContact mContact = contactsIter.next();

            Contact storedProtoContact = mContact.getContact(
                protoContact.getAddress(), protoContact.getProtocolProvider());

            if( storedProtoContact != null)
                return mContact;
        }

        //if we didn't find it here, let's try in the subgroups
        Iterator<MetaContactGroup> groupsIter = getSubgroups();

        while( groupsIter.hasNext() )
        {
            MetaContactGroupImpl mGroup = (MetaContactGroupImpl) groupsIter.next();

            MetaContact mContact = mGroup.findMetaContactByContact(
                                                                protoContact);

            if (mContact != null)
                return mContact;
        }

        return null;
    }

    /**
     * Returns a meta contact, a child of this group or its subgroups, with
     * address equald to <tt>contactAddress</tt> and a source protocol provider
     * with the matching <tt>accountID</tt>. If no such meta contact exists,
     * the method would return null.
     *
     * @param contactAddress the address of the protocol specific contact whose
     * meta contact we're looking for.
     * @param accountID the ID of the account that the contact we are looking
     * for must belong to.
     *
     * @return the MetaContactImpl that contains the specified protocol specific
     * contact.
     */
    public MetaContact findMetaContactByContact(String contactAddress,
                                                    String accountID)
    {
        //first go through the contacts that are direct children of this method.
        Iterator<MetaContact> contactsIter = getChildContacts();

        while(contactsIter.hasNext())
        {
            MetaContactImpl mContact = (MetaContactImpl) contactsIter.next();

            Contact storedProtoContact = mContact.getContact(
                contactAddress, accountID);

            if( storedProtoContact != null)
                return mContact;
        }

        //if we didn't find it here, let's try in the subgroups
        Iterator<MetaContactGroup> groupsIter = getSubgroups();

        while( groupsIter.hasNext() )
        {
            MetaContactGroupImpl mGroup = (MetaContactGroupImpl) groupsIter.next();

            MetaContact mContact = mGroup.findMetaContactByContact(
                                        contactAddress, accountID);

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


        //if we didn't find it here, let's try in the subgroups
        Iterator<MetaContactGroup> groupsIter = getSubgroups();

        while( groupsIter.hasNext() )
        {
            MetaContactGroupImpl mGroup = (MetaContactGroupImpl) groupsIter.next();

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
        return this.childContactsOrderedCopy.get(index);
    }

    /**
     * Adds the specified <tt>metaContact</tt> to ths local list of child
     * contacts.
     * @param metaContact the <tt>MetaContact</tt> to add in the local vector.
     */
    void addMetaContact(MetaContactImpl metaContact)
    {
        //set this group as a callback in the meta contact
        metaContact.setParentGroup(this);

        lightAddMetaContact(metaContact);
    }

    /**
     * Adds the <tt>metaContact</tt> to the local list of child
     * contacts without setting its parent contact and without any
     * synchronization. This method is meant for use _PRIMARILY_ by the
     * <tt>MetaContact</tt> itself upon change in its encapsulated protocol
     * specific contacts.
     *
     * @param metaContact the <tt>MetaContact</tt> to add in the local vector.
     * @return the index at which the contact was added.
     */
    int lightAddMetaContact(MetaContactImpl metaContact)
    {
        synchronized(childContacts)
        {
            this.childContacts.add(metaContact);
            //no need to synch it's not a disaster if s.o. else reads the old copy.
            childContactsOrderedCopy
                = new LinkedList<MetaContact>(childContacts);
            return childContactsOrderedCopy.indexOf(metaContact);
        }
    }

    /**
      * Removes the <tt>metaContact</tt> from the local list of child
      * contacts without unsetting its parent contact and without any
      * synchronization. This method is meant for use _PRIMARILY_ by the
      * <tt>MetaContact</tt> itself upon change in its encapsulated protocol
      * specific contacts. The method would also regenerate the ordered copy
      * used for generating iterators and performing search operations over
      * the group.
      *
      * @param metaContact the <tt>MetaContact</tt> to remove from the local
      * vector.
      */
    void lightRemoveMetaContact(MetaContactImpl metaContact)
    {
        synchronized(childContacts)
        {
            this.childContacts.remove(metaContact);
            //no need to synch it's not a disaster if s.o. else reads the old copy.
            childContactsOrderedCopy
                            = new LinkedList<MetaContact>(childContacts);
        }
    }


    /**
     * Removes the specified <tt>metaContact</tt> from the local list of
     * contacts.
     * @param metaContact the <tt>MetaContact</tt>
     */
    void removeMetaContact(MetaContactImpl metaContact)
    {
        metaContact.unsetParentGroup(this);
        lightRemoveMetaContact(metaContact);
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
        return subgroupsOrderedCopy.get(index);
    }

    /**
     * Returns the <tt>MetaContactGroup</tt> with the specified name.
     *
     * @param grpName the name of the group to return.
     * @return the <tt>MetaContactGroup</tt> with the specified name or null
     *   if no such group exists.
     */
    public MetaContactGroup getMetaContactSubgroup(String grpName)
    {
        Iterator<MetaContactGroup> groupsIter = getSubgroups();

        while(groupsIter.hasNext())
        {
            MetaContactGroup mcGroup = groupsIter.next();

            if(mcGroup.getGroupName().equals(grpName))
                return mcGroup;
        }

        return null;
    }

    /**
     * Returns the <tt>MetaContactGroup</tt> with the specified groupUID.
     *
     * @param grpUID the uid of the group to return.
     * @return the <tt>MetaContactGroup</tt> with the specified uid or null
     *   if no such group exists.
     */
    public MetaContactGroup getMetaContactSubgroupByUID(String grpUID)
    {
        Iterator<MetaContactGroup> groupsIter = getSubgroups();

        while(groupsIter.hasNext())
        {
            MetaContactGroup mcGroup = groupsIter.next();

            if(mcGroup.getMetaUID().equals(grpUID))
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
    public Iterator<MetaContactGroup> getSubgroups()
    {
        return subgroupsOrderedCopy.iterator();
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
     @Override
    public String toString()
     {

        StringBuffer buff = new StringBuffer(getGroupName());
        buff.append(".subGroups=" + countSubgroups() + ":\n");

        Iterator<MetaContactGroup> subGroups = getSubgroups();
        while (subGroups.hasNext())
        {
            MetaContactGroup group = subGroups.next();
            buff.append(group.getGroupName());
            if (subGroups.hasNext())
                buff.append("\n");
        }

        buff.append("\nProtoGroups="+countContactGroups()+":[");

        Iterator<ContactGroup> contactGroups = getContactGroups();
        while (contactGroups.hasNext())
        {
            ContactGroup contactGroup = contactGroups.next();
            buff.append(contactGroup.getProtocolProvider());
            buff.append(".");
            buff.append(contactGroup.getGroupName());
            if(contactGroups.hasNext())
                buff.append(", ");
        }
        buff.append("]");

        buff.append("\nRootChildContacts="+countChildContacts()+":[");

        Iterator<MetaContact> contacts = getChildContacts();
        while (contacts.hasNext())
        {
            MetaContact contact = contacts.next();
            buff.append(contact.toString());
            if(contacts.hasNext())
                buff.append(", ");
        }
        return buff.append("]").toString();
    }

    /**
     * Adds the specified group to the list of protocol specific groups
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
        if (logger.isTraceEnabled())
            logger.trace("Adding subgroup " + subgroup.getGroupName()
                     + " to" + getGroupName());
        this.subgroups.add((MetaContactGroupImpl)subgroup);
        ((MetaContactGroupImpl)subgroup).parentMetaContactGroup = this;

        this.subgroupsOrderedCopy =
            new LinkedList<MetaContactGroup>(subgroups);
    }

    /**
     * Removes the meta contact group with the specified index.
     * @param index the index of the group to remove.
     * @return the <tt>MetaContactGroup</tt> that has just been removed.
     */
    MetaContactGroupImpl removeSubgroup(int index)
    {
        MetaContactGroupImpl subgroup =
            (MetaContactGroupImpl)subgroupsOrderedCopy.get(index);

        if (subgroups.remove(subgroup))
            subgroup.parentMetaContactGroup = null;

        subgroupsOrderedCopy = new LinkedList<MetaContactGroup>(subgroups);

        return subgroup;
    }

    /**
     * Removes the specified group from the list of groups in this list.
     * @param group the <tt>MetaContactGroup</tt> to remove.
     * @return true if the group has been successfully removed and false
     * otherwise.
     */
    boolean removeSubgroup(MetaContactGroup group)
    {
        if(subgroups.contains(group))
        {
            removeSubgroup(subgroupsOrderedCopy.indexOf(group));
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Returns the implementation of the <tt>MetaContactListService</tt>, to
     * which this group belongs.
     * @return the implementation of the <tt>MetaContactListService</tt>
     */
    final MetaContactListServiceImpl getMclServiceImpl()
    {
        return mclServiceImpl;
    }

    /**
     * Implements {@link MetaContactGroup#getData(Object)}.
     * @return the data value corresponding to the given key
     */
    public Object getData(Object key)
    {
        if (key == null)
            throw new NullPointerException("key");

        int index = dataIndexOf(key);

        return (index == -1) ? null : data[index + 1];
    }

    /**
     * Implements {@link MetaContactGroup#setData(Object, Object)}.
     * @param key the of the data
     * @param value the value of the data
     */
    public void setData(Object key, Object value)
    {
        if (key == null)
            throw new NullPointerException("key");

        int index = dataIndexOf(key);

        if (index == -1)
        {

            /*
             * If value is null, remove the association with key (or just don't
             * add it).
             */
            if (data == null)
            {
                if (value != null)
                    data = new Object[] { key, value };
            }
            else if (value == null)
            {
                int length = data.length - 2;

                if (length > 0)
                {
                    Object[] newData = new Object[length];

                    System.arraycopy(data, 0, newData, 0, index);
                    System.arraycopy(
                        data, index + 2, newData, index, length - index);
                    data = newData;
                }
                else
                    data = null;
            }
            else
            {
                int length = data.length;
                Object[] newData = new Object[length + 2];

                System.arraycopy(data, 0, newData, 0, length);
                data = newData;
                data[length++] = key;
                data[length++] = value;
            }
        }
        else
            data[index + 1] = value;
    }

    /**
     * Determines whether or not this meta group contains only groups that are
     * being stored by a server.
     *
     * @return true if the meta group is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        Iterator<ContactGroup> contactGroupsIter = getContactGroups();

        while (contactGroupsIter.hasNext())
        {
            ContactGroup contactGroup = contactGroupsIter.next();
            if (contactGroup.isPersistent())
                return true;
        }

        // this is new and empty group, we can store it as user want this
        if(countContactGroups() == 0)
            return true;
        else
            return false;
    }

    /**
     * Determines the index in <code>#data</code> of a specific key.
     *
     * @param key
     *            the key to retrieve the index in <code>#data</code> of
     * @return the index in <code>#data</code> of the specified <code>key</code>
     *         if it is contained; <tt>-1</tt> if <code>key</code> is not
     *         contained in <code>#data</code>
     */
    private int dataIndexOf(Object key)
    {
        if (data != null)
            for (int index = 0; index < data.length; index += 2)
                if (key.equals(data[index]))
                    return index;
        return -1;
    }

    /**
     * Compares this meta contact group with the specified object for order.
     * Returns a negative integer, zero, or a positive integer as this
     * meta contact group is less than, equal to, or greater than the specified
     * object.
     * <p>
     * The result of this method is calculated the following way:
     * <p>
     * + getGroupName().compareTo(o.getGroupName()) * 10 000
     * + getMetaUID().compareTo(o.getMetaUID())<br>
     * <p>
     * Or in other words ordering of meta groups would be first done by
     * display name, and finally (in order to avoid
     * equalities) be the fairly random meta contact group metaUID.
     * <p>
     * @param   target the <code>MetaContactGroup</code> to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *      is less than, equal to, or greater than the specified object.
     */
    public int compareTo(MetaContactGroup target)
    {
        return getGroupName().compareToIgnoreCase(target.getGroupName())
            * 10000
            + getMetaUID().compareTo(target.getMetaUID());
    }
}
