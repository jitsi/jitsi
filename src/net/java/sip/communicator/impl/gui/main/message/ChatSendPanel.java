/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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

import net.java.sip.communicator.impl.gui.main.customcontrols.AntialiasedPanel;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.service.protocol.Message;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;

public class ChatSendPanel extends JPanel 
    implements ActionListener{

	private JButton sendButton = new JButton(Messages.getString("send"));
	
	private AntialiasedPanel statusPanel 
        = new AntialiasedPanel(new FlowLayout(FlowLayout.LEFT));
	
	private JPanel sendPanel = new JPanel(new BorderLayout(3, 0));

	private JLabel statusLabel = new JLabel();

	private ChatPanel chatPanel;
    
	public ChatSendPanel(ChatPanel chatPanel) {

		super(new BorderLayout(5, 5));

		this.chatPanel = chatPanel;

		this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

		this.statusPanel.add(statusLabel);
						
		//this.sendPanel.add(sendButton, BorderLayout.CENTER);
		//this.sendPanel.add(protocolSelectorBox, BorderLayout.WEST);

		this.add(statusPanel, BorderLayout.CENTER);
		this.add(sendButton, BorderLayout.EAST);
		
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
        JEditorPane messagePane = this.chatPanel.getWriteMessagePanel()
        .getEditorPane(); 
        
		if(messagePane.getText() != null && !messagePane.getText().equals("")){
            OperationSetBasicInstantMessaging im = this.chatPanel.getImOperationSet();
            
            Message msg = im.createMessage(messagePane.getText());
            
            this.chatPanel.getChatWindow().getMainFrame()
                .getWaitToBeDeliveredMsgs().put(msg.getMessageUID(), this.chatPanel);
            im.sendInstantMessage(chatPanel.getDefaultContact().getDefaultContact(), msg);
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
    
    public void setTypingStatus(String statusMessage){
        statusLabel.setText(statusMessage);
    }
}
