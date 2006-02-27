/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Image;

import javax.swing.tree.DefaultMutableTreeNode;

import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;

public class ContactNode extends DefaultMutableTreeNode {

	private boolean leafExpanded = false;
	
	private Image icon = ImageLoader.getImage(ImageLoader.USER_OFFLINE_ICON);
	
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
            
			if (userObject instanceof MetaContact)
				result = ((MetaContact)userObject).getDisplayName();
			else if (userObject instanceof MetaContactGroup)
				result = ((MetaContactGroup)userObject).getGroupName();
		}
		
		return result;
    }

	public boolean isLeafExpanded() {
		return leafExpanded;
	}

	public void setLeafExpanded(boolean leafExpanded) {
		this.leafExpanded = leafExpanded;
	}

	public Image getIcon() {
		return icon;
	}

	public void setIcon(Image icon) {
		this.icon = icon;
	}
	
}
