/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

public class ChatSendPanel extends JPanel implements ActionListener {

	private JButton sendButton = new JButton(Messages.getString("send"));
	
	private JPanel statusPanel = new JPanel();
	
	private JPanel sendPanel = new JPanel(new BorderLayout(3, 0));

	private JLabel statusLabel = new JLabel();

	private ChatWindow msgWindow;

	public ChatSendPanel(ChatWindow msgWindow) {

		super(new BorderLayout(5, 5));

		this.msgWindow = msgWindow;

		this.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

		this.statusPanel.add(statusLabel);
						
		this.sendPanel.add(sendButton, BorderLayout.CENTER);
		//this.sendPanel.add(protocolSelectorBox, BorderLayout.WEST);

		this.add(statusPanel, BorderLayout.CENTER);
		this.add(sendPanel, BorderLayout.EAST);
		
		this.sendButton.addActionListener(this);		
	}

	public void paint(Graphics g) {
		AntialiasingManager.activateAntialiasing(g);

		super.paint(g);

		Graphics2D g2 = (Graphics2D) g;

		g2.setColor(Constants.CONTACTPANEL_MOVER_START_COLOR);
		g2.setStroke(new BasicStroke(1f));

		g2.drawRoundRect(3, 7, this.statusPanel.getWidth() - 4,
				this.statusPanel.getHeight() - 4, 8, 8);
	}

	public void actionPerformed(ActionEvent e) {
		JEditorPane messagePane = this.msgWindow.getWriteMessagePanel()
				.getEditorPane();

		if(messagePane.getText() != null && !messagePane.getText().equals("")){
			
			// TODO: Send the text to the protocol service.
	
			// TODO: Receive a notice that message is delivered.
	
			this.msgWindow.getConversationPanel().processSentMessage(
											this.msgWindow.getParentWindow().getAccount(),
											Calendar.getInstance(),										
											messagePane.getText());
	
			messagePane.setText("");
	
			messagePane.requestFocus();
		}
	}

	public JButton getSendButton() {
		return sendButton;
	}

	public void addProtocols(String[] protocolList) {
		
		for(int i = 0; i < protocolList.length; i ++){
			
		/*	protocolSelectorBox.addItem(protocolList[i], 
						new ImageIcon(Constants.getProtocolIcon(protocolList[i])));
                        */
		}
		
	}
}
