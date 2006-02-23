/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.impl.gui.main.utils.SelectorBoxItem;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetPersistentPresence;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ProviderPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ProviderPresenceStatusListener;
import net.java.sip.communicator.service.protocol.icqconstants.IcqStatusEnum;

/**
 * @author Yana Stamcheva
 * 
 * The MainFrame of the application.
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

	private ContactList contactList;

	private User user;

    private Map supportedOperationSets;
    
	private Dimension minimumFrameSize = new Dimension(
			Constants.MAINFRAME_MIN_WIDTH, Constants.MAINFRAME_MIN_HEIGHT);
    
    private ProtocolProviderService protocolProvider;
    
    private OperationSetPresence presence;

	public MainFrame(User user, ContactList contactList) {
		
		this.user = user;

		this.contactList = contactList;
		
		callPanel = new CallPanel(this);
		tabbedPane = new MainTabbedPane(this);
		quickMenu = new QuickMenu(this);
		statusPanel = new StatusPanel(this, user.getProtocols());

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setInitialBounds();

		this.setTitle(Messages.getString("sipCommunicator"));

		this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));
		
		this.setSize(Constants.MAINFRAME_WIDTH, Constants.MAINFRAME_HEIGHT);	
		
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
		
		this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width
				- MainFrame.WIDTH, 50);
	
		this.tabbedPane.setMinimumSize(minimumFrameSize);
	}

	public CallPanel getCallPanel() {

		return callPanel;
	}

	public ContactList getContactList() {
		
		return this.contactList;
	}

	public void setContactList(ContactList contactList) {
		
		this.contactList = contactList;
	}

	public User getUser() {
		
		return user;
	}

	public void setUser(User user) {
	
		this.user = user;
	}

	public ConfigurationFrame getConfigFrame() {
	
		return configFrame;
	}

	public void setConfigFrame(ConfigurationFrame configFrame) {
	
		this.configFrame = configFrame;
	}

    public Map getSupportedOperationSets() {
        return supportedOperationSets;
    }

    public void setSupportedOperationSets(
            Map supportedOperationSets) {
        
        this.supportedOperationSets = supportedOperationSets;
        
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
                
                presence
                    .addProviderPresenceStatusListener
                        (new ProviderPresenceStatusAdapter());
                
                this.setPresence(presence);
                
                try {   
                    presence
                        .publishPresenceStatus(IcqStatusEnum.ONLINE, "");                    
                     
                    this.getStatusPanel().stopConnecting(Constants.ICQ);
                    
                    this.statusPanel.setSelectedStatus
                        (Constants.ICQ, Constants.ONLINE_STATUS);
                        
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

    public StatusPanel getStatusPanel() {
        return statusPanel;
    }

    public ProtocolProviderService getProtocolProvider() {
        return protocolProvider;
    }

    public void setProtocolProvider(
            ProtocolProviderService protocolProvider) {
        this.protocolProvider = protocolProvider;
    }

    public OperationSetPresence getPresence() {
        return presence;
    }

    public void setPresence(OperationSetPresence presence) {
        this.presence = presence;
    }
}
