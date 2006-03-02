/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.login.LoginWindow;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;

public class MessageWindow extends JFrame{
	
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
				
		this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));
		
		menusPanel = new MenusPanel(this);
		
		chatPanel = new ChatPanel(this);
		
		writeMessagePanel = new WriteMessagePanel(this);
		
		sendPanel = new MessageSendPanel(this);
		
		this.init();
        
        this.enableKeyActions();
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
	
	public void addContactToChat (MetaContact contactItem){		
		
		this.chatContacts.add(contactItem);
		
		this.chatConferencePanel.addContactToChat(contactItem);
		
		//this.sendPanel.addProtocols(contactItem.getProtocolList());
		
		this.windowTitle += contactItem.getDisplayName() + " ";
		
		this.setTitle(this.windowTitle);
	}

	public void removeContactFromChat (MetaContact contactItem){
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

	public ChatPanel getChatPanel() {
		return chatPanel;
	}

    /**
     * Enables the actions when a key is pressed, for now 
     * closes the window when esc is pressed.
     */
    private void enableKeyActions(){
        
        AbstractAction act = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                MessageWindow.this.setVisible(false);
            }
        };
        
        getRootPane().getActionMap().put("close", act);
        
        InputMap imap =
            this.getRootPane()
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }
}
