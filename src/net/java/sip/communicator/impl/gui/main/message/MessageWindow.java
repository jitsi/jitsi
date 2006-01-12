package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JWindow;

import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.ContactItem;
import net.java.sip.communicator.impl.gui.main.MainFrame;

public class MessageWindow extends JFrame {
	
	private JSplitPane messagePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	
	private ChatPanel chatPanel;
	
	private WriteMessagePanel writeMessagePanel;
	
	private MenusPanel menusPanel;
	
	private MessageSendPanel sendPanel;
	
	private JPanel topPanel = new JPanel(new BorderLayout());
	
	private JPanel bottomPanel = new JPanel(new BorderLayout());
	
	private ChatConferencePanel chatConferencePanel = new ChatConferencePanel();
	
	private JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	
	private MainFrame parentWindow;	
	
	private String windowTitle = "";
	
	private Vector chatContacts = new Vector();
	
	
	
	public MessageWindow (MainFrame parentWindow){		
				
		this.parentWindow = parentWindow;		
		
		this.setSize(550, 450);
				
		this.setIconImage(Constants.SIP_LOGO);
		
		menusPanel = new MenusPanel(this);
		
		chatPanel = new ChatPanel(this);
		
		writeMessagePanel = new WriteMessagePanel(this);
		
		sendPanel = new MessageSendPanel(this);
		
		this.init();
	}
	
	public void init (){
					
		this.topSplitPane.setDividerLocation(370);
		this.topSplitPane.setOneTouchExpandable(true);
		
		topSplitPane.add(chatPanel);
		topSplitPane.add(chatConferencePanel);
		
		this.topPanel.add(menusPanel, BorderLayout.NORTH);
		this.topPanel.add(topSplitPane, BorderLayout.CENTER);		
				
		this.bottomPanel.add(writeMessagePanel, BorderLayout.CENTER);
		this.bottomPanel.add(sendPanel, BorderLayout.SOUTH);
		
		this.messagePane.setDividerLocation(300);
		
		this.messagePane.add(topPanel);
		this.messagePane.add(bottomPanel);
		
		this.getContentPane().add(messagePane);		
	}
	
	public void addContactToChat (ContactItem contactItem){		
		
		this.chatContacts.add(contactItem);
		
		this.chatConferencePanel.addContactToChat(contactItem);
		
		this.windowTitle += contactItem.getNickName() + " ";
		
		this.setTitle(this.windowTitle);
	}

	public void removeContactFromChat (ContactItem contactItem){
		this.chatContacts.remove(contactItem);
	}
	
	public Vector getChatContacts() {
		return chatContacts;
	}

	public void setChatContacts(Vector chatContacts) {
		this.chatContacts = chatContacts;
	}

	public MainFrame getParentWindow() {
		return parentWindow;
	}

	public void setParentWindow(MainFrame parentWindow) {
		this.parentWindow = parentWindow;
	}

	public WriteMessagePanel getWriteMessagePanel() {
		return writeMessagePanel;
	}

	public MessageSendPanel getSendPanel() {
		return sendPanel;
	}

	public void enableKeyboardSending(){
		this.writeMessagePanel.enableKeyboardSending();
	}

	public ChatPanel getChatPanel() {
		return chatPanel;
	}
}
