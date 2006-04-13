/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Cursor;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.text.Position;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.java.sip.communicator.impl.gui.main.ui.SIPCommTreeUI;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.contactlist.event.MetaContactEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactGroupEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactListListener;
import net.java.sip.communicator.service.contactlist.event.MetaContactMovedEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactRenamedEvent;
import net.java.sip.communicator.service.contactlist.event.ProtoContactEvent;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.PresenceStatus;

public class ContactList extends JList 
    implements MetaContactListListener {

    private MetaContactListService contactList;

    private ContactListModel listModel;    
    
    public ContactList(MetaContactListService contactList){

        this.contactList = contactList;

        this.listModel = new ContactListModel(contactList);
        
        this.setModel(listModel);

        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.getSelectionModel().setSelectionMode
                (TreeSelectionModel .SINGLE_TREE_SELECTION);

        this.setCellRenderer(new ContactListCellRenderer());

        this.putClientProperty("JTree.lineStyle", "None");

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        this.addKeyListener(new CListKeySearchListener(this));
        
        this.contactList.addContactListListener(this);
    }
   
    public void metaContactAdded(MetaContactEvent evt) {
        // TODO Auto-generated method stub
        
    }

    public void metaContactRenamed(MetaContactRenamedEvent evt) {
        // TODO Auto-generated method stub
        
    }

    public void protoContactAdded(ProtoContactEvent evt) {
        // TODO Auto-generated method stub
        
    }

    public void protoContactRemoved(ProtoContactEvent evt) {
        // TODO Auto-generated method stub
        
    }

    public void protoContactMoved(ProtoContactEvent evt) {
        // TODO Auto-generated method stub
        
    }

    public void metaContactRemoved(MetaContactEvent evt) {
        // TODO Auto-generated method stub
        
    }

    public void metaContactMoved(MetaContactMovedEvent evt) {
        // TODO Auto-generated method stub
        
    }

    /**
     * Indicates that a MetaContactGroup has been added.
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt) {
        
        MetaContactGroup sourceGroup = evt.getSourceMetaContactGroup();
       
        this.groupAdded(sourceGroup);
        
        this.ensureIndexIsVisible(0);  
    }
   
    public void metaContactGroupModified(MetaContactGroupEvent evt) {
        // TODO Auto-generated method stub
        
    }

    public void metaContactGroupRemoved(MetaContactGroupEvent evt) {
        // TODO Auto-generated method stub
        
    }

    public void childContactsReordered(MetaContactGroupEvent evt) {
        
        MetaContactGroup group = evt.getSourceMetaContactGroup();

        int startIndex 
            = this.listModel.indexOf(group.getMetaContact(0));
        int endIndex 
            = this.listModel.indexOf(group.getMetaContact(group.countChildContacts() - 1));
        
        this.listModel.contentChanged(startIndex, endIndex);
    }
    
    /**
     * Refreshes the jlist when a group is added.
     * 
     * @param group The group which is added.
     */
    private void groupAdded(MetaContactGroup group){
        
        int index = this.listModel.indexOf(group);

        this.listModel.contentAdded(index, index);        
                
        Iterator childContacts = group.getChildContacts();
        
        while(childContacts.hasNext()){
            MetaContact contact = (MetaContact)childContacts.next();
            
            int contactIndex = this.listModel.indexOf(contact);
            this.listModel.contentAdded(contactIndex, contactIndex);
        }
        
        Iterator subGroups = group.getSubgroups();
        
        while(subGroups.hasNext()){
            MetaContactGroup subGroup = (MetaContactGroup)subGroups.next();
            
            this.groupAdded(subGroup);
        }
    }
    
    /**
     * Returns the next list element that starts with 
     * a prefix.
     *
     * @param prefix the string to test for a match
     * @param startIndex the index for starting the search
     * @param bias the search direction, either 
     * Position.Bias.Forward or Position.Bias.Backward.
     * @return the index of the next list element that
     * starts with the prefix; otherwise -1
     * @exception IllegalArgumentException if prefix is null
     * or startIndex is out of bounds
     */
    public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
        ContactListModel model = (ContactListModel)this.getModel();
        int max = model.getSize();
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        if (startIndex < 0 || startIndex >= max) {
            throw new IllegalArgumentException();
        }
        prefix = prefix.toUpperCase();
    
        // start search from the next element after the selected element
        int increment = (bias == Position.Bias.Forward) ? 1 : -1;
        int index = startIndex;
        do {
            Object o = model.getElementAt(index);
            
            if (o != null) {            
                String contactName = null;
                
                if (o instanceof MetaContact) {
                    contactName = ((MetaContact)o).getDisplayName().toUpperCase();
                }
                
                if (contactName != null && contactName.startsWith(prefix)) {
                    return index;
                }
            }
            index = (index + increment + max) % max;
        } while (index != startIndex);
        return -1;
    }
}


