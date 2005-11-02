package net.java.sip.communicator.impl.gui.main;

import java.util.Vector;

/**
 * @author Yana Stamcheva
 *  
 * The contact list.
 * TODO: to be removed when the contact list service is ready. 
 */

public class ContactList {

	private Vector contacts = new Vector();
		
	public Vector getAllContacts(){
		return contacts;		
	}
	
	public void addContact(ContactItem contactItem){		
		contacts.add(contactItem);
	}
}
