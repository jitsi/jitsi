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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.Timer;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import net.java.sip.communicator.impl.gui.main.StatusSelectorBox;
import net.java.sip.communicator.impl.gui.utils.AntialiasingManager;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications;
import net.java.sip.communicator.util.Logger;

public class ChatWritePanel extends JScrollPane
	implements UndoableEditListener, KeyListener {
    
    private Logger logger = Logger.getLogger(ChatWritePanel.class);

	private JEditorPane editorPane = new JEditorPane();
	
	private UndoManager undo = new UndoManager();
	
	private ChatPanel chatPanel;
	
    private TypingTimer typingTimer = new TypingTimer();
    
    private int typingState = -1; 
    
	public ChatWritePanel (ChatPanel chatPanel){		
		
		super();		
		
		this.chatPanel = chatPanel;
		
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
		this.setHorizontalScrollBarPolicy(
		                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		this.editorPane.getDocument().addUndoableEditListener(this);
        this.editorPane.addKeyListener(this);
		
        this.getViewport().add(editorPane, BorderLayout.CENTER);
		
		this.getVerticalScrollBar().setUnitIncrement(30);
	}
		
	public void paint(Graphics g){
	
		AntialiasingManager.activateAntialiasing(g);
		
		super.paint(g);
	
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setColor(Constants.MSG_WINDOW_BORDER_COLOR);
		g2.setStroke(new BasicStroke(1.5f));
		
		g2.drawRoundRect(3, 3, this.getWidth() - 4, 
                this.getHeight() - 4, 8, 8);
	}

	public JEditorPane getEditorPane() {
		return editorPane;
	}
	
	public void undoableEditHappened(UndoableEditEvent e) {
		//Remember the edit		
        this.undo.addEdit(e.getEdit());        
	}
	
	private void undo() {
        try {
            undo.undo();
        } catch (CannotUndoException e) {
            logger.error("Unable to undo.", e);
        }       
    }   

    private void redo() {
        try {
            undo.redo();
        } catch (CannotRedoException e) {
            logger.error("Unable to redo.", e);
        }
    }
        
    public void keyTyped(KeyEvent e){        
        
    }
    
    public void keyPressed(KeyEvent e){
        if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK && 
                (e.getKeyCode() == KeyEvent.VK_ENTER)){
            stopTyping();
        
            JButton sendButton = chatPanel.getSendPanel().getSendButton();
            
            sendButton.requestFocus();
            sendButton.doClick();
        }
        else if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK && 
                (e.getKeyCode() == KeyEvent.VK_Z)){                 
            
            if(undo.canUndo()){
                undo();
            }
        }
        else if ((e.getModifiers() & KeyEvent.CTRL_MASK) == KeyEvent.CTRL_MASK && 
                (e.getKeyCode() == KeyEvent.VK_R)){
            
            if(undo.canRedo()){
                redo();
            }                   
        }
        else{
            if(typingState != OperationSetTypingNotifications.STATE_TYPING){
                typingTimer.setDelay(2*1000);                
                typingState =  OperationSetTypingNotifications.STATE_TYPING;
                chatPanel.getTnOperationSet()
                    .sendTypingNotification(chatPanel.getProtocolContact(), 
                                            typingState);
            }
            
            if(!typingTimer.isRunning()){
                typingTimer.start();
            }
            else{            
                typingTimer.restart();
            }
        }
    }
    
    public void keyReleased(KeyEvent e){
    }
    
    private class TypingTimer extends Timer {
        
        public TypingTimer(){
            //Set the initial delay
            super(2*1000, null);
            
            this.addActionListener(new TimerActionListener());
        }
        
        private class TimerActionListener implements ActionListener {
            public void actionPerformed(ActionEvent e){
                if(getDelay() == 2*1000){
                    chatPanel.getTnOperationSet()
                        .sendTypingNotification(chatPanel.getProtocolContact(), 
                                OperationSetTypingNotifications.STATE_PAUSED);
                    typingState = OperationSetTypingNotifications.STATE_PAUSED;
                    setDelay(3*1000);
                }
                else{
                    stopTyping();
                }
            }
        }
    }    
    
    public void stopTyping(){
        chatPanel.getTnOperationSet()
            .sendTypingNotification(chatPanel.getProtocolContact(), 
                    OperationSetTypingNotifications.STATE_STOPPED);
        typingState = OperationSetTypingNotifications.STATE_STOPPED;                    
        typingTimer.stop();
    }
}
