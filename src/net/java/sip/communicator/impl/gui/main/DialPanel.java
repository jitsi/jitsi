/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * @author Yana Stamcheva
 *
 * The DialPanel contains the dial buttons.
 */

public class DialPanel extends JPanel implements ActionListener {
		
	private JComboBox phoneNumberCombo;
	
	private SIPCommButton oneButton	= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.ONE_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton twoButton		= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.TWO_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton threeButton	= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.THREE_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton fourButton	= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.FOUR_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton fiveButton	= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.FIVE_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton sixButton		= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.SIX_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton sevenButton	= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.SEVEN_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton eightButton	= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.EIGHT_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton nineButton	= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.NINE_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton starButton	= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.STAR_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton zeroButton	= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.ZERO_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private SIPCommButton diezButton	= new SIPCommButton
					(ImageLoader.getImage(ImageLoader.DIEZ_DIAL_BUTTON),
					 SIPCommButton.LEFT_ICON_LAYOUT);
	
	private JPanel dialPadPanel = new JPanel(new GridLayout(4, 3, 5, 5));
	
	public DialPanel(){
		super(new FlowLayout(FlowLayout.CENTER));
		
		this.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
		
		this.init();
	}
	
	public void init(){
			
		oneButton.setName("one");
		twoButton.setName("two");
		threeButton.setName("three");
		fourButton.setName("four");
		fiveButton.setName("five");
		sixButton.setName("six");
		sevenButton.setName("seven");
		eightButton.setName("eight");
		nineButton.setName("nine");
		zeroButton.setName("zero");
		diezButton.setName("diez");
		starButton.setName("star");
		
		oneButton.addActionListener(this);
		twoButton.addActionListener(this);
		threeButton.addActionListener(this);
		fourButton.addActionListener(this);
		fiveButton.addActionListener(this);
		sixButton.addActionListener(this);
		sevenButton.addActionListener(this);
		eightButton.addActionListener(this);
		nineButton.addActionListener(this);
		zeroButton.addActionListener(this);
		diezButton.addActionListener(this);
		starButton.addActionListener(this);
		
		oneButton.setFont(Constants.FONT);
		twoButton.setFont(Constants.FONT);
		threeButton.setFont(Constants.FONT);
		fourButton.setFont(Constants.FONT);
		fiveButton.setFont(Constants.FONT);
		sixButton.setFont(Constants.FONT);
		sevenButton.setFont(Constants.FONT);
		eightButton.setFont(Constants.FONT);
		nineButton.setFont(Constants.FONT);
		zeroButton.setFont(Constants.FONT);
		diezButton.setFont(Constants.FONT);
		starButton.setFont(Constants.FONT);
				
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

	public void actionPerformed(ActionEvent e) {
		JButton button = (JButton)e.getSource();
		String buttonName = button.getName();
		String phoneNumber = "";
		
		if (this.phoneNumberCombo.getEditor().getItem() != null)
			phoneNumber = (String)this.phoneNumberCombo.getEditor().getItem();
				
		if(buttonName.equals("one"))
			this.phoneNumberCombo.getEditor().setItem(phoneNumber + "1");
		
		else if (buttonName.equals("two"))
			this.phoneNumberCombo.getEditor().setItem(phoneNumber + "2");
		
		else if (buttonName.equals("three"))
			this.phoneNumberCombo.getEditor().setItem(phoneNumber + "3");
		
		else if (buttonName.equals("four"))
			this.phoneNumberCombo.getEditor().setItem(phoneNumber + "4");
		
		else if (buttonName.equals("five"))
			this.phoneNumberCombo.getEditor().setItem(phoneNumber + "5");
		
		else if (buttonName.equals("six"))
			this.phoneNumberCombo.getEditor().setItem(phoneNumber + "6");
				
		else if (buttonName.equals("seven"))
			this.phoneNumberCombo.getEditor().setItem(phoneNumber + "7");
		
		else if (buttonName.equals("eight"))
			this.phoneNumberCombo.getEditor().setItem(phoneNumber + "8");
		
		else if (buttonName.equals("nine"))
			this.phoneNumberCombo.getEditor().setItem(phoneNumber + "9");
		
		else if (buttonName.equals("zero"))
			this.phoneNumberCombo.getEditor().setItem(phoneNumber + "0");
		
		this.phoneNumberCombo.requestFocus();
	}	
	
	
	public void setPhoneNumberCombo(JComboBox combo){
		this.phoneNumberCombo = combo;
	}
}
