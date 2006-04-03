/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.text.Position;

import net.java.sip.communicator.service.contactlist.MetaContact;

/**
 * Search the ContactList by typing a letter.
 */
public class CListKeySearchListener implements KeyListener {
   
	private ContactList contactList;
	
	private String lastTypedKey;
    private long lastTypedTimestamp = 0;
    private StringBuffer keyBuffer = new StringBuffer();
	
	public CListKeySearchListener(ContactList contactList){
		this.contactList = contactList;
	}
	public void keyPressed(KeyEvent e) {	
	}

	public void keyReleased(KeyEvent e) {	
	}

	public void keyTyped(KeyEvent e) {
        long eventTimestamp = e.getWhen();
        String keyChar = String.valueOf(e.getKeyChar());
        
	    if((lastTypedTimestamp - eventTimestamp) > 1000){	        
            keyBuffer.delete(0, keyBuffer.length() - 1);
        }        
        this.lastTypedTimestamp = eventTimestamp;
        this.keyBuffer.append(keyChar);
        
		boolean selectedSameLetterContact = false;
		
		int selectedIndex = this.contactList.getSelectedIndex();
		
		//Checks if there's any selected contact node and gets its name.
		if(selectedIndex != -1){
			Object selectedObject = this.contactList.getSelectedValue();
									
			if(selectedObject instanceof MetaContact){
				String selectedContactName 
						= ((MetaContact)selectedObject).getDisplayName();
								
				if (selectedContactName != null) 
					selectedSameLetterContact 
						= selectedContactName.substring(0, 1)
							.equalsIgnoreCase(keyBuffer.toString());
			}
		}
		 // The search starts from the beginning if:
		 // 1) the newly entered character is different from the last one
		 // or  
		 // 2) the currently selected contact starts with a different letter
        int contactIndex = -1;
		if(lastTypedKey != keyChar || !selectedSameLetterContact) {
			contactIndex = this.contactList.getNextMatch(keyBuffer.toString(), 
                                                0, Position.Bias.Forward);
		}
		else {
			contactIndex = this.contactList.getNextMatch(keyBuffer.toString(), 
												selectedIndex + 1, Position.Bias.Forward);
		}		
     
        int currentlySelectedIndex = this.contactList.getSelectedIndex();
        
        if(currentlySelectedIndex != contactIndex && contactIndex != -1)
            this.contactList.setSelectedIndex(contactIndex);
        
        this.contactList.ensureIndexIsVisible(currentlySelectedIndex);

        this.lastTypedKey = keyChar;
	}
}