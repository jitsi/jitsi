package net.java.sip.communicator.impl.gui.main;

import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
/**
 * @author Yana Stamcheva
 *
 * The quick menu. 
 */
public class QuickMenu extends JToolBar{
	
	private Image addButtonIcon 		= LookAndFeelConstants.QUICK_MENU_ADD_ICON;
	private Image configureButtonIcon 	= LookAndFeelConstants.QUICK_MENU_CONFIGURE_ICON;
	
	SIPCommButton infoButton;
	SIPCommButton toolsButton;
	SIPCommButton addButton;
	SIPCommButton searchButton;
	
	public QuickMenu(){
		
		this.setRollover(true);
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
				
		infoButton 		= new SIPCommButton();						
		toolsButton 	= new SIPCommButton(configureButtonIcon);
		searchButton 	= new SIPCommButton();
		addButton 		= new SIPCommButton(addButtonIcon);
				
		this.init();
	}
	
	private void init() {
		this.add(addButton);		
		this.add(toolsButton);
		this.add(infoButton);
		this.add(searchButton);
	}
	
	public void paint(Graphics g){
		super.paint(g);
		
		Graphics2D g2 = (Graphics2D)g;
		
		GradientPaint p = new GradientPaint(this.getWidth()/2, 
				0, 
				LookAndFeelConstants.CONTACTPANEL_SELECTED_START_COLOR, 
				this.getWidth()/2, 
				LookAndFeelConstants.CONTACTPANEL_SELECTED_GRADIENT_SIZE, 
				LookAndFeelConstants.CONTACTPANEL_SELECTED_END_COLOR);		

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setPaint(p);
		
		//g2.dra
	}
}
