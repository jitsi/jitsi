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
package net.java.sip.communicator.service.contactlist;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>MetaContactGroup</tt>s are used to merge groups (often originating
 * in different protocols).
 * <p>
 * A <tt>MetaContactGroup</tt> may contain contacts and some groups may
 * also have sub-groups as children. To verify whether or not a particular
 * group may contain subgroups, a developer has to call the
 * <tt>canContainSubgroups()</tt> method
 * <p>
 * @author Emil Ivov
 */
public interface MetaContactGroup
    extends Comparable<MetaContactGroup>
{
    /**
     * Returns an iterator over all the protocol specific groups that this
     * contact group represents.
     * <p>
     * Note to implementors:  In order to prevent problems with concurrency, the
     * <tt>Iterator</tt> returned by this method should not be over the actual
     * list of groups but rather over a copy of that list.
     * <p>
     * @return an Iterator over the protocol specific groups that this group
     * represents.
     */
    public Iterator<ContactGroup> getContactGroups();

    /**
     * Returns all protocol specific ContactGroups, encapsulated by this
     * MetaContactGroup and coming from the indicated ProtocolProviderService.
     * If none of the contacts encapsulated by this MetaContact is originating
     * from the specified provider then an empty iterator is returned.
     * <p>
     * Note to implementors:  In order to prevent problems with concurrency, the
     * <tt>Iterator</tt> returned by this method should not be over the actual
     * list of groups but rather over a copy of that list.
     * <p>
     * @param provider a reference to the <tt>ProtocolProviderService</tt>
     * whose ContactGroups we'd like to get.
     * @return an <tt>Iterator</tt> over all contacts encapsulated in this
     * <tt>MetaContact</tt> and originating from the specified provider.
     */
    public Iterator<ContactGroup> getContactGroupsForProvider(
                                            ProtocolProviderService provider);

    /**
     * Returns all protocol specific ContactGroups, encapsulated by this
     * MetaContactGroup and coming from the provider matching the
     * <tt>accountID</tt> param. If none of the contacts encapsulated by this
     * MetaContact is originating from the specified account then an empty
     * iterator is returned.
     * <p>
     * Note to implementors:  In order to prevent problems with concurrency, the
     * <tt>Iterator</tt> returned by this method should not be over the actual
     * list of groups but rather over a copy of that list.
     * <p>
     * @param accountID the id of the account whose contact groups we'd like to
     * retrieve.
     * @return an <tt>Iterator</tt> over all contacts encapsulated in this
     * <tt>MetaContact</tt> and originating from the provider with the specified
     * account id.
     */
    public Iterator<ContactGroup> getContactGroupsForAccountID(String accountID);


    /**
     * Returns true if and only if <tt>contact</tt> is a direct child of this
     * group.
     * @param contact the <tt>MetaContact</tt> whose relation to this group
     * we'd like to determine.
     * @return <tt>true</tt> if <tt>contact</tt> is a direct child of this group
     * and <tt>false</tt> otherwise.
     */
    public boolean contains(MetaContact contact);

    /**
     * Returns true if and only if <tt>group</tt> is a direct subgroup of this
     * <tt>MetaContactGroup</tt>.
     * @param group the <tt>MetaContactGroup</tt> whose relation to this group
     * we'd like to determine.
     * @return <tt>true</tt> if <tt>group</tt> is a direct child of this
     * <tt>MetaContactGroup</tt> and <tt>false</tt> otherwise.
     */
    public boolean contains(MetaContactGroup group);

    /**
     * Returns a contact group encapsulated by this meta contact group, having
     * the specified groupName and coming from the indicated ownerProvider.
     *
     * @param groupName the name of the contact group who we're looking for.
     * @param ownerProvider a reference to the ProtocolProviderService that
     * the contact we're looking for belongs to.
     * @return a reference to a <tt>ContactGroup</tt>, encapsulated by this
     * MetaContactGroup, carrying the specified name and originating from the
     * specified ownerProvider.
     */
    public ContactGroup getContactGroup(String groupName,
                                        ProtocolProviderService ownerProvider);


    /**
     * Returns a <tt>java.util.Iterator</tt> over the <tt>MetaContact</tt>s
     * contained in this <tt>MetaContactGroup</tt>.
     * <p>
     * Note to implementors:  In order to prevent problems with concurrency, the
     * <tt>Iterator</tt> returned by this method should not be over the actual
     * list of contacts but rather over a copy of that list.
     * <p>
     * @return a <tt>java.util.Iterator</tt> over the <tt>MetaContacts</tt> in
     * this group.
     */
    public Iterator<MetaContact> getChildContacts();

    /**
     * Returns the number of <tt>MetaContact</tt>s that this group contains
     * <p>
     * @return an int indicating the number of MetaContact-s that this group
     * contains.
     */
    public int countChildContacts();

    /**
     * Returns the number of online <tt>MetaContact</tt>s that this group
     * contains.
     *
     * @return the number of online <tt>MetaContact</tt>s that this group
     * contains.
     */
    public int countOnlineChildContacts();

    /**
     * Returns the number of <tt>ContactGroups</tt>s that this group
     * encapsulates
     * <p>
     * @return an int indicating the number of ContactGroups-s that this group
     * encapsulates.
     */
    public int countContactGroups();

    /**
     * Returns an <tt>java.util.Iterator</tt> over the sub groups that this
     * <tt>MetaContactGroup</tt> contains. Not all <tt>MetaContactGroup</tt>s
     * can have sub groups. In case there are no subgroups in this
     * <tt>MetaContactGroup</tt>, the method would return an empty list.
     * The <tt>canContainSubgroups()</tt> method allows us to verify whether
     * this is the case with the group at hand.
     * <p>
     * <p>
     * Note to implementors:  In order to prevent problems with concurrency, the
     * <tt>Iterator</tt> returned by this method should not be over the actual
     * list of groups but rather over a copy of that list.
     * <p>
     * @return a <tt>java.util.Iterator</tt> containing all subgroups.
     */
    public Iterator<MetaContactGroup> getSubgroups();

    /**
     * Returns the number of subgroups that this <tt>MetaContactGroup</tt>
     * contains.
     * @return an int indicating the number of subgroups in this group.
     */
    public int countSubgroups();

    /**
     * Determines whether or not this group can contain subgroups. The method
     * should be called befor creating subgroups in order to avoir invalid
     * argument exceptions.
     * <p>
     * @return <tt>true</tt> if this groups can contain subgroups and
     * <tt>false</tt> otherwise.
     */
    public boolean canContainSubgroups();

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
                                      String contactID);

    /**
     * Returns the contact with the specified identifier
     * @param metaUID a String identifier obtained through the
     * <tt>MetaContact.getMetaUID()</tt> method.
     * <p>
     * @return the <tt>MetaContact</tt> with the specified idnetifier.
     */
    public MetaContact getMetaContact(String metaUID);

    /**
     * Returns the index of metaContact in relation to other contacts in this or
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
    public int indexOf(MetaContact metaContact);

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
    public int indexOf(MetaContactGroup metaContactGroup);

    /**
     * Returns the meta contact on the specified index.
     * @param index the index of the meta contact to return.
     * @return the MetaContact with the specified index,
     * <p>
     * @throws java.lang.IndexOutOfBoundsException in case <tt>index</tt> is
     * not a valid index for this group.
     */
    public MetaContact getMetaContact(int index)
        throws IndexOutOfBoundsException;

    /**
     * Returns the name of this group.
     * @return a String containing the name of this group.
     */
    public String getGroupName();

    /**
     * Returns the <tt>MetaContactGroup</tt> with the specified name.
     * @param groupName the name of the group to return.
     * @return the <tt>MetaContactGroup</tt> with the specified name or null
     * if no such group exists.
     */
    public MetaContactGroup getMetaContactSubgroup(String groupName);

    /**
     * Returns the <tt>MetaContactGroup</tt> with the specified index.
     * <p>
     * @param index the index of the group to return.
     * @return the <tt>MetaContactGroup</tt> with the specified index.
     * <p>
     * @throws java.lang.IndexOutOfBoundsException if <tt>index</tt> is not
     * a valid index.
     */
    public MetaContactGroup getMetaContactSubgroup(int index)
        throws IndexOutOfBoundsException;

    /**
     * Returns the MetaContactGroup currently containing this group or null if
     * this is the root group
     * @return a reference to the MetaContactGroup currently containing this
     * meta contact group or null if this is the root group.
     */
    public MetaContactGroup getParentMetaContactGroup();

    /**
     * Returns a String representation of this group and the contacts it
     * contains (may turn out to be a relatively long string).
     * @return a String representing this group and its child contacts.
     */
    public String toString();

    /**
     * Returns a String identifier (the actual contents is left to
     * implementations) that uniquely represents this <tt>MetaContact</tt> in
     * the containing <tt>MetaContactList</tt>
     *
     * @return a String uniquely identifying this meta contact.
     */
    public String getMetaUID();

    /**
     * Gets the user data associated with this instance and a specific key.
     *
     * @param key
     *            the key of the user data associated with this instance to be
     *            retrieved
     * @return an <code>Object</code> which represents the value associated with
     *         this instance and the specified <code>key</code>; <tt>null</tt>
     *         if no association with the specified <code>key</code> exists in
     *         this instance
     */
    public Object getData(Object key);

    /**
     * Sets a user-specific association in this instance in the form of a
     * key-value pair. If the specified <code>key</code> is already associated
     * in this instance with a value, the existing value is overwritten with the
     * specified <code>value</code>.
     * <p>
     * The user-defined association created by this method and stored in this
     * instance is not serialized by this instance and is thus only meant for
     * runtime use.
     * </p>
     * <p>
     * The storage of the user data is implementation-specific and is thus not
     * guaranteed to be optimized for execution time and memory use.
     * </p>
     *
     * @param key
     *            the key to associate in this instance with the specified value
     * @param value
     *            the value to be associated in this instance with the specified
     *            <code>key</code>
     */
    public void setData(Object key, Object value);

    /**
     * Determines whether or not this meta group contains only groups that are
     * being stored by a server.
     *
     * @return true if the meta group is persistent and false otherwise.
     */
    public boolean isPersistent();
}
