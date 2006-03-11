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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.main.contactlist.CListKeySearchListener;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactList;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListModel;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPanel;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactNode;
import net.java.sip.communicator.impl.gui.main.contactlist.MetaContactNode;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.message.ChatWindow;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
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
    
	private Dimension minimumFrameSize = new Dimension(
			Constants.MAINFRAME_MIN_WIDTH, Constants.MAINFRAME_MIN_HEIGHT);
    
    private Hashtable protocolProviders = new Hashtable();
   
    private MetaContactListService contactList;
    
    private ArrayList accounts = new ArrayList();
    
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

	private void init() {

		this.menusPanel.add(menu, BorderLayout.NORTH);
		this.menusPanel.add(quickMenu, BorderLayout.CENTER);

		this.contactListPanel.add(tabbedPane, BorderLayout.CENTER);
		this.contactListPanel.add(callPanel, BorderLayout.SOUTH);

		this.getContentPane().add(menusPanel, BorderLayout.NORTH);
		this.getContentPane().add(contactListPanel, BorderLayout.CENTER);
		this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
	}

	private void setInitialBounds() {
		
		this.setSize(155, 400);
        this.contactListPanel.setPreferredSize(new Dimension(140, 350));
        this.contactListPanel.setMinimumSize(new Dimension(80, 200));
        
		this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width
				- this.getWidth(), 50);
	    
	}

	public CallPanel getCallPanel() {

		return callPanel;
	}

	public MetaContactListService getContactList() {
		
		return this.contactList;
	}

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

    public Map getSupportedOperationSets
        (ProtocolProviderService protocolProvider) {
        return (Map)this.protocolSupportedOperationSets.get(protocolProvider);
    }

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
        }
    }
    
    private class ProviderPresenceStatusAdapter
        implements ProviderPresenceStatusListener {

        public void providerStatusChanged
            (ProviderPresenceStatusChangeEvent evt) {
            
        }
    
        public void providerStatusMessageChanged
            (PropertyChangeEvent evt) {           
            
        }
    }

    private class ContactPresenceStatusAdapter
        implements ContactPresenceStatusListener {

        public void contactPresenceStatusChanged
            (ContactPresenceStatusChangeEvent evt) {
          
            MetaContact metaContact
                = contactList.findMetaContactByContact(evt.getSourceContact());
           
            if (metaContact != null){
                ContactListModel model 
                    = (ContactListModel)tabbedPane.getContactListPanel()
                        .getContactList().getModel();
                
                MetaContactNode node 
                    = model.getContactNodeByContact(metaContact);
                
                if(node != null){
                   
                    node.setStatusIcon
                        (new ImageIcon(Constants.getStatusIcon
                                (evt.getNewStatus())));
                   
                    //Refresh the node status icon.
                    model.contactStatusChanged(model.indexOf(node));
                }
            }
        }
    }
    
    public StatusPanel getStatusPanel() {
        return statusPanel;
    }

    public Map getProtocolProviders() {
        return this.protocolProviders;
    }

    public void addProtocolProvider(
            ProtocolProviderService protocolProvider) {
        
        this.protocolProviders.put( protocolProvider.getProtocolName(),
                                    protocolProvider);
    }
    
    public void addAccount(Account account){
        this.accounts.add(account);
    }
    
    public Account getAccount(){
        return (Account)this.accounts.get(0);
    }

    public OperationSetPresence getProtocolPresence
        (ProtocolProviderService protocolProvider) {
        return (OperationSetPresence)
            this.protocolPresenceSets.get(protocolProvider);
    }
}
