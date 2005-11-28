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
					(LookAndFeelConstants.ONE_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton twoButton		= new SIPCommButton
					(LookAndFeelConstants.TWO_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton threeButton	= new SIPCommButton
					(LookAndFeelConstants.THREE_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton fourButton	= new SIPCommButton
					(LookAndFeelConstants.FOUR_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton fiveButton	= new SIPCommButton
					(LookAndFeelConstants.FIVE_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton sixButton		= new SIPCommButton
					(LookAndFeelConstants.SIX_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton sevenButton	= new SIPCommButton
					(LookAndFeelConstants.SEVEN_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton eightButton	= new SIPCommButton
					(LookAndFeelConstants.EIGHT_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton nineButton	= new SIPCommButton
					(LookAndFeelConstants.NINE_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton starButton	= new SIPCommButton
					(LookAndFeelConstants.STAR_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton zeroButton	= new SIPCommButton
					(LookAndFeelConstants.ZERO_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton diezButton	= new SIPCommButton
					(LookAndFeelConstants.DIEZ_DIAL_BUTTON,
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
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
