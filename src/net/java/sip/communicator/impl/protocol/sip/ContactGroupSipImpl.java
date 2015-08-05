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
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import javax.xml.namespace.*;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.resourcelists.*;
import net.java.sip.communicator.service.protocol.*;

import org.w3c.dom.*;

/**
 * A simple, straightforward implementation of a SIP ContactGroup. Since
 * the SIP protocol is not a real one, we simply store all group details
 * in class fields. You should know that when implementing a real protocol,
 * the contact group implementation would rather encapsulate group objects from
 * the protocol stack and group property values should be returned by consulting
 * the encapsulated object.
 *
 * @author Emil Ivov
 * @author Grigorii Balutsel
 */
public class ContactGroupSipImpl
    implements ContactGroup
{

    /**
     * The list of this group's members.
     */
    private Vector<Contact> contacts = new Vector<Contact>();

    /**
     * The list of sub groups belonging to this group.
     */
    private Vector<ContactGroup> subGroups = new Vector<ContactGroup>();

    /**
     * The group that this group belongs to (or null if this is the root group).
     */
    private ContactGroupSipImpl parentGroup = null;

    /**
     * Determines whether this group is really in the contact list or whether
     * it is here only temporarily and will be gone next time we restart.
     */
    private boolean isPersistent = true;

    /**
     * The protocol provider that created us.
     */
    private ProtocolProviderServiceSipImpl parentProvider = null;

    /**
     * Determines whether this group has been resolved on the server.
     * Unresolved groups are groups that were available on previous runs and
     * that the meta contact list has stored. During all next runs, when
     * bootstrapping, the meta contact list would create these groups as
     * unresolved. Once a protocol provider implementation confirms that the
     * groups are still on the server, it would issue an event indicating that
     * the groups are now resolved.
     */
    private boolean isResolved = true;

    /**
     * An id uniquely identifying the group. For many protocols this could be
     * the group name itself.
     */
    private String uid = null;
    private static final String UID_SUFFIX = ".uid";

    /**
     * The XCAP equivalent of SIP contact group.
     */
    private final ListType list;

    /**
     * Creates a ContactGroupSipImpl with the specified name.
     *
     * @param groupName the name of the group.
     * @param parentProvider the protocol provider that created this group.
     */
    public ContactGroupSipImpl(
                    String groupName,
                    ProtocolProviderServiceSipImpl parentProvider)
    {
        this.list = new ListType();
        this.list.setName(groupName);
        this.uid = list.getName() + UID_SUFFIX;
        this.parentProvider = parentProvider;
    }

    /**
     * Gets the list.
     *
     * @return the list.
     */
    ListType getList()
    {
        return list;
    }

    /**
     * Sets the list custom attributes.
     *
     * @param otherAttributes the custom attributes.
     */
    void setOtherAttributes(Map<QName, String> otherAttributes)
    {
        this.list.setAnyAttributes(otherAttributes);
    }

    /**
     * Sets the list custom elements.
     *
     * @param any the custom elemets.
     */
    void setAny(List<Element> any)
    {
        this.list.setAny(any);
    }

    /**
     * Sets the list name.
     *
     * @param newName the name.
     */
    void setName(String newName)
    {
        this.list.setName(newName);
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
    public Iterator<Contact> contacts()
    {
        return contacts.iterator();
    }

    /**
     * Adds the specified contact to this group.
     * @param contactToAdd the ContactSipImpl to add to this group.
     */
    public void addContact(ContactSipImpl contactToAdd)
    {
        this.contacts.add(contactToAdd);
        contactToAdd.setParentGroup(this);
        if(contactToAdd.isPersistent())
        {
            this.list.getEntries().add(contactToAdd.getEntry());
        }
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
     * Sets the group that is the new parent of this group
     * @param parent ContactGroupSipImpl
     */
    void setParentGroup(ContactGroupSipImpl parent)
    {
        this.parentGroup = parent;
    }

    /**
     * Returns the contact group that currently contains this group or null if
     * this is the root contact group.
     * @return the contact group that currently contains this group or null if
     * this is the root contact group.
     */
    public ContactGroup getParentContactGroup()
    {
        return this.parentGroup;
    }

    /**
     * Adds the specified contact group to the contained by this group.
     * @param subgroup the ContactGroupSipImpl to add as a subgroup to this group.
     */
    public void addSubgroup(ContactGroupSipImpl subgroup)
    {
        this.subGroups.add(subgroup);
        subgroup.setParentGroup(this);
        if(subgroup.isPersistent())
        {
            this.list.getLists().add(subgroup.getList());
        }
    }

    /**
     * Removes the specified contact group from the this group's subgroups.
     * @param subgroup the ContactGroupSipImpl subgroup to remove.
     */
    public void removeSubGroup(ContactGroupSipImpl subgroup)
    {
        this.subGroups.remove(subgroup);
        subgroup.setParentGroup(null);
        if(subgroup.isPersistent())
        {
            this.list.getLists().remove(subgroup.getList());
        }
    }

    /**
     * Returns the group that is parent of the specified sipGroup or null
     * if no parent was found.
     * @param sipGroup the group whose parent we're looking for.
     * @return the ContactGroupSipImpl instance that sipGroup
     * belongs to or null if no parent was found.
     */
    public ContactGroupSipImpl findGroupParent(
                                    ContactGroupSipImpl sipGroup)
    {
        if ( subGroups.contains(sipGroup) )
            return this;

        Iterator<ContactGroup> subGroupsIter = subgroups();
        while (subGroupsIter.hasNext())
        {
            ContactGroupSipImpl subgroup
                = (ContactGroupSipImpl) subGroupsIter.next();

            ContactGroupSipImpl parent
                = subgroup.findGroupParent(sipGroup);

            if(parent != null)
                return parent;
        }
        return null;
    }

    /**
     * Returns the group that is parent of the specified sipContact or
     * null if no parent was found.
     *
     * @param sipContact the contact whose parent we're looking for.
     * @return the ContactGroupSipImpl instance that sipContact
     * belongs to or <tt>null</tt> if no parent was found.
     */
    public ContactGroupSipImpl findContactParent(
                                        ContactSipImpl sipContact)
    {
        if ( contacts.contains(sipContact) )
        {
            return this;
        }

        Iterator<ContactGroup> subGroupsIter = subgroups();
        while (subGroupsIter.hasNext())
        {
            ContactGroupSipImpl subgroup
                = (ContactGroupSipImpl) subGroupsIter.next();

            ContactGroupSipImpl parent
                = subgroup.findContactParent(sipContact);

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
        Iterator<Contact> contactsIter = contacts();
        while (contactsIter.hasNext())
        {
            ContactSipImpl contact = (ContactSipImpl) contactsIter.next();
            if (contact.getUri().equals(id) || contact.getAddress().equals(id))
            {
                return contact;
            }
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
        return subGroups.get(index);
    }

    /**
     * Returns the subgroup with the specified name.
     *
     * @param groupName the name of the <tt>ContactGroup</tt> to retrieve.
     * @return the <tt>ContactGroup</tt> with the specified index.
     */
    public ContactGroup getGroup(String groupName)
    {
        Iterator<ContactGroup> groupsIter = subgroups();
        while (groupsIter.hasNext())
        {
            ContactGroupSipImpl contactGroup
                = (ContactGroupSipImpl) groupsIter.next();
            if (contactGroup.getGroupName().equals(groupName))
            {
                return contactGroup;
            }
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
        return this.list.getName();
    }

    /**
     * Returns an iterator over the sub groups that this
     * <tt>ContactGroup</tt> contains.
     *
     * @return a java.util.Iterator over the <tt>ContactGroup</tt> children
     *   of this group (i.e. subgroups).
     */
    public Iterator<ContactGroup> subgroups()
    {
        return subGroups.iterator();
    }

    /**
     * Removes the specified contact from this group.
     * @param contact the ContactSipImpl to remove from this group
     */
    public void removeContact(ContactSipImpl contact)
    {
        this.contacts.remove(contact);
        if(contact.isPersistent())
        {
            this.list.getEntries().remove(contact.getEntry());
        }
    }

    /**
     * Returns the contact with the specified id or null if no such contact
     * exists.
     * @param id the id of the contact we're looking for.
     * @return ContactSipImpl
     */
    public ContactSipImpl findContactByID(String id)
    {
        //first go through the contacts that are direct children.
        Iterator<Contact> contactsIter = contacts();

        while(contactsIter.hasNext())
        {
            ContactSipImpl mContact = (ContactSipImpl)contactsIter.next();

            if( mContact.getAddress().equals(id) )
                return mContact;
        }

        //if we didn't find it here, let's try in the subgroups
        Iterator<ContactGroup> groupsIter = subgroups();

        while( groupsIter.hasNext() )
        {
            ContactGroupSipImpl mGroup = (ContactGroupSipImpl)groupsIter.next();

            ContactSipImpl mContact = mGroup.findContactByID(id);

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
     @Override
    public String toString()
     {

        StringBuffer buff = new StringBuffer(getGroupName());
        buff.append(".subGroups=" + countSubgroups() + ":\n");

        Iterator<ContactGroup> subGroups = subgroups();
        while (subGroups.hasNext())
        {
            ContactGroupSipImpl group = (ContactGroupSipImpl)subGroups.next();
            buff.append(group.toString());
            if (subGroups.hasNext())
                buff.append("\n");
        }

        buff.append("\nChildContacts="+countContacts()+":[");

        Iterator<Contact> contacts = contacts();
        while (contacts.hasNext())
        {
            ContactSipImpl contact = (ContactSipImpl) contacts.next();
            buff.append(contact.toString());
            if(contacts.hasNext())
                buff.append(", ");
        }
        return buff.append("]").toString();
    }

    /**
     * Specifies whether or not this contact group is being stored by the server.
     * Non persistent contact groups are common in the case of simple,
     * non-persistent presence operation sets. They could however also be seen
     * in persistent presence operation sets when for example we have received
     * an event from someone not on our contact list and the contact that we
     * associated with that user is placed in a non persistent group. Non
     * persistent contact groups are volatile even when coming from a persistent
     * presence op. set. They would only exist until the application is closed
     * and will not be there next time it is loaded.
     *
     * @param isPersistent true if the contact group is to be persistent and
     * false otherwise.
     */
    public void setPersistent(boolean isPersistent)
    {
        this.isPersistent = isPersistent;
    }

    /**
     * Determines whether or not this contact group is being stored by the
     * server. Non persistent contact groups exist for the sole purpose of
     * containing non persistent contacts.
     * @return true if the contact group is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return isPersistent;
    }

    /**
     * Returns null as no persistent data is required and the contact address is
     * sufficient for restoring the contact.
     * <p>
     * @return null as no such data is needed.
     */
    public String getPersistentData()
    {
        return null;
    }

    /**
     * Determines whether or not this contact has been resolved against the
     * server. Unresolved contacts are used when initially loading a contact
     * list that has been stored in a local file until the presence operation
     * set has managed to retrieve all the contact list from the server and has
     * properly mapped contacts to their on-line buddies.
     * @return true if the contact has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolved()
    {
        return isResolved;
    }

    /**
     * Makes the group resolved or unresolved.
     *
     * @param resolved  true to make the group resolved; false to
     *                  make it unresolved
     */
    public void setResolved(boolean resolved)
    {
        this.isResolved = resolved;
    }

    /**
     * Returns a <tt>String</tt> that uniquely represnets the group inside
     * the current protocol. The string MUST be persistent (it must not change
     * across connections or runs of the application). In many cases (Jabber,
     * ICQ) the string may match the name of the group as these protocols
     * only allow a single level of contact groups and there is no danger of
     * having the same name twice in the same contact list. Other protocols
     * (no examples come to mind but that doesn't bother me ;) ) may be
     * supporting mutilple levels of grooups so it might be possible for group
     * A and group B to both contain groups named C. In such cases the
     * implementation must find a way to return a unique identifier in this
     * method and this UID should never change for a given group.
     *
     * @return a String representing this group in a unique and persistent
     * way.
     */
    public String getUID()
    {
        return uid;
    }

    /**
     * Ugly but tricky conversion method.
     * @param uid the uid we'd like to get a name from
     * @return the name of the group with the specified <tt>uid</tt>.
     */
    static String createNameFromUID(String uid)
    {
        return uid.substring(0, uid.length() - (UID_SUFFIX.length()));
    }

    /**
     * Indicates whether some other object is "equal to" this one which in terms
     * of contact groups translates to having the equal names and matching
     * subgroups and child contacts. The resolved status of contactgroups and
     * contacts is deliberately ignored so that groups and/or contacts would
     * be assumed equal even if it differs.
     * <p>
     * @param   obj   the reference object with which to compare.
     * @return  <code>true</code> if this contact group has the equal child
     * contacts and subgroups to those of the <code>obj</code> argument.
     */
    @Override
    public boolean equals(Object obj)
    {
        if(obj == null
           || !(obj instanceof ContactGroupSipImpl))
            return false;

        ContactGroupSipImpl sipGroup
            = (ContactGroupSipImpl)obj;

        if(    ! sipGroup.getGroupName().equals(getGroupName())
            || ! sipGroup.getUID().equals(getUID())
            || sipGroup.countContacts() != countContacts()
            || sipGroup.countSubgroups() != countSubgroups())
            return false;

        if(getProtocolProvider() != ((ContactGroup)obj).getProtocolProvider())
            return false;

        //traverse child contacts
        Iterator<Contact> theirContacts = sipGroup.contacts();

        while(theirContacts.hasNext())
        {
            ContactSipImpl theirContact
                = (ContactSipImpl)theirContacts.next();

            ContactSipImpl ourContact
                = (ContactSipImpl)getContact(theirContact.getAddress());

            if(ourContact == null
                || !ourContact.equals(theirContact))
                return false;
        }

        //traverse subgroups
        Iterator<ContactGroup> theirSubgroups = sipGroup.subgroups();

        while(theirSubgroups.hasNext())
        {
            ContactGroupSipImpl theirSubgroup
                = (ContactGroupSipImpl)theirSubgroups.next();

            ContactGroupSipImpl ourSubgroup
                = (ContactGroupSipImpl)getGroup(
                        theirSubgroup.getGroupName());

            if(ourSubgroup == null
                || !ourSubgroup.equals(theirSubgroup))
                return false;
        }

        return true;
    }
}

