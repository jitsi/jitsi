/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * A <tt>ContactGroup</tt> is a collection of Contacts/Buddies/Subscriptions,
 * stored by a communications service (e.g. AIM/ICQ or Skype)returned by
 * persistent presence operation sets. A group may contain simple members or
 * subgroups. Instances of <tt>ContactGroup</tt> cannot be directly modified
 * by users of the protocol provider service. In order to add buddies or
 * subgroups to a <tt>ContactGroup</tt> one needs to do so through the
 * <tt>OperationSetPersistentPresence</tt> interface.
 *
 * @author Emil Ivov
 */
public interface ContactGroup
{
    /**
     * Returns an iterator over the sub groups that this
     * <tt>ContactGroup</tt> contains.
     *
     * @return a java.util.Iterator over the <tt>ContactGroup</tt> children
     * of this group (i.e. subgroups).
     */
    public Iterator subGroups();

    /**
     * Returns the number of subgroups contained by this <tt>ContactGroup</tt>.
     * @return an int indicating the number of subgroups that this ContactGroup
     * contains.
     */
    public int countSubgroups();

    /**
     * Returns the subgroup with the specified index.
     * @param index the index of the <tt>ContactGroup</tt> to retrieve.
     * @return the <tt>ContactGroup</tt> with the specified index.
     */
    public ContactGroup getGroup(int index);

    /**
     * Returns the subgroup with the specified name.
     * @param groupName the name of the <tt>ContactGroup</tt> to retrieve.
     * @return the <tt>ContactGroup</tt> with the specified index.
     */
    public ContactGroup getGroup(String groupName);


    /**
     * Returns an Iterator over all contacts, member of this
     * <tt>ContactGroup</tt>.
     * @return a java.util.Iterator over all contacts inside this
     * <tt>ContactGroup</tt>
     */
    public Iterator contacts();

    /**
     * Returns the number of <tt>Contact</tt> members of this
     * <tt>ContactGroup</tt>
     * @return an int indicating the number of <tt>Contact</tt>s, members
     * of this <tt>ContactGroup</tt>.
     */
    public int countContacts();

    /**
     * Returns the <tt>Contact</tt> with the specified index.
     * @param index the index of the <tt>Contact</tt> to return.
     * @return the <tt>Contact</tt> with the specified index.
     */
    public Contact getContact(int index);

    /**
     * Returns the <tt>Contact</tt> with the specified address or
     * identifier.
     * @param id the addres or identifier of the <tt>Contact</tt> we are
     * looking for.
     * @return the <tt>Contact</tt> with the specified id or address.
     */
    public Contact getContact(String id);

    /**
     * Determines whether the group may contain subgroups or not.
     * @return true if the groups may be a parent of other
     * <tt>ContactGroup</tt>s and false otherwise.
     */
    public boolean canContainSubgroups();

    /**
     * Returns the name of this group.
     * @return a String containing the name of this group.
     */
    public String getGroupName();

    /**
     * Returns the protocol provider that this group belongs to.
     * @return a regerence to the ProtocolProviderService instance that this
     * ContactGroup belongs to.
     */
    public ProtocolProviderService getProtocolProvider();
}
