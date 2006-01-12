package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.User;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.main.utils.Constants;

public class ChatPanel extends JPanel {

	private MessageWindow msgWindow;
	
	private JEditorPane chatEditorPane = new JEditorPane();
	
	public ChatPanel(MessageWindow msgWindow){	
		
		super();
		
		this.msgWindow = msgWindow;
		
		this.chatEditorPane.setEditable(false);		
		
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		//this.add(chatEditorPane);
		
	}
	
	public void paintComponent(Graphics g){	
		
		g.setClip(3, 3, this.getWidth() - 7, this.getHeight() - 5);
		
		super.paintComponent(g);
	}
	
	public void paint(Graphics g){
		
		AntialiasingManager.activateAntialiasing(g);
		
		super.paint(g);
	
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setColor(Constants.MSG_WINDOW_BORDER_COLOR);
		g2.setStroke(new BasicStroke(1.5f));
		
		g2.drawRoundRect(3, 3, this.getWidth() - 7, this.getHeight() - 5, 8, 8);
	
	}
	
	public void showReceivedMessage(){
		
	}
	
	public void showSentMessage(User sender, Calendar calendar, String message){
		
		String messageString = "<HTML><B><FONT COLOR=\"#2e538b\">" + sender.getName() + " at " + calendar.getTime() + 
								"</FONT></B><BR>"+ message + "</HTML>";
		
		JLabel messageLabel = new JLabel(messageString);
		
		this.add(messageLabel);
		
		this.validate();
	}
}
