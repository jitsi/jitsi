package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

/**
 * @author Yana Stamcheva
 *
 * The MainFrame of the application. 
 */
public class MainFrame extends JFrame{	
	
	private JPanel			mainPanel	= new JPanel(new BorderLayout());
	private JPanel 			menusPanel 	= new JPanel(new BorderLayout());
	private Menu 			menu 		= new Menu();	
	private QuickMenu 		quickMenu 	= new QuickMenu();	
	private CallPanel 		callPanel;
	private StatusPanel		statusPanel;
	private MainTabbedPane 	tabbedPane;
	private ContactList 	clist;
	private User 			user;
	
	public MainFrame(ContactList clist, User user){	
				
		this.clist = clist;
		this.user = user;
		
		callPanel 	= new CallPanel(this);
		tabbedPane = new MainTabbedPane(this);
		statusPanel = new StatusPanel(user.getProtocols());
				
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setInitialBounds();
		
		this.setTitle(Messages.getString("sipCommunicator"));
	    
	    this.setIconImage(Constants.SIP_LOGO);
	  
		this.init();		
	}

	private void init(){		
		this.menusPanel.add(menu, BorderLayout.NORTH);
		this.menusPanel.add(quickMenu, BorderLayout.CENTER);
		
		this.mainPanel.add(tabbedPane, BorderLayout.CENTER);
		this.mainPanel.add(callPanel, BorderLayout.SOUTH);
		
		this.getContentPane().add(menusPanel, BorderLayout.NORTH);		
		this.getContentPane().add(mainPanel, BorderLayout.CENTER);
		this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
	}
	
	private void setInitialBounds(){
		this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width - MainFrame.WIDTH, 50);
		
		this.getContentPane().setSize(	Constants.MAINFRAME_WIDTH, 
										Constants.MAINFRAME_HEIGHT);
			
		this.tabbedPane.setPreferredSize(new Dimension(Constants.MAINFRAME_WIDTH, 
				Constants.MAINFRAME_HEIGHT));
	}

	public CallPanel getCallPanel() {
		return callPanel;
	}

	public ContactList getContactList() {
		return clist;
	}

	public void setContactList(ContactList clist) {
		this.clist = clist;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}	
	
}
