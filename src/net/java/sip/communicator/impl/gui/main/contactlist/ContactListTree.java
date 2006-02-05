/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Cursor;

import javax.swing.BorderFactory;
import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.java.sip.communicator.impl.gui.main.ui.SIPCommTreeUI;

public class ContactListTree extends JTree {
	
	private ContactListTreeModel treeModel;
	
	private ContactNode rootNode;
	
	public ContactListTree(ContactNode rootNode){
		
		this.rootNode = rootNode;
		
		this.treeModel 	= new ContactListTreeModel(rootNode);  
				
		this.setModel(this.treeModel);
		
		this.setRootVisible(false);
		
		this.setEditable(true);
		
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
}
