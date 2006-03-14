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

import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.PresenceStatus;

public class MetaContactNode {

    private MetaContact contact;
    
    private Map protocolIcons = new Hashtable();
    
    private PresenceStatus defaultStatus = Constants.OFFLINE_STATUS;
    
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
    				(protocolName).get(protocolContact.getPresenceStatus()));
        }
    }

    /**
     * Returns the general status of the MetaContact. Detects the status using the priority status
     * table. The priority is defined on the "availablity" factor and here the most "available" status 
     * is returned.
     * 
     * @return The most "available" status from all subcontact statuses.
     * 
     * @see net.java.sip.communicator.impl.gui.main.utils.Constants#statusPriorityTable 
     * StatusPriorityTable
     */
    public PresenceStatus getStatus() {
        
        Iterator i = this.getContact().getContacts();
        while(i.hasNext()){
            Contact protoContact = (Contact)i.next();
            PresenceStatus contactStatus = protoContact.getPresenceStatus();
            
            this.defaultStatus 
                = (Constants.getPriority(contactStatus) < Constants.getPriority(defaultStatus))
                    ?contactStatus:defaultStatus;
            
        }
        return this.defaultStatus;
    }
    
    /**
     * Returns the status icon for this MetaContact.
     * 
     * @return the status icon for this MetaContact.
     */
    public ImageIcon getStatusIcon() {
        return new ImageIcon(Constants.getStatusIcon(this.getStatus()));
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
    
    /**
     * Returns the contact display name as a String representation of 
     * this node.
     */
    public String toString(){
        return this.contact.getDisplayName();
    }
}
