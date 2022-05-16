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
    public Iterator<ContactGroup> subgroups();

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
    public Iterator<Contact> contacts();

    /**
     * Returns the number of <tt>Contact</tt> members of this
     * <tt>ContactGroup</tt>
     * @return an int indicating the number of <tt>Contact</tt>s, members
     * of this <tt>ContactGroup</tt>.
     */
    public int countContacts();

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
     * @return a reference to the ProtocolProviderService instance that this
     * ContactGroup belongs to.
     */
    public ProtocolProviderService getProtocolProvider();

    /**
     * Returns the contact group that currently contains this group or null if
     * this is the root contact group.
     * @return the contact group that currently contains this group or null if
     * this is the root contact group.
     */
    public ContactGroup getParentContactGroup();

    /**
     * Determines whether or not this contact group is being stored by the
     * server. Non persistent contact groups exist for the sole purpose of
     * containing non persistent contacts.
     * @return true if the contact group is persistent and false otherwise.
     */
    public boolean isPersistent();

    /**
     * Returns a <tt>String</tt> that uniquely represents the group inside
     * the current protocol. The string MUST be persistent (it must not change
     * across connections or runs of the application). In many cases (Jabber,
     * ICQ) the string may match the name of the group as these protocols
     * only allow a single level of contact groups and there is no danger of
     * having the same name twice in the same contact list. Other protocols
     * (no examples come to mind but that doesn't bother me ;) ) may be
     * supporting multiple levels of groups so it might be possible for group
     * A and group B to both contain groups named C. In such cases the
     * implementation must find a way to return a unique identifier in this
     * method and this UID should never change for a given group.
     *
     * @return a String representing this group in a unique and persistent
     * way.
     */
    public String getUID();

    /**
     * Determines whether or not this group has been resolved against the server.
     * Unresolved groups are used when initially loading a contact list that
     * has been stored in a local file until the presence operation set has
     * managed to retrieve all the contact list from the server and has properly
     * mapped contact groups to their corresponding server stored groups.
     * @return true if the group has been resolved (mapped against a server
     * stored group) and false otherwise.
     */
    public boolean isResolved();

    /**
     * Returns a String that can be used to create a unresolved instance of
     * this group. Unresolved contacts and groups are created through the
     * createUnresolvedContactGroup() method in the persistent presence
     * operation set. The method may also return null if no such data is
     * required and the contact address is sufficient for restoring the contact
     * group.
     * <p>
     * @return A <tt>String</tt> that could be used to create a unresolved
     * instance of this contact group during a next run of the application,
     * before establishing network connectivity or null if no such data is
     * required and a UID would be sufficient.
     */
    public String getPersistentData();
}
