/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatMessage;
import net.java.sip.communicator.impl.gui.main.message.ChatPanel;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import net.java.sip.communicator.service.protocol.event.TypingNotificationEvent;
import net.java.sip.communicator.service.protocol.event.TypingNotificationsListener;

/**
 * The contactlist panel not only contains the contact 
 * list but it has the role of a message dispatcher. It 
 * process all sent and received messages as well as
 * all typing notifications. Here are managed all
 * contact list mouse events.
 * 
 * @author Yana Stamcheva   
 */
public class ContactListPanel extends JScrollPane 
	implements MouseListener, MessageListener, TypingNotificationsListener {
       
	private MainFrame mainFrame;

	private ContactList contactList;

	private JPanel treePanel = new JPanel(new BorderLayout());

	private Hashtable contactMsgWindows = new Hashtable();
    
    private ChatWindow tabbedChatWindow;
    
    private TypingTimer typingTimer = new TypingTimer();
    /**
     * Creates the contactlist scroll panel defining the parent frame.
     * 
     * @param mainFrame The parent frame.
     */
	public ContactListPanel(MainFrame mainFrame) {

		this.mainFrame = mainFrame;        
		
		this.getViewport().add(treePanel);

		this.treePanel.setBackground(Color.WHITE);
        
        this.setHorizontalScrollBarPolicy
            (JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        this.getVerticalScrollBar().setUnitIncrement(30);
       
	}

     public void initTree(MetaContactListService contactListService) {
            
            this.contactList = new ContactList(contactListService);
            
            this.contactList.addMouseListener(this);            
            
            this.treePanel.add(contactList, BorderLayout.NORTH);
            
            this.addKeyListener(new CListKeySearchListener(this.contactList));
            
            this.getRootPane().getActionMap().put("runChat", new RunMessageWindowAction());
            
            InputMap imap = this.getRootPane()
                    .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            
            imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "runChat");
     }
        
        public ContactList getContactList(){
            
            return this.contactList;
        }
        
	public void mouseClicked(MouseEvent e) {		
        //Expand and collapse groups on double click.
        if(e.getClickCount() > 1){
            
            int selectedIndex 
                = this.contactList.locationToIndex(e.getPoint());
            
            ContactListModel listModel 
            		= (ContactListModel)this.contactList.getModel();
            
            Object element 
                = listModel.getElementAt(selectedIndex);
            
            if(element instanceof MetaContactGroup){
                
                MetaContactGroup group = (MetaContactGroup)element;
                
                if(listModel.isGroupClosed(group)){
                    listModel.openGroup(group);
                }
                else{
                    listModel.closeGroup(group);
                }
            }
        }
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {        
        // Select the contact under the right button click.
		if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
			this.contactList.setSelectedIndex
				(contactList.locationToIndex(e.getPoint()));
		// Open message window, right button menu or contact info when 
        // mouse is pressed. Distinguish on which component was pressed 
        // the mouse and make the appropriate work.
        if (this.contactList.getSelectedValue() instanceof MetaContact){        		
            MetaContact contact 
                = (MetaContact) this.contactList.getSelectedValue();
            
            int selectedIndex = this.contactList.getSelectedIndex();
            
			ContactListCellRenderer renderer = (ContactListCellRenderer) 
                this.contactList.getCellRenderer()
                    .getListCellRendererComponent(
							this.contactList,
                            contact, 
                            selectedIndex,
							true, true);
           
            Point selectedCellPoint 
                = this.contactList.indexToLocation(selectedIndex);
            
			int translatedX = e.getX() 
                - selectedCellPoint.x;

            int translatedY = e.getY() 
                - selectedCellPoint.y;
            
            //get the component under the mouse
			Component component 
                = renderer.getComponentAt(translatedX, translatedY);
			
			if (component instanceof JLabel) {
				if ((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) {
                    
                    //Left click on the contact label opens Chat window
					SwingUtilities.invokeLater(new RunMessageWindow(
							contact));
					
				} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {					
                    //Right click on the contact label opens Popup menu
					ContactRightButtonMenu popupMenu 
                        = new ContactRightButtonMenu(
							mainFrame, contact);
					
					SwingUtilities.convertPointToScreen(selectedCellPoint,
							renderer);
					
					popupMenu.setInvoker(this.contactList);
					
					popupMenu.setLocation(selectedCellPoint.x, 
							selectedCellPoint.y + renderer.getHeight());
					
					popupMenu.setVisible(true);
				}
			} else if (component instanceof JButton) {                
                //Click on the info button opens the info popup panel
				SwingUtilities.invokeLater
                    (new RunInfoWindow(selectedCellPoint, 
                            contact));
			} else if (component instanceof JPanel
			        && component.getName().equals("buttonsPanel")){
                JPanel panel = (JPanel)component;
                
			    int internalX = translatedX 
                                - (renderer.getWidth() 
                                        - panel.getWidth() - 2);
                int internalY = translatedY 
                                - (renderer.getHeight() 
                                        - panel.getHeight());
                                
                Component c = panel.getComponentAt(4, 4);
                
                if(c instanceof ContactProtocolButton){
                    
                    SwingUtilities.invokeLater
                        (new RunMessageWindow(contact,
                                ((ContactProtocolButton)c)
                                    .getProtocolContact()));
                }
            }
            
		}
	}

	public void mouseReleased(MouseEvent e) {

	}

    /**
     * Runs the chat window for the specified contact.
     * 
     * @author Yana Stamcheva
     */
	public class RunMessageWindow implements Runnable {

		private MetaContact contactItem;
        private Contact protocolContact;

		public RunMessageWindow(MetaContact contactItem) {
			this.contactItem = contactItem;
            this.protocolContact 
                = contactItem.getDefaultContact();
		}
        
        public RunMessageWindow(MetaContact contactItem,
                                Contact protocolContact){
            this.contactItem = contactItem;
            this.protocolContact = protocolContact;
        }

		public void run() {
			PresenceStatus contactStatus 
     			= ((ContactListModel)contactList.getModel())
     				.getMetaContactStatus(this.contactItem);
			
            if(!Constants.TABBED_CHAT_WINDOW){                
                //If in mode "open all messages in new window"                
	    			if (contactMsgWindows.containsKey(this.contactItem)) {    			    
	                    /*
	                     * If a chat window for this contact is already opened
	                     * show it. 
	    			     */
	    				ChatWindow msgWindow = (ChatWindow) contactMsgWindows
	    						.get(this.contactItem);
	    
	    				if (msgWindow.getExtendedState() == JFrame.ICONIFIED)
	    					msgWindow.setExtendedState(JFrame.NORMAL);
	    				
	                msgWindow.setVisible(true);                    
	    			} else {
	                /*
	                 * If there's no chat window for the contact
	                 * create it and show it. 
	                 */
			        ChatWindow msgWindow = new ChatWindow(mainFrame);
	          
	                contactMsgWindows.put(this.contactItem, msgWindow);
	                
	                msgWindow.addChat(this.contactItem, 
                            contactStatus, protocolContact);
	                    
	                msgWindow.pack();
	                
	    				msgWindow.setVisible(true);
	    
	    				msgWindow.requestFocusInCurrentChat();
	    			}
            }
            else{            	
                // If in mode "group messages in one chat window"                
                if(tabbedChatWindow == null){                    
                    // If there's no open chat window
                    tabbedChatWindow = new ChatWindow(mainFrame);
                     
                    tabbedChatWindow.addWindowListener(new WindowAdapter(){                        
                        public void windowClosing(WindowEvent e) {                        		
                            tabbedChatWindow = null;
                        }
                    });
                }                
                /*
                 * Get the hashtable containg all tabs and corresponding 
                 * chat panels.
                 */
                Hashtable contactTabsTable 
                    = tabbedChatWindow.getContactTabsTable();

                if(contactTabsTable.get(this.contactItem.getMetaUID()) 
                        == null){
                    
                    // If there's no open tab for the given contact.                    
                    ChatPanel chatPanel = tabbedChatWindow.addChatTab(this.contactItem,
                    		contactStatus, protocolContact);
                    
                    tabbedChatWindow.setCurrentChatPanel(chatPanel);
                    
                    if(tabbedChatWindow.getTabCount() > 1)
                        tabbedChatWindow.setSelectedContactTab(this.contactItem);
                    
                    if (tabbedChatWindow.getExtendedState() == JFrame.ICONIFIED)
                        tabbedChatWindow.setExtendedState(JFrame.NORMAL);
                    
                    tabbedChatWindow.setVisible(true);
    
                    tabbedChatWindow.requestFocusInCurrentChat();
                }
                else{
                    //If a tab for the given contact already exists.
                    if(tabbedChatWindow.getTabCount() > 1){                        
                        tabbedChatWindow.setSelectedContactTab(this.contactItem);
                    }
                    
                    if (tabbedChatWindow.getExtendedState() == JFrame.ICONIFIED)
                        tabbedChatWindow.setExtendedState(JFrame.NORMAL);
                    
                    tabbedChatWindow.setVisible(true);
                    
                    tabbedChatWindow.requestFocusInCurrentChat();
                }
            }
		}
	}
	
    /**
     * Runs the info window for the specified contact at the 
     * appropriate position.
     * 
     * @author Yana Stamcheva
     */
	private class RunInfoWindow implements Runnable {

		private MetaContact contactItem;

		private Point p;
		
		private RunInfoWindow(Point p, MetaContact contactItem) {
		
			this.p = p;
			this.contactItem = contactItem;
		}

		public void run() {

			ContactInfoPanel contactInfoPanel 
				= new ContactInfoPanel(mainFrame, contactItem);

			SwingUtilities.convertPointToScreen(p, contactList);

			// TODO: to calculate popup window posititon properly.
			contactInfoPanel.setPopupLocation(p.x - 140, p.y - 15);
			
			contactInfoPanel.setVisible(true);														
									
			contactInfoPanel.requestFocusInWindow();
		}
	}
   
    /**
     * When message is received determines whether to open a new chat
     * window or chat window tab, or to indicate that a message is received
     * from a contact which already has an open chat. When the chat is found 
     * shows the message in the appropriate chat panel.
     */
    public void messageReceived(MessageReceivedEvent evt) {
        MetaContact metaContact
            = mainFrame.getContactList()
            		.findMetaContactByContact(evt.getSourceContact());
        
        PresenceStatus contactStatus
        		= ((ContactListModel)this.contactList.getModel())
        			.getMetaContactStatus(metaContact);
        
        Contact protocolContact
            = evt.getSourceContact();
                
        if(!Constants.TABBED_CHAT_WINDOW){                
            //If in mode "open all messages in new window"                
            if (contactMsgWindows.containsKey(metaContact)) {                  
                /*
                 * If a chat window for this contact is already opened
                 * show it. 
                 */
                ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                        .get(metaContact);

                msgWindow.getCurrentChatPanel().getConversationPanel()
                    .processMessage(evt.getSourceContact().getDisplayName(),
                            evt.getTimestamp(), 
                            ChatMessage.INCOMING_MESSAGE, 
                            evt.getSourceMessage().getContent());
                
                msgWindow.setVisible(true);                    
            } else {
                /*
                 * If there's no chat window for the contact
                 * create it and show it. 
                 */
                ChatWindow msgWindow = new ChatWindow(mainFrame);
          
                contactMsgWindows.put(metaContact, msgWindow);

                msgWindow.addChat(metaContact, contactStatus,
                        protocolContact);
                
                msgWindow.getCurrentChatPanel().getConversationPanel()
                    .processMessage(evt.getSourceContact().getDisplayName(), 
                            evt.getTimestamp(), ChatMessage.INCOMING_MESSAGE, 
                            evt.getSourceMessage().getContent());
                
                msgWindow.pack();
                
                msgWindow.setVisible(true);                  
            }
        }
        else{            
            // If in mode "group messages in one chat window"                
            if(tabbedChatWindow == null){                    
                // If there's no open chat window
                tabbedChatWindow = new ChatWindow(mainFrame);
                 
                tabbedChatWindow.addWindowListener(new WindowAdapter(){
                    
                    public void windowClosing(WindowEvent e) {
                        tabbedChatWindow = null;
                    }
                });
            }                
            /*
             * Get the hashtable containg all tabs and corresponding 
             * chat panels.
             */
            Hashtable contactTabsTable 
                = tabbedChatWindow.getContactTabsTable();
            ChatPanel chatPanel;
            
            if(contactTabsTable.get(metaContact.getMetaUID()) 
                    == null){                
                // If there's no open tab for the given contact.                    
                chatPanel 
                    = tabbedChatWindow.addChatTab(metaContact, contactStatus,
                            protocolContact);
                
                chatPanel.getConversationPanel()
                    .processMessage(evt.getSourceContact().getDisplayName(), 
                            evt.getTimestamp(), ChatMessage.INCOMING_MESSAGE, 
                            evt.getSourceMessage().getContent());
                
                tabbedChatWindow.setVisible(true);                
            }
            else{
                chatPanel = tabbedChatWindow.getChatPanel(metaContact); 
                chatPanel.getConversationPanel()
                    .processMessage(evt.getSourceContact().getDisplayName(), 
                            evt.getTimestamp(), ChatMessage.INCOMING_MESSAGE, 
                            evt.getSourceMessage().getContent());
                
                tabbedChatWindow.setVisible(true);
            }            
            
            if(tabbedChatWindow.getTabCount() > 1){
                tabbedChatWindow.highlightTab(metaContact);
            }
        }
        Constants.getDefaultAudio().play();
    }

    /**
     * Shows message in the chat conversation panel when
     * delivered.
     */
    public void messageDelivered(MessageDeliveredEvent evt) {
        
        Message msg = evt.getSourceMessage();
        Hashtable waitToBeDelivered = this.mainFrame.getWaitToBeDeliveredMsgs();
        String msgUID = msg.getMessageUID();
        
        if(waitToBeDelivered.containsKey(msgUID)){
            ChatPanel chatPanel = (ChatPanel)waitToBeDelivered.get(msgUID);
        
            JEditorPane messagePane = chatPanel.getWriteMessagePanel()
                .getEditorPane(); 
            
            ProtocolProviderService protocolProvider 
            	= evt.getDestinationContact().getProtocolProvider();
            
            chatPanel.getConversationPanel().processMessage(
                    this.mainFrame.getDefaultAccount(protocolProvider),
                    evt.getTimestamp(),                                 
                    ChatMessage.OUTGOING_MESSAGE,
                    msg.getContent());
    
            messagePane.setText("");
    
            messagePane.requestFocus();
        }
    }
    
    /**
     * Shows message to the user when message delivery failed.
     */
    public void messageDeliveryFailed(MessageDeliveryFailedEvent evt) {
    	String msg = Messages.getString("msgNotDelivered", 
    					evt.getDestinationContact().getDisplayName());
    	String title = Messages.getString("msgDeliveryFailure");
    	
        JOptionPane.showMessageDialog(this, msg, title, 
        		JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Informs the user what is the typing state of his chat contacts.
     */
    public void typingNotificationReceifed(TypingNotificationEvent evt) {
        if(typingTimer.isRunning()) 
            typingTimer.stop();
        
        String notificationMsg = "";
        
        String contactName = this.mainFrame.getContactList()
            .findMetaContactByContact(evt.getSourceContact())
                    .getDisplayName() + " ";
        
        if(contactName.equals("")){
            contactName = Messages.getString("unknown") + " ";
        }
        
        int typingState = evt.getTypingState();
        MetaContact metaContact
        		= mainFrame.getContactList()
        			.findMetaContactByContact(evt.getSourceContact());
               
        
        if(typingState == OperationSetTypingNotifications.STATE_TYPING){
            notificationMsg = Messages.getString("contactTyping", contactName);
        }
        else if(typingState == OperationSetTypingNotifications.STATE_PAUSED){
            notificationMsg = Messages.getString("contactPausedTyping", contactName);
            typingTimer.setMetaContact(metaContact);
            typingTimer.start();
        }
        else if(typingState == OperationSetTypingNotifications.STATE_STOPPED){
            notificationMsg = "";            
        }
        else if(typingState == OperationSetTypingNotifications.STATE_STALE){
            notificationMsg = Messages.getString("contactTypingStateStale");
        }
        else if(typingState == OperationSetTypingNotifications.STATE_UNKNOWN){
            //TODO: Implement state unknown
        }        
        this.setChatNotificationMsg(metaContact, notificationMsg);
    }
    
    /**
     * Sets the typing notification message at the appropriate chat.
     * 
     * @param metaContact The meta contact.
     * @param notificationMsg The typing notification message.
     */
    public void setChatNotificationMsg(MetaContact metaContact, 
                                    String notificationMsg){
        if(!Constants.TABBED_CHAT_WINDOW){                
            //If in mode "open all messages in new window"                
            if (contactMsgWindows.containsKey(metaContact)) {
                ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                        .get(metaContact);
                msgWindow.getChatPanel(metaContact)
                    .getSendPanel().setTypingStatus(notificationMsg);
            } 
        }
        else if(tabbedChatWindow != null){
            Hashtable contactTabsTable 
                = tabbedChatWindow.getContactTabsTable();
            
            if(contactTabsTable.get(metaContact.getMetaUID()) 
                    != null){
                
                tabbedChatWindow.getChatPanel(metaContact)
                    .getSendPanel().setTypingStatus(notificationMsg);
            }
        }
    }
    
    /**
     * Updates the status of the given metacontact in all opened
     * chats containing this contact.
     * 
     * @param metaContact
     */
	public void updateChatContactStatus(MetaContact metaContact) {
		
		ContactListModel listModel 
			= (ContactListModel)this.getContactList().getModel();
		
		if(!Constants.TABBED_CHAT_WINDOW){
            if (contactMsgWindows.containsKey(metaContact)) {
                ChatWindow msgWindow = (ChatWindow) contactMsgWindows
                        .get(metaContact);
                msgWindow.getCurrentChatPanel()
                    .updateContactStatus(listModel
                            .getMetaContactStatus(metaContact));
            }
        }
        else if(tabbedChatWindow != null){
            
            Hashtable contactTabsTable 
                = tabbedChatWindow.getContactTabsTable();
            
            ChatPanel chatPanel 
                = (ChatPanel)contactTabsTable.get(metaContact.getMetaUID());
            
            if(chatPanel != null){
                if(tabbedChatWindow.getTabCount() > 0){
                    tabbedChatWindow.setTabIcon(metaContact,
                            listModel.getMetaContactStatusIcon(metaContact));
                }	
        		chatPanel.updateContactStatus(
        				listModel.getMetaContactStatus(metaContact));
            }
        }
	}

	public ChatWindow getTabbedChatWindow() {
		return tabbedChatWindow;
	}

	public void setTabbedChatWindow(ChatWindow tabbedChatWindow) {
		this.tabbedChatWindow = tabbedChatWindow;
	}
	
	private class RunMessageWindowAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e){
        	MetaContact contact = (MetaContact)getContactList().getSelectedValue();
        	SwingUtilities.invokeLater(new RunMessageWindow(
					contact));
        }
    };
    
    /**
     * The TypingTimer is started after a PAUSED typing notification
     * is received. It waits 5 seconds and if no other typing event 
     * occurs removes the PAUSED message from the chat status panel.
     */
    private class TypingTimer extends Timer {
        
        private MetaContact metaContact;
        
        public TypingTimer(){
            //Set delay
            super(5*1000, null);            
            
            this.addActionListener(new TimerActionListener());
        }
        
        private class TimerActionListener implements ActionListener {
            public void actionPerformed(ActionEvent e){
                setChatNotificationMsg(metaContact, "");
            }
        }
        
        private void setMetaContact(MetaContact metaContact){
            this.metaContact = metaContact;
        }
    }    
}