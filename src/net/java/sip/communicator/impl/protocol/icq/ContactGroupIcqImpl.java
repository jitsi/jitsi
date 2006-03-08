/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.kano.joustsim.oscar.oscar.service.ssi.*;
import net.kano.joustsim.*;

/**
 * The ICQ implementation of the ContactGroup interface. Intances of this class
 * (contrary to <tt>RootContactGroupIcqImpl</tt>) may only contain buddies
 * and cannot have sub groups. Note that instances of this class only use the
 * corresponding joust sim source group for reading their names and only
 * initially fill their <tt>buddies</tt> <tt>java.util.List</tt> with
 * the ContactIcqImpl objects corresponding to those contained in the source
 * group at the moment it is being created. They would, however, never try to
 * sync or update their contents ulteriorly. This would have to be done through
 * the addContact()/removeContact() methods.
 *
 * @author Emil Ivov
 */
public class ContactGroupIcqImpl
    extends AbstractContactGroupIcqImpl
{
    private List buddies = new LinkedList();

    /**
     * The JoustSIM Group corresponding to this contact group.
     */
    private MutableGroup joustSimSourceGroup = null;

    /**
     * a list that would always remain empty. We only use it so that we're able
     * to extract empty iterators
     */
    private List dummyGroupsList = new LinkedList();

    /**
     * A variable that we use as a means of detecting changes in the name
     * of this group.
     */
    private String nameCopy = null;

    private ServerStoredContactListIcqImpl ssclCallback = null;

    /**
     * Creates an ICQ group using the specified <tt>joustSimGroup</tt> as
     * a source. The newly created group will always return the name of the
     * underlying joustSimGroup and would thus automatically adapt to changes.
     * It would, however, not receive or try to poll for modifications of the
     * buddies it contains and would therefore have to be updated manually by
     * ServerStoredContactListImpl.
     * <p>
     * Note that we MUST NOT use the list of buddies obtained through the
     * getBuddiesCopy() of the joustSimGroup arg as we'd later need to be able
     * to directly compare ( == ) instances of buddies we've stored and others
     * that are returned by the framework.
     * <p>
     * @param joustSimGroup the JoustSIM Group correspoinding to the group
     * @param groupMembers the group members that we should add to the group.
     * @param ssclCallback a callback to the server stored contact list
     *
     * we're creating.
     */
    ContactGroupIcqImpl(MutableGroup joustSimGroup,
                        List groupMembers,
                        ServerStoredContactListIcqImpl ssclCallback)
    {
        this.joustSimSourceGroup = joustSimGroup;

        this.ssclCallback = ssclCallback;

        //store a copy of the name now so that we can detect changes in the
        //name of the underlying joustSimSourceGroup
        initNameCopy();

        //do not use the buddies in the joustSimGroup since we want to keep
        //their real addresses and we can only get a list of copies from the
        //group itself.

        for (int i = 0; i < groupMembers.size(); i++)
        {
            addContact( new ContactIcqImpl((Buddy)groupMembers.get(i),
                                           ssclCallback) );
        }

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
     * Adds the specified contact at the specified position.
     * @param contact the new contact to add to this group
     * @param index the position where the new contact should be added.
     */
    void addContact(int index, ContactIcqImpl contact)
    {
        buddies.add(index, contact);
    }

    /**
     * Adds the specified contact to the end of this group.
     * @param contact the new contact to add to this group
     */
    void addContact(ContactIcqImpl contact)
    {
        addContact(countContacts(), contact);
    }


    /**
     * Removes the specified contact from this contact group
     * @param contact the contact to remove.
     */
    void removeContact(ContactIcqImpl contact)
    {
        removeContact(buddies.indexOf(contact));
    }

    /**
     * Removes the contact with the specified index.
     * @param index the index of the cntact to remove
     */
    void removeContact(int index)
    {
        buddies.remove(index);
    }

    /**
     * Removes all buddies in this group and reinsterts them as specified
     * by the <tt>newOrder</tt> param. Contacts not contained in the
     * newOrder list are left at the end of this group.
     *
     * @param newOrder a list containing all contacts in the order that is
     * to be applied.
     *
     */
    void reorderContacts(List newOrder)
    {
        buddies.removeAll(newOrder);
        buddies.addAll(0, newOrder);
    }

    /**
     * Returns an Iterator over all contacts, member of this
     * <tt>ContactGroup</tt>.
     *
     * @return a java.util.Iterator over all contacts inside this
     *   <tt>ContactGroup</tt>. In case the group doesn't contain any
     * memebers it will return an empty iterator.
     */
    public Iterator contacts()
    {
        return buddies.iterator();
    }

    /**
     * Returns the <tt>Contact</tt> with the specified index.
     *
     * @param index the index of the <tt>Contact</tt> to return.
     * @return the <tt>Contact</tt> with the specified index.
     */
    public Contact getContact(int index)
    {
        return (ContactIcqImpl) buddies.get(index);
    }

    /**
     * Returns the <tt>Contact</tt> with the specified address or
     * identifier.
     * @param id the addres or identifier of the <tt>Contact</tt> we are
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
        return joustSimSourceGroup.getName();
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
    public Iterator subGroups()
    {
        return dummyGroupsList.iterator();
    }

    /**
     * Returns the number of subgroups contained by this group, which is
     * always 0 since sub groups in the icq protocol may only be contained
     * by the root group - <tt>RootContactGroupIcqImpl</tt>.
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
    public int hashCode()
    {
        return getGroupName().hashCode();
    }

    /**
     * Returns the JoustSIM group that this class is encapsulating.
     * @return the JoustSIM group corresponding to this SC group.
     */
    MutableGroup getJoustSimSourceGroup()
    {
        return joustSimSourceGroup;
    }

    /**
     * Indicates whether some other object is "equal to" this group. A group is
     * considered equal to another group if it hase the same sets of (equal)
     * contacts.
     * <p>
     *
     * @param   obj   the reference object with which to compare.
     * @return  <tt>true</tt> if this object is the same as the obj
     *          argument; <tt>false</tt> otherwise.
     */
    public boolean equals(Object obj)
    {
        if(    obj == this )
            return true;

        if (obj == null
            || !(obj instanceof ContactGroupIcqImpl) )
               return false;

        if(!((ContactGroup)obj).getGroupName().equals(getGroupName()))
            return false;

        //since ICQ does not support having two groups with the same name
        // at this point we could bravely state that the groups are the same
        // and not bother to compare buddies. (gotta check that though)
        return true;
    }

    /**
     * Returns the protocol provider that this group belongs to.
     * @return a regerence to the ProtocolProviderService instance that this
     * ContactGroup belongs to.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return this.ssclCallback.getParentProvider();
    }

    /**
     * Returns a string representation of this group, in the form
     * IcqGroup.GroupName[size]{ buddy1.toString(), buddy2.toString(), ...}.
     * @return  a String representation of the object.
     */
    public String toString()
    {
        StringBuffer buff = new StringBuffer("IcqGroup.");
        buff.append(getGroupName());
        buff.append(", childContacts="+countContacts()+":[");

        Iterator contacts = contacts();
        while (contacts.hasNext())
        {
            ContactIcqImpl contact = (ContactIcqImpl) contacts.next();
            buff.append(contact.toString());
            if(contacts.hasNext())
                buff.append(", ");
        }
        return buff.append("]").toString();
    }

    /**
     * Returns the icq contact encapsulating the specified joustSim buddy or null
     * if no such buddy was found.
     *
     * @param joustSimBuddy the buddy whose encapsulating contact we're looking
     * for.
     * @return the <tt>ContactIcqImpl</tt> corresponding to the specified
     * joustSimBuddy or null if no such contact was found.
     */
    ContactIcqImpl findContact(Buddy joustSimBuddy)
    {
        Iterator contacts = contacts();
        while (contacts.hasNext())
        {
            ContactIcqImpl item = (ContactIcqImpl) contacts.next();
            if(item.getJoustSimBuddy() == joustSimBuddy)
                return item;
        }
        return null;
    }

    /**
     * Returns the index of icq contact encapsulating the specified joustSim
     * buddy or -1 if no such buddy was found.
     *
     * @param joustSimBuddy the buddy whose encapsulating contact's index we're
     * looking for.
     * @return the index of the contact corresponding to the specified
     * joustSimBuddy or null if no such contact was found.
     */
    int findContactIndex(Buddy joustSimBuddy)
    {
        Iterator contacts = contacts();
        int i = 0;
        while (contacts.hasNext())
        {
            ContactIcqImpl item = (ContactIcqImpl) contacts.next();
            if(item.getJoustSimBuddy() == joustSimBuddy)
                return i;
            i++;
        }
        return -1;
    }

    /**
     * Returns the icq contact encapsulating with the spcieified screen name or
     * null if no such contact was found.
     *
     * @param screenName the screenName (or icq UIN) for the contact we're
     * looking for.
     * @return the <tt>ContactIcqImpl</tt> corresponding to the specified
     * screnname or null if no such contact existed.
     */
    ContactIcqImpl findContact(String screenName)
    {
        Iterator contacts = contacts();
        while (contacts.hasNext())
        {
            ContactIcqImpl item = (ContactIcqImpl) contacts.next();
            if(item.getJoustSimBuddy().getScreenname()
                .equals(new Screenname(screenName)))
                return item;
        }
        return null;
    }

    /**
     * Sets the name copy field that we use as a means of detecing changes in
     * the group name.
     */
    void initNameCopy()
    {
        this.nameCopy = getGroupName();
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
}
