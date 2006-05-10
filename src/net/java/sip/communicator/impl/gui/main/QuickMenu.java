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

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.java.sip.communicator.impl.gui.events.ContainerPluginListener;
import net.java.sip.communicator.impl.gui.events.PluginComponentEvent;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListModel;
import net.java.sip.communicator.impl.gui.main.customcontrols.RolloverButton;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommToolBar;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
/**
 * @author Yana Stamcheva
 *
 * The quick menu. 
 */
public class QuickMenu extends SIPCommToolBar 
	implements ActionListener, ContainerPluginListener {

    RolloverButton infoButton      = new RolloverButton
                                (new ImageIcon(ImageLoader.getImage
                                    (ImageLoader.QUICK_MENU_INFO_ICON)));

    RolloverButton configureButton = new RolloverButton
                                (new ImageIcon(ImageLoader.getImage
                                    (ImageLoader.QUICK_MENU_CONFIGURE_ICON))); 

    RolloverButton searchButton    = new RolloverButton
                                (new ImageIcon(ImageLoader.getImage
                                    (ImageLoader.QUICK_MENU_SEARCH_ICON))); 

    RolloverButton addButton       = new RolloverButton
                                (new ImageIcon(ImageLoader.getImage
                                    (ImageLoader.QUICK_MENU_ADD_ICON)));
	
	private MainFrame mainFrame;
    
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
        
        this.infoButton.setToolTipText(Messages.getString("userInfo"));
        this.configureButton.setToolTipText(Messages.getString("configure"));
        this.searchButton.setToolTipText(Messages.getString("showOfflineUsers"));
        this.addButton.setToolTipText(Messages.getString("addContact"));
        
		this.init();
	}
	
	private void init() {
		
		this.add(addButton);		
		this.add(configureButton);		
		this.add(infoButton);
		this.add(searchButton);
		
		this.addButton.setName("add");
		this.configureButton.setName("config");
		this.searchButton.setName("search");
		this.infoButton.setName("info");
		
		this.addButton.addActionListener(this);
		this.configureButton.addActionListener(this);
		this.searchButton.addActionListener(this);
		this.infoButton.addActionListener(this);
		
		//Disable all buttons that do nothing.
		this.addButton.setEnabled(false);
		this.configureButton.setEnabled(false);		
		this.infoButton.setEnabled(false);
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
            
            ContactListModel listModel = (ContactListModel)mainFrame.getTabbedPane()
                .getContactListPanel().getContactList().getModel();
            
            if(listModel.showOffline()){
                listModel.setShowOffline(false);
                listModel.removeOfflineContacts();                
            }
            else{
                listModel.setShowOffline(true);
                listModel.addOfflineContacts();
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
