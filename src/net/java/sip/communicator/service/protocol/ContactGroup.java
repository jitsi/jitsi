/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * A <code>ContactGroup</code> is a collection of Contacts/Buddies/Subscriptions,
 * stored by a communications service (e.g. AIM/ICQ or Skype)returned by
 * persistent presence operation sets. A group may contain simple members or
 * subgroups. Instances of <code>ContactGroup</code> cannot be directly modified
 * by users of the protocol provider service. In order to add buddies or
 * subgroups to a <code>ContactGroup</code> one needs to do so through the
 * <code>OperationSetPersistentPresence</code> interface.
 *
 * @author Emil Ivov
 */
public interface ContactGroup
{
    /**
     * Returns an iterator over the sub groups that this
     * <code>ContactGroup</code> contains.
     *
     * @return a java.util.Iterator over the <code>ContactGroup</code> children
     * of this group (i.e. subgroups).
     */
    public Iterator subGroups();

    /**
     * Returns the number of subgroups contained by this <code>ContactGroup</code>.
     * @return an int indicating the number of subgroups that this ContactGroup
     * contains.
     */
    public int countSubGroups();

    /**
     * Returns the subgroup with the specified index.
     * @param index the index of the <code>ContactGroup</code> to retrieve.
     * @return the <code>ContactGroup</code> with the specified index.
     */
    public ContactGroup getGroup(int index);

    /**
     * Returns the subgroup with the specified name.
     * @param groupName the name of the <code>ContactGroup</code> to retrieve.
     * @return the <code>ContactGroup</code> with the specified index.
     */
    public ContactGroup getGroup(String groupName);


    /**
     * Returns an Iterator over all contacts, member of this
     * <code>ContactGroup</code>.
     * @return a java.util.Iterator over all contacts inside this
     * <code>ContactGroup</code>
     */
    public Iterator contacts();

    /**
     * Returns the number of <code>Contact</code> members of this
     * <code>ContactGroup</code>
     * @return an int indicating the number of <code>Contact</code>s, members
     * of this <code>ContactGroup</code>.
     */
    public int countContacts();

    /**
     * Returns the <code>Contact</code> with the specified index.
     * @param index the index of the <code>Contact</code> to return.
     * @return the <code>Contact</code> with the specified index.
     */
    public Contact getContact(int index);

    /**
     * Returns the <code>Contact</code> with the specified address or
     * identifier.
     * @param id the addres or identifier of the <code>Contact</code> we are
     * looking for.
     * @return the <code>Contact</code> with the specified id or address.
     */
    public Contact getContact(String id);

    /**
     * Determines whether the group may contain subgroups or not.
     * @return true if the groups may be a parent of other
     * <code>ContactGroup</code>s and false otherwise.
     */
    public boolean canContainSubgroups();

    /**
     * Returns the name of this group.
     * @return a String containing the name of this group.
     */
    public String getGroupName();
}
