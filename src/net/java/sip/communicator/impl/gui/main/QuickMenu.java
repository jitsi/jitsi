package net.java.sip.communicator.impl.gui.main;

import java.awt.Dimension;
import java.awt.FlowLayout;
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
			
	SIPCommButton infoButton;
	SIPCommButton toolsButton;
	SIPCommButton addButton;
	SIPCommButton searchButton;
	
	public QuickMenu(){
		
		this.setRollover(true);
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
		this.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
				
		infoButton 		= new SIPCommButton
							(LookAndFeelConstants.QUICK_MENU_BUTTON_BG,
							 LookAndFeelConstants.QUICK_MENU_BUTTON_ROLLOVER_BG,
							 LookAndFeelConstants.QUICK_MENU_INFO_ICON);
		
		toolsButton 	= new SIPCommButton
							(LookAndFeelConstants.QUICK_MENU_BUTTON_BG, 
							 LookAndFeelConstants.QUICK_MENU_BUTTON_ROLLOVER_BG,
							 LookAndFeelConstants.QUICK_MENU_CONFIGURE_ICON);
		
		searchButton 	= new SIPCommButton
							(LookAndFeelConstants.QUICK_MENU_BUTTON_BG, 
							 LookAndFeelConstants.QUICK_MENU_BUTTON_ROLLOVER_BG,
							 LookAndFeelConstants.QUICK_MENU_SEARCH_ICON);
		
		addButton 		= new SIPCommButton
							(LookAndFeelConstants.QUICK_MENU_BUTTON_BG, 
							 LookAndFeelConstants.QUICK_MENU_BUTTON_ROLLOVER_BG,
							 LookAndFeelConstants.QUICK_MENU_ADD_ICON);
				
		this.init();
	}
	
	private void init() {
		this.add(addButton);		
		this.add(toolsButton);		
		this.add(searchButton);
		this.add(infoButton);
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
