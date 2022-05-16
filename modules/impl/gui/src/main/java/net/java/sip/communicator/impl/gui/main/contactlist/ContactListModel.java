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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>ListModel</tt> for <tt>MetaContactListService</tt> in order to
 * display it in <tt>ContactList</tt> as a list instead of a tree.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class ContactListModel
    extends AbstractListModel
{
    private final MetaContactListService contactList;

    private final MetaContactGroup rootGroup;

    private final List<MetaContactGroup> closedGroups
        = new Vector<MetaContactGroup>();

    private boolean showOffline = true;

    /**
     * Initializes a new <tt>ContactListModel</tt> instance which is to
     * implement <tt>ListModel</tt> for a specific
     * <tt>MetaContactListService</tt> in order to display it in
     * <tt>ContactList</tt> as a list instead of a tree.
     *
     * @param contactList the <tt>MetaContactListService</tt> which contains the
     * contact list to be represented as a list by the new instance
     */
    public ContactListModel(MetaContactListService contactList)
    {
        this.contactList = contactList;
        this.rootGroup = this.contactList.getRoot();

        this.initGroupsStatus(rootGroup);
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
        return this.getElementAt(this.rootGroup, -1, index);
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
                Iterator<MetaContact> i = group.getChildContacts();
                while (i.hasNext())
                {
                    MetaContact contact = i.next();

                    if (isContactOnline(contact))
                        size++;
                }

                //count the group itself only if it contains any online contacts
                if(!group.equals(rootGroup) && size > 0)
                    size++;
            }

            Iterator<MetaContactGroup> subgroups = group.getSubgroups();
            while (subgroups.hasNext())
            {
                MetaContactGroup subGroup = subgroups.next();

                size += getContactListSize(subGroup);
            }
        }
        else
        {
            // If offline contacts are shown we just count the closed group;
            if(showOffline)
            {
                //count the closed group
                size++;
            }
            else
            {
                // If offline contacts are not shown we'll count the group
                // only if it contains online contacts.
                if( containsOnlineContacts(group))
                    size++;
            }
        }

        return size;
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
            MetaContactGroup parentGroup
                = this.contactList.findParentMetaContactGroup(contact);

            if (parentGroup != null && !this.isGroupClosed(parentGroup))
            {
                index = this.indexOf(parentGroup)
                        + (parentGroup.indexOf(contact) + 1);
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
            MetaContactGroup parentGroup
                = this.contactList.findParentMetaContactGroup(group);

            if (parentGroup != null && !this.isGroupClosed(parentGroup))
            {
                int indexOfGroupInParentGroup = parentGroup.indexOf(group);

                index = this.indexOf(parentGroup)
                        + countChildContacts(parentGroup)
                        + (indexOfGroupInParentGroup + 1);

                for (int i = 0; i < indexOfGroupInParentGroup; i++)
                {
                    MetaContactGroup siblingGroup
                        = parentGroup.getMetaContactSubgroup(i);

                    index += countContactsAndSubgroups(siblingGroup);
                }
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
                Iterator<MetaContact> i = parentGroup.getChildContacts();
                while (i.hasNext())
                {
                    MetaContact contact = i.next();

                    if (isContactOnline(contact))
                        count++;
                }
            }

            Iterator<MetaContactGroup> subgroups = parentGroup.getSubgroups();
            while (subgroups.hasNext())
            {
                MetaContactGroup subgroup = subgroups.next();

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

                    Iterator<MetaContactGroup> subgroups = group.getSubgroups();
                    while (subgroups.hasNext())
                    {
                        MetaContactGroup subgroup = subgroups.next();

                        if(showOffline || containsOnlineContacts(subgroup))
                            element = getElementAt(subgroup, currentIndex + 1,
                                searchedIndex);

                        if (element != null)
                            break;

                        // if we haven't found the element on this iteration
                        // we update the current index and we continue
                        else if (showOffline || containsOnlineContacts(subgroup))
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
        return element;
    }

    /**
     * Closes the given group by hiding all containing contacts.
     *
     * @param group The group to close.
     */
    public void closeGroup(MetaContactGroup group)
    {
        if (!isGroupClosed(group)
            && !isGroupEmpty(group))
        {
            if (showOffline || containsOnlineContacts(group))
            {
                contentRemoved(
                    indexOf(group.getMetaContact(0)),
                    indexOf(
                        group.getMetaContact(
                            countContactsAndSubgroups(group) - 1)));
            }

            this.closedGroups.add(group);

            ConfigurationUtils.setContactListGroupCollapsed(
                group.getMetaUID(),
                true);
        }
    }

    /**
     * Opens the given group by showing all containing contacts.
     *
     * @param group The group to open.
     */
    public void openGroup(MetaContactGroup group)
    {
        if (isGroupClosed(group))
        {
            this.closedGroups.remove(group);
            contentAdded(this.indexOf(group.getMetaContact(0)), this.indexOf(group
                .getMetaContact(countContactsAndSubgroups(group) - 1)));

            ConfigurationUtils.setContactListGroupCollapsed(
                group.getMetaUID(),
                false);
        }
    }

    /**
     * Checks whether the group is closed.
     *
     * @param group The group to check.
     * @return True if the group is closed, false - otherwise.
     */
    public boolean isGroupClosed(MetaContactGroup group)
    {
        return this.closedGroups.contains(group);
    }

    /**
     * Checks whether the group is closed.
     *
     * @param group The group to check.
     * @return True if the group is closed, false - otherwise.
     */
    public boolean isGroupEmpty(MetaContactGroup group)
    {
        return !(group.countChildContacts() > 0 || group.countSubgroups() > 0);
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
        // If for some reason the default contact is null we return false.
        Contact defaultContact = contact.getDefaultContact();
        if(defaultContact == null)
            return false;

        // Lays on the fact that the default contact is the most connected.
        return
            defaultContact.getPresenceStatus().getStatus()
                >= PresenceStatus.ONLINE_THRESHOLD;
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
            Iterator<MetaContact> i = group.getChildContacts();

            while (i.hasNext() && isContactOnline(i.next()))
                count++;

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
        Iterator<MetaContact> childContacts = group.getChildContacts();
        while (childContacts.hasNext())
        {
            MetaContact contact = childContacts.next();

            if (isContactOnline(contact))
                return true;
        }

        return false;
    }

    private void initGroupsStatus(MetaContactGroup group)
    {
        boolean isClosed = ConfigurationUtils
            .isContactListGroupCollapsed(group.getMetaUID());

        if (isClosed)
        {
            closedGroups.add(group);
        }

        Iterator<MetaContactGroup> subgroups = group.getSubgroups();
        while (subgroups.hasNext())
        {
            MetaContactGroup subgroup = subgroups.next();

            this.initGroupsStatus(subgroup);
        }
    }
}
