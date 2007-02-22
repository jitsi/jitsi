/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The list model of the ContactList. This class use as a data model the
 * <tt>MetaContactListService</tt> itself. The <tt>ContactListModel</tt>
 * plays only a role of a "list" face of the "tree" MetaContactListService
 * structure. It provides an implementation of the AbstractListModel and adds
 * some methods facilitating the access to the contact list. Some more contact
 * list specific methods are added like: getMetaContactStatus,
 * getMetaContactStatusIcon, changeContactStatus, etc.
 *
 * @author Yana Stamcheva
 *
 */
public class ContactListModel
    extends AbstractListModel
{

    private MetaContactListService contactList;

    private MetaContactGroup rootGroup;

    private Vector closedGroups = new Vector();

    private boolean showOffline = true;

    /**
     * Creates a List Model, which gets its data from the given
     * MetaContactListService.
     *
     * @param contactList The MetaContactListService which contains the contact
     *            list.
     */
    public ContactListModel(MetaContactListService contactList)
    {

        this.contactList = contactList;

        this.rootGroup = this.contactList.getRoot();
    }

    /**
     * Informs interested listeners that the content has changed of the cells
     * given by the range from startIndex to endIndex.
     *
     * @param startIndex The start index of the range .
     * @param endIndex The end index of the range.
     */
    public void contentChanged(int startIndex, int endIndex)
    {

        fireContentsChanged(this, startIndex, endIndex);
    }

    /**
     * Informs interested listeners that new cells are added from startIndex to
     * endIndex.
     *
     * @param startIndex The start index of the range .
     * @param endIndex The end index of the range.
     */
    public void contentAdded(final int startIndex, final int endIndex)
    {

        fireIntervalAdded(this, startIndex, endIndex);
    }

    /**
     * Informs interested listeners that a range of cells is removed.
     *
     * @param startIndex The start index of the range.
     * @param endIndex The end index of the range.
     */
    public void contentRemoved(final int startIndex, final int endIndex)
    {

        fireIntervalRemoved(this, startIndex, endIndex);
    }

    /**
     * Returns the size of this list model.
     *
     * @return The size of this list model.
     */
    public int getSize()
    {
        return this.getContactListSize(rootGroup);
    }

    /**
     * Returns the object at the given index.
     *
     * @param index The index.
     * @return The object at the given index.
     */
    public Object getElementAt(int index)
    {
        Object element = this.getElementAt(this.rootGroup, -1, index);

        return element;
    }

    /**
     * Goes through all subgroups and contacts and determines the final size of
     * the contact list.
     *
     * @param group The group which to be measured.
     * @return The size of the contactlist
     */
    private int getContactListSize(MetaContactGroup group)
    {
        int size = 0;

        if (!isGroupClosed(group))
        {
            if (showOffline)
            {
                size = group.countChildContacts();
                //count the group itself
                if(!group.equals(rootGroup))
                    size++;
            }
            else
            {
                Iterator i = group.getChildContacts();
                while (i.hasNext())
                {
                    MetaContact contact = (MetaContact) i.next();

                    if (isContactOnline(contact))
                        size++;
                }

                //count the group itself only if it contains any online contacts
                if(!group.equals(rootGroup) && size > 0)
                    size++;
            }

            Iterator subgroups = group.getSubgroups();

            while (subgroups.hasNext())
            {
                MetaContactGroup subGroup = (MetaContactGroup) subgroups.next();
                size += getContactListSize(subGroup);
            }
        }
        else
        {
            //count the closed group
            size++;
        }

        return size;
    }

    /**
     * Returns the general status of the given MetaContact. Detects the status
     * using the priority status table. The priority is defined on the
     * "availablity" factor and here the most "available" status is returned.
     *
     * @param metaContact The metaContact fot which the status is asked.
     * @return PresenceStatus The most "available" status from all subcontact
     *         statuses.
     */
    public PresenceStatus getMetaContactStatus(MetaContact metaContact)
    {
        PresenceStatus status = null;
        Iterator i = metaContact.getContacts();
        while (i.hasNext())
        {
            Contact protoContact = (Contact) i.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();

            if (status == null)
            {
                status = contactStatus;
            }
            else
            {
                status = (contactStatus.compareTo(status) > 0) ? contactStatus
                    : status;
            }
        }
        return status;
    }

    /**
     * Returns the status icon for this MetaContact.
     *
     * @param contact The metaContact for which the status icon is asked.
     * @return the status icon for this MetaContact.
     */
    public ImageIcon getMetaContactStatusIcon(MetaContact contact)
    {
        return new ImageIcon(Constants.getStatusIcon(this
            .getMetaContactStatus(contact)));
    }

    /**
     * If the given object is instance of MetaContact or MetaContactGroup
     * returns the index of this meta contact or group, otherwiser returns -1.
     *
     * @param o the object, which index we search
     * @return the index of the given object if it founds it, otherwise -1
     */
    public int indexOf(Object o)
    {
        if (o instanceof MetaContact)
        {
            return this.indexOf((MetaContact) o);
        }
        else if (o instanceof MetaContactGroup)
        {
            return this.indexOf((MetaContactGroup) o);
        }
        else
        {
            return -1;
        }
    }

    /**
     * Returns the index of the given MetaContact.
     *
     * @param contact The MetaContact to search for.
     * @return The index of the given MetaContact.
     */
    private int indexOf(MetaContact contact)
    {
        int index = -1;

        if (showOffline || isContactOnline(contact))
        {
            int currentIndex = 0;
            MetaContactGroup parentGroup = this.contactList
                .findParentMetaContactGroup(contact);

            if (parentGroup != null && !this.isGroupClosed(parentGroup))
            {

                currentIndex += this.indexOf(parentGroup);

                currentIndex += parentGroup.indexOf(contact) + 1;

                index = currentIndex;
            }
        }
        return index;
    }

    /**
     * Returns the index of the given MetaContactGroup.
     *
     * @param group The given MetaContactGroup to search for.
     * @return The index of the given MetaContactGroup.
     */
    private int indexOf(MetaContactGroup group)
    {
        int index = -1;

        if (showOffline || containsOnlineContacts(group))
        {
            int currentIndex = 0;
            MetaContactGroup parentGroup = this.contactList
                .findParentMetaContactGroup(group);

            if (parentGroup != null && !this.isGroupClosed(parentGroup))
            {

                currentIndex += this.indexOf(parentGroup);

                currentIndex += countChildContacts(parentGroup);

                currentIndex += parentGroup.indexOf(group) + 1;

                for (int i = 0; i < parentGroup.indexOf(group); i++)
                {
                    MetaContactGroup subGroup = parentGroup
                        .getMetaContactSubgroup(i);

                    currentIndex += countContactsAndSubgroups(subGroup);
                }
                index = currentIndex;
            }
        }
        return index;
    }

    /**
     * Returns the number of all children of the given MetaContactGroup. Counts
     * in depth all subgroups and child contacts.
     *
     * @param parentGroup The parent MetaContactGroup.
     * @return The number of all children of the given MetaContactGroup
     */
    public int countContactsAndSubgroups(MetaContactGroup parentGroup)
    {

        int count = 0;

        if (parentGroup != null && !this.isGroupClosed(parentGroup))
        {
            if (showOffline)
            {
                count = parentGroup.countChildContacts();
            }
            else
            {
                Iterator i = parentGroup.getChildContacts();
                while (i.hasNext())
                {
                    MetaContact contact = (MetaContact) i.next();
                    if (isContactOnline(contact))
                        count++;
                }
            }

            Iterator subgroups = parentGroup.getSubgroups();

            while (subgroups.hasNext())
            {
                MetaContactGroup subgroup = (MetaContactGroup) subgroups.next();

                count += countContactsAndSubgroups(subgroup);
            }
        }
        return count;
    }

    /**
     * Recursively searches the given group in depth for the element at the
     * given index.
     *
     * @param group the group in which we search
     * @param currentIndex the index, where we currently are
     * @param searchedIndex the index to search for
     * @return The element at the given index, if we find it, otherwise null.
     */
    private Object getElementAt(MetaContactGroup group, int currentIndex,
        int searchedIndex)
    {

        Object element = null;
        if (currentIndex == searchedIndex)
        {
            // the current index is the index of the group so if this is the
            // searched index we return the group
            element = group;
        }
        else
        {
            // if the group is closed don't count its children
            if (!isGroupClosed(group))
            {
                int childCount = countChildContacts(group);

                if (searchedIndex <= (currentIndex + childCount))
                {
                    // if the searched index is lower than or equal to
                    // the greater child index in this group then our element is
                    // here
                    MetaContact contact = group.getMetaContact(searchedIndex
                        - currentIndex - 1);

                    if (showOffline || isContactOnline(contact))
                        element = contact;
                }
                else
                {
                    // if we haven't found the contact we search the subgroups
                    currentIndex += childCount;
                    Iterator subgroups = group.getSubgroups();

                    while (subgroups.hasNext())
                    {
                        MetaContactGroup subgroup = (MetaContactGroup) subgroups
                            .next();

                        if(showOffline || containsOnlineContacts(subgroup))
                            element = getElementAt(subgroup, currentIndex + 1,
                                searchedIndex);

                        if (element != null)
                            break;
                        else
                        {
                            // if we haven't found the element on this iteration
                            // we update the current index and we continue
                            if (showOffline || containsOnlineContacts(subgroup))
                            {
                                if (!isGroupClosed(subgroup))
                                    currentIndex += countChildContacts(subgroup) + 1;
                                else
                                    currentIndex++;
                            }
                        }
                    }
                }
            }
        }
        return element;
    }

    /**
     * Closes the given group by hiding all containing contacts.
     *
     * @param group The group to close.
     */
    public void closeGroup(MetaContactGroup group)
    {
        if (!isGroupClosed(group))
        {
            if(showOffline || containsOnlineContacts(group))
            {
                contentRemoved(this.indexOf(group.getMetaContact(0)),
                    this.indexOf(group.getMetaContact(
                        countContactsAndSubgroups(group) - 1)));
            }

            this.closedGroups.add(group);
        }
    }

    /**
     * Opens the given group by showing all containing contacts.
     *
     * @param group The group to open.
     */
    public void openGroup(MetaContactGroup group)
    {
        this.closedGroups.remove(group);
        contentAdded(this.indexOf(group.getMetaContact(0)), this.indexOf(group
            .getMetaContact(countContactsAndSubgroups(group) - 1)));
    }

    /**
     * Checks whether the group is closed.
     *
     * @param group The group to check.
     * @return True if the group is closed, false - otherwise.
     */
    public boolean isGroupClosed(MetaContactGroup group)
    {
        if (this.closedGroups.contains(group))
            return true;
        else
            return false;
    }

    /**
     * Returns true if offline contacts should be shown, false otherwise.
     *
     * @return boolean true if offline contacts should be shown, false
     *         otherwise.
     */
    public boolean isShowOffline()
    {
        return showOffline;
    }

    /**
     * Sets the showOffline variable to indicate whether or not offline contacts
     * should be shown.
     *
     * @param showOffline true if offline contacts should be shown, false
     *            otherwise.
     */
    public void setShowOffline(boolean showOffline)
    {
        this.showOffline = showOffline;
    }

    /**
     * Returns TRUE if the given meta contact is online, FALSE otherwise.
     *
     * @param contact the meta contact
     * @return TRUE if the given meta contact is online, FALSE otherwise
     */
    public boolean isContactOnline(MetaContact contact)
    {
        // Lays on the fact that the default contact is the most connected.
        if (contact.getDefaultContact().getPresenceStatus()
                .getStatus() >= PresenceStatus.ONLINE_THRESHOLD)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Counts group child contacts depending on the showOffline option.
     *
     * @param group the parent group to count for
     * @return child contacts count for the given group
     */
    public int countChildContacts(MetaContactGroup group)
    {
        if (showOffline)
            return group.countChildContacts();
        else
        {
            int count = 0;
            Iterator i = group.getChildContacts();

            while (i.hasNext())
            {
                MetaContact metaContact = (MetaContact) i.next();

                if (isContactOnline(metaContact))
                {
                    count++;
                }
                else
                {
                    break;
                }
            }
            return count;
        }
    }

    /**
     * Checks if the given group contains online contacts.
     *
     * @param group the group to check for online contacts
     * @return TRUE if the given group contains online contacts, FALSE otherwise
     */
    private boolean containsOnlineContacts(MetaContactGroup group)
    {
        Iterator childContacts = group.getChildContacts();
        while (childContacts.hasNext())
        {
            MetaContact contact = (MetaContact) childContacts.next();

            if (isContactOnline(contact))
                return true;
        }

        return false;
    }
}
