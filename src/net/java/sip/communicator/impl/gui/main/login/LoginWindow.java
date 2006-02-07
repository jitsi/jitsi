/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.login;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.java.sip.communicator.impl.gui.main.customcontrols.TransparentBackground;
import net.java.sip.communicator.impl.gui.main.customcontrols.TransparentFrameBackground;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;

public class LoginWindow extends JFrame {

	private JLabel uinLabel = new JLabel(Messages.getString("uin"));
	
	private JLabel passwdLabel = new JLabel(Messages.getString("passwd"));
	
	private JTextField uinTextField = new JTextField();
	
	private JTextField passwdTextField = new JTextField();
	
	private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
	
	private JPanel textFieldsPanel = new JPanel(new GridLayout(0, 1));
	
	private JPanel mainPanel = new JPanel(new BorderLayout());
	
	public LoginWindow(){
		
		TransparentFrameBackground background = new TransparentFrameBackground(this);
		
		background.add(mainPanel);
		
		this.getContentPane().add(background);
		
		this.init();
	}

	private void init() {
		
		this.labelsPanel.add(uinLabel);
		this.labelsPanel.add(passwdLabel);
		
		this.textFieldsPanel.add(uinTextField);
		this.textFieldsPanel.add(passwdTextField);
		
		this.mainPanel.add(labelsPanel);
		this.mainPanel.add(textFieldsPanel);
	}
	
	public void show(){
		
		this.pack();
		
		this.setVisible(true);
	}
}
