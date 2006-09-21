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
public class ContactListModel extends AbstractListModel {

    private MetaContactListService contactList;

    private MetaContactGroup rootGroup;

    private Vector closedGroups = new Vector();

    private Vector offlineContacts = new Vector();

    private boolean showOffline = true;

    /**
     * Creates a List Model, which gets its data from 
     * the given MetaContactListService. 
     * 
     * @param contactList The MetaContactListService 
     * which contains the contact list.
     */
    public ContactListModel(MetaContactListService contactList) {

        this.contactList = contactList;

        this.rootGroup = this.contactList.getRoot();
    }

    /**
     * Informs interested listeners that the content has 
     * changed of the cells given by the range from 
     * startIndex to endIndex.
     * 
     * @param startIndex The start index of the range .
     * @param endIndex The end index of the range.
     */
    public void contentChanged(int startIndex, int endIndex) {
        fireContentsChanged(this, startIndex, endIndex);
    }

    /**
     * Informs interested listeners that new cells are 
     * added from startIndex to endIndex. 
     * 
     * @param startIndex The start index of the range .
     * @param endIndex The end index of the range.
     */
    public void contentAdded(final int startIndex, final int endIndex) {
        SwingUtilities.invokeLater(new Thread() {
            public void run() {
                fireIntervalAdded(this, startIndex, endIndex);
            }
        });
    }

    /**
     * Informs interested listeners that a range of cells is removed.
     * 
     * @param startIndex The start index of the range.
     * @param endIndex The end index of the range.
     */
    public void contentRemoved(final int startIndex, final int endIndex) {
        SwingUtilities.invokeLater(new Thread() {
            public void run() {
                fireIntervalRemoved(this, startIndex, endIndex);
            }
        });
    }

    /**
     * Returns the size of this list model.
     * @return The size of this list model.
     */
    public int getSize() {
        return this.getContactListSize(rootGroup);
    }

    /**
     * Returns the object at the given index.
     * @param index The index.
     * @return The object at the given index.
     */
    public Object getElementAt(int index) {
        Object element = this.getElementAt(this.rootGroup, index);

        return element;
    }

    /**
     * Goes through all subgroups and contacts and determines 
     * the final size of the contact list.
     * 
     * @param group The group which to be measured.
     * @return The size of the contactlist
     */
    private int getContactListSize(MetaContactGroup group) {
        int size = 0;

        if (!this.isGroupClosed(group)) {
            if (showOffline) {
                size = group.countChildContacts();
            }
            else {
                Iterator i = group.getChildContacts();
                while (i.hasNext()) {
                    MetaContact contact = (MetaContact) i.next();
                    synchronized (offlineContacts) {
                        if (!offlineContacts.contains(contact))
                            size++;
                    }
                }
            }
            size += group.countSubgroups();

            Iterator subgroups = group.getSubgroups();

            while (subgroups.hasNext()) {
                size += getContactListSize((MetaContactGroup) subgroups.next());
            }
        }
        return size;
    }

    /**
     * Returns the general status of the given MetaContact. Detects the 
     * status using the priority status table. The priority is defined 
     * on the "availablity" factor and here the most "available" status 
     * is returned.
     * 
     * @param metaContact The metaContact fot which the status is asked.
     * @return PresenceStatus The most "available" status 
     * from all subcontact  statuses.
     */
    public PresenceStatus getMetaContactStatus(MetaContact metaContact) {
        PresenceStatus status = null;
        Iterator i = metaContact.getContacts();
        while (i.hasNext()) {
            Contact protoContact = (Contact) i.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();

            if(status == null) {
                status = contactStatus;
            }
            else {
                status
                    = (contactStatus.compareTo(status) > 0)
                    ? contactStatus : status;
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
    public ImageIcon getMetaContactStatusIcon(MetaContact contact) {
        return new ImageIcon(Constants.getStatusIcon(this
                .getMetaContactStatus(contact)));
    }

    /**
     * Returns the index of the given MetaContact.
     *  
     * @param contact The MetaContact to search for.
     * @return The index of the given MetaContact.
     */
    public int indexOf(MetaContact contact) {

        int index = -1;

        if (!offlineContacts.contains(contact)) {
            int currentIndex = 0;
            MetaContactGroup parentGroup = this.contactList
                    .findParentMetaContactGroup(contact);

            if (parentGroup != null && !this.isGroupClosed(parentGroup)) {

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
    public int indexOf(MetaContactGroup group) {

        int index = -1;
        int currentIndex = 0;
        MetaContactGroup parentGroup = this.contactList
                .findParentMetaContactGroup(group);

        if (parentGroup != null && !this.isGroupClosed(parentGroup)) {
          
            currentIndex += this.indexOf(parentGroup);
          
            currentIndex += countChildContacts(parentGroup);
          
            currentIndex += parentGroup.indexOf(group) + 1;
            
            for (int i = 0; i < parentGroup.indexOf(group); i++) {
                MetaContactGroup subGroup = parentGroup
                        .getMetaContactSubgroup(i);
                
                currentIndex += countSubgroupContacts(subGroup);
            }            
            index = currentIndex;
        }
        return index;
    }

    /**
     * Returns the number of all children of the given
     * MetaContactGroup. Counts in depth all subgroups 
     * and child contacts.
     * 
     * @param parentGroup The parent MetaContactGroup.
     * @return The number of all children of the given MetaContactGroup
     */
    private int countSubgroupContacts(MetaContactGroup parentGroup) {
 
        int count = 0;

        if (parentGroup != null && !this.isGroupClosed(parentGroup)) {
            if (showOffline) {
                count = parentGroup.countChildContacts();
            }
            else {                
                Iterator i = parentGroup.getChildContacts();
                while (i.hasNext()) {
                    MetaContact contact = (MetaContact) i.next();
                    if (!offlineContacts.contains(contact))
                        count++;
                }
            }

            Iterator subgroups = parentGroup.getSubgroups();

            while (subgroups.hasNext()) {
                MetaContactGroup subgroup = (MetaContactGroup) subgroups.next();

                count += countSubgroupContacts(subgroup);
            }
        }
        return count;
    }
    
    /**
     * Returns the number of all child contacts of the given
     * MetaContactGroup.
     * 
     * @param parentGroup The parent MetaContactGroup.
     * @return The number of all children of the given MetaContactGroup
     */
    private int countChildContacts(MetaContactGroup parentGroup) {
 
        int count = 0;

        if (parentGroup != null && !this.isGroupClosed(parentGroup)) {
            if (showOffline) {
                count = parentGroup.countChildContacts();
            }
            else {                
                Iterator i = parentGroup.getChildContacts();
                while (i.hasNext()) {
                    MetaContact contact = (MetaContact) i.next();
                    if (!offlineContacts.contains(contact))
                        count++;
                }
            }
        }
        return count;
    }

    /**
     * Recursively searches all groups for the element at the given index. 
     * 
     * @param group The group in which we search.
     * @param searchedIndex The index to search for.
     * @return The element at the given index, if it finds it, otherwise null.
     */
    private Object getElementAt(MetaContactGroup group, int searchedIndex) {
        Object element = null;

        if (!this.isGroupClosed(group)) {
            Iterator contacts = group.getChildContacts();

            while (contacts.hasNext()) {
                MetaContact contact = (MetaContact) contacts.next();                
                if (!offlineContacts.contains(contact)
                        && this.indexOf(contact) == searchedIndex) {                    
                    element = contact;
                    break;
                }
            }

            if (element == null) {
                Iterator subgroups = group.getSubgroups();

                while (subgroups.hasNext()) {
                    MetaContactGroup subgroup = (MetaContactGroup) subgroups
                            .next();
                    
                    if (this.indexOf(subgroup) != searchedIndex) {
                        element = getElementAt(subgroup, searchedIndex);
                        if (element != null) {
                            break;
                        }
                    } else {
                        element = subgroup;
                        break;
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
    public void closeGroup(MetaContactGroup group) {        
        if (countSubgroupContacts(group) > 0) {
            contentRemoved(this.indexOf(group.getMetaContact(0)),
                this.indexOf(group.getMetaContact(
                        countSubgroupContacts(group) - 1)));
            
            this.closedGroups.add(group);
        }
    }

    /**
     * Opens the given group by showing all containing contacts.
     * 
     * @param group The group to open.
     */
    public void openGroup(MetaContactGroup group) {        
        this.closedGroups.remove(group);
        contentAdded(this.indexOf(group.getMetaContact(0)), 
            this.indexOf(group.getMetaContact(
                    countSubgroupContacts(group) - 1)));
    }

    /**
     * Checks whether the group is closed.
     * 
     * @param group The group to check.
     * @return True if the group is closed, false - otherwise.
     */
    public boolean isGroupClosed(MetaContactGroup group) {
        if (this.closedGroups.contains(group))
            return true;
        else
            return false;
    }

    /**
     * Removes all offline contacts from the contact list.
     */
    public void removeOfflineContacts() {
        // Stock offline contacts before adding them in 
        // offlineContacts Vector in order to have 
        // getElementAt and indexOf working as nothing 
        // was changed.
        Vector offlineContactsCopy = new Vector();
        // A copy of the size as it was before removing an offline contact.
        int size = this.getSize();
        for (int i = 0; i < size; i++) {
            Object element = this.getElementAt(i);

            if (element instanceof MetaContact) {
                MetaContact contactNode = (MetaContact) element;

                if (!getMetaContactStatus(contactNode).isOnline()) {
                    int index = indexOf(contactNode);
                    this.contentRemoved(index, index);
                                        
                    offlineContactsCopy.add(contactNode);
                }
            }
        }
        //Remove also offline contacts in closed groups.
        for (int j = 0; j < closedGroups.size(); j++) {
            MetaContactGroup closedGroup 
                = (MetaContactGroup) closedGroups.get(j);
            
            removeClosedOfflineContacts(closedGroup, offlineContactsCopy);
        }
        
        this.offlineContacts = offlineContactsCopy;
    }
    
    /**
     * Recursively removes offline contacts contained in closed groups.
     *  
     * @param group A MetaContactGroup.
     * @param offlineContactsCopy A copy of the offlineContacts Vector. 
     */
    private void removeClosedOfflineContacts(MetaContactGroup group,
            Vector offlineContactsCopy) {
        Iterator iter = group.getChildContacts();
        while (iter.hasNext()) {
            MetaContact contact = (MetaContact) iter.next();
            if (!getMetaContactStatus(contact).isOnline()) {
                offlineContactsCopy.add(contact);
            }
        }
        
        Iterator iter1 = group.getSubgroups();
        while (iter1.hasNext()) {
            MetaContactGroup subgroup = (MetaContactGroup) iter1.next();
            if (subgroup.countChildContacts() > 0
                    || subgroup.countSubgroups() > 0) {
                removeClosedOfflineContacts(subgroup, offlineContactsCopy);
            }
        }
    }

    /**
     * Adds all offline contacts back to the contact list.
     */
    public void addOfflineContacts() {
        //Create a copy of the list containing offline contacts
        //and after that delete it. We need that the list is empty
        //when calculating indexOf.
        Vector contacts = (Vector) this.offlineContacts.clone();
        this.offlineContacts.removeAllElements();
        
        for (int i = 0; i < contacts.size(); i++) {
            
            MetaContact contact = (MetaContact) contacts.get(i);
            
            if (!isGroupClosed(contact.getParentMetaContactGroup())) {
                int index = this.indexOf(contact);
                contentAdded(index, index);
            }
        }
    }

    /**
     * Returns true if offline contacts should be shown, 
     * false otherwise.
     * @return boolean true if offline contacts should be 
     * shown, false otherwise.
     */
    public boolean showOffline() {
        return showOffline;
    }

    /**
     * Sets the showOffline variable to indicate whether or not 
     * offline contacts should be shown.
     * @param showOffline true if offline contacts should be shown, 
     * false otherwise.
     */
    public void setShowOffline(boolean showOffline) {
        this.showOffline = showOffline;
    }

    /**
     * Informs the model that the contect of the given contact 
     * was changed. When in mode "hide offline contacts", shows
     * or hides the given contact depending on the new status.
     *  
     * @param contact The MetaContact which status has changed.
     * @param newStatus The new status of the contact.
     */
    public void updateContactStatus(MetaContact contact,
            PresenceStatus newStatus) {
        if (!showOffline) {
            if (newStatus.isOnline() && offlineContacts.contains(contact)) {
                offlineContacts.remove(contact);
            } else if (!newStatus.isOnline()
                    && !offlineContacts.contains(contact)) {
                offlineContacts.add(contact);
            }
        }

        int index = this.indexOf(contact);
        this.contentChanged(index, index);
    }
}
