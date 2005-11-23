package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;

/**
 * @author Yana Stamcheva
 *
 * The DialPanel contains the dial buttons.
 */

public class DialPanel extends JPanel {
	private Font 	buttonTextFont = new Font("Verdana", Font.BOLD, 12);
	
	private SIPCommButton oneButton	= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
			         LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton twoButton		= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
					 LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton threeButton	= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
			         LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton fourButton	= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
	                 LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton fiveButton	= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
					 LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton sixButton		= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
					 LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton sevenButton	= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
					 LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton eightButton	= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
					 LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton nineButton	= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
					 LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton starButton	= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
					 LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton zeroButton	= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG, 
					 LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private SIPCommButton diezButton	= new SIPCommButton
					(LookAndFeelConstants.DIAL_BUTTON_BG,
					 LookAndFeelConstants.DIAL_BUTTON_ROLLOVER_BG);
	
	private JPanel dialPadPanel = new JPanel(new GridLayout(4, 3, 5, 5));
	
	public DialPanel(){
		super(new FlowLayout(FlowLayout.CENTER));
		
		this.init();
	}
	
	public void init(){
	
		oneButton.setFont(this.buttonTextFont);
		twoButton.setFont(this.buttonTextFont);
		threeButton.setFont(this.buttonTextFont);
		fourButton.setFont(this.buttonTextFont);
		fiveButton.setFont(this.buttonTextFont);
		sixButton.setFont(this.buttonTextFont);
		sevenButton.setFont(this.buttonTextFont);
		eightButton.setFont(this.buttonTextFont);
		nineButton.setFont(this.buttonTextFont);
		zeroButton.setFont(this.buttonTextFont);
		diezButton.setFont(this.buttonTextFont);
		starButton.setFont(this.buttonTextFont);
		
		dialPadPanel.add(oneButton);
		dialPadPanel.add(twoButton);
		dialPadPanel.add(threeButton);
		dialPadPanel.add(fourButton);
		dialPadPanel.add(fiveButton);
		dialPadPanel.add(sixButton);
		dialPadPanel.add(sevenButton);
		dialPadPanel.add(eightButton);
		dialPadPanel.add(nineButton);
		dialPadPanel.add(starButton);
		dialPadPanel.add(zeroButton);
		dialPadPanel.add(diezButton);
		
		this.add(dialPadPanel, BorderLayout.CENTER);
	}
}
