/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import net.java.sip.communicator.impl.gui.main.ContactItem;
import net.java.sip.communicator.impl.gui.main.customcontrols.TransparentBackground;

import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

/**
 * @author Yana Stamcheva
 * 
 * The ContactListPanel contains the contact list.
 */

public class ContactInfoPanel extends JDialog
	implements WindowFocusListener {

	private JPanel	protocolsPanel = new JPanel(new GridLayout(0, 1));
	
	private ContactItem contactItem;
	
	TransparentBackground bg;
	
	public ContactInfoPanel(Frame owner, ContactItem contactItem){		
		
		super(owner);
				
		this.contactItem = contactItem;
		
		this.setUndecorated(true);
		
		this.setModal(true);
		
		this.protocolsPanel.setOpaque(false);
		
		//Create the transparent background component
		this.bg = new TransparentBackground(this);	
		
		this.bg.setLayout(new BorderLayout());
		
		this.bg.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		this.getContentPane().setLayout(new BorderLayout());
		
		this.init();
		
		this.getContentPane().add(bg, BorderLayout.CENTER);
		
		this.pack();
		
		this.setSize(140, 50);
		
		this.addWindowFocusListener(this);
	}
	
	private void init() {
		
		String[] protocolList = this.contactItem.getProtocolList();
		
		if(protocolsPanel.getComponentCount() == 0){
			for(int i = 0; i < protocolList.length; i ++){
				
				JLabel protocolLabel = new JLabel(protocolList[i],
						new ImageIcon(Constants.getProtocolIcon(protocolList[i])),
						JLabel.LEFT);
				
				this.protocolsPanel.add(protocolLabel);		
			}
		}
					
		this.bg.add(protocolsPanel, BorderLayout.CENTER);
	}

	public JPanel getProtocolsPanel() {
		return protocolsPanel;
	}

	
	public void windowGainedFocus(WindowEvent e) {
		
	}
	
	public void windowLostFocus(WindowEvent e) {
		
		this.setVisible(false);
		this.dispose();
	}
	
	
	public void setPopupLocation(int x, int y){
		
		this.setLocation(x, y);
		
		this.bg.updateBackground(x, y);
	}
}
