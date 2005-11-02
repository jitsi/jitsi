package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * @author Yana Stamcheva
 *
 * The DialPanel contains the dial buttons.
 */

public class DialPanel extends JPanel {
	private Font 	buttonTextFont = new Font("Verdana", Font.BOLD, 12);
	
	private JButton oneButton	= new JButton("1");
	private JButton twoButton	= new JButton("2");
	private JButton threeButton	= new JButton("3");
	private JButton fourButton	= new JButton("4");
	private JButton fiveButton	= new JButton("5");
	private JButton sixButton	= new JButton("6");
	private JButton sevenButton	= new JButton("7");
	private JButton eightButton	= new JButton("8");
	private JButton nineButton	= new JButton("9");
	private JButton starButton	= new JButton("*");
	private JButton zeroButton	= new JButton("0+");
	private JButton diezButton	= new JButton("#");
	
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
