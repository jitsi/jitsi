/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Cursor;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.java.sip.communicator.impl.gui.main.ui.SIPCommTreeUI;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.contactlist.event.MetaContactEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactGroupEvent;
import net.java.sip.communicator.service.contactlist.event.MetaContactListListener;
import net.java.sip.communicator.service.protocol.Contact;

public class ContactListTree extends JTree 
    implements MetaContactListListener {
	
	private ContactListTreeModel treeModel;
	
	private ContactNode rootNode;
    
    private MetaContactListService contactList;
    
    private MetaContactGroup root;
	
	public ContactListTree(MetaContactListService contactList){
		
		this.contactList = contactList;
        
		this.contactList.addContactListListener(this);
        
        this.root = contactList.getRoot();
		
        this.rootNode = new ContactNode(this.root);
        
		this.treeModel 	= new ContactListTreeModel(rootNode);  
				
		this.setModel(this.treeModel);
		
		this.setRootVisible(false);
		
		this.setEditable(false);
		
		this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		
		this.getSelectionModel().setSelectionMode
        		(TreeSelectionModel	.SINGLE_TREE_SELECTION);		
		
		this.setCellRenderer(new ContactListCellRenderer());
						
		this.putClientProperty("JTree.lineStyle", "None");		
		
		this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		
		this.setUI(new SIPCommTreeUI());
		
		((BasicTreeUI)this.getUI()).setLeftChildIndent(0);
		
		((BasicTreeUI)this.getUI()).setRightChildIndent(0);			
	}
    
	/**
	 * Adds a child directly to the root node.
	 * 
	 * @param child The child object to be added.
	 * @return The added node.
	 */
	public ContactNode addChild(Object child) {
		
	    ContactNode parentNode = null;
	    
	    TreePath parentPath = this.getSelectionPath();

	    if (parentPath == null) {
	        
	    	//There's no selection. Default to the root node.
	        parentNode = this.rootNode;
	        
	    } else {
	        
	    	parentNode = (ContactNode)
	                     (parentPath.getLastPathComponent());
	    }

	    return addChild(parentNode, child, true);
	}
	
	
	/**
	 * Adds a child to a given parent.
	 * 
	 * @param parent The parent node.
	 * @param child The child object.
	 * @param shouldBeVisible
	 * @return The added node.
	 */
	public ContactNode addChild(ContactNode parent,
					            Object child,
					            boolean shouldBeVisible) {

		ContactNode childNode =
			new ContactNode(child);
	
		treeModel.insertNodeInto(childNode, parent,
				parent.getChildCount());
	
		//Make sure the user can see the new node.
		if (shouldBeVisible) {
			this.scrollPathToVisible(new TreePath(childNode.getPath()));
		}
		
		return childNode;
	}
    
	public void addAllContacts(ContactNode groupNode, MetaContactGroup group){
        
        if(group.countSubgroups() > 0){
            
            Iterator groups = group.getSubgroups();
            
            while(groups.hasNext()){
                
                MetaContactGroup subGroup = (MetaContactGroup)groups.next();
                
                ContactNode subGroupNode 
                    = this.addChild(groupNode, subGroup, true);
                                    
                this.addAllContacts(subGroupNode, subGroup);                
            }
        } 
        
        if(group.countChildContacts() > 0 ){
            
            Iterator childContacts = group.getChildContacts();
            
            while(childContacts.hasNext()){
                
                MetaContact childContact = (MetaContact)childContacts.next();
                
                this.addChild(groupNode, childContact, true);
            }
        }
    }

    public void metaContactAdded(MetaContactEvent evt) {
        
        this.addChild(this.rootNode, evt.getSourceContact(), true);
    }

    public void metaContactRemoved(MetaContactEvent evt) {
                
    }

    public void metaContactGroupAdded(MetaContactGroupEvent evt) {
        
        MetaContactGroup contactGroup = evt.getSourceContactGroup();
               
        ContactNode newGroupNode 
            = this.addChild(this.rootNode, contactGroup, true);
        
        Iterator childContacts = contactGroup.getChildContacts();
        while (childContacts.hasNext()){
            
            MetaContact childContact 
                = (MetaContact)childContacts.next();
            
            this.addChild(newGroupNode, childContact, true);
        }
    }

    public void metaContactGroupRemoved(MetaContactGroupEvent evt) {
        // TODO Auto-generated method stub
    }

	public MetaContactGroup getRoot() {
		return root;
	}

	public ContactNode getRootNode() {
		return rootNode;
	}
    
	public ContactNode contains(Contact contact){
	    
        return this.contains(this.rootNode, contact);
	}
	
	private ContactNode contains(  ContactNode parentNode, 
                                   Contact contact){
        
        Enumeration childNodes = parentNode.children();
		
		while(childNodes.hasMoreElements()){
			
			ContactNode node = (ContactNode)childNodes.nextElement();
			
			if(node.getUserObject() instanceof MetaContact){
				
				MetaContact currentContact = (MetaContact)node.getUserObject();
				
                if(currentContact.getDefaultContact().getDisplayName()
						.equals(contact.getDisplayName())){
                    
                    return node;
				}
			}
			else if(node.getUserObject() instanceof MetaContactGroup){
				return this.contains(node, contact);
			}
		}
        return null;
	}
}
				
			