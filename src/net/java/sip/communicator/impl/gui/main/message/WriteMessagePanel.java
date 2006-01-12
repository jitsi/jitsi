package net.java.sip.communicator.impl.gui.main.message;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.utils.Constants;
import net.java.sip.communicator.impl.gui.main.utils.AntialiasingManager;

public class WriteMessagePanel extends JPanel {

	private JEditorPane editorPane = new JEditorPane();
	
	private MessageWindow msgWindow;
	
	public WriteMessagePanel (MessageWindow msgWindow){		
		
		super(new BorderLayout());		
		
		this.msgWindow = msgWindow;
		
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		this.add(editorPane, BorderLayout.CENTER);		
	}
		
	public void paint(Graphics g){
	
		AntialiasingManager.activateAntialiasing(g);
		
		super.paint(g);
	
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setColor(Constants.MSG_WINDOW_BORDER_COLOR);
		g2.setStroke(new BasicStroke(1.5f));
		
		g2.drawRoundRect(3, 3, this.getWidth() - 4, this.getHeight() - 4, 8, 8);
	
	}

	public JEditorPane getEditorPane() {
		return editorPane;
	}
	
	public void enableKeyboardSending(){
		
		this.editorPane.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e){
				
				if( (e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK && 
							(e.getKeyCode() == KeyEvent.VK_ENTER) ){
										
					msgWindow.getSendPanel().getSendButton().requestFocus();
					
					msgWindow.getSendPanel().getSendButton().doClick();
				}
			}
		});		
	}
}
