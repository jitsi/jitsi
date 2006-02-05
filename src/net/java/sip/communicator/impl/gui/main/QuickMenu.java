/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommToolBar;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
/**
 * @author Yana Stamcheva
 *
 * The quick menu. 
 */
public class QuickMenu extends SIPCommToolBar 
	implements ActionListener {
			
	SIPCommButton infoButton;
	SIPCommButton configureButton;
	SIPCommButton addButton;
	SIPCommButton searchButton;
	
	private MainFrame mainFrame;
	
	public QuickMenu(MainFrame mainFrame){

		this.mainFrame = mainFrame;
	
		this.setRollover(true);
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		this.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
				
		infoButton 		= new SIPCommButton
							(ImageLoader.getImage(ImageLoader.QUICK_MENU_BUTTON_BG),
							 ImageLoader.getImage(ImageLoader.QUICK_MENU_BUTTON_ROLLOVER_BG),
							 ImageLoader.getImage(ImageLoader.QUICK_MENU_INFO_ICON));
		
		configureButton 	= new SIPCommButton
							(ImageLoader.getImage(ImageLoader.QUICK_MENU_BUTTON_BG), 
							 ImageLoader.getImage(ImageLoader.QUICK_MENU_BUTTON_ROLLOVER_BG),
							 ImageLoader.getImage(ImageLoader.QUICK_MENU_CONFIGURE_ICON));
		
		searchButton 	= new SIPCommButton
							(ImageLoader.getImage(ImageLoader.QUICK_MENU_BUTTON_BG), 
							 ImageLoader.getImage(ImageLoader.QUICK_MENU_BUTTON_ROLLOVER_BG),
							 ImageLoader.getImage(ImageLoader.QUICK_MENU_SEARCH_ICON));
		
		addButton 		= new SIPCommButton
							(ImageLoader.getImage(ImageLoader.QUICK_MENU_BUTTON_BG), 
							 ImageLoader.getImage(ImageLoader.QUICK_MENU_BUTTON_ROLLOVER_BG),
							 ImageLoader.getImage(ImageLoader.QUICK_MENU_ADD_ICON));
				
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
			
		}
		else if (buttonName.equals("info")){
			
		}
	}
}
