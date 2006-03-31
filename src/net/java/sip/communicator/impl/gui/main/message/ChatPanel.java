/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;

/**
 * Chat panel for one or group of contacts.
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
    
    private int tabIndex;
    
    /**
     * Creates a chat panel which is added to the given chat window.
     * 
     * @param chatWindow The parent window of this chat panel.
     */
    public ChatPanel(ChatWindow chatWindow, 
            OperationSetBasicInstantMessaging imOperationSet){
        
        super(new BorderLayout());
        
        this.chatWindow = chatWindow;
        this.imOperationSet = imOperationSet;
        
        conversationPanel = new ChatConversationPanel(this);
        
        sendPanel = new ChatSendPanel(this);
        
        writeMessagePanel = new ChatWritePanel(this);
                
        this.init();
    }
    
    /**
     * Initialize the chat panel.
     */
    private void init(){
        
        this.topSplitPane.setDividerLocation(370);
        this.topSplitPane.setOneTouchExpandable(true);
        
        topSplitPane.add(conversationPanel);
        topSplitPane.add(chatConferencePanel);
        
        this.messagePane.setDividerLocation(250);
        
        this.messagePane.add(topSplitPane);
        this.messagePane.add(writeMessagePanel);
        
        this.add(messagePane, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.SOUTH);       
    }
    
    /**
     * Adds a new MetaContact to this chat panel.
     * 
     * @param contactItem The MetaContact to add.
     */
    public void addContactToChat (MetaContact contactItem){     
        
        this.chatContacts.add(contactItem);
        
        this.chatConferencePanel.addContactToChat(contactItem);
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
     * @return A Vector containing all MetaContact-s for the chat.
     */
    public Vector getChatContacts() {
        return chatContacts;
    }

    /**
     * Sets all contacts for this chat. This is in the case when we 
     * creates a conference chat.
     * 
     * @param chatContacts A Vector of MetaContact-s.
     */
    public void setChatContacts(Vector chatContacts) {
        this.chatContacts = chatContacts;
    }
    
    /**
     * Returns the panel that contains the "write" editor pane of this chat.
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
     * Returns the tab index of this chat panel in case of tabbed chat
     * window.
     * 
     * @return The tab index of this chat panel.
     */
    public int getTabIndex() {
        return tabIndex;
    }

    /**
     * Sets the tab index of this chat panel in case of tabbed chat 
     * window.
     * 
     * @param tabIndex The tab index, where the panel will be added in the
     * tabbedPane.
     */
    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public ChatWindow getChatWindow() {
        return chatWindow;
    }

    public OperationSetBasicInstantMessaging getImOperationSet() {
        return imOperationSet;
    }

    public void setImOperationSet(OperationSetBasicInstantMessaging imOperationSet) {
        this.imOperationSet = imOperationSet;
    }

    public ChatSendPanel getSendPanel() {
        return sendPanel;
    }

}
