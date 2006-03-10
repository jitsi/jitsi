/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.customcontrols.tabbedPane.CloseListener;
import net.java.sip.communicator.impl.gui.main.customcontrols.tabbedPane.SIPCommTabbedPane;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;

/**
 * The chat window.
 * 
 * @author Yana Stamcheva
 */
public class ChatWindow extends JFrame{
	
    private ChatPanel currentChatPanel;
    
	private MenusPanel menusPanel;
	
	private ChatSendPanel sendPanel;
	
	private JPanel topPanel = new JPanel(new BorderLayout());
	
	private JPanel bottomPanel = new JPanel(new BorderLayout());
	
	private MainFrame parentWindow;	
	
	private String windowTitle = "";
		
    private SIPCommTabbedPane chatTabbedPane = null;
    
    private Hashtable contactTabsTable = new Hashtable();
    
    /**
     * Creates a chat window.
     * 
     * @param parentWindow
     */
	public ChatWindow (MainFrame parentWindow){		
				
		this.parentWindow = parentWindow;		
		
		this.setSize(550, 450);
				
		this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));
		
		menusPanel = new MenusPanel(this);
		
		sendPanel = new ChatSendPanel(this);		
        
		this.init();
        
        this.enableKeyActions();
	}
	
    /**
     * Initialize the chat window.
     */
	public void init (){
        
		this.getContentPane().add(menusPanel, BorderLayout.NORTH);
                
		this.getContentPane().add(sendPanel, BorderLayout.SOUTH);				
	}
	
	/**
     * Returns the parent window.
     * 
     * @return The parent window.
	 */
	public MainFrame getParentWindow() {
		return parentWindow;
	}

    /**
     * Sets the parent window.
     * 
     * @param parentWindow The parent window for this chat window.
     */
	public void setParentWindow(MainFrame parentWindow) {
		this.parentWindow = parentWindow;
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
     * Returns the panel, containing the send button.
     * 
     * @return The ChatSendPanel.
     */
	public ChatSendPanel getSendPanel() {
		return sendPanel;
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
     * Enables all key actions on this chat window. For now 
     * closes the window when esc is pressed.
     */
    private void enableKeyActions(){
        
        AbstractAction close = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                ChatWindow.this.dispose();
            }
        };
        
        AbstractAction changeTabForword = new AbstractAction()
        {   
            public void actionPerformed(ActionEvent e)
            {
                if(chatTabbedPane != null 
                        && chatTabbedPane.getSelectedIndex() 
                            < chatTabbedPane.getTabCount() - 1){
                    
                    ChatWindow.this.chatTabbedPane
                        .setSelectedIndex
                            (chatTabbedPane.getSelectedIndex() + 1);
                }
            }
        };
        
        AbstractAction changeTabBackword = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {   
                if(chatTabbedPane != null
                        && chatTabbedPane.getSelectedIndex() != 0){
                    
                    ChatWindow.this.chatTabbedPane
                        .setSelectedIndex
                            (chatTabbedPane.getSelectedIndex() - 1);
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
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "changeTabForword");
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "changeTabBackword");
    }
    
    /**
     * Creates a ChatPanel for the given contact and adds it directly to
     * the chat window.
     */
    public void addChat(MetaContact contact){
        
        this.setCurrentChatPanel(new ChatPanel(this));
        
        this.currentChatPanel.addContactToChat(contact);
        
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
    public void addChatTab(MetaContact contact){
        
        if(chatTabbedPane == null){
            //Initialize the tabbed pane for the first time
            
            this.setCurrentChatPanel(new ChatPanel(this));
            
            this.currentChatPanel.addContactToChat(contact);
            
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
            
            if(chatTabbedPane.getTabCount() > 0){
                
                //The tabbed pane contains already tabs.   
                
                this.setCurrentChatPanel(new ChatPanel(this));
                
                this.currentChatPanel.addContactToChat(contact);
                
                chatTabbedPane.addTab(  contact.getDisplayName(), 
                                        currentChatPanel);
                
                //Set the tab index to the newly added chat panel
                this.currentChatPanel
                    .setTabIndex(chatTabbedPane.getTabCount() - 1);
                
                this.contactTabsTable.put(contact.getDisplayName(),
                                    currentChatPanel);
                
                this.setSelectedContactTab(contact);
            }
            else{
                
                //Add the first two tabs to the tabbed pane.
                
                chatTabbedPane.addTab(  currentChatPanel.getDefaultContact()
                                            .getDisplayName(), 
                                        currentChatPanel);
                
                this.setCurrentChatPanel(new ChatPanel(this));
                
                this.currentChatPanel.addContactToChat(contact);
                
                chatTabbedPane.addTab(  contact.getDisplayName(), 
                                        currentChatPanel);
                
                currentChatPanel
                    .setTabIndex(chatTabbedPane.getTabCount() - 1);
                
                this.contactTabsTable.put(contact.getDisplayName(),
                                currentChatPanel);
                
                this.setSelectedContactTab(contact);
            }
            
            this.getContentPane().add(chatTabbedPane, BorderLayout.CENTER);
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
     * Removes the chat tab with the given tab index.
     * 
     * @param index
     */
    public void removeContactTab(int index){

        String title = chatTabbedPane.getTitleAt(index);
        
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
}
