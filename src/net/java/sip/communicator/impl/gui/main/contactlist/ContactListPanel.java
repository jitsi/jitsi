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
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatMessage;
import net.java.sip.communicator.impl.gui.main.message.ChatPanel;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.event.MessageDeliveredEvent;
import net.java.sip.communicator.service.protocol.event.MessageDeliveryFailedEvent;
import net.java.sip.communicator.service.protocol.event.MessageListener;
import net.java.sip.communicator.service.protocol.event.MessageReceivedEvent;
import net.java.sip.communicator.service.protocol.event.TypingNotificationEvent;
import net.java.sip.communicator.service.protocol.event.TypingNotificationsListener;

/**
 * Creates the contactlist panel. The contactlist panel not only contains the contact list
 * but it has the role of a message dispatcher by implementing the MessageListener.
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
	}

     public void initTree(MetaContactListService contactListService) {
            
            this.contactList = new ContactList(contactListService);
            
            this.contactList.addMouseListener(this);            
            
            this.treePanel.add(contactList, BorderLayout.NORTH);
            
            this.addKeyListener(new CListKeySearchListener(this.contactList));
        }
        
        public ContactList getContactList(){
            
            return this.contactList;
        }
        
	public void mouseClicked(MouseEvent e) {
		
        //Expand and collapse groups on double click.
        if(e.getClickCount() > 1){
            
            int selectedIndex 
                = this.contactList.locationToIndex(e.getPoint());
            
            ContactListModel listModel = (ContactListModel)this.contactList.getModel();
            
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
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
        
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
                
				if ((e.getModifiers() & InputEvent.BUTTON1_MASK) 
                            == InputEvent.BUTTON1_MASK) {
                    
                    //Left click on the contact label opens Chat window
					SwingUtilities.invokeLater(new RunMessageWindow(
							contact));
					
				} else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) 
                            == InputEvent.BUTTON3_MASK) {
				    
                    //Right click on the contact label opens Popup menu
					ContactRightButtonMenu popupMenu 
                        = new ContactRightButtonMenu(
							mainFrame, contact);

					popupMenu.setInvoker(this.contactList);

					popupMenu.setLocation(popupMenu.getPopupLocation());

					popupMenu.setVisible(true);
				}
			} else if (component instanceof JButton) {
				
                //Click on the info button opens the info popup panel
				SwingUtilities.invokeLater
                    (new RunInfoWindow(selectedCellPoint, 
                            contact));
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
	private class RunMessageWindow implements Runnable {

		private MetaContact contactItem;

		private RunMessageWindow(MetaContact contactItem) {
			this.contactItem = contactItem;
		}

		public void run() {

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
                
                msgWindow.addChat(this.contactItem);
                    
                msgWindow.pack();
                
    				msgWindow.setVisible(true);
    
    				msgWindow.getWriteMessagePanel()
                        .getEditorPane().requestFocus();
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

                if(contactTabsTable.get(this.contactItem.getDisplayName()) 
                        == null){
                    
                    // If there's no open tab for the given contact.                    
                    tabbedChatWindow.addChatTab(this.contactItem);
                    
                    if(tabbedChatWindow.getTabCount() > 1)
                        tabbedChatWindow.setSelectedContactTab(this.contactItem);
                    
                    if (tabbedChatWindow.getExtendedState() == JFrame.ICONIFIED)
                        tabbedChatWindow.setExtendedState(JFrame.NORMAL);
                    
                    if(!tabbedChatWindow.isVisible())
                        tabbedChatWindow.setVisible(true);
    
                    tabbedChatWindow.getWriteMessagePanel()
                        .getEditorPane().requestFocus();
                }
                else{
                    //If a tab fot the given contact already exists.
                    if(tabbedChatWindow.getTabCount() > 1){                        
                        tabbedChatWindow.setSelectedContactTab(this.contactItem);
                    }
                    
                    if (tabbedChatWindow.getExtendedState() == JFrame.ICONIFIED)
                        tabbedChatWindow.setExtendedState(JFrame.NORMAL);
                    
                    tabbedChatWindow.setVisible(true);
                    
                    tabbedChatWindow.getWriteMessagePanel()
                        .getEditorPane().requestFocus();
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

			ContactInfoPanel contactInfoPanel = new ContactInfoPanel(mainFrame, contactItem);

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
     * from a contact which already has chat opened. When chat is found show 
     * the message in the appropriate chat panel.
     */
    public void messageReceived(MessageReceivedEvent evt) {
        
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(evt.getTimestamp());
        
        MetaContact metaContact
            = mainFrame.getContactList().findMetaContactByContact(evt.getSourceContact());
        
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
                            calendar, 
                            ChatMessage.INCOMING_MESSAGE, 
                            evt.getSourceMessage().getContent());
                
                if(!msgWindow.isVisible())
                    msgWindow.setVisible(true);                    
            } else {
                /*
                 * If there's no chat window for the contact
                 * create it and show it. 
                 */
                ChatWindow msgWindow = new ChatWindow(mainFrame);
          
                contactMsgWindows.put(metaContact, msgWindow);

                msgWindow.addChat(metaContact);
                
                msgWindow.getCurrentChatPanel().getConversationPanel()
                    .processMessage(evt.getSourceContact().getDisplayName(), 
                            calendar, ChatMessage.INCOMING_MESSAGE, 
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

            if(contactTabsTable.get(metaContact.getDisplayName()) 
                    == null){                
                // If there's no open tab for the given contact.                    
                tabbedChatWindow.addChatTab(metaContact);
 
                tabbedChatWindow.getCurrentChatPanel().getConversationPanel()
                    .processMessage(evt.getSourceContact().getDisplayName(), 
                            calendar, ChatMessage.INCOMING_MESSAGE, 
                            evt.getSourceMessage().getContent());
                
                if(!tabbedChatWindow.isVisible())
                    tabbedChatWindow.setVisible(true);
                
                tabbedChatWindow.getWriteMessagePanel()
                    .getEditorPane().requestFocus();
            }
            else{
                tabbedChatWindow.getChatPanel(metaContact).getConversationPanel()
                    .processMessage(evt.getSourceContact().getDisplayName(), 
                            calendar, ChatMessage.INCOMING_MESSAGE, 
                            evt.getSourceMessage().getContent());
                
                if(!tabbedChatWindow.isVisible())
                    tabbedChatWindow.setVisible(true);
                
                tabbedChatWindow.getWriteMessagePanel()
                    .getEditorPane().requestFocus();
            }            
            if(tabbedChatWindow.getTabCount() > 1){
                // If a tab fot the given contact already exists.
                tabbedChatWindow.highlightTab(metaContact);
            }
        }
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
            
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(evt.getTimestamp());
            
            chatPanel.getConversationPanel().processMessage(
                    this.mainFrame.getAccount().getIdentifier(),
                    calendar,                                 
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
        //TODO: Show message to the user when message delivery failed.
    }

    /**
     * Informs the user what is the typing state of his chat contacts.
     */
    public void typingNotificationReceifed(TypingNotificationEvent evt) {
        String notificationMsg = "";
        
        notificationMsg 
            += this.mainFrame.getContactList().findMetaContactByContact(evt.getSourceContact())
                    .getDisplayName() + " ";
        
        if(notificationMsg.equals("")){
            notificationMsg += Messages.getString("unknown") + " ";
        }
        
        int typingState = evt.getTypingState();
        MetaContact metaContact
        = mainFrame.getContactList().findMetaContactByContact(evt.getSourceContact());
        
        if(typingState == OperationSetTypingNotifications.STATE_TYPING){
            notificationMsg += Messages.getString("contactTyping");
        }
        else if(typingState == OperationSetTypingNotifications.STATE_PAUSED){
            notificationMsg += Messages.getString("contactPausedTyping");
        }
        else if(typingState == OperationSetTypingNotifications.STATE_STOPPED){
            notificationMsg = "";
        }
        else if(typingState == OperationSetTypingNotifications.STATE_STALE){
            notificationMsg += Messages.getString("contactTypingStateStale");
        }
        else if(typingState == OperationSetTypingNotifications.STATE_UNKNOWN){
            //TODO: Implement state unknown
        }
        
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
            
            if(contactTabsTable.get(metaContact.getDisplayName()) 
                    != null){
                
                tabbedChatWindow.getChatPanel(metaContact)
                    .getSendPanel().setTypingStatus(notificationMsg);
            }
        }
    }
}