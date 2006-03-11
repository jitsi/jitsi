package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.text.Position;

/**
 * Search the ContactList by typing a letter.
 */
public class CListKeySearchListener implements KeyListener {
   
	private ContactList contactList;
	
	private char lastTypedKey = 0;
	
	public CListKeySearchListener(ContactList contactList){
		this.contactList = contactList;
	}
	public void keyPressed(KeyEvent e) {
		
	}

	public void keyReleased(KeyEvent e) {
	
	}

	public void keyTyped(KeyEvent e) {
		
		int contactIndex = - 1;
		
		boolean selectedSameLetterContact = false;
		
		int selectedIndex = this.contactList.getSelectedIndex();
		
		String keyChar = String.valueOf(e.getKeyChar());
		
		//Checks if there's any selected contact node and gets its name.
		if(selectedIndex != -1){
			Object selectedObject = this.contactList.getSelectedValue();
									
			if(selectedObject instanceof MetaContactNode){
				String selectedContactName 
						= ((MetaContactNode)selectedObject)
							.getContact().getDisplayName();
								
				if (selectedContactName != null) 
					selectedSameLetterContact 
						= selectedContactName.substring(0, 1)
							.equalsIgnoreCase(keyChar);
			}
		}
						
		/*
		 * The search starts from the beginning if:
		 * 1) the newly entered character is different from the last one
		 * or  
		 * 2) the currently selected contact starts with a different letter 
		 */
		if(lastTypedKey != e.getKeyChar() || !selectedSameLetterContact) {
			contactIndex = this.getNextMatch(keyChar, 0);
		}
		else {
			contactIndex = this.getNextMatch(	keyChar, 
												selectedIndex + 1);
		}
		
		if(contactIndex != -1){
			this.contactList.setSelectedIndex(contactIndex);
			this.contactList.ensureIndexIsVisible(contactIndex);
		}
		
		this.lastTypedKey = e.getKeyChar();
	}
	
	/**
	 * Returns the next contact node that starts with the given prefix.
	 * 
	 * @param keyChar The char to test for a match.
	 * @param startIndex The index for starting the search.
	 * @return The index of the next contact that starts with the prefix.
	 */
	private int getNextMatch(String keyChar, int startIndex){
		
		int indexToSelect = -1;
		
		int index = this.contactList.getNextMatch(	keyChar, 
													startIndex, 
													Position.Bias.Forward);
		if(index != -1){
			Object element = this.contactList.getModel().getElementAt(index);
			
			if(element instanceof MetaContactNode){
				indexToSelect = index;
			}
			else{
				indexToSelect = getNextMatch(keyChar, index + 1);
			}
		}
		
		return indexToSelect;
	}
}