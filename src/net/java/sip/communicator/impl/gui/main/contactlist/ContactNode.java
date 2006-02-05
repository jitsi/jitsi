/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Cursor;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import net.java.sip.communicator.impl.gui.main.ContactItem;
import net.java.sip.communicator.impl.gui.main.GroupItem;

public class ContactNode extends DefaultMutableTreeNode {

	private boolean leafExpanded = false;
	
	public ContactNode(){
		
	}
	
	public ContactNode(Object userObject){
		super(userObject);
	}
	
    public String toString() {
    	
    	String result = "";
    	
		if (userObject == null) {
		    return null;
		} else {
			//TODO: to replace ContactItem with MetaContact and GroupItem with MetaGroup
			if (userObject instanceof ContactItem)
				result = ((ContactItem)userObject).getNickName();
			else if (userObject instanceof GroupItem)
				result = ((GroupItem)userObject).getGroupName();
		}
		
		return result;
    }

	public boolean isLeafExpanded() {
		return leafExpanded;
	}

	public void setLeafExpanded(boolean leafExpanded) {
		this.leafExpanded = leafExpanded;
	}
	
}
