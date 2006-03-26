/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.java.sip.communicator.impl.gui.main.contactlist.ContactList;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommToolBar;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.gui.event.ContainerPluginListener;
import net.java.sip.communicator.service.gui.event.PluginComponentEvent;
/**
 * @author Yana Stamcheva
 *
 * The quick menu. 
 */
public class QuickMenu extends SIPCommToolBar 
	implements ActionListener, ContainerPluginListener {

    JButton infoButton      = new JButton
                                (new ImageIcon(ImageLoader.getImage
                                    (ImageLoader.QUICK_MENU_INFO_ICON)));

    JButton configureButton = new JButton
                                (new ImageIcon(ImageLoader.getImage
                                    (ImageLoader.QUICK_MENU_CONFIGURE_ICON))); 

    JButton searchButton    = new JButton
                                (new ImageIcon(ImageLoader.getImage
                                    (ImageLoader.QUICK_MENU_SEARCH_ICON))); 

    JButton addButton       = new JButton
                                (new ImageIcon(ImageLoader.getImage
                                    (ImageLoader.QUICK_MENU_ADD_ICON)));
	
	private MainFrame mainFrame;
    
    private boolean showOnlineContactsOnly = false;
	
	public QuickMenu(MainFrame mainFrame){

		this.mainFrame = mainFrame;
	    
		this.setRollover(true);
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		this.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		this.setFloatable(false);		
        
		this.infoButton.setPreferredSize(new Dimension(28, 28));
        this.configureButton.setPreferredSize(new Dimension(28, 28));		
        this.searchButton.setPreferredSize(new Dimension(28, 28));
        this.addButton.setPreferredSize(new Dimension(28, 28));
        
		this.init();
	}
	
	private void init() {
		
		this.add(addButton);		
		this.add(configureButton);		
		this.add(searchButton);
		this.add(infoButton);
        		
		this.addButton.setName("add");
		this.configureButton.setName("config");
		this.searchButton.setName("search");
		this.infoButton.setName("info");
		
		this.addButton.addActionListener(this);
		this.configureButton.addActionListener(this);
		this.searchButton.addActionListener(this);
		this.infoButton.addActionListener(this);
	}

	public void actionPerformed(ActionEvent e) {
		
		JButton button = (JButton)e.getSource();
		String buttonName = button.getName();
		
		if (buttonName.equals("add")){
			
		}
		else if (buttonName.equals("config")){
			
			mainFrame.getConfigFrame().setCalculatedSize();
			
			mainFrame.getConfigFrame().setVisible(true);
		}
		else if (buttonName.equals("search")){
            
            ContactList contactList = mainFrame.getTabbedPane()
                .getContactListPanel().getContactList();
            
            showOnlineContactsOnly = !showOnlineContactsOnly;
            
            if(showOnlineContactsOnly){
                 
                contactList.removeOfflineContacts();
            }
            else{
                contactList.addOfflineContacts();
            }
		}
		else if (buttonName.equals("info")){
			
		}
	}

    public void pluginComponentAdded(PluginComponentEvent event) {
        // TODO Auto-generated method stub
        
    }

    public void pluginComponentRemoved(PluginComponentEvent event) {
        // TODO Auto-generated method stub
        
    }
}
