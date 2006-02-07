/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContactListService;

/**
 * @author Yana Stamcheva
 * 
 * The MainFrame of the application.
 */
public class MainFrame extends JFrame {

	private JPanel contactListPanel = new JPanel(new BorderLayout());

	private JPanel menusPanel = new JPanel(new BorderLayout());

	private Menu menu = new Menu();

	private ConfigurationFrame configFrame = new ConfigurationFrame();

	private CallPanel callPanel;

	private StatusPanel statusPanel;

	private MainTabbedPane tabbedPane;

	private QuickMenu quickMenu;

	private ContactList contactList;

	private User user;

	private Dimension minimumFrameSize = new Dimension(
			Constants.MAINFRAME_MIN_WIDTH, Constants.MAINFRAME_MIN_HEIGHT);

	public MainFrame(User user, ContactList contactList) {
		
		this.user = user;

		this.contactList = contactList;
		
		callPanel = new CallPanel(this);
		tabbedPane = new MainTabbedPane(this);
		quickMenu = new QuickMenu(this);
		statusPanel = new StatusPanel(user.getProtocols());

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setInitialBounds();

		this.setTitle(Messages.getString("sipCommunicator"));

		this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));
		
		this.setSize(Constants.MAINFRAME_WIDTH, Constants.MAINFRAME_HEIGHT);	
		
		this.init();
	}

	private void init() {

		this.menusPanel.add(menu, BorderLayout.NORTH);
		this.menusPanel.add(quickMenu, BorderLayout.CENTER);

		this.contactListPanel.add(tabbedPane, BorderLayout.CENTER);
		this.contactListPanel.add(callPanel, BorderLayout.SOUTH);

		this.getContentPane().add(menusPanel, BorderLayout.NORTH);
		this.getContentPane().add(contactListPanel, BorderLayout.CENTER);
		this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
	}

	private void setInitialBounds() {
		
		this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width
				- MainFrame.WIDTH, 50);
	
		this.tabbedPane.setMinimumSize(minimumFrameSize);
	}

	public CallPanel getCallPanel() {

		return callPanel;
	}

	public ContactList getContactList() {
		
		return this.contactList;
	}

	public void setContactList(ContactList contactList) {
		
		this.contactList = contactList;
	}

	public User getUser() {
		
		return user;
	}

	public void setUser(User user) {
	
		this.user = user;
	}

	public ConfigurationFrame getConfigFrame() {
	
		return configFrame;
	}

	public void setConfigFrame(ConfigurationFrame configFrame) {
	
		this.configFrame = configFrame;
	}
}
