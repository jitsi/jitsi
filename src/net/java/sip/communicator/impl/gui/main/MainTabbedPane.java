package net.java.sip.communicator.impl.gui.main;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

/**
 * @author Yana Stamcheva
 *
 * The main tabbed pane containing the contact list panel, the 
 * call list panel and the dial panel. 
 */
public class MainTabbedPane extends JTabbedPane {
	
	private DialPanel dialPanel = new DialPanel();
	
	private ContactListPanel contactList;
	
	public MainTabbedPane(MainFrame parent){		
		
		contactList = new ContactListPanel(parent, parent.getContactList());
		
		dialPanel.setPhoneNumberCombo(parent.getCallPanel().getPhoneNumberCombo());
				
		this.addTab(Messages.getString("contacts"), contactList);
		this.addTab(Messages.getString("callList"), new JPanel());
		this.addTab(Messages.getString("dial"), dialPanel);
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}

}
