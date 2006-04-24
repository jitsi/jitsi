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
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.customcontrols.tabbedPane.CloseListener;
import net.java.sip.communicator.impl.gui.main.customcontrols.tabbedPane.SIPCommTabbedPane;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.PresenceStatus;

/**
 * The chat window is the place, where users 
 * can write and send messages, view received messages. 
 * The ChatWindow supports two modes of use: "Group all 
 * messages in one window" and "Open messages in new 
 * window". In the first case a TabbedPane is added in
 * the window, where each tab contains a ChatPanel. In
 * the second case the ChatPanel is added directly to 
 * the window. The ChatPanel itself contains all message 
 * containers and corresponds to a contact or a conference.
 * 
 * @author Yana Stamcheva
 */
public class ChatWindow extends JFrame{
	
    private ChatPanel currentChatPanel;
    
	private MenusPanel menusPanel;
	
	private MainFrame mainFrame;	
	
	private String windowTitle = "";
		
    private SIPCommTabbedPane chatTabbedPane = null;
    
    private Hashtable contactTabsTable = new Hashtable();
    
    /**
     * Creates a chat window.
     * 
     * @param mainFrame
     */
	public ChatWindow (MainFrame mainFrame){		
				
		this.mainFrame = mainFrame;		
        
		this.setSize(550, 450);
				
		this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));
		
		menusPanel = new MenusPanel(this);
        
		this.init();
        
        this.enableKeyActions();
	}
	
    /**
     * Initialize the chat window.
     */
	public void init (){
		this.getContentPane().add(menusPanel, BorderLayout.NORTH);
	}
	
	/**
     * Returns the main frame.
     * 
     * @return The parent window.
	 */
	public MainFrame getMainFrame() {
		return mainFrame;
	}

    /**
     * Sets the main frame.
     * 
     * @param mainFrame The parent window for this chat window.
     */
	public void setMainFrame(MainFrame mainFrame) {
		this.mainFrame = mainFrame;
	}

    /**
     * Returns the chat write message panel for the currently selected 
     * chat panel.
     * 
     * @return The ChatWritePanel for the currently selected 
     * chat panel.
     */
	public ChatWritePanel getWriteMessagePanel() {
		return this.currentChatPanel.getWriteMessagePanel();
	}

    /**
     * Returns the conversation panel for the currently selected 
     * chat panel.
     *  
     * @return The ChatConversationPanel for the currently selected
     * chat panel.
     */
	public ChatConversationPanel getConversationPanel() {
		return this.currentChatPanel.getConversationPanel();
	}    

    /**
     * Enables all key actions on this chat window. Closes 
     * tab or window when esc is pressed and changes tabs when 
     * Alt+Right/Left Arrow is pressed. 
     */
    private void enableKeyActions(){        
        AbstractAction close = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                if(chatTabbedPane.getTabCount() > 1){
                    removeContactTab(chatTabbedPane.getSelectedIndex());
                }
                else{
                    ChatWindow.this.dispose();
                    mainFrame.getTabbedPane().getContactListPanel()
                    		.setTabbedChatWindow(null);
                }
            }
        };
        
        AbstractAction changeTabForword = new AbstractAction(){   
            public void actionPerformed(ActionEvent e)
            {
                if(chatTabbedPane != null){ 
                    if(chatTabbedPane.getSelectedIndex() 
                            < chatTabbedPane.getTabCount() - 1){                    
	                    ChatWindow.this.chatTabbedPane
	                        .setSelectedIndex
	                            (chatTabbedPane.getSelectedIndex() + 1);
                    }
                    else{
                    	ChatWindow.this.chatTabbedPane
                        .setSelectedIndex(0);
                    }
                }
            }
        };
        
        AbstractAction changeTabBackword = new AbstractAction(){
            public void actionPerformed(ActionEvent e)
            {   
                if(chatTabbedPane != null){
                    if(chatTabbedPane.getSelectedIndex() != 0){                    
	                    ChatWindow.this.chatTabbedPane
	                        .setSelectedIndex
	                            (chatTabbedPane.getSelectedIndex() - 1);
                    }
                    else{
                    	ChatWindow.this.chatTabbedPane
                        .setSelectedIndex
                            (chatTabbedPane.getTabCount() - 1);
                    }
                }
            }
        };
        
        getRootPane().getActionMap().put("close", close);
        getRootPane().getActionMap().put("changeTabForword", changeTabForword);
        getRootPane().getActionMap().put("changeTabBackword", changeTabBackword);
        
        InputMap imap =
            this.getRootPane()
                .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        imap.put(KeyStroke.getKeyStroke
                (KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK), "changeTabForword");
        imap.put(KeyStroke.getKeyStroke
                (KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK), "changeTabBackword");
    }
    
    /**
     * Creates a ChatPanel for the given contact and 
     * adds it directly to the chat window.
     */
    public void addChat(MetaContact contact, 
    						PresenceStatus status){
        
        OperationSetBasicInstantMessaging contactIMOperationSet 
        = this.mainFrame.getProtocolIM
                (contact.getDefaultContact().getProtocolProvider());
        
        this.setCurrentChatPanel(new ChatPanel(this, contactIMOperationSet));
        
        this.currentChatPanel.addContactToChat(contact, status);
        
        this.getContentPane().add(this.currentChatPanel, 
                    BorderLayout.CENTER);
        
        //this.sendPanel.addProtocols(contactItem.getProtocolList());
        
        this.windowTitle += contact.getDisplayName() + " ";
        
        this.setTitle(this.windowTitle);    
    }
    
    /**
     * Creates a ChatPanel for the given contact and adds it to a tabbedPane.
     * 
     * @param contact The MetaContact added to the chat.
     */
    public void addChatTab(MetaContact contact, PresenceStatus status){
        
        OperationSetBasicInstantMessaging contactIMOperationSet 
        = this.mainFrame.getProtocolIM
                (contact.getDefaultContact().getProtocolProvider());
        
        if(chatTabbedPane == null){
            //Initialize the tabbed pane for the first time           
            
            this.setCurrentChatPanel(new ChatPanel(this, contactIMOperationSet));
            
            this.currentChatPanel.addContactToChat(contact, status);
            
            chatTabbedPane = new SIPCommTabbedPane(true);
            
            chatTabbedPane.addCloseListener(new CloseListener(){
                public void closeOperation(MouseEvent e){
                   
                    int selectedIndex = chatTabbedPane.getOverTabIndex();
                    
                    removeContactTab(selectedIndex);
                }
            });
            
            chatTabbedPane.addChangeListener(new ChangeListener(){

                public void stateChanged(ChangeEvent e) {
                    
                    ChatPanel chatPanel = (ChatPanel)chatTabbedPane
                                            .getSelectedComponent();

                    if(chatPanel != null){
                            
                            setTitle(chatPanel.getDefaultContact()
                                    .getDisplayName());
                            
                            setCurrentChatPanel(chatPanel);
                        }
                    }
                });
            
            this.getContentPane().add(  this.currentChatPanel, 
                                        BorderLayout.CENTER);
            
            //Set the tab index even it's not yet shown in tabbed pane
            this.currentChatPanel.setTabIndex(0);
            
            this.contactTabsTable.put(contact.getDisplayName(),
                                 currentChatPanel);
            
            this.setTitle(contact.getDisplayName());
        }
        else{           
        		PresenceStatus defaultStatus 
    				=  contact.getDefaultContact().getPresenceStatus();
        		
            if(chatTabbedPane.getTabCount() > 0){                
                //The tabbed pane contains already tabs.
                this.setCurrentChatPanel(new ChatPanel(this, contactIMOperationSet));
                
                this.currentChatPanel.addContactToChat(contact, status);
                
                chatTabbedPane.addTab(contact.getDisplayName(),
					new ImageIcon(Constants.getStatusIcon(defaultStatus)),	
					currentChatPanel);
                
                chatTabbedPane.getParent().validate();
                
                //Set the tab index to the newly added chat panel
                this.currentChatPanel
                    .setTabIndex(chatTabbedPane.getTabCount() - 1);
                
                this.contactTabsTable.put(contact.getDisplayName(),
                                    currentChatPanel);
            }
            else{
            		PresenceStatus currentContactStatus 
            			= currentChatPanel.getDefaultContact()
            				.getDefaultContact().getPresenceStatus();
                //Add the first two tabs to the tabbed pane.                
                chatTabbedPane.addTab
                		(currentChatPanel.getDefaultContact().getDisplayName(),
                    new ImageIcon(Constants.getStatusIcon
                    			(currentContactStatus)),
                    currentChatPanel);
                               
                this.setCurrentChatPanel
                		(new ChatPanel(this, contactIMOperationSet));
                
                this.currentChatPanel.addContactToChat(contact, status);
                
                chatTabbedPane.addTab(  contact.getDisplayName(),
                		new ImageIcon(Constants.getStatusIcon(defaultStatus)),
                    currentChatPanel);
                
                currentChatPanel
                    .setTabIndex(chatTabbedPane.getTabCount() - 1);
                
                this.contactTabsTable.put(contact.getDisplayName(),
                                currentChatPanel);
            }
            
            this.getContentPane().add(chatTabbedPane, BorderLayout.CENTER);
            this.getContentPane().validate();
        }
    }
    
    /**
     * Selects the chat tab which corresponds to the given MetaContact.
     * 
     * @param contact The MetaContact to select.
     */
    public void setSelectedContactTab(MetaContact contact){
        
        if(this.contactTabsTable != null && !this.contactTabsTable.isEmpty()){
                        
            int selectedIndex = ((ChatPanel)this.contactTabsTable
                                    .get(contact.getDisplayName())).getTabIndex();
            
            this.chatTabbedPane.setSelectedIndex(selectedIndex);
        }
    }    
    
    /**
     * Removes the tab with the given index.
     * 
     * @param index Tab index.
     */
    public void removeContactTab(int index){

        String title = chatTabbedPane.getTitleAt(index);
        
        if(title != null){
	        if(chatTabbedPane.getTabCount() > 1)	        		
	        		this.contactTabsTable.remove(title);
	        
	        Enumeration contactTabs = this.contactTabsTable.elements();
	        
	        while(contactTabs.hasMoreElements()){
	            
	            ChatPanel chatPanel = (ChatPanel)contactTabs.nextElement();
	            
	            int tabIndex = chatPanel.getTabIndex();
	            
	            if(tabIndex > index){
	                chatPanel.setTabIndex(tabIndex - 1);
	            }
	        }
	        
	        int selectedIndex = chatTabbedPane.getSelectedIndex();
	        
	        if( selectedIndex > index){
	            chatTabbedPane.setSelectedIndex(selectedIndex - 1);
	        }
	        
	        if(chatTabbedPane.getTabCount() > 1)
	            chatTabbedPane.remove(index);
	        
	        if(chatTabbedPane.getTabCount() == 1){
	            
	            String onlyTabtitle = chatTabbedPane.getTitleAt(0);
	            
	            this.getContentPane().remove(chatTabbedPane);
	            
	            this.chatTabbedPane.removeAll();
	            
	            ChatPanel chatPanel 
	                = (ChatPanel)this.contactTabsTable.get(onlyTabtitle);
	            
	            this.getContentPane().add(chatPanel, BorderLayout.CENTER);
	            
	            this.setCurrentChatPanel(chatPanel);
	            
	            this.setTitle(onlyTabtitle);
	        }
        }
    }

    /**
     * Returns the table of all MetaContact-s for this chat window. 
     * This is used in case of tabbed chat window.
     * 
     * @return The table of all MetaContact-s for this chat window.
     */
    public Hashtable getContactTabsTable() {
        return this.contactTabsTable;
    }

    /**
     * Returns the currently selected chat panel.
     * 
     * @return the currently selected chat panel.
     */
    public ChatPanel getCurrentChatPanel() {
        return this.currentChatPanel;
    }

    /**
     * Sets the currently selected chat panel.
     * 
     * @param currentChatPanel The chat panel which is currently selected.
     */
    public void setCurrentChatPanel(ChatPanel currentChatPanel) {
        this.currentChatPanel = currentChatPanel;
    }
    
    /**
     * Returns the tab count of the chat tabbed pane. Meant to be
     * used when in "Group chat windows" mode.
     * 
     * @return int The number of opened tabs.
     */
    public int getTabCount(){
        return this.chatTabbedPane.getTabCount();
    }
    
    /**
     * Returns the chat tab index for the given MetaContact.
     * 
     * @param contact The MetaContact we are searching for.
     * @return int The chat tab index for the given MetaContact.
     */
    public int getTabInex(MetaContact contact){
        return ((ChatPanel)this.contactTabsTable
                .get(contact.getDisplayName())).getTabIndex();
    }
    
    /**
     * Highlights the corresponding tab when a message from
     * the given MetaContact is received.
     * 
     * @param contact The MetaContact to highlight.
     */
    public void highlightTab(MetaContact contact){
        this.chatTabbedPane.highlightTab(getTabInex(contact));
    }
    
    /**
     * Returns the ChatPanel for the given MetaContact.
     * @param contact The MetaContact.
     * @return ChatPanel The ChatPanel for the given MetaContact.
     */
    public ChatPanel getChatPanel(MetaContact contact){
        return (ChatPanel)this.contactTabsTable.get(contact.getDisplayName());
    }
    
    /**
     * Sets the icon at the given index to the new
     * icon.
     */
    public void setTabIcon(int index, Icon icon){
        this.chatTabbedPane.setIconAt(index, icon);
    }
}
