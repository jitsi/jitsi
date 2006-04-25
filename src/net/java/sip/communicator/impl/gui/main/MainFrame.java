/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.main.contactlist.CListKeySearchListener;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListModel;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPanel;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ContactPresenceStatusListener;
import net.java.sip.communicator.service.protocol.event.ProviderPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ProviderPresenceStatusListener;
import net.java.sip.communicator.service.protocol.icqconstants.IcqStatusEnum;

/**
 * The main application frame.
 * 
 * @author Yana Stamcheva
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
	
    private Hashtable protocolSupportedOperationSets = new Hashtable();
    
    private Hashtable protocolPresenceSets = new Hashtable();
    
    private Hashtable protocolProviders = new Hashtable();
    
    private Hashtable imOperationSets = new Hashtable();
   
    private MetaContactListService contactList;
    
    private Hashtable accounts = new Hashtable();
    
    private Hashtable waitToBeDeliveredMsgs = new Hashtable();
    
	public MainFrame() {		
		callPanel = new CallPanel(this);
		tabbedPane = new MainTabbedPane(this);
		quickMenu = new QuickMenu(this);
		statusPanel = new StatusPanel(this);

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setInitialBounds();

		this.setTitle(Messages.getString("sipCommunicator"));

		this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));
		
		this.init();
	}

    /**
     * Initiates the content of this frame.
     */
	private void init() {
		this.menusPanel.add(menu, BorderLayout.NORTH);
		this.menusPanel.add(quickMenu, BorderLayout.CENTER);

		this.contactListPanel.add(tabbedPane, BorderLayout.CENTER);
		this.contactListPanel.add(callPanel, BorderLayout.SOUTH);

		this.getContentPane().add(menusPanel, BorderLayout.NORTH);
		this.getContentPane().add(contactListPanel, BorderLayout.CENTER);
		this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
	}

    /**
     * Sets frame size and position.
     */
	private void setInitialBounds() {
		
		this.setSize(200, 450);
        this.contactListPanel.setPreferredSize(new Dimension(180, 400));
        this.contactListPanel.setMinimumSize(new Dimension(80, 200));
        
		this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width
				- this.getWidth(), 50);
	}
    
    /**
     * Returns the current meta contactlist.
     * 
     * @return MetaContactListService The current meta contactlist.
     */
	public MetaContactListService getContactList() {
		return this.contactList;
	}

    /**
     * Inits the contact list tree.
     * 
     * @param contactList The MetaContactListService containing 
     * the contact list data.
     */
	public void setContactList(MetaContactListService contactList) {
		
		this.contactList = contactList;
        
		ContactListPanel clistPanel = this.tabbedPane.getContactListPanel();
        
		clistPanel.initTree(contactList);
        
		//add a key listener to the tabbed pane, when the contactlist is 
		//initialized
        this.tabbedPane.addKeyListener(new CListKeySearchListener
        		(clistPanel.getContactList()));
	}
	
	public ConfigurationFrame getConfigFrame() {	
		return configFrame;
	}
    
	public void setConfigFrame(ConfigurationFrame configFrame) {	
		this.configFrame = configFrame;
	}

    /**
     * Returns a set of all operation sets supported by the given 
     * protocol provider.
     * 
     * @param protocolProvider The protocol provider.
     * @return a set of all operation sets supported by the given 
     * protocol provider.
     */
    public Map getSupportedOperationSets
        (ProtocolProviderService protocolProvider) {
        return (Map)this.protocolSupportedOperationSets.get(protocolProvider);
    }

    /**
     * Adds protocol supported operation sets. For now adds the presence and
     * instant messaging operation sets.
     * 
     * @param protocolProvider 
     * @param supportedOperationSets
     */
    public void addProtocolSupportedOperationSets
            (ProtocolProviderService protocolProvider,
                    Map supportedOperationSets) {
        
        this.protocolSupportedOperationSets.put(protocolProvider, 
                supportedOperationSets);
        
        Iterator entrySetIter = supportedOperationSets.entrySet().iterator();
        
        for (int i = 0; i < supportedOperationSets.size(); i++)
        {
            Map.Entry entry = (Map.Entry) entrySetIter.next();

            Object key = entry.getKey();
            Object value = entry.getValue();        
            
            if(key.equals(OperationSetPersistentPresence.class.getName())
                    || key.equals(OperationSetPresence.class.getName())){

                OperationSetPresence presence 
                    = (OperationSetPresence)value;
                
                this.protocolPresenceSets.put(  protocolProvider,
                                                presence);                
                presence
                    .addProviderPresenceStatusListener
                        (new ProviderPresenceStatusAdapter());
                presence
                    .addContactPresenceStatusListener
                        (new ContactPresenceStatusAdapter());
                
                try {   
                    presence
                        .publishPresenceStatus(IcqStatusEnum.ONLINE, "");                    
                     
                    this.getStatusPanel().stopConnecting(
                            protocolProvider.getProtocolName());
                    
                    this.statusPanel.setSelectedStatus
                        (protocolProvider.getProtocolName(), 
                                Constants.ONLINE_STATUS);
                    
                    //request the focus int the contact list panel, which
                    //permits to search in the contact list
                    this.tabbedPane.getContactListPanel()
                    	.getContactList().requestFocus();
                        
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (OperationFailedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            else if(key.equals(OperationSetBasicInstantMessaging.class.getName())
                    || key.equals(OperationSetPresence.class.getName())){
                
                OperationSetBasicInstantMessaging im 
                    = (OperationSetBasicInstantMessaging)value;
                
                this.imOperationSets.put(protocolProvider, im);
                //Add to all instant messaging operation sets the Message 
                //listener implemented in the ContactListPanel, which handles 
                //all received messages.
                im.addMessageListener(this.getTabbedPane().getContactListPanel());
            }
            else if(key.equals(OperationSetTypingNotifications.class.getName())){
                OperationSetTypingNotifications tn 
                    = (OperationSetTypingNotifications)value;
                tn.addTypingNotificationsListener
                	(this.getTabbedPane().getContactListPanel());
            }
        }        
    }
    
    /**
     * Returns a set of all protocol providers.
     * 
     * @return Map a set of all protocol providers.
     */
    public Map getProtocolProviders() {
        return this.protocolProviders;
    }

    /**
     * Adds a protocol provider.
     * @param protocolProvider The protocol provider to be added.
     */
    public void addProtocolProvider(
            ProtocolProviderService protocolProvider) {
        
        this.protocolProviders.put( protocolProvider.getProtocolName(),
                                    protocolProvider);
    }
    
    /**
     * Adds an account.
     * 
     * @param account The account to be added.
     */
    public void addAccount(String identifier, 
    		ProtocolProviderService protocolProvider){
    	String protocolName = protocolProvider.getProtocolName();
    	
        if(!getStatusPanel().isProtocolActivated(protocolName)){
        	this.accounts.put(protocolProvider, identifier);
	    	this.getStatusPanel().activateAccount(protocolProvider);
        }
        this.getStatusPanel()
        	.startConnecting(protocolName);
    }
    
    /**
     * Returns the default account (for now is returning the first one).
     * @return the default account (for now is returning the first one).
     */
    public String getDefaultAccount(ProtocolProviderService protocolProvider){
        return (String)this.accounts.get(protocolProvider);
    }

    /**
     * Returns the presence operation set for the given protocol provider.
     * 
     * @param protocolProvider The protocol provider for which the 
     * presence operation set is searched.
     * @return the presence operation set for the given protocol provider.
     */
    public OperationSetPresence getProtocolPresence
        (ProtocolProviderService protocolProvider) {
        return (OperationSetPresence)
            this.protocolPresenceSets.get(protocolProvider);
    }

    /**
     * Returns the basic instant messaging operation set for the given 
     * protocol provider.
     * 
     * @param protocolProvider The protocol provider for which the IM 
     * is searched.
     * @return OperationSetBasicInstantMessaging The basic instant messaging 
     * operation set for the given protocol provider.
     */
    public OperationSetBasicInstantMessaging getProtocolIM
        (ProtocolProviderService protocolProvider) {
        return (OperationSetBasicInstantMessaging)
            this.imOperationSets.get(protocolProvider);
    }
    
    /**
     * Returns the main tabbed pane containing the contactlist, call list etc.
     * @return MainTabbedPane The main tabbed pane containing the 
     * contactlist, call list etc.
     */
    public MainTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /**
     * Returns the call panel.
     * @return CallPanel The call panel.
     */
    public CallPanel getCallPanel() {
        return callPanel;
    }
    
    /**
     * Returns the quick menu, placed above the main tabbed pane.
     * @return QuickMenu The quick menu, placed above the main tabbed pane.
     */
    public QuickMenu getQuickMenu() {
        return quickMenu;
    }
    
    /**
     * Returns the status panel.
     * @return StatusPanel The status panel.
     */
    public StatusPanel getStatusPanel() {
        return statusPanel;
    }
    
    /**
     * Listens for all contactPresenceStatusChanged events in order 
     * to refresh tha contact list, when a status is changed.
     */
    private class ContactPresenceStatusAdapter
        implements ContactPresenceStatusListener {

        public void contactPresenceStatusChanged
            (ContactPresenceStatusChangeEvent evt) {
          
            Contact sourceContact = evt.getSourceContact();
            
            MetaContact metaContact
                = contactList.findMetaContactByContact(sourceContact);
           
            if (metaContact != null){
            		ContactListPanel clistPanel 
            			= tabbedPane.getContactListPanel();
            		
                ContactListModel model 
                    = (ContactListModel)clistPanel.getContactList().getModel();
                
                model.updateContactStatus(metaContact, evt.getNewStatus());
                
                clistPanel.updateChatContactStatus(metaContact);
            }
        }
    }        

    /**
     * Listens for all providerStatusChanged and providerStatusMessageChanged
     * events in order to refresh the account status panel, when a status is changed.
     */
    private class ProviderPresenceStatusAdapter
        implements ProviderPresenceStatusListener {
    
        public void providerStatusChanged
            (ProviderPresenceStatusChangeEvent evt) {
            
        }
    
        public void providerStatusMessageChanged
            (PropertyChangeEvent evt) {           
            
        }
    }

    public Hashtable getWaitToBeDeliveredMsgs() {
        return waitToBeDeliveredMsgs;
    }
}
