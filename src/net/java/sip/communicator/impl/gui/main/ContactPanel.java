package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

/**
 * @author Yana Stamcheva
 *
 * The ContactPanel contains the contact item.
 */

public class ContactPanel extends JPanel {
	
	private ContactItem contactItem;
	
	private boolean isMouseOver = false; 
	private boolean isSelected = false;
		
	private JLabel 	nicknameLabel = new JLabel(); 
	
	public ContactPanel(ContactItem contactItem){	
		super(new BorderLayout());
				
		this.contactItem = contactItem;
		
		this.setBackground(Color.WHITE);
		
		this.setOpaque(true);
		
		this.init();
	}
	
	private void init(){
		
		this.nicknameLabel.setFont(this.getFont().deriveFont(Font.BOLD));
		
		this.setUserData();
				
		this.add(nicknameLabel, BorderLayout.CENTER);
		
	}
	
	public void setUserData(){
		this.nicknameLabel.setText(this.contactItem.getNickName());
		this.nicknameLabel.setIcon(this.contactItem.getUserIcon());
		this.nicknameLabel.setIconTextGap(0);
	}
	
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D)g;
				
		if(this.isSelected()){
			GradientPaint p = new GradientPaint(this.getWidth()/2, 
					0, 
					Constants.CONTACTPANEL_SELECTED_START_COLOR, 
					this.getWidth()/2, 
					Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE, 
					Constants.CONTACTPANEL_SELECTED_END_COLOR);		

			GradientPaint p1 = new GradientPaint(	this.getWidth()/2, 
						this.getHeight() - Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE, 
						Constants.CONTACTPANEL_SELECTED_END_COLOR, 
						this.getWidth()/2, 
						this.getHeight(), 
						Constants.CONTACTPANEL_SELECTED_START_COLOR);

			g2.setPaint(p);
			g2.fillRect(0, 0, this.getWidth(), Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE);
			
			g2.setColor(Constants.CONTACTPANEL_SELECTED_END_COLOR);
			g2.fillRect(0, 
						Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE, 
						this.getWidth(), 
						this.getHeight() - Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE);
			
			g2.setPaint(p1);
			g2.fillRect(0, this.getHeight() - Constants.CONTACTPANEL_SELECTED_GRADIENT_SIZE, this.getWidth(), this.getHeight() - 1);						
		}
		else if(this.isMouseOver()){
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
	}
	
	public boolean isMouseOver() {
		return isMouseOver;
	}
	
	public void setMouseOver(boolean isMouseOver) {
		this.isMouseOver = isMouseOver;
	}
	
	public boolean isSelected() {
		return isSelected;
	}
	
	public void setSelected(boolean isSelected) {
		if(this.isSelected != isSelected) {
			this.isSelected = isSelected;
			/*if(isSelected) {
				this.setSize(new Dimension(this.getWidth(), LookAndFeelConstants.CONTACTPANEL_SELECTED_HEIGHT));
			} else {
				this.setSize(new Dimension(this.getWidth(), LookAndFeelConstants.CONTACTPANEL_HEIGHT));
			}*/
			this.repaint();
		}
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}

	public ContactItem getContactItem() {
		return contactItem;
	}

}
