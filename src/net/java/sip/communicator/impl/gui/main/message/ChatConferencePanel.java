package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.ContactItem;
import net.java.sip.communicator.impl.gui.main.ContactPanel;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;

public class ChatConferencePanel extends JPanel {
	
	private JScrollPane contactsScrollPane = new JScrollPane ();
	
	private JPanel contactsPanel = new JPanel ();
	
	private JPanel mainPanel = new JPanel(new BorderLayout());
 	
	private SIPCommButton addToChatButton 
				= new SIPCommButton(Constants.ADD_TO_CHAT_BUTTON,
									Constants.ADD_TO_CHAT_ROLLOVER_BUTTON,
									Constants.ADD_TO_CHAT_ICON);
	
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
	
	public void addContactToChat (ContactItem contactItem){		
		
		chatContactPanel = new ChatContactPanel(contactItem);
				
		this.contactsPanel.add(chatContactPanel);
	}	
}
