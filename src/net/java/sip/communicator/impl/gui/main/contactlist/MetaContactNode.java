/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ImageIcon;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.PresenceStatus;

public class MetaContactNode {

    private MetaContact contact;
    
    private Map protocolIcons = new Hashtable();
    
    private ImageIcon statusIcon 
        = new ImageIcon(ImageLoader.getImage(ImageLoader.USER_OFFLINE_ICON));
    
    public MetaContactNode(MetaContact contact){
        this.contact = contact;
        
        //setProtocolIcons for this MetaContact
        Iterator i = contact.getContacts();
        while(i.hasNext()){
        	Contact protocolContact = (Contact)i.next();
        	
        	String protocolName 
        		= protocolContact.getProtocolProvider().getProtocolName();
        	
        	this.protocolIcons.put(protocolName, 
    				Constants.getProtocolStatusIcons
    				(protocolName).get(Constants.OFFLINE_STATUS));
        }
    }

    /**
     * Returns the status icon for this MetaContact.
     * 
     * @return the status icon for this MetaContact.
     */
    public ImageIcon getStatusIcon() {
        return statusIcon;
    }
    
    /**
     * Sets the status icon of this MetaContact.
     * 
     * @param statusIcon the status icon of this MetaContact.
     */
    public void setStatusIcon(ImageIcon statusIcon) {
        this.statusIcon = statusIcon;
    }

    /**
     * Returns the MetaContact in this node.
     * 
     * @return the MetaContact in this node.
     */
    public MetaContact getContact() {
        return contact;
    }
    
    /**
     * Returns the contact display name as a String representation of 
     * this node.
     */
    public String toString(){
		return this.contact.getDisplayName();
    }

    /**
     * Returns a Map containing pairs of (protocolName, protocolImage).
     * 
     * @return a Map containing pairs of (protocolName, protocolImage). 
     */
	public Map getProtocolIcons() {
		return protocolIcons;
	}
	
	/**
	 * Change the protocol status icon for this MetaContactNode.
	 * 
	 * @param protocolName The protocol which status has changed.
	 * @param status The new status.
	 */
	public void changeProtocolContactStatus(String protocolName, 
											PresenceStatus status){
		this.protocolIcons.put(protocolName, Constants.getProtocolStatusIcons
				(protocolName).get(status));	
	}
}
