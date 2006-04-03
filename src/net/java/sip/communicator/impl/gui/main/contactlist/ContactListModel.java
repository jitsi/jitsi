/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.SwingUtilities;

import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.PresenceStatus;

/**
 * The list model of the ContactList.
 * 
 * @author Yana Stamcheva
 *
 */
public class ContactListModel extends AbstractListModel {

    private MetaContactListService contactList;
    
    private MetaContactGroup rootGroup;
    
    private Vector closedGroups = new Vector();
    
    /**
     * Creates a List Model, which gets its data from the given MetaContactListService. 
     * 
     * @param contactList The MetaContactListService which contains the contact list.
     */
    public ContactListModel(MetaContactListService contactList){
        
        this.contactList = contactList;
        
        this.rootGroup = this.contactList.getRoot();
    }
        
    /**
     * Informs interested listeners that the content has changed of the 
     * cells given by the range of from startIndex to endIndex.
     * 
     * @param startIndex The start index of the range .
     * @param endIndex The end index of the range.
     */
    public void contentChanged(int startIndex, int endIndex) {
        fireContentsChanged(this, startIndex, endIndex);
    }

    /**
     * Informs interested listeners that new cells are added from startIndex 
     * to endIndex. 
     * 
     * @param startIndex The start index of the range .
     * @param endIndex The end index of the range.
     */
    public void contentAdded(final int startIndex, final int endIndex) {
        SwingUtilities.invokeLater(new Thread(){
            public void run(){
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
        SwingUtilities.invokeLater(new Thread(){
            public void run(){
                fireIntervalRemoved(this, startIndex, endIndex);
            }
        });
    }
    
    /**
     * Returns the size of this list model.
     */
    public int getSize() {        
        return this.getContactListSize(rootGroup);
    }

    /**
     * Returns the object at the given index.
     */
    public Object getElementAt(int index) {
        Object element = this.getElementAt(this.rootGroup, index);
        
        return element;
    }
        
    /**
     * Goes through all subgroups and contacts and determines the final size of the
     * contact list.
     * 
     * @param group
     * @return The size of the contactlist
     */
    private int getContactListSize(MetaContactGroup group){
        
        int size = 0;
        
        if(!this.isGroupClosed(group)){
            size = group.countChildContacts();
          
            size += group.countSubgroups();
            
            Iterator subgroups = group.getSubgroups();
            
            while(subgroups.hasNext()){
                size += getContactListSize((MetaContactGroup)subgroups.next());
            }
        }
        return size;
    }
    
    /**
     * Returns the general status of the given MetaContact. Detects the status using the 
     * priority status table. The priority is defined on the "availablity" factor and here the most 
     * "available" status is returned.
     * 
     * @return The most "available" status from all subcontact  statuses.
     */
    public PresenceStatus getMetaContactStatus(MetaContact metaContact) {
        
        PresenceStatus defaultStatus = Constants.OFFLINE_STATUS;
        
        Iterator i = metaContact.getContacts();
        while(i.hasNext()){
            Contact protoContact = (Contact)i.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();
            
            defaultStatus 
                = (contactStatus.compareTo(defaultStatus) > 0 )
                    ?contactStatus:defaultStatus;
        }
        return defaultStatus;
    }
    
    /**
     * Returns the status icon for this MetaContact.
     *  
     * @return the status icon for this MetaContact.
     */
    public ImageIcon getMetaContactStatusIcon(MetaContact contact) {
        return new ImageIcon(Constants.getStatusIcon(this.getMetaContactStatus(contact)));
    }
   
    /**
     * Returns the index of the given MetaContact.
     *  
     * @param contact The MetaContact to search for.
     * @return The index of the given MetaContact.
     */
    public int indexOf(MetaContact contact){
        
        int index = -1;
        int currentIndex = 0;
        MetaContactGroup parentGroup = this.contactList.findParentMetaContactGroup(contact);
        
        if(parentGroup != null && !this.isGroupClosed(parentGroup)){
            currentIndex += this.indexOf(parentGroup);
            
            for(int i = 0; i < parentGroup.countSubgroups(); i ++){
                MetaContactGroup subGroup = parentGroup.getMetaContactSubgroup(i);
                
                currentIndex += countSubgroupContacts(subGroup);
            }
            
            currentIndex += parentGroup.indexOf(contact) + 1;
            
            index = currentIndex;
        }
        
        return index;
    }
    
    /**
     * Returns the index of the given MetaContactGroup.
     * 
     * @param group The given MetaContactGroup to search for.
     * @return The index of the given MetaContactGroup.
     */
    public int indexOf(MetaContactGroup group){
        
        int index = -1;
        int currentIndex = 0;
        MetaContactGroup parentGroup = this.contactList.findParentMetaContactGroup(group);
        
        if(parentGroup != null && !this.isGroupClosed(parentGroup)){            
            currentIndex += this.indexOf(parentGroup);                        
            currentIndex += parentGroup.indexOf(group) + 1;
            
            for(int i = 0; i < parentGroup.indexOf(group); i ++){
                MetaContactGroup subGroup = parentGroup.getMetaContactSubgroup(i);
                
                currentIndex += countSubgroupContacts(subGroup);
            }
            
            index = currentIndex;
        }
        
        return index;
    }
    
    /**
     * Returns the number of all children of the given MetaContactGroup. Counts in depth 
     * all subgroups and child contacts.
     * 
     * @param parentGroup The parent MetaContactGroup.
     * @return The number of all children of the given MetaContactGroup
     */
    private int countSubgroupContacts(MetaContactGroup parentGroup){
        
        int count = 0;
        
        if(parentGroup != null && !this.isGroupClosed(parentGroup)){
            count += parentGroup.countChildContacts();
            
            Iterator subgroups = parentGroup.getSubgroups();
                    
            while(subgroups.hasNext()){
                MetaContactGroup subgroup = (MetaContactGroup)subgroups.next();
                
                count += countSubgroupContacts(subgroup);
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
    private Object getElementAt(MetaContactGroup group, int searchedIndex){
        
        Object element = null;
        
        if(!this.isGroupClosed(group)){
            Iterator contacts = group.getChildContacts();
            
            while(contacts.hasNext()){
                MetaContact contact = (MetaContact)contacts.next();
                
                if(this.indexOf(contact) == searchedIndex){
                    
                    element = contact;
                    break;
                }
            }
            
            if(element == null){
                Iterator subgroups = group.getSubgroups();
                    
                while(subgroups.hasNext()){
                    MetaContactGroup subgroup = (MetaContactGroup)subgroups.next();
                     
                    if(this.indexOf(subgroup) != searchedIndex){
                        element = getElementAt(subgroup, searchedIndex);
                        if (element != null)
                            break;  
                    }
                    else{
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
    public void closeGroup(MetaContactGroup group){
        fireIntervalRemoved(this, this.indexOf(group.getMetaContact(0)),
                this.indexOf(group.getMetaContact(group.countChildContacts() - 1)));
        
        this.closedGroups.add(group);
    }
    
    /**
     * Opens the given group by showing all containing contacts.
     * 
     * @param group The group to open.
     */
    public void openGroup(MetaContactGroup group){        
        this.closedGroups.remove(group);
        
        fireIntervalAdded(this, this.indexOf(group.getMetaContact(0)), 
                this.indexOf(group.getMetaContact(group.countChildContacts() - 1)));
    }
    
    /**
     * Checks whether the group is closed.
     * 
     * @param group The group to check.
     * @return True if the group is closed, false - otherwise.
     */
    public boolean isGroupClosed(MetaContactGroup group){
        if(this.closedGroups.contains(group))
            return true;
        else
            return false;
    }
}
