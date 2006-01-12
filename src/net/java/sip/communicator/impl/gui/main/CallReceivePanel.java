package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

public class CallReceivePanel extends JDialog{
	
	private Image callButtonPressedIcon = Constants.CALL_PRESSED_BUTTON_BG;

	private Image hangupButtonPressedIcon = Constants.HANGUP_PRESSED_BUTTON_BG;

	private Image callButtonBG = Constants.CALL_BUTTON_BG;

	private Image callButtonRolloverBG = Constants.CALL_ROLLOVER_BUTTON_BG;

	private Image hangupButtonBG = Constants.HANGUP_BUTTON_BG;

	private Image hangupButtonRolloverBG = Constants.HANGUP_ROLLOVER_BUTTON_BG;
	
	private JLabel personPhoto = new JLabel(new ImageIcon (Constants.DEFAULT_USER_PHOTO));
	
	private JLabel personName = new JLabel ("John Smith");
	
	private JLabel birthDate = new JLabel ("11/11/1900");
	
	private JLabel emptyLabel = new JLabel ("  ");
	
	private JLabel personInfo = new JLabel ("additional info");
	
	private SIPCommButton callButton;

	private SIPCommButton hangupButton;
	
	private JPanel userInfoPanel = new JPanel();
	
	private JPanel buttonsPanel = new JPanel(
			new FlowLayout(FlowLayout.CENTER, 15, 5));
	
	
	
	public CallReceivePanel (MainFrame parent){
		
		super(parent);
		
		this.setSize(300, 200);
		
		this.getContentPane().setLayout(new BorderLayout());
		
		callButton = new SIPCommButton(callButtonBG, callButtonRolloverBG,
				callButtonPressedIcon, null);

		hangupButton = new SIPCommButton(hangupButtonBG,
				hangupButtonRolloverBG, hangupButtonPressedIcon, null);
		
		
		
		this.init();
	}
	
	public void init () {
		
		this.personName.setFont(new Font("Sans Serif", Font.BOLD, 12));
		
		//this.buttonsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
		this.buttonsPanel.add(callButton);
		this.buttonsPanel.add(hangupButton);

		this.userInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 0));
		this.userInfoPanel.setLayout(new BoxLayout(userInfoPanel, BoxLayout.Y_AXIS));
		
		this.userInfoPanel.add(personName);
		this.userInfoPanel.add(birthDate);
		this.userInfoPanel.add(emptyLabel);
		this.userInfoPanel.add(personInfo);
		
		this.getContentPane().add(personPhoto, BorderLayout.WEST);
		this.getContentPane().add(userInfoPanel, BorderLayout.CENTER);
		this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
	}
}
