package net.java.sip.communicator.impl.gui.main.history;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.sip.communicator.impl.gui.main.ContactItem;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

public class HistoryWindow extends JFrame {	
	
	private JScrollPane historyPane = new JScrollPane();
	
	private JPanel mainPanel = new JPanel(new BorderLayout());
	
	private NavigationPanel navigationPanel = new NavigationPanel();
	
	private SearchPanel searchPanel = new SearchPanel();	
	
	private JMenuBar	historyMenuBar = new JMenuBar();
		
	private HistoryMenu historyMenu;
	
	private JMenu	settingsMenu = new JMenu(Messages.getString("settings"));
	
	private JPanel northPanel = new JPanel(new BorderLayout());
	
	private Vector contacts;
	
	private ContactItem contactItem;
	
	String title = Messages.getString("history") + " - ";
	
	public HistoryWindow(){
				
		historyMenu = new HistoryMenu(this);
		
		this.setSize(Constants.HISTORY_WINDOW_WIDTH, 
					Constants.HISTORY_WINDOW_HEIGHT);	
		
		this.setIconImage(Constants.SIP_LOGO);
		
		this.init();
	}
	
	public void init(){
		
		this.historyMenuBar.add(historyMenu);
		
		this.historyMenuBar.add(settingsMenu);
		
		this.northPanel.add(historyMenuBar, BorderLayout.NORTH);
		
		this.northPanel.add(searchPanel, BorderLayout.CENTER);
		
		this.mainPanel.add(northPanel, BorderLayout.NORTH);
		
		this.mainPanel.add(historyPane, BorderLayout.CENTER);
		
		this.mainPanel.add(navigationPanel, BorderLayout.SOUTH);
		
		
		this.getContentPane().add(mainPanel);		
	}

	public Vector getContacts() {
		return contacts;
	}

	public void setContacts(Vector contacts) {
		
		this.contacts = contacts;		
		
		for (int i = 0; i < contacts.size(); i ++){
			
			ContactItem contact = (ContactItem)contacts.get(i);
			
			this.title += " " + contact.getNickName(); 
		}
		
		this.setTitle(title);
	}	
	
	public void setContact(ContactItem contact) {
		
		this.contactItem = contact;
		
		this.title += " " + contact.getNickName(); 
				
		this.setTitle(title);
	}	
}
