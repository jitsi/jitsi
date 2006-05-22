/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.LayoutFocusTraversalPolicy;
import javax.swing.SwingUtilities;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

/**
 * The ChatPanel is the panel, where users can write
 * and send messages, view received messages.
 * A ChatPanel is created for a contact or for a
 * group of contacts in case of a chat conference. There
 * is always one default contact for the chat, which is the
 * first contact which was added to the chat.
 * When chat is in mode "open all messages in new window",
 * each ChatPanel corresponds to a ChatWindow. When 
 * chat is in mode "group all messages in one chat window",
 * each ChatPanel corresponds to a tab in the ChatWindow.
 * 
 * @author Yana Stamcheva
 */
public class ChatPanel extends JPanel {
    
    private JSplitPane topSplitPane 
    		= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    
    private JSplitPane messagePane 
        = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
       
    private ChatConversationPanel conversationPanel;
    
    private ChatWritePanel writeMessagePanel;
    
    private ChatConferencePanel chatConferencePanel 
        = new ChatConferencePanel();
    
    private ChatSendPanel sendPanel;
    
    private Vector chatContacts = new Vector();
    
    private ChatWindow chatWindow;
    
    private OperationSetBasicInstantMessaging imOperationSet;
    
    private OperationSetTypingNotifications tnOperationSet;
    
    private Contact protocolContact;
    
    /**
     * Creates a chat panel which is added to the 
     * given chat window.
     * 
     * @param chatWindow The parent window of this 
     * chat panel.
     */
    public ChatPanel(ChatWindow chatWindow, 
            Contact protocolContact){
        
        super(new BorderLayout());
        
        this.chatWindow = chatWindow;
        this.protocolContact = protocolContact;
        this.imOperationSet = this.chatWindow.getMainFrame()
                                .getProtocolIM(protocolContact
                                        .getProtocolProvider());
        this.tnOperationSet = this.chatWindow.getMainFrame()
                                .getTypingNotifications(protocolContact
                                        .getProtocolProvider());
        
        this.conversationPanel = new ChatConversationPanel(this);
        
        this.sendPanel = new ChatSendPanel(this);
        
        this.writeMessagePanel = new ChatWritePanel(this);
        
        this.topSplitPane.setResizeWeight(1.0D);
        this.messagePane.setResizeWeight(1.0D);
        this.chatConferencePanel
        		.setPreferredSize(new Dimension(120, 100));        
        this.chatConferencePanel
        		.setMinimumSize(new Dimension(120, 100));
        this.writeMessagePanel
        		.setPreferredSize(new Dimension(400, 100));        
        this.writeMessagePanel
        		.setMinimumSize(new Dimension(400, 100));
        
        this.init();
        
        addComponentListener(new TabSelectionFocusGainListener());
    }
    
    /**
     * Initialize the chat panel.
     */
    private void init(){        
        this.topSplitPane.setOneTouchExpandable(true);
        
        topSplitPane.setLeftComponent(conversationPanel);
        topSplitPane.setRightComponent(chatConferencePanel);
        
        this.messagePane.setTopComponent(topSplitPane);
        this.messagePane.setBottomComponent(writeMessagePanel);
        
        this.add(messagePane, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.SOUTH);       
    }
    
    /**
     * Adds a new MetaContact to this chat panel.
     * 
     * @param contactItem The MetaContact to add.
     */
    public void addContactToChat(MetaContact contactItem,
                                PresenceStatus status){        
        this.chatContacts.add(contactItem);
        
        this.chatConferencePanel.addContactToChat(contactItem, status);
        
        this.sendPanel.addProtocolContacts(contactItem);
        
        this.sendPanel.setSelectedProtocolContact(this.protocolContact);
    }
    
    /**
     * Adds a new MetaContact to this chat panel.
     * 
     * @param contactItem The MetaContact to add.
     */
    public void addContactToChat(MetaContact contactItem){
        
        this.chatContacts.add(contactItem);
        
        this.chatConferencePanel.addContactToChat(contactItem);
        
        this.sendPanel.addProtocolContacts(contactItem);
    }

    /**
     * Removes a MetaContact from the chat.
     * 
     * @param contactItem The MetaContact to remove.
     */
    public void removeContactFromChat (MetaContact contactItem){
        this.chatContacts.remove(contactItem);
    }
    
    /**
     * Returns all contacts for this chat.
     * 
     * @return A Vector containing all MetaContact-s 
     * for the chat.
     */
    public Vector getChatContacts() {
        return chatContacts;
    }

    /**
     * Sets all contacts for this chat. This is in the 
     * case when we creates a conference chat.
     * 
     * @param chatContacts A Vector of MetaContact-s.
     */
    public void setChatContacts(Vector chatContacts) {
        this.chatContacts = chatContacts;
    }
    
    /**
     * Updates the contact status in the contact info panel.
     * 
     * @param status The presence status of the contact.
     */
    public void updateContactStatus(PresenceStatus status){
    		this.chatConferencePanel.updateContactStatus(status);
    }
    /**
     * Returns the panel that contains the "write" editor 
     * pane of this chat.
     * 
     * @return The ChatWritePanel.
     */
    public ChatWritePanel getWriteMessagePanel() {
        return writeMessagePanel;
    }
    

    /**
     * Returns the panel that contains the conversation.
     * 
     * @return The ChatConversationPanel.
     */
    public ChatConversationPanel getConversationPanel() {
        return conversationPanel;
    }

    /**
     * Returns the default contact for the chat. The case of conference 
     * is not yet implemented and for now it returns the first contact.
     * 
     * @return The default contact for the chat.
     */
    public MetaContact getDefaultContact(){
        return (MetaContact)this.getChatContacts().get(0);
    }

    /**
     * Returns the chat window, where this chat panel
     * is located.
     * 
     * @return ChatWindow The chat window, where this 
     * chat panel is located.
     */
    public ChatWindow getChatWindow() {
        return chatWindow;
    }

    /**
     * Returns the instant messaging operation set for 
     * this chat panel.
     * 
     * @return OperationSetBasicInstantMessaging The instant 
     * messaging operation set for this chat panel.
     */
    public OperationSetBasicInstantMessaging getImOperationSet() {
        return imOperationSet;
    }

    /**
     * Sets the instant messaging operation set for 
     * this chat panel.
     * @param imOperationSet The operation set to be set.
     */
    public void setImOperationSet
    		(OperationSetBasicInstantMessaging imOperationSet) {
        this.imOperationSet = imOperationSet;
    }

    /**
     * Returns the typing notifications operation set for 
     * this chat panel.
     * 
     * @return OperationSetTypingNotifications The typing
     * notifications operation set for this chat panel.
     */
    public OperationSetTypingNotifications getTnOperationSet() {
        return tnOperationSet;
    }

    /**
     * Sets the typing notifications operation set for 
     * this chat panel.
     * @param tnOperationSet The operation set to be set.
     */
    public void setTnOperationSet
            (OperationSetTypingNotifications tnOperationSet) {
        this.tnOperationSet = tnOperationSet;
    }
    
    /**
     * Returns the chat send panel.
     * @return ChatSendPanel The chat send panel.
     */
    public ChatSendPanel getSendPanel() {
        return sendPanel;
    }
    
    public class TabSelectionFocusGainListener implements ComponentListener {

    	public TabSelectionFocusGainListener() {
    		super();
    	}

    	public void componentResized(ComponentEvent e) {
    	}

    	public void componentMoved(ComponentEvent e) {
    	}

    	public void componentShown(ComponentEvent e) {
    		Component component = e.getComponent();
    		Container parent = component.getParent();
    		if ( parent instanceof JTabbedPane ) {    			
    			JTabbedPane tabbedPane = (JTabbedPane)parent;
    			if ( tabbedPane.getSelectedComponent() == component ){
    				SwingUtilities.invokeLater(new Runnable(){
    					public void run(){
    						getChatWindow().setTitle(getDefaultContact()
    	                            .getDisplayName());
    	                    
    	                    getChatWindow().setCurrentChatPanel(ChatPanel.this);
    	                    
    	                    getWriteMessagePanel()
    	                    	.getEditorPane().requestFocus();
    					}
    				});    				
    			}
    		}
    	}

    	public void componentHidden(ComponentEvent e) {
    	}
    }

    public Contact getProtocolContact() {
        return protocolContact;
    }
    
    public void setProtocolContact(Contact protocolContact) {
        this.protocolContact = protocolContact;
    }
}
