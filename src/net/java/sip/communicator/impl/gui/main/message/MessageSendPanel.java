package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

public class MessageSendPanel extends JPanel implements ActionListener {

	private JButton sendButton = new JButton(Messages.getString("send"));

	private JPanel statusPanel = new JPanel();

	private JLabel statusLabel = new JLabel();

	private MessageWindow msgWindow;

	public MessageSendPanel(MessageWindow msgWindow) {

		super(new BorderLayout(5, 5));

		this.msgWindow = msgWindow;

		this.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

		this.statusPanel.add(statusLabel);

		this.add(sendButton, BorderLayout.EAST);
		this.add(statusPanel, BorderLayout.CENTER);

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

		// TODO: Send the text to the protocol service.

		// TODO: Receive a notice that message is delivered.

		this.msgWindow.getChatPanel().showSentMessage(
										this.msgWindow.getParentWindow().getUser(),
										Calendar.getInstance(),										
										messagePane.getText());

		messagePane.setText("");

		messagePane.requestFocus();
	}

	public JButton getSendButton() {
		return sendButton;
	}
}
