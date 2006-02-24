/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;

public class ChatContactPanel extends JPanel {
	
	private SIPCommButton callButton 
			= new SIPCommButton(ImageLoader.getImage(ImageLoader.CHAT_CONTACT_CALL_BUTTON),
								ImageLoader.getImage(ImageLoader.CHAT_CONTACT_CALL_ROLLOVER_BUTTON));
			
	private SIPCommButton infoButton 
			= new SIPCommButton(ImageLoader.getImage(ImageLoader.CHAT_CONTACT_INFO_BUTTON),
								ImageLoader.getImage(ImageLoader.CHAT_CONTACT_INFO_ROLLOVER_BUTTON));
			
	private SIPCommButton sendFileButton 
			= new SIPCommButton(ImageLoader.getImage(ImageLoader.CHAT_CONTACT_SEND_FILE_BUTTON),
								ImageLoader.getImage(ImageLoader.CHAT_SEND_FILE_ROLLOVER_BUTTON));
					
	private JLabel personPhotoLabel = new JLabel();
	
	private JLabel personNameLabel = new JLabel();
	
	private JPanel buttonsPanel 
					= new JPanel(new FlowLayout(FlowLayout.CENTER));
	
	private JPanel mainPanel = new JPanel(new BorderLayout());
	
	private JPanel contactNamePanel 
					= new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
	
	private MetaContact contactItem;
	
	private boolean isMouseOver = false; 
	private boolean isSelected = false;
	
	public ChatContactPanel(MetaContact contactItem){
		
		super(new BorderLayout());
	
		this.setPreferredSize(new Dimension (100, 60));
		
		this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
				
		this.contactItem = contactItem;
		
		this.setOpaque(false);
		this.mainPanel.setOpaque(false);
		this.contactNamePanel.setOpaque(false);		
		this.buttonsPanel.setOpaque(false);
		
		this.init();
	}
	
	public void init(){
		
		this.personNameLabel.setText(contactItem.getDisplayName());
		this.personNameLabel.setFont(this.getFont().deriveFont(Font.BOLD));
		
		//this.personPhotoLabel.setIcon(new ImageIcon(contactItem.getPhoto()));
		
		this.buttonsPanel.add(callButton);
		this.buttonsPanel.add(infoButton);
		this.buttonsPanel.add(sendFileButton);
		
		this.contactNamePanel.add(personNameLabel);
		
		this.mainPanel.add(buttonsPanel, BorderLayout.NORTH);
		this.mainPanel.add(contactNamePanel, BorderLayout.CENTER);
		
		this.add(personPhotoLabel, BorderLayout.WEST);
		this.add(mainPanel, BorderLayout.CENTER);
		
	}
		
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D)g;
		
		GradientPaint p = new GradientPaint(this.getWidth()/2, 
				0, 
				Constants.CONTACTPANEL_MOVER_START_COLOR, 
				this.getWidth()/2, 
				Constants.CONTACTPANEL_GRADIENT_SIZE, 
				Constants.CONTACTPANEL_MOVER_END_COLOR);		

		GradientPaint p1 = new GradientPaint(	this.getWidth()/2, 
					this.getHeight() - Constants.CONTACTPANEL_GRADIENT_SIZE, 
					Constants.CONTACTPANEL_MOVER_END_COLOR, 
					this.getWidth()/2, 
					this.getHeight(), 
					Constants.CONTACTPANEL_MOVER_START_COLOR);

		g2.setPaint(p);
		g2.fillRect(0, 0, this.getWidth(), Constants.CONTACTPANEL_GRADIENT_SIZE);
		
		g2.setColor(Constants.CONTACTPANEL_MOVER_END_COLOR);
		g2.fillRect(0, 
					Constants.CONTACTPANEL_GRADIENT_SIZE, 
					this.getWidth(), 
					this.getHeight() - Constants.CONTACTPANEL_GRADIENT_SIZE);
		
		g2.setPaint(p1);
		g2.fillRect(0, this.getHeight() - Constants.CONTACTPANEL_GRADIENT_SIZE - 1, this.getWidth(), this.getHeight() - 1);
	}
	
	public void paint(Graphics g){			
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);			
	}
	
	
	public void setMouseOver(boolean isMouseOver) {
		this.isMouseOver = isMouseOver;
	}
	
	public void setSelected(boolean isSelected) {
		if(this.isSelected != isSelected) {
			this.isSelected = isSelected;
			
			this.repaint();
		}
	}
	
	public boolean isMouseOver() {
		return isMouseOver;
	}
	
	public boolean isSelected() {
		return isSelected;
	}
	
}
