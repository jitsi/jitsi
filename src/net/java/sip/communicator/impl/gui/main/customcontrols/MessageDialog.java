/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

public class MessageDialog extends JDialog {

	private JButton noButton = new JButton(Messages.getString("cancel"));
	private JButton yesButton = new JButton(Messages.getString("remove"));
	
	private JCheckBox doNotAskAgain 
		= new JCheckBox(Messages.getString("doNotAskAgain"));
	
	private JLabel iconLabel 
		= new JLabel(new ImageIcon(ImageLoader.getImage(ImageLoader.WARNING_ICON)));
	
	private JLabel messageLabel = new JLabel();
		
	private JPanel buttonsPanel 
		= new JPanel(new FlowLayout(FlowLayout.CENTER));
	
	private JPanel checkBoxPanel 
		= new JPanel(new FlowLayout(FlowLayout.LEADING));
	
	private JPanel messagePanel 
		= new JPanel(new BorderLayout(5, 5));
	
	public MessageDialog(Frame owner){
		super(owner);
		
		this.setLocationRelativeTo(owner);
		
		this.setTitle(Messages.getString("removeContact"));
		
		this.setModal(true);
		
		this.setSize(Constants.OPTION_PANE_WIDTH, Constants.OPTION_PANE_HEIGHT);
		
		this.getContentPane().setLayout(new BorderLayout(5, 5));
		
		this.messagePanel
			.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		this.checkBoxPanel
			.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		
		this.init();
	}
	
	private void init(){		
		this.checkBoxPanel.add(doNotAskAgain);
		
		this.buttonsPanel.add(yesButton);
		this.buttonsPanel.add(noButton);
		
		this.messagePanel.add(iconLabel, BorderLayout.WEST);
		this.messagePanel.add(messageLabel, BorderLayout.CENTER);
		
		this.getContentPane().add(messagePanel, BorderLayout.NORTH);
		this.getContentPane().add(checkBoxPanel, BorderLayout.CENTER);
		this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
	}
	
	public void setMessage(String message) {
		this.messageLabel.setText(message);
	}	
	
	public void paint(Graphics g){
		AntialiasingManager.activateAntialiasing(g);
	
		super.paint(g);
	}	
}
