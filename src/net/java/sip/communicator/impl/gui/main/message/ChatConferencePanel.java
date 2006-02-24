/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;

public class ChatConferencePanel extends JPanel {
	
	private JScrollPane contactsScrollPane = new JScrollPane ();
	
	private JPanel contactsPanel = new JPanel ();
	
	private JPanel mainPanel = new JPanel(new BorderLayout());
 	
	private SIPCommButton addToChatButton 
				= new SIPCommButton(ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_BUTTON),
									ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_ROLLOVER_BUTTON),
									ImageLoader.getImage(ImageLoader.ADD_TO_CHAT_ICON));
	
	private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
	
	private ChatContactPanel chatContactPanel;
	
	
	public ChatConferencePanel (){
		
		super (new BorderLayout(5, 5));
	
		this.setMinimumSize(new Dimension(150, 100)); 
		
		this.init();
	}	
	
	public void init(){
		this.contactsPanel.setLayout(new BoxLayout(this.contactsPanel, BoxLayout.Y_AXIS));
		
		this.mainPanel.add(contactsPanel, BorderLayout.NORTH);
		this.contactsScrollPane.getViewport().add(this.mainPanel);
		
		this.buttonPanel.add(addToChatButton);		
		
		this.add(contactsScrollPane, BorderLayout.CENTER);
		this.add(buttonPanel, BorderLayout.SOUTH);		
	}
	
	public void addContactToChat (MetaContact contactItem){		
		
		chatContactPanel = new ChatContactPanel(contactItem);
				
		this.contactsPanel.add(chatContactPanel);
	}	
}
