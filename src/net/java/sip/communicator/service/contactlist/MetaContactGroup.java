/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist;

import java.util.*;

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
{
    /**
     * Returns a <tt>java.util.Iterator</tt> over the <tt>MetaContact</tt>s
     * contained in this <tt>MetaContactGroup</tt>.
     * @return a <tt>java.util.Iterator</tt> over the <tt>MetaContacts</tt> in
     * this group.
     */
    public Iterator getChildContacts();

    /**
     * Returns the number of <tt>MetaContact</tt>s that this group contains
     * <p>
     * @return an int indicating the number of MetaContact-s that this group
     * contains.
     */
    public int countChildContacts();

    /**
     * Returns an <tt>java.util.Iterator</tt> over the sub groups that this
     * <tt>MetaContactGroup</tt> contains. Not all <tt>MetaContactGroup</tt>s
     * can have sub groups. In case there are no subgroups in this
     * <tt>MetaContactGroup</tt>, the method would return an empty list.
     * The <tt>canContainSubgroups()</tt> method allows us to verify whether
     * this is the case with the group at hand.
     * <p>
     * @return a <tt>java.util.Iterator</tt> containing all subgroups.
     */
    public Iterator getSubgroups();

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
     * Returns the contact with the specified identifier
     * @param metaContactID a String identifier obtained through the
     * <tt>MetaContact.getMetaContactID()</tt> method.
     * <p>
     * @return the <tt>MetaContact</tt> with the specified idnetifier.
     */
    public MetaContact getMetaContact(String metaContactID);

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


}
