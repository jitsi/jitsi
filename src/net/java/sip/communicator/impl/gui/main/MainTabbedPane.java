package net.java.sip.communicator.impl.gui.main;

import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

/**
 * @author Yana Stamcheva
 *
 * The main tabbed pane containing the contact list panel, the 
 * call list panel and the dial panel. 
 */
public class MainTabbedPane extends JTabbedPane {
	
	private DialPanel dialPanel = new DialPanel();
	
	public MainTabbedPane(ContactList clist){		
		
		ContactListPanel contactList = new ContactListPanel(clist);
		
		this.addTab("Contacts", contactList);
		this.addTab("Call list", new JPanel());
		this.addTab("Dial", dialPanel);
	}
}
