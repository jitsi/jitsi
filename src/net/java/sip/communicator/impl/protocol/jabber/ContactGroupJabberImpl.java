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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

import org.jivesoftware.smack.*;

/**
 * The Jabber implementation of the ContactGroup interface. Instances of this class
 * (contrary to <tt>RootContactGroupJabberImpl</tt>) may only contain buddies
 * and cannot have sub groups. Note that instances of this class only use the
 * corresponding smack source group for reading their names and only
 * initially fill their <tt>buddies</tt> <tt>java.util.List</tt> with
 * the ContactJabberImpl objects corresponding to those contained in the source
 * group at the moment it is being created. They would, however, never try to
 * sync or update their contents ulteriorly. This would have to be done through
 * the addContact()/removeContact() methods.
 * The content of buddies is created on creating of the group and when the smack
 * source group is changed.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 */
public class ContactGroupJabberImpl
    extends AbstractContactGroupJabberImpl
{

    /**
     * Maps all JIDs in our roster to the actual contacts so that we could
     * easily search the set of existing contacts. Note that we only store
     * lower case  strings in the left column because JIDs in XMPP are not case
     * sensitive.
     */
    private Map<String, Contact> buddies = new Hashtable<String, Contact>();

    /**
     * Whether or not this contact group has been resolved against
     * the server.
     */
    private boolean isResolved = false;

    /**
     * The Jabber Group id(the name), corresponding to this contact group.
     * Used to resolve the RosterGroup against the rouster.
     */
    private String id = null;

    /**
     * a list that would always remain empty. We only use it so that we're able
     * to extract empty iterators
     */
    private List<ContactGroup> dummyGroupsList = new LinkedList<ContactGroup>();

    /**
     * A variable that we use as a means of detecting changes in the name
     * of this group.
     */
    private String nameCopy = null;

    /**
     * Used when creating unresolved groups, temporally id.
     */
    private String tempId = null;

    /**
     * The contact list handler that creates us.
     */
    private final ServerStoredContactListJabberImpl ssclCallback;

    /**
     * Creates an Jabber group using the specified <tt>RosterGroup</tt> as
     * a source. The newly created group will always return the name of the
     * underlying RosterGroup and would thus automatically adapt to changes.
     * It would, however, not receive or try to poll for modifications of the
     * buddies it contains and would therefore have to be updated manually by
     * ServerStoredContactListImpl update will only be done if source group
     * is changed.

     * @param rosterGroup the Jabber Group corresponding to the group
     * @param groupMembers the group members that we should add to the group.
     * @param ssclCallback a callback to the server stored contact list
     * we're creating.
     * @param isResolved a boolean indicating whether or not the group has been
     * resolved against the server.
     */
    ContactGroupJabberImpl(
                        RosterGroup rosterGroup,
                        Iterator<RosterEntry> groupMembers,
                        ServerStoredContactListJabberImpl ssclCallback,
                        boolean isResolved)
    {
        // rosterGroup can be null when creating volatile contact group
        if(rosterGroup != null)
            this.id = rosterGroup.getName();

        this.isResolved = isResolved;
        this.ssclCallback = ssclCallback;

        // init the name copy if its not volatile
        if(rosterGroup != null)
            this.nameCopy = rosterGroup.getName();

        while (groupMembers.hasNext())
        {
            RosterEntry rEntry = groupMembers.next();

            if(!ServerStoredContactListJabberImpl.isEntryDisplayable(rEntry))
                continue;

            //only add the buddy if it doesn't already exist in some other group
            //this is necessary because XMPP would allow having one and the
            //same buddy in more than one group.
            if(ssclCallback.findContactById(rEntry.getUser()) != null)
            {
                continue;
            }

            addContact(
                new ContactJabberImpl(rEntry, ssclCallback, true, true));
        }
    }

    /**
     * Used when creating unresolved groups.
     * @param id the id of the group.
     * @param ssclCallback the contact list handler that created us.
     */
    ContactGroupJabberImpl(
                        String id,
                        ServerStoredContactListJabberImpl ssclCallback)
    {
        this.tempId = id;
        this.isResolved = false;
        this.ssclCallback = ssclCallback;
    }

    /**
     * Returns the number of <tt>Contact</tt> members of this
     * <tt>ContactGroup</tt>
     *
     * @return an int indicating the number of <tt>Contact</tt>s,
     *   members of this <tt>ContactGroup</tt>.
     */
    public int countContacts()
    {
        return buddies.size();
    }

    /**
     * Returns a reference to the root group which in Jabber is the parent of
     * any other group since the protocol does not support subgroups.
     * @return a reference to the root group.
     */
    public ContactGroup getParentContactGroup()
    {
        return ssclCallback.getRootGroup();
    }

    /**
     * Adds the specified contact to the end of this group.
     * @param contact the new contact to add to this group
     */
    public void addContact(ContactJabberImpl contact)
    {
        buddies.put(contact.getAddress().toLowerCase(), contact);
    }


    /**
     * Removes the specified contact from this contact group
     * @param contact the contact to remove.
     */
    void removeContact(ContactJabberImpl contact)
    {
        buddies.remove(contact.getAddress().toLowerCase());
    }

    /**
     * Returns an Iterator over all contacts, member of this
     * <tt>ContactGroup</tt>.
     *
     * @return a java.util.Iterator over all contacts inside this
     *   <tt>ContactGroup</tt>. In case the group doesn't contain any
     * members it will return an empty iterator.
     */
    public Iterator<Contact> contacts()
    {
        return buddies.values().iterator();
    }

    /**
     * Returns the <tt>Contact</tt> with the specified address or
     * identifier.
     * @param id the address or identifier of the <tt>Contact</tt> we are
     * looking for.
     * @return the <tt>Contact</tt> with the specified id or address.
     */
    public Contact getContact(String id)
    {
        return this.findContact(id);
    }

    /**
     * Returns the name of this group.
     * @return a String containing the name of this group.
     */
    public String getGroupName()
    {
        if(isResolved)
            // now we use the id field to store the rosterGroup name
            // for later retrieval from roster.
            //return rosterGroup.getName();
            return id;
        else
            return tempId;
    }

    /**
     * Determines whether the group may contain subgroups or not.
     *
     * @return always false since only the root group may contain subgroups.
     */
    public boolean canContainSubgroups()
    {
        return false;
    }

    /**
     * Returns the subgroup with the specified index (i.e. always null since
     * this group may not contain subgroups).
     *
     * @param index the index of the <tt>ContactGroup</tt> to retrieve.
     * @return always null
     */
    public ContactGroup getGroup(int index)
    {
        return null;
    }

    /**
     * Returns the subgroup with the specified name.
     * @param groupName the name of the <tt>ContactGroup</tt> to retrieve.
     * @return the <tt>ContactGroup</tt> with the specified index.
     */
    public ContactGroup getGroup(String groupName)
    {
        return null;
    }

    /**
     * Returns an empty iterator. Subgroups may only be present in the root
     * group.
     *
     * @return an empty iterator
     */
    public Iterator<ContactGroup> subgroups()
    {
        return dummyGroupsList.iterator();
    }

    /**
     * Returns the number of subgroups contained by this group, which is
     * always 0 since sub groups in the protocol may only be contained
     * by the root group - <tt>RootContactGroupImpl</tt>.
     * @return a 0 int.
     */
    public int countSubgroups()
    {
        return 0;
    }

    /**
     * Returns a hash code value for the object, which is actually the hashcode
     * value of the groupname.
     *
     * @return  a hash code value for this ContactGroup.
     */
    @Override
    public int hashCode()
    {
        return getGroupName().hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this group.
     *
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this object is the same as the obj
     *          argument; <tt>false</tt> otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        if(    obj == this )
            return true;

        if (obj == null
            || !(obj instanceof ContactGroupJabberImpl) )
               return false;

        if(!((ContactGroup)obj).getGroupName().equals(getGroupName()))
            return false;

        if(getProtocolProvider() != ((ContactGroup)obj).getProtocolProvider())
            return false;

        //since Jabber does not support having two groups with the same name
        // at this point we could bravely state that the groups are the same
        // and not bother to compare buddies. (gotta check that though)
        return true;
    }

    /**
     * Returns the protocol provider that this group belongs to.
     * @return a reference to the ProtocolProviderService instance that this
     * ContactGroup belongs to.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return this.ssclCallback.getParentProvider();
    }

    /**
     * Returns a string representation of this group, in the form
     * JabberGroup.GroupName[size]{ buddy1.toString(), buddy2.toString(), ...}.
     * @return  a String representation of the object.
     */
    @Override
    public String toString()
    {
        StringBuffer buff = new StringBuffer("JabberGroup.");
        buff.append(getGroupName())
            .append(", childContacts=")
            .append(countContacts())
            .append(":[");

        Iterator<Contact> contacts = contacts();
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();
            buff.append(contact.toString());
            if(contacts.hasNext())
                buff.append(", ");
        }
        return buff.append("]").toString();
    }

    /**
     * Returns the contact encapsulating with the spcieified name or
     * null if no such contact was found.
     *
     * @param id the id for the contact we're looking for.
     * @return the <tt>ContactJabberImpl</tt> corresponding to the specified
     * screnname or null if no such contact existed.
     */
    ContactJabberImpl findContact(String id)
    {
        if(id == null)
            return null;
        return (ContactJabberImpl)buddies.get(id.toLowerCase());
    }

    /**
     * Sets the name copy field that we use as a means of detecting changes in
     * the group name.
     * @param newName String
     */
    void setNameCopy(String newName)
    {
        this.nameCopy = newName;
    }

    /**
     * Returns the name of the group as it was at the last call of initNameCopy.
     * @return a String containing a copy of the name of this group as it was
     * last time when we called <tt>initNameCopy</tt>.
     */
    String getNameCopy()
    {
        return this.nameCopy;
    }

    /**
     * Determines whether or not this contact group is being stored by the
     * server. Non persistent contact groups exist for the sole purpose of
     * containing non persistent contacts.
     * @return true if the contact group is persistent and false otherwise.
     */
    public boolean isPersistent()
    {
        return true;
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
     * Determines whether or not this contact group has been resolved against
     * the server. Unresolved group are used when initially loading a contact
     * list that has been stored in a local file until the presence operation
     * set has managed to retrieve all the contact list from the server and has
     * properly mapped contacts and groups to their corresponding on-line
     * buddies.
     * @return true if the contact has been resolved (mapped against a buddy)
     * and false otherwise.
     */
    public boolean isResolved()
    {
        return isResolved;
    }

    /**
     * Resolve this contact group against the specified group
     * @param source the server stored group
     */
    void setResolved(RosterGroup source)
    {
        if(isResolved)
            return;

        this.isResolved = true;

        this.id = source.getName();

        for (RosterEntry item : source.getEntries())
        {
            ContactJabberImpl contact =
                ssclCallback.findContactById(item.getUser());

            // some services automatically adds contacts from an addressbook
            // to our roster and this contacts are with subscription none.
            // if such already exist, remove it. This is typically our
            // own contact
            if(!ServerStoredContactListJabberImpl.isEntryDisplayable(item))
            {
                if(contact != null)
                {
                    removeContact(contact);
                    ssclCallback.fireContactRemoved(this, contact);
                }

                continue;
            }

            if(contact != null)
            {
                contact.setResolved(item);

                ssclCallback.fireContactResolved(this, contact);
            }
            else
            {
                ContactJabberImpl newContact =
                    new ContactJabberImpl(item, ssclCallback, true, true);
                addContact(newContact);

                ssclCallback.fireContactAdded(this, newContact);
            }
        }
    }

    /**
     * Returns a <tt>String</tt> that uniquely represents the group. In this we
     * use the name of the group as an identifier. This may cause problems
     * though, in case the name is changed by some other application between
     * consecutive runs of the sip-communicator.
     *
     * @return a String representing this group in a unique and persistent
     * way.
     */
    public String getUID()
    {
        return getGroupName();
    }

    /**
     * The source group we are encapsulating
     * @return RosterGroup
     */
    RosterGroup getSourceGroup()
    {
        return ssclCallback.getRosterGroup(id);
    }

    /**
     * Change the source group, used when renaming groups.
     *
     * @param newGroup RosterGroup
     */
    void setSourceGroup(RosterGroup newGroup)
    {
        this.id = newGroup.getName();
    }
}
